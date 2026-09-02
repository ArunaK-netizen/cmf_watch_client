"""
Pydantic v2 typed models for Experiment 01 (Distance & Cadence Data Collection).
Enforces schema versioning (1.0), strict typing, and clear separation between:
1. System Under Test (Watch) metrics
2. Independent Reference measurements (Strava, Track, Survey Wheel)
3. Derived error metrics
"""

from datetime import datetime
from enum import Enum
from typing import List, Optional
from pydantic import BaseModel, Field


class ActivityType(str, Enum):
    WALK = "walk"
    JOG = "jog"
    RUN = "run"
    OTHER = "other"


class IntensityLevel(str, Enum):
    SLOW = "slow"
    NORMAL = "normal"
    FAST = "fast"
    OTHER = "other"


class TrialStatus(str, Enum):
    COMPLETED = "completed"
    INCOMPLETE = "incomplete"
    INTERRUPTED = "interrupted"


class RawSamplePoint(BaseModel):
    """Single raw time-stamped observation point captured during active session."""
    timestamp: datetime = Field(description="Sample UTC timestamp")
    elapsed_seconds: float = Field(ge=0.0, description="Elapsed seconds since session start")
    steps: int = Field(ge=0, description="Accumulated step count")
    distance_meters: float = Field(ge=0.0, description="Accumulated watch distance in meters")
    heart_rate: Optional[int] = Field(default=None, ge=0, le=255, description="Heart rate in bpm")
    latitude: Optional[float] = Field(default=None, description="GPS Latitude coordinate")
    longitude: Optional[float] = Field(default=None, description="GPS Longitude coordinate")


class RawObservations(BaseModel):
    """Container for un-altered raw physical sensor readings and GATT notifications."""
    start_time: datetime = Field(description="Trial start UTC timestamp")
    end_time: Optional[datetime] = Field(default=None, description="Trial end UTC timestamp")
    total_duration_seconds: float = Field(ge=0.0, description="Active trial duration in seconds")
    samples: List[RawSamplePoint] = Field(default_factory=list, description="Ordered time-series sample stream")


class TrialMetadata(BaseModel):
    """Metadata describing trial conditions, activity type, and user height parameters."""
    experiment_id: str = Field(default="experiment_01_distance", description="Parent experiment ID")
    trial_id: str = Field(description="Unique trial identifier (e.g. trial_001)")
    date_time: datetime = Field(description="Trial execution start timestamp")
    activity: ActivityType = Field(default=ActivityType.WALK, description="Physical activity type")
    intensity: IntensityLevel = Field(default=IntensityLevel.NORMAL, description="Self-reported pace intensity")
    planned_distance_m: Optional[float] = Field(default=None, ge=0.0, description="Optional target distance parameter")
    user_height_cm: Optional[float] = Field(default=None, ge=50.0, le=250.0, description="User height in cm")
    status: TrialStatus = Field(default=TrialStatus.COMPLETED, description="Completion status of trial")


class WatchSummaryData(BaseModel):
    """Finalized metrics reported directly by the CMF Watch (System Under Test)."""
    watch_final_steps: int = Field(ge=0, description="Final step count reported by watch")
    watch_final_distance_m: float = Field(ge=0.0, description="Final distance in meters reported by watch")
    watch_total_duration_seconds: float = Field(ge=0.0, description="Total active workout duration in seconds")
    watch_calories_kcal: Optional[float] = Field(default=None, ge=0.0, description="Active calories reported by watch")
    watch_average_hr: Optional[float] = Field(default=None, ge=0.0, le=255.0, description="Average workout heart rate")
    watch_average_cadence_spm: float = Field(ge=0.0, description="Average step cadence in spm")
    watch_calculated_mean_stride_m: float = Field(ge=0.0, description="Watch calculated mean stride length in meters")


class ReferenceData(BaseModel):
    """Independent reference measurements (Strava, Measured Track, Surveyor Wheel, etc.)."""
    reference_method: str = Field(description="Reference source (e.g. 'strava', 'track', 'survey_wheel')")
    reference_distance_m: float = Field(ge=0.0, description="Independent physical distance in meters")
    reference_duration_seconds: Optional[float] = Field(default=None, ge=0.0, description="Independent duration in seconds")
    reference_source: Optional[str] = Field(default=None, description="Reference device/app (e.g. 'Garmin Forerunner', 'Strava')")
    notes: Optional[str] = Field(default=None, description="Free-form observational notes")


class DerivedMetrics(BaseModel):
    """Computed error metrics evaluating Watch vs. Independent Reference."""
    distance_error_m: Optional[float] = Field(default=None, description="Watch distance - Reference distance (meters)")
    distance_error_percentage: Optional[float] = Field(
        default=None, description="Percentage error: (Watch - Ref) / Ref * 100"
    )
    duration_difference_seconds: Optional[float] = Field(
        default=None, description="Watch duration - Reference duration (seconds)"
    )


class ExperimentTrial(BaseModel):
    """Complete, versioned, reproducible experiment trial document."""
    schema_version: str = Field(default="1.0", description="Data schema version specification")
    metadata: TrialMetadata
    watch_data: WatchSummaryData
    reference_data: Optional[ReferenceData] = Field(default=None)
    derived_metrics: DerivedMetrics
    raw_observations: RawObservations
