"""
Pydantic v2 typed models for Experiment 01 (Distance & Cadence Data Collection).
Enforces schema versioning, strict typing, and separation of raw observations from derived metrics.
"""

from datetime import datetime
from enum import Enum
from typing import List, Optional
from pydantic import BaseModel, Field


class GroundTruthMethod(str, Enum):
    TRACK = "track"
    SURVEY_WHEEL = "survey_wheel"
    MEASURED_ROUTE = "measured_route"
    OTHER = "other"


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
    """Single raw time-stamped observation captured during trial execution."""
    timestamp: datetime = Field(description="Sample UTC timestamp")
    elapsed_seconds: float = Field(ge=0.0, description="Elapsed time in seconds since trial start")
    steps: int = Field(ge=0, description="Accumulated step count from watch")
    distance_meters: float = Field(ge=0.0, description="Accumulated distance in meters reported by watch")
    heart_rate: Optional[int] = Field(default=None, ge=0, le=255, description="Current heart rate (bpm)")
    latitude: Optional[float] = Field(default=None, description="GPS Latitude coordinate in decimal degrees")
    longitude: Optional[float] = Field(default=None, description="GPS Longitude coordinate in decimal degrees")


class TrialMetadata(BaseModel):
    """Metadata describing trial conditions, user profile, and ground-truth measurement method."""
    experiment_id: str = Field(default="experiment_01_distance", description="Parent experiment ID")
    trial_id: str = Field(description="Unique trial identifier (e.g. trial_001)")
    date_time: datetime = Field(description="Trial execution start timestamp")
    activity: ActivityType = Field(default=ActivityType.WALK, description="Physical activity type")
    intensity: IntensityLevel = Field(default=IntensityLevel.NORMAL, description="Self-reported pace intensity")
    ground_truth_method: GroundTruthMethod = Field(
        default=GroundTruthMethod.TRACK, description="Method used for ground-truth measurement"
    )
    target_distance_m: Optional[float] = Field(default=None, ge=0.0, description="Target trial distance in meters")
    user_height_cm: Optional[float] = Field(default=None, ge=50.0, le=250.0, description="User height in cm")
    status: TrialStatus = Field(default=TrialStatus.COMPLETED, description="Completion status of trial")
    notes: Optional[str] = Field(default=None, description="Free-form observational notes")


class RawObservations(BaseModel):
    """Container for un-altered raw physical sensor readings and GATT notifications."""
    start_time: datetime = Field(description="Trial start UTC timestamp")
    end_time: Optional[datetime] = Field(default=None, description="Trial end UTC timestamp")
    total_duration_seconds: float = Field(ge=0.0, description="Active trial duration in seconds")
    watch_final_steps: int = Field(ge=0, description="Final step count accumulated during trial")
    watch_final_distance_m: float = Field(ge=0.0, description="Final distance in meters reported by watch")
    samples: List[RawSamplePoint] = Field(default_factory=list, description="Ordered time-series sample stream")


class DerivedMetrics(BaseModel):
    """Derived analysis parameters and benchmark error metrics (separated from raw data)."""
    ground_truth_distance_m: Optional[float] = Field(default=None, description="Physical measured ground truth distance")
    watch_reported_distance_m: float = Field(ge=0.0, description="Total distance reported by watch baseline")
    gps_calculated_distance_m: float = Field(default=0.0, ge=0.0, description="Cumulative distance from GPS track")
    average_cadence_spm: float = Field(ge=0.0, description="Average step cadence in steps per minute (spm)")
    calculated_mean_stride_m: float = Field(ge=0.0, description="Computed mean stride length in meters")
    baseline_error_m: Optional[float] = Field(default=None, description="Absolute error: watch_distance - ground_truth")
    baseline_error_percentage: Optional[float] = Field(
        default=None, description="Percentage error: (watch_distance - ground_truth) / ground_truth * 100"
    )


class ExperimentTrial(BaseModel):
    """Complete, versioned, reproducible experiment trial data document."""
    schema_version: str = Field(default="1.0", description="Data schema version specification")
    metadata: TrialMetadata
    raw_observations: RawObservations
    derived_metrics: DerivedMetrics
