"""
Experiment recording and dataset collection subsystem for Experiment 01 (Distance & Cadence).
"""

from .geo import calculate_gps_track_distance, haversine_distance
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
from .recorder import ExperimentRecorder
from .storage import TrialStorageManager

__all__ = [
    "ExperimentTrial",
    "TrialMetadata",
    "WatchSummaryData",
    "ReferenceData",
    "RawObservations",
    "RawSamplePoint",
    "DerivedMetrics",
    "ActivityType",
    "IntensityLevel",
    "TrialStatus",
    "ExperimentRecorder",
    "TrialStorageManager",
    "haversine_distance",
    "calculate_gps_track_distance",
]
