"""
Example 04: Implementing a Custom Storage Exporter.
"""

from typing import Any, Dict
from cmf_watch_client import BaseStorageExporter, ActivitySample


class ConsoleSummaryExporter(BaseStorageExporter):
    """Custom storage exporter printing telemetry summaries directly to console."""

    def export(self, telemetry: Dict[str, Any], metadata: Dict[str, Any]) -> str:
        print("\n================ TELEMETRY SUMMARY ================")
        print(f"Device MAC:   {metadata.get('mac_address')}")
        print(f"Synced At:    {metadata.get('synced_at')}")

        activities = telemetry.get("activity", [])
        print(f"\n[Activity Intervals] Total Count: {len(activities)}")
        for act in activities[:5]:
            if isinstance(act, ActivitySample):
                print(f"  - {act.timestamp}: {act.steps} steps, {act.distance_meters}m, {act.calories_kcal} kcal")

        heart_rates = telemetry.get("heart_rate", [])
        print(f"\n[Heart Rate Samples] Total Count: {len(heart_rates)}")

        sleep_sessions = telemetry.get("sleep", [])
        print(f"\n[Sleep Sessions] Total Count: {len(sleep_sessions)}")
        print("====================================================")
        return "ConsoleSummaryOK"


if __name__ == "__main__":
    exporter = ConsoleSummaryExporter()
    mock_telemetry = {
        "activity": [
            ActivitySample(timestamp="2026-08-21T12:00:00Z", steps=1200, distance_meters=900, calories_kcal=45.0)
        ],
        "heart_rate": [],
        "sleep": [],
    }
    mock_metadata = {"mac_address": "00:11:22:33:44:55", "synced_at": "2026-08-21T12:00:00Z"}
    exporter.export(mock_telemetry, mock_metadata)
