"""
Experiment recording and dataset collection subsystem for Experiment 01 (Distance & Cadence).
"""

from .geo import calculate_gps_track_distance, haversine_distance
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
from .recorder import ExperimentRecorder
from .storage import TrialStorageManager

__all__ = [
    "ExperimentTrial",
    "TrialMetadata",
    "RawObservations",
    "RawSamplePoint",
    "DerivedMetrics",
    "ActivityType",
    "IntensityLevel",
    "GroundTruthMethod",
    "TrialStatus",
    "ExperimentRecorder",
    "TrialStorageManager",
    "haversine_distance",
    "calculate_gps_track_distance",
]
