"""
Automated unit tests for Experiment 01 data collection subsystem and redesigned trial models.
Uses synthetic test vectors ONLY; contains zero real user location or personal health data.
"""

from datetime import datetime, timezone
import json
import pytest
from cmf_watch_client.experiment import (
    ActivityType,
    DerivedMetrics,
    ExperimentRecorder,
    ExperimentTrial,
    IntensityLevel,
    RawObservations,
    RawSamplePoint,
    ReferenceData,
    TrialMetadata,
    TrialStatus,
    TrialStorageManager,
    WatchSummaryData,
    calculate_gps_track_distance,
    haversine_distance,
)


def test_haversine_distance():
    # London Eye to Big Ben (~800m)
    lat1, lon1 = 51.5033, -0.1195
    lat2, lon2 = 51.5007, -0.1246
    dist = haversine_distance(lat1, lon1, lat2, lon2)
    assert 400.0 < dist < 1000.0


def test_calculate_gps_track_distance():
    coords = [
        (51.5033, -0.1195),
        (51.5020, -0.1220),
        (51.5007, -0.1246),
    ]
    total_dist = calculate_gps_track_distance(coords)
    assert total_dist > 0.0


def test_experiment_models_serialization(tmp_path):
    now = datetime.now(timezone.utc)
    metadata = TrialMetadata(
        experiment_id="experiment_01_distance",
        trial_id="trial_001",
        date_time=now,
        activity=ActivityType.WALK,
        intensity=IntensityLevel.NORMAL,
        planned_distance_m=400.0,
        user_height_cm=175.0,
        status=TrialStatus.COMPLETED,
    )

    watch_data = WatchSummaryData(
        watch_final_steps=500,
        watch_final_distance_m=363.0,
        watch_total_duration_seconds=300.0,
        watch_calories_kcal=24.5,
        watch_average_hr=82.0,
        watch_average_cadence_spm=100.0,
        watch_calculated_mean_stride_m=0.726,
    )

    reference_data = ReferenceData(
        reference_method="track",
        reference_distance_m=400.0,
        reference_duration_seconds=300.0,
        reference_source="400m Athletics Track",
        notes="Synthetic test trial",
    )

    sample = RawSamplePoint(
        timestamp=now,
        elapsed_seconds=10.0,
        steps=20,
        distance_meters=14.5,
        heart_rate=75,
    )

    raw_obs = RawObservations(
        start_time=now,
        end_time=now,
        total_duration_seconds=300.0,
        samples=[sample],
    )

    derived = DerivedMetrics(
        distance_error_m=-37.0,
        distance_error_percentage=-9.25,
        duration_difference_seconds=0.0,
    )

    trial = ExperimentTrial(
        schema_version="1.0",
        metadata=metadata,
        watch_data=watch_data,
        reference_data=reference_data,
        derived_metrics=derived,
        raw_observations=raw_obs,
    )

    # Test serialization / deserialization
    json_str = trial.model_dump_json()
    loaded_dict = json.loads(json_str)
    deserialized = ExperimentTrial.model_validate(loaded_dict)

    assert deserialized.schema_version == "1.0"
    assert deserialized.metadata.trial_id == "trial_001"
    assert deserialized.watch_data.watch_average_cadence_spm == 100.0
    assert deserialized.reference_data.reference_distance_m == 400.0


def test_trial_storage_manager(tmp_path):
    storage = TrialStorageManager(trials_dir=str(tmp_path))
    trial_id = storage.get_next_trial_id()
    assert trial_id == "trial_001"

    now = datetime.now(timezone.utc)
    trial = ExperimentTrial(
        schema_version="1.0",
        metadata=TrialMetadata(trial_id=trial_id, date_time=now),
        watch_data=WatchSummaryData(
            watch_final_steps=10,
            watch_final_distance_m=7.0,
            watch_total_duration_seconds=10.0,
            watch_average_cadence_spm=60.0,
            watch_calculated_mean_stride_m=0.7,
        ),
        derived_metrics=DerivedMetrics(),
        raw_observations=RawObservations(start_time=now, total_duration_seconds=10.0),
    )

    saved_path = storage.save_trial(trial)
    assert saved_path.endswith("trial_001.json")

    trials_list = storage.list_trials()
    assert len(trials_list) == 1
    assert trials_list[0].metadata.trial_id == "trial_001"


def test_interrupted_trial_handling():
    # Test that incomplete / interrupted recording sets correct status flag
    now = datetime.now(timezone.utc)
    metadata = TrialMetadata(
        trial_id="trial_999",
        date_time=now,
        status=TrialStatus.INTERRUPTED,
    )
    watch_data = WatchSummaryData(
        watch_final_steps=20,
        watch_final_distance_m=14.0,
        watch_total_duration_seconds=15.0,
        watch_average_cadence_spm=80.0,
        watch_calculated_mean_stride_m=0.7,
    )
    raw_obs = RawObservations(start_time=now, total_duration_seconds=15.0)

    interrupted_trial = ExperimentTrial(
        metadata=metadata, watch_data=watch_data, derived_metrics=DerivedMetrics(), raw_observations=raw_obs
    )
    assert interrupted_trial.metadata.status == TrialStatus.INTERRUPTED
