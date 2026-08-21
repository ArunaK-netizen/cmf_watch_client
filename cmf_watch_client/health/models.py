"""
Pydantic data models for normalized health, activity, and workout telemetry metrics.
"""

from datetime import datetime
from enum import Enum
from typing import List, Optional
from pydantic import BaseModel, Field


class SleepStageType(str, Enum):
    DEEP = "deep"
    LIGHT = "light"
    REM = "rem"
    AWAKE = "awake"
    UNKNOWN = "unknown"

    @classmethod
    def from_code(cls, code: int) -> 'SleepStageType':
        mapping = {
            1: cls.DEEP,
            2: cls.LIGHT,
            3: cls.REM,
            4: cls.AWAKE,
        }
        return mapping.get(code, cls.UNKNOWN)


class StressCategory(str, Enum):
    RELAXED = "relaxed"    # 1-29
    NORMAL = "normal"      # 30-59
    MEDIUM = "medium"      # 60-79
    HIGH = "high"          # 80-99
    UNKNOWN = "unknown"

    @classmethod
    def from_score(cls, score: int) -> 'StressCategory':
        if 1 <= score <= 29:
            return cls.RELAXED
        elif 30 <= score <= 59:
            return cls.NORMAL
        elif 60 <= score <= 79:
            return cls.MEDIUM
        elif 80 <= score <= 99:
            return cls.HIGH
        return cls.UNKNOWN


class ActivitySample(BaseModel):
    """Activity / step telemetry sample."""
    timestamp: datetime = Field(description="Sample UTC timestamp")
    steps: int = Field(ge=0, description="Step count accumulated in interval")
    distance_meters: int = Field(ge=0, description="Distance traveled in meters")
    calories_kcal: float = Field(ge=0.0, description="Calories burned in kilocalories (divided by 1000 from raw cal)")


class HeartRateSample(BaseModel):
    """Heart rate sample (bpm)."""
    timestamp: datetime = Field(description="Sample UTC timestamp")
    bpm: int = Field(ge=0, le=255, description="Heart rate in beats per minute")
    sample_type: str = Field(default="auto", description="Source: auto, manual, workout")


class RestingHeartRateSample(BaseModel):
    """Resting heart rate daily record."""
    timestamp: datetime = Field(description="Sample UTC timestamp")
    bpm: int = Field(ge=0, le=255, description="Resting heart rate in beats per minute")


class SpO2Sample(BaseModel):
    """Blood oxygen saturation level sample (SpO2)."""
    timestamp: datetime = Field(description="Sample UTC timestamp")
    percentage: int = Field(ge=0, le=100, description="Blood oxygen level percentage (0-100%)")


class StressSample(BaseModel):
    """Stress measurement sample."""
    timestamp: datetime = Field(description="Sample UTC timestamp")
    score: int = Field(ge=0, le=100, description="Stress index score (1-99)")
    category: StressCategory = Field(description="Categorized stress level")


class SleepStage(BaseModel):
    """Individual sleep stage segment within a sleep session."""
    timestamp: datetime = Field(description="Stage start UTC timestamp")
    duration_seconds: int = Field(ge=0, description="Stage duration in seconds")
    stage: SleepStageType = Field(description="Sleep stage classification")


class SleepSession(BaseModel):
    """Full sleep session header and parsed stage intervals."""
    start_time: datetime = Field(description="Session start UTC timestamp")
    end_time: datetime = Field(description="Wakeup UTC timestamp")
    total_deep_seconds: int = Field(ge=0, description="Total deep sleep duration in seconds")
    total_light_seconds: int = Field(ge=0, description="Total core/light sleep duration in seconds")
    total_rem_seconds: int = Field(ge=0, description="Total REM sleep duration in seconds")
    total_awake_seconds: int = Field(ge=0, description="Total awake duration in seconds")
    stages: List[SleepStage] = Field(default_factory=list, description="Ordered list of sleep stage records")


class WorkoutGpsPoint(BaseModel):
    """GPS location coordinate sample during a workout session."""
    timestamp: datetime = Field(description="Location UTC timestamp")
    longitude: float = Field(description="Longitude coordinate in decimal degrees")
    latitude: float = Field(description="Latitude coordinate in decimal degrees")


class WorkoutSummary(BaseModel):
    """Workout summary session record."""
    start_time: datetime = Field(description="Workout start UTC timestamp")
    end_time: datetime = Field(description="Workout completion UTC timestamp")
    duration_seconds: int = Field(ge=0, description="Active workout duration in seconds")
    workout_type_code: int = Field(description="Firmware activity type code")
    has_gps: bool = Field(default=False, description="Flag indicating recorded GPS track")
    calories_kcal: Optional[float] = Field(default=None, description="Total calories burned in kcal")
    steps: Optional[int] = Field(default=None, description="Step count recorded during workout")
    distance_meters: Optional[int] = Field(default=None, description="Total distance covered in meters")
    avg_heart_rate: Optional[int] = Field(default=None, description="Average workout heart rate in bpm")
    max_heart_rate: Optional[int] = Field(default=None, description="Maximum workout heart rate in bpm")
