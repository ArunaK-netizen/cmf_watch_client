"""
Live Experiment Recorder session manager for capturing native watch activity sessions.
"""

import asyncio
from datetime import datetime, timezone
import logging
from typing import Dict, List, Optional, Tuple

from ..ble.client import CmfWatchClient
from .geo import calculate_gps_track_distance
from .models import (
    ActivityType,
    DerivedMetrics,
    ExperimentTrial,
    IntensityLevel,
    RawObservations,
    RawSamplePoint,
    ReferenceData,
    TrialMetadata,
    TrialStatus,
    WatchSummaryData,
)

logger = logging.getLogger("cmf_watch_client.experiment.recorder")


class ExperimentRecorder:
    """
    Session recorder managing native watch activity trial lifecycle and telemetry processing.
    """

    def __init__(
        self,
        client: CmfWatchClient,
        trial_id: str,
        activity: ActivityType = ActivityType.WALK,
        intensity: IntensityLevel = IntensityLevel.NORMAL,
        planned_distance_m: Optional[float] = None,
        user_height_cm: Optional[float] = None,
    ):
        self.client = client
        self.trial_id = trial_id
        self.activity = activity
        self.intensity = intensity
        self.planned_distance_m = planned_distance_m
        self.user_height_cm = user_height_cm

        self.is_recording: bool = False
        self.start_time: Optional[datetime] = None
        self.end_time: Optional[datetime] = None

        self.samples: List[RawSamplePoint] = []
        self.gps_coords: List[Tuple[float, float]] = []

        self._sample_task: Optional[asyncio.Task] = None
        self.status: TrialStatus = TrialStatus.INCOMPLETE

    def start_recording(self) -> None:
        """Begin active trial session recording."""
        self.start_time = datetime.now(timezone.utc)
        self.is_recording = True
        self.status = TrialStatus.INCOMPLETE
        self.samples.clear()
        self.gps_coords.clear()

        self._sample_task = asyncio.create_task(self._sampling_loop())
        logger.info(f"Started experiment trial session '{self.trial_id}' at {self.start_time.isoformat()}")

    async def _sampling_loop(self) -> None:
        """Periodic 1Hz sampling loop recording live telemetry snapshot points."""
        while self.is_recording:
            try:
                now = datetime.now(timezone.utc)
                elapsed = (now - self.start_time).total_seconds() if self.start_time else 0.0

                sample = RawSamplePoint(
                    timestamp=now,
                    elapsed_seconds=round(elapsed, 2),
                    steps=0,
                    distance_meters=0.0,
                    heart_rate=None,
                    latitude=None,
                    longitude=None,
                )
                self.samples.append(sample)
                await asyncio.sleep(1.0)
            except asyncio.CancelledError:
                break
            except Exception as e:
                logger.error(f"Error in experiment sampling loop: {e}")
                await asyncio.sleep(1.0)

    def stop_recording(self) -> None:
        """Stop active trial sampling loop."""
        self.is_recording = False
        if self._sample_task and not self._sample_task.done():
            self._sample_task.cancel()
        self.end_time = datetime.now(timezone.utc)

    def finalize_trial(
        self,
        telemetry: Dict,
        reference_data: Optional[ReferenceData] = None,
    ) -> ExperimentTrial:
        """
        Process synced telemetry stream records, extract finalized watch activity summary,
        compute baseline error metrics against independent reference, and build ExperimentTrial.
        """
        self.stop_recording()
        self.status = TrialStatus.COMPLETED

        total_duration_seconds = (
            (self.end_time - self.start_time).total_seconds() if (self.start_time and self.end_time) else 0.0
        )

        # Extract workout summary or activity interval totals from telemetry
        watch_steps = 0
        watch_distance_m = 0.0
        watch_duration_s = total_duration_seconds
        watch_calories = None
        watch_avg_hr = None

        if telemetry.get("workout_summary"):
            ws = telemetry["workout_summary"][-1]
            watch_steps = ws.steps
            watch_distance_m = ws.distance_m
            watch_duration_s = float(ws.duration_sec) if ws.duration_sec > 0 else total_duration_seconds
            watch_calories = float(ws.calories_kcal) if ws.calories_kcal > 0 else None
            watch_avg_hr = float(ws.avg_hr) if ws.avg_hr > 0 else None
        elif telemetry.get("activity"):
            # Sum un-synced activity intervals
            watch_steps = sum(act.steps for act in telemetry["activity"])
            watch_distance_m = sum(act.distance_m for act in telemetry["activity"])
            watch_calories = sum(act.calories_kcal for act in telemetry["activity"])

        # Compute heart rate average from heart_rate samples if available
        if watch_avg_hr is None and telemetry.get("heart_rate"):
            hr_vals = [h.bpm for h in telemetry["heart_rate"] if h.bpm > 0]
            if hr_vals:
                watch_avg_hr = sum(hr_vals) / len(hr_vals)

        # Calculate cadence and stride length
        dur_minutes = watch_duration_s / 60.0 if watch_duration_s > 0 else 0.0
        avg_cadence_spm = (watch_steps / dur_minutes) if dur_minutes > 0 else 0.0
        mean_stride_m = (watch_distance_m / watch_steps) if watch_steps > 0 else 0.0

        watch_summary = WatchSummaryData(
            watch_final_steps=watch_steps,
            watch_final_distance_m=round(watch_distance_m, 2),
            watch_total_duration_seconds=round(watch_duration_s, 2),
            watch_calories_kcal=round(watch_calories, 1) if watch_calories is not None else None,
            watch_average_hr=round(watch_avg_hr, 1) if watch_avg_hr is not None else None,
            watch_average_cadence_spm=round(avg_cadence_spm, 1),
            watch_calculated_mean_stride_m=round(mean_stride_m, 3),
        )

        # Derived error metrics
        dist_error_m = None
        dist_error_pct = None
        dur_diff_s = None

        if reference_data and reference_data.reference_distance_m > 0:
            ref_dist = reference_data.reference_distance_m
            dist_error_m = watch_distance_m - ref_dist
            dist_error_pct = (dist_error_m / ref_dist) * 100.0

            if reference_data.reference_duration_seconds:
                dur_diff_s = watch_duration_s - reference_data.reference_duration_seconds

        derived = DerivedMetrics(
            distance_error_m=round(dist_error_m, 2) if dist_error_m is not None else None,
            distance_error_percentage=round(dist_error_pct, 2) if dist_error_pct is not None else None,
            duration_difference_seconds=round(dur_diff_s, 2) if dur_diff_s is not None else None,
        )

        metadata = TrialMetadata(
            experiment_id="experiment_01_distance",
            trial_id=self.trial_id,
            date_time=self.start_time or datetime.now(timezone.utc),
            activity=self.activity,
            intensity=self.intensity,
            planned_distance_m=self.planned_distance_m,
            user_height_cm=self.user_height_cm,
            status=self.status,
        )

        raw_obs = RawObservations(
            start_time=self.start_time or datetime.now(timezone.utc),
            end_time=self.end_time,
            total_duration_seconds=round(total_duration_seconds, 2),
            samples=self.samples,
        )

        return ExperimentTrial(
            schema_version="1.0",
            metadata=metadata,
            watch_data=watch_summary,
            reference_data=reference_data,
            derived_metrics=derived,
            raw_observations=raw_obs,
        )
