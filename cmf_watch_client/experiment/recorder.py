"""
Live Experiment Recorder session manager for capturing real-time watch observations.
"""

import asyncio
from datetime import datetime, timezone
import logging
from typing import List, Optional, Tuple

from ..ble.client import CmfWatchClient
from ..protocol.opcodes import CmfOpcode
from .geo import calculate_gps_track_distance
from .models import (
    ActivityType,
    DerivedMetrics,
    ExperimentTrial,
    GroundTruthMethod,
    IntensityLevel,
    RawObservations,
    RawSamplePoint,
    TrialMetadata,
    TrialStatus,
)

logger = logging.getLogger("cmf_watch_client.experiment.recorder")


class ExperimentRecorder:
    """
    Session recorder capturing live GATT notifications during physical trials.
    """

    def __init__(
        self,
        client: CmfWatchClient,
        trial_id: str,
        activity: ActivityType = ActivityType.WALK,
        intensity: IntensityLevel = IntensityLevel.NORMAL,
        ground_truth_method: GroundTruthMethod = GroundTruthMethod.TRACK,
        target_distance_m: Optional[float] = None,
        user_height_cm: Optional[float] = None,
        notes: Optional[str] = None,
    ):
        self.client = client
        self.trial_id = trial_id
        self.activity = activity
        self.intensity = intensity
        self.ground_truth_method = ground_truth_method
        self.target_distance_m = target_distance_m
        self.user_height_cm = user_height_cm
        self.notes = notes

        self.is_recording: bool = False
        self.start_time: Optional[datetime] = None
        self.end_time: Optional[datetime] = None

        self.samples: List[RawSamplePoint] = []
        self.gps_coords: List[Tuple[float, float]] = []

        self._initial_steps: Optional[int] = None
        self._initial_distance: Optional[float] = None
        self._latest_steps: int = 0
        self._latest_distance: float = 0.0
        self._latest_hr: Optional[int] = None
        self._latest_lat: Optional[float] = None
        self._latest_lon: Optional[float] = None

        self._sample_task: Optional[asyncio.Task] = None
        self.status: TrialStatus = TrialStatus.INCOMPLETE

    def start_recording(self) -> None:
        """Begin active trial recording and sampling loop."""
        self.start_time = datetime.now(timezone.utc)
        self.is_recording = True
        self.status = TrialStatus.INCOMPLETE
        self.samples.clear()
        self.gps_coords.clear()

        # Attach custom frame hooks to watch client
        self._sample_task = asyncio.create_task(self._sampling_loop())
        logger.info(f"Started experiment trial recording '{self.trial_id}' at {self.start_time.isoformat()}")

    async def _sampling_loop(self) -> None:
        """Periodic 1Hz sampling loop recording live telemetry snapshot points."""
        while self.is_recording:
            try:
                now = datetime.now(timezone.utc)
                elapsed = (now - self.start_time).total_seconds() if self.start_time else 0.0

                # Capture current telemetry state from watch client buffers
                steps = self.client.raw_activity_bytes  # Updated via notifications
                # Use Bleak client's latest received activity metrics
                current_steps = self._latest_steps
                current_dist = self._latest_distance
                current_hr = self._latest_hr

                sample = RawSamplePoint(
                    timestamp=now,
                    elapsed_seconds=round(elapsed, 2),
                    steps=current_steps,
                    distance_meters=current_dist,
                    heart_rate=current_hr,
                    latitude=self._latest_lat,
                    longitude=self._latest_lon,
                )
                self.samples.append(sample)
                await asyncio.sleep(1.0)
            except asyncio.CancelledError:
                break
            except Exception as e:
                logger.error(f"Error in experiment sampling loop: {e}")
                await asyncio.sleep(1.0)

    def record_activity_update(self, total_steps: int, total_distance_m: float) -> None:
        """Callback for incoming activity interval notifications."""
        if self._initial_steps is None:
            self._initial_steps = total_steps
            self._initial_distance = total_distance_m

        self._latest_steps = max(0, total_steps - (self._initial_steps or 0))
        self._latest_distance = max(0.0, total_distance_m - (self._initial_distance or 0.0))

    def record_hr_update(self, bpm: int) -> None:
        """Callback for incoming heart rate notifications."""
        self._latest_hr = bpm

    def record_gps_update(self, lat: float, lon: float) -> None:
        """Callback for incoming GPS coordinate notifications."""
        self._latest_lat = lat
        self._latest_lon = lon
        self.gps_coords.append((lat, lon))

    def stop_recording(self, ground_truth_distance_m: Optional[float] = None) -> ExperimentTrial:
        """
        Stop active trial recording, compute derived metrics, and return final ExperimentTrial.
        """
        self.is_recording = False
        if self._sample_task and not self._sample_task.done():
            self._sample_task.cancel()

        self.end_time = datetime.now(timezone.utc)
        self.status = TrialStatus.COMPLETED

        duration_sec = (self.end_time - self.start_time).total_seconds() if self.start_time else 0.0
        final_steps = self._latest_steps
        final_distance_m = self._latest_distance

        gps_dist_m = calculate_gps_track_distance(self.gps_coords)
        avg_cadence_spm = (final_steps / (duration_sec / 60.0)) if duration_sec > 0 else 0.0
        mean_stride_m = (final_distance_m / final_steps) if final_steps > 0 else 0.0

        baseline_error_m = None
        baseline_error_pct = None
        if ground_truth_distance_m is not None and ground_truth_distance_m > 0:
            baseline_error_m = final_distance_m - ground_truth_distance_m
            baseline_error_pct = (baseline_error_m / ground_truth_distance_m) * 100.0

        metadata = TrialMetadata(
            experiment_id="experiment_01_distance",
            trial_id=self.trial_id,
            date_time=self.start_time or datetime.now(timezone.utc),
            activity=self.activity,
            intensity=self.intensity,
            ground_truth_method=self.ground_truth_method,
            target_distance_m=self.target_distance_m,
            user_height_cm=self.user_height_cm,
            status=self.status,
            notes=self.notes,
        )

        raw_obs = RawObservations(
            start_time=self.start_time or datetime.now(timezone.utc),
            end_time=self.end_time,
            total_duration_seconds=round(duration_sec, 2),
            watch_final_steps=final_steps,
            watch_final_distance_m=round(final_distance_m, 2),
            samples=self.samples,
        )

        derived = DerivedMetrics(
            ground_truth_distance_m=ground_truth_distance_m,
            watch_reported_distance_m=round(final_distance_m, 2),
            gps_calculated_distance_m=round(gps_dist_m, 2),
            average_cadence_spm=round(avg_cadence_spm, 1),
            calculated_mean_stride_m=round(mean_stride_m, 3),
            baseline_error_m=round(baseline_error_m, 2) if baseline_error_m is not None else None,
            baseline_error_percentage=round(baseline_error_pct, 2) if baseline_error_pct is not None else None,
        )

        logger.info(f"Trial '{self.trial_id}' completed. Duration: {duration_sec:.1f}s, Steps: {final_steps}, Cadence: {avg_cadence_spm:.1f} spm")

        return ExperimentTrial(
            schema_version="1.0",
            metadata=metadata,
            raw_observations=raw_obs,
            derived_metrics=derived,
        )
