# Data Truth & State Model Specification

This document defines the strict data truth model, state classification, freshness boundaries, and display rules for the **CMF Watch Companion Application**.

---

## 1. Zero-Fabrication Rule

> [!CAUTION]
> **Strict System Constraint**: The application must **NEVER** hardcode, invent, or display static placeholder health values (e.g. 72 BPM, 8,421 steps, 6.3 km, 1,842 kcal, 7h 42m sleep) in production screens.

When no real telemetry has been received from the watch and no real historical data exists in local storage, every metric MUST render as **Unavailable (`"--"`)**.

---

## 2. Telemetry Classification Taxonomy

```text
 ┌─────────────────────────────────────────────────────────────────────────────┐
 │                           1. LIVE TELEMETRY                                 │
 │  - Real-time GATT notifications received during active BLE session          │
 │  - Opcode 0x0053 (HR 1Hz), 0x005C (Battery level %), 0xA05A (GPS coordinates)│
 │  - Displayed immediately with "LIVE" status indicator                       │
 └─────────────────────────────────────────────────────────────────────────────┘

 ┌─────────────────────────────────────────────────────────────────────────────┐
 │                       2. SYNCHRONIZED TELEMETRY                             │
 │  - Historical memory blocks fetched via ACTIVITY_FETCH_1 & 2 (0xFFFF 8005) │
 │  - 0x0056 (Step intervals), 0x0058 (Sleep sessions), 0x009D (Stress)        │
 │  - Persisted directly to Room SQLite Database (`cmf_health.db`)             │
 └─────────────────────────────────────────────────────────────────────────────┘

 ┌─────────────────────────────────────────────────────────────────────────────┐
 │                          3. CACHED / HISTORICAL                             │
 │  - Real telemetry retrieved from local Room DB from previous sessions        │
 │  - Displayed with explicit timestamp ("Synced 2h ago")                     │
 │  - NEVER presented as current live telemetry                                │
 └─────────────────────────────────────────────────────────────────────────────┘

 ┌─────────────────────────────────────────────────────────────────────────────┐
 │                            4. UNAVAILABLE ("--")                            │
 │  - Fresh installation, watch disconnected, or un-synced feature            │
 │  - Value renders strictly as "--" with "No telemetry" badge                  │
 └─────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Explicit Connection States

The application state machine defines 8 explicit connection states:

| Connection State | Definition | UI Status Pill Display |
|---|---|---|
| `DISCONNECTED` | GATT peripheral disconnected or Bluetooth disabled | `DISCONNECTED` (Gray dot) |
| `SCANNING` | Scanning for CMF Watch BLE advertisement | `SCANNING...` (Yellow dot) |
| `CONNECTING` | Establishing GATT connection | `CONNECTING...` (Yellow dot) |
| `CONNECTED` | GATT connection established | `CONNECTED` (Green dot) |
| `AUTHENTICATING` | Executing session handshake (`0xFFFF 8047`) | `AUTHENTICATING...` (Yellow dot) |
| `CONNECTED_PAIRED` | Handshake complete; session key active | `CONNECTED • 86%` (Green dot) |
| `SYNCING` | Active `ACTIVITY_FETCH_1/2` burst in progress | `SYNCING...` (Cyan dot) |
| `ERROR` | BLE connection failure or GATT timeout | `CONNECTION ERROR` (Red dot) |

---

## 4. Origin Audit of Screen Values

| UI Screen | Metric Field | Disconnected Value | Live Value Source | Origin & Verification |
|---|---|:---:|---|---|
| **Home Dashboard** | Connection Pill | `DISCONNECTED` | `CmfBleManager.connectionState` | Live BLE Manager Flow |
| **Home Dashboard** | Heart Rate | `"--"` | `CmfBleManager.heartRateFlow` | Live 0x0053 / 0x00DA GATT decode |
| **Home Dashboard** | Steps | `"--"` | `CmfBleManager.stepFlow` | 0x0056 Activity interval decode |
| **Home Dashboard** | Distance | `"--"` | Calculated from 0x0056 | Step interval distance sum |
| **Home Dashboard** | Active Energy | `"--"` | 0x0056 / 0x0057 | Calorie expenditure sum |
| **Home Dashboard** | Sleep Duration | `"--"` | 0x0058 Sleep session | Finalized sleep session decode |
| **Home Dashboard** | Stress Score | `"--"` | 0x009D Stress stream | Periodic 1–99 stress score decode |
| **Device Screen** | Battery Level | `"--%"` | `CmfBleManager.batteryState` | Encrypted 0x005C query decode |
