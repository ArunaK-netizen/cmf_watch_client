"""
BLE Client and GATT Constants for CMF / Nothing Smartwatch communication.
"""

from .client import CmfWatchClient
from .constants import (
    DEFAULT_MTU,
    UUID_CHAR_CMD_NOTIFY,
    UUID_CHAR_CMD_WRITE,
    UUID_CHAR_DATA_NOTIFY,
    UUID_CHAR_DATA_WRITE,
    UUID_CHAR_SHELL_NOTIFY,
    UUID_CHAR_SHELL_WRITE,
    UUID_SERVICE_CMD,
    UUID_SERVICE_DATA,
    UUID_SERVICE_SHELL,
)

__all__ = [
    "CmfWatchClient",
    "UUID_SERVICE_CMD",
    "UUID_CHAR_CMD_NOTIFY",
    "UUID_CHAR_CMD_WRITE",
    "UUID_SERVICE_SHELL",
    "UUID_CHAR_SHELL_NOTIFY",
    "UUID_CHAR_SHELL_WRITE",
    "UUID_SERVICE_DATA",
    "UUID_CHAR_DATA_NOTIFY",
    "UUID_CHAR_DATA_WRITE",
    "DEFAULT_MTU",
]
