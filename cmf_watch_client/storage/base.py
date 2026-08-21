"""
Abstract base class for CMF Watch telemetry storage exporters.
"""

from abc import ABC, abstractmethod
from typing import Any, Dict


class BaseStorageExporter(ABC):
    """
    Abstract interface for persisting or exporting CMF Watch telemetry data.
    Implementations can export to JSON files, SQLite/PostgreSQL databases,
    REST API endpoints, or message queues.
    """

    @abstractmethod
    def export(self, telemetry: Dict[str, Any], metadata: Dict[str, Any]) -> Any:
        """
        Export normalized health telemetry.

        Args:
            telemetry: Dictionary of metric lists (activity, heart_rate, spo2, stress, sleep, workout).
            metadata: Metadata map (mac_address, device_name, battery_level, synced_at).

        Returns:
            Exporter-specific result (e.g. filepath, row count, response object).
        """
        pass
