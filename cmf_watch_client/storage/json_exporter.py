"""
JSON file storage exporter for CMF Watch telemetry.
"""

import json
import logging
from typing import Any, Dict, Optional
from pydantic import BaseModel

from .base import BaseStorageExporter

logger = logging.getLogger("cmf_watch_client.storage.json")


class JSONStorageExporter(BaseStorageExporter):
    """Exports normalized health telemetry data to JSON formatted files."""

    def __init__(self, filepath: str = "health_sync_latest.json", indent: int = 2):
        self.filepath = filepath
        self.indent = indent

    def export(self, telemetry: Dict[str, Any], metadata: Dict[str, Any]) -> str:
        """
        Serialize telemetry data dictionary and metadata to JSON file.

        Args:
            telemetry: Dict of normalized metric Pydantic objects or dicts.
            metadata: Device and session metadata dictionary.

        Returns:
            Absolute or relative path to saved JSON file.
        """
        serialized_metrics = {}
        for metric_name, sample_list in telemetry.items():
            serialized_metrics[metric_name] = [
                item.model_dump(mode="json") if isinstance(item, BaseModel) else item
                for item in sample_list
            ]

        export_structure = {
            "metadata": metadata,
            "metrics": serialized_metrics,
        }

        with open(self.filepath, "w", encoding="utf-8") as f:
            json.dump(export_structure, f, indent=self.indent, ensure_ascii=False)

        logger.info(f"Successfully exported telemetry data to '{self.filepath}'")
        return self.filepath
