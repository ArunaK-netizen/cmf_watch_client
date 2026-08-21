"""
Health data models and payload decoders for Nothing X / CMF Watch telemetry.
"""

from .decoders import (
    decode_activity_data,
    decode_heart_rate,
    decode_resting_heart_rate,
    decode_sleep_data,
    decode_spo2,
    decode_stress,
    decode_workout_gps,
    decode_workout_summary,
)
from .models import (
    ActivitySample,
    HeartRateSample,
    RestingHeartRateSample,
    SleepSession,
    SleepStage,
    SleepStageType,
    SpO2Sample,
    StressCategory,
    StressSample,
    WorkoutGpsPoint,
    WorkoutSummary,
)

__all__ = [
    "ActivitySample",
    "HeartRateSample",
    "RestingHeartRateSample",
    "SleepSession",
    "SleepStage",
    "SleepStageType",
    "SpO2Sample",
    "StressCategory",
    "StressSample",
    "WorkoutGpsPoint",
    "WorkoutSummary",
    "decode_activity_data",
    "decode_heart_rate",
    "decode_resting_heart_rate",
    "decode_sleep_data",
    "decode_spo2",
    "decode_stress",
    "decode_workout_gps",
    "decode_workout_summary",
]
