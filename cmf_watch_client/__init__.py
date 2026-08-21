"""
Production CMF / Nothing Smartwatch Client package.
"""

from .ble.client import CmfWatchClient
from .crypto.cipher import decrypt_aes_cbc, encrypt_aes_cbc
from .crypto.keys import derive_auth_key, derive_session_key
from .exceptions import (
    CmfAuthError,
    CmfConnectionError,
    CmfError,
    CmfProtocolError,
    CmfTimeoutError,
)
from .health.models import (
    ActivitySample,
    HeartRateSample,
    RestingHeartRateSample,
    SleepSession,
    SleepStage,
    SpO2Sample,
    StressSample,
    WorkoutGpsPoint,
    WorkoutSummary,
)
from .protocol.frame import FrameAssembler, build_frames
from .protocol.opcodes import CmfOpcode
from .storage.base import BaseStorageExporter
from .storage.json_exporter import JSONStorageExporter

__version__ = "1.0.0"

__all__ = [
    "CmfWatchClient",
    "CmfOpcode",
    "build_frames",
    "FrameAssembler",
    "encrypt_aes_cbc",
    "decrypt_aes_cbc",
    "derive_auth_key",
    "derive_session_key",
    "ActivitySample",
    "HeartRateSample",
    "RestingHeartRateSample",
    "SleepSession",
    "SleepStage",
    "SpO2Sample",
    "StressSample",
    "WorkoutGpsPoint",
    "WorkoutSummary",
    "BaseStorageExporter",
    "JSONStorageExporter",
    "CmfError",
    "CmfConnectionError",
    "CmfAuthError",
    "CmfProtocolError",
    "CmfTimeoutError",
]
