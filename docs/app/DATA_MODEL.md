# CMF Watch Companion Data Model & Database Specification

This document details the local Room SQLite Database entities, MongoDB cloud schemas, domain model mappings, and the strict separation between **Raw Protocol Data** vs **Derived Analytics**.

---

## 1. Raw Protocol Data vs. Derived Analytics Boundary

To ensure complete data integrity, our architecture maintains a strict boundary:

```text
 ┌─────────────────────────────────────────────────────────────────────────────┐
 │                           RAW PROTOCOL OBSERVATIONS                         │
 │  - Exact byte decodings received from GATT notifications (0000fff1)        │
 │  - Unmodified step counts, raw HR timestamps, SpO2 %, sleep epoch stages   │
 │  - Stored permanently in Room DB tables (`heart_rate_raw`, `sleep_raw`)    │
 └─────────────────────────────────────────────────────────────────────────────┘
                                       │
                                       ▼ (Processed by Algorithm Engines)
 ┌─────────────────────────────────────────────────────────────────────────────┐
 │                              DERIVED ANALYTICS                              │
 │  - Computed cadence-adaptive stride length and distance                     │
 │  - Individualized 14-day rolling z-score stress index                       │
 │  - Banister TRIMP workload and Acute-to-Chronic Workload Ratio (ACWR)        │
 │  - HMM-smoothed sleep stage architecture & nocturnal recovery score          │
 └─────────────────────────────────────────────────────────────────────────────┘
```

> [!IMPORTANT]
> Raw protocol observation records are **NEVER** overwritten or modified by derived algorithm calculations.

---

## 2. Room SQLite Database Schema (`cmf_health.db`)

### 2.1 `heart_rate_raw`
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | INTEGER | PRIMARY KEY AUTOINCREMENT | Unique record ID |
| `timestamp` | INTEGER | NOT NULL, INDEX | Epoch timestamp in milliseconds |
| `bpm` | INTEGER | NOT NULL | Heart rate in beats per minute (1–255) |
| `source_opcode` | TEXT | NOT NULL | BLE Opcode (`0x0053`, `0x00E0`, `0x00DA`) |

### 2.2 `activity_interval_raw`
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | INTEGER | PRIMARY KEY AUTOINCREMENT | Unique record ID |
| `timestamp` | INTEGER | NOT NULL, INDEX | Interval start timestamp in milliseconds |
| `steps` | INTEGER | NOT NULL | Accumulated step count in 1-min interval |
| `distance_m` | REAL | NOT NULL | Watch-calculated distance in meters |
| `calories_kcal` | REAL | NOT NULL | Active calories burned in interval |

### 2.3 `sleep_session_raw`
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | INTEGER | PRIMARY KEY AUTOINCREMENT | Unique record ID |
| `start_time` | INTEGER | NOT NULL | Sleep start timestamp |
| `end_time` | INTEGER | NOT NULL | Sleep end timestamp |
| `stage_epochs_json` | TEXT | NOT NULL | JSON string array of `(stage_code, start_time, duration)` |

---

## 3. MongoDB Cloud JSON Document Schema

When syncing with the cloud API backend, records are structured as versioned document collections:

```json
{
  "_id": "66d63d0b2f1a8c9e4120",
  "user_id": "usr_99182",
  "device_mac": "3C:B0:ED:3F:BA:70",
  "schema_version": "1.0",
  "synced_at": "2026-09-02T21:30:00Z",
  "raw_telemetry": {
    "heart_rate_samples": [
      {"ts": 1787333684000, "bpm": 72, "opcode": "0x0053"}
    ],
    "activity_intervals": [
      {"ts": 1787333684000, "steps": 85, "dist_m": 61.2, "kcal": 4.1}
    ]
  },
  "derived_analytics": {
    "cadence_adaptive_distance_m": 64.8,
    "z_score_normalized_stress": 24,
    "trimp_workout_load": 42.5
  }
}
```
