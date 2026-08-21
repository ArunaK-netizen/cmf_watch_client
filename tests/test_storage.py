"""
Unit tests for storage exporters.
"""

import json
import os
import pytest
from cmf_watch_client.storage import JSONStorageExporter


def test_json_storage_exporter(tmp_path):
    out_file = str(tmp_path / "test_telemetry.json")
    exporter = JSONStorageExporter(filepath=out_file)

    telemetry = {
        "activity": [{"timestamp": "2026-08-21T12:00:00Z", "steps": 500, "distance_meters": 350, "calories_kcal": 15.0}],
        "heart_rate": [],
    }
    metadata = {
        "mac_address": "00:11:22:33:44:55",
        "device_name": "CMF Watch Pro 2",
        "battery": {"level": 85, "charging": False},
    }

    result_path = exporter.export(telemetry, metadata)
    assert os.path.exists(result_path)

    with open(result_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    assert data["metadata"]["mac_address"] == "00:11:22:33:44:55"
    assert len(data["metrics"]["activity"]) == 1
    assert data["metrics"]["activity"][0]["steps"] == 500
