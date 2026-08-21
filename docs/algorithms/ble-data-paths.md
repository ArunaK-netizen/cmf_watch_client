# End-to-End BLE Data Path Tracing

This document traces every BLE notification packet from initial GATT arrival to final database storage and UI presentation in **Nothing X**.

---

## 1. End-to-End Architecture

```text
  [ BLE GATT Peripheral ]
             │
             │ Notifications on 0000fff1
             ▼
  [ XBluetoothManager ]
             │
             │ Raw byte array pass-through
             ▼
  [ XByteArrayParser ]
             │
             ├────────► Validate 0xF5 11-byte Header
             ├────────► Reassemble MTU chunks
             ├────────► AES-128-CBC Decrypt with sessionKey
             └────────► Verify CRC32 LE Checksum
             │
             ▼
  [ Opcode Dispatcher ] ── (cmd1, cmd2)
             │
   ┌─────────┼──────────────────────────┬──────────────────────────┐
   │ 0x0056  │ 0x0053 / 0x00E0 / 0x00DA │ 0x0058                   │ 0x0057 / 0x0160
   ▼         ▼                          ▼                          ▼
[Activity] [Heart Rate]              [Sleep]                    [Workout]
   │         │                          │                          │
   ▼         ▼                          ▼                          ▼
  [ DeviceDao / Room DB (`ntwatch.db`) ]
   │
   ├──────────────────────────┐
   ▼                          ▼
[ NtSyncHealthPlugin ]   [ libGoMoreEdgeKit.so ]
   │                          │
   │ Flutter Pigeon IPC       │ Training Load / VO₂ Max
   ▼                          ▼
[ Flutter UI Components ] [ Card View Widgets ]
```

---

## 2. Packet Opcode → Database → Metric Mapping

| Opcode `(cmd1, cmd2)` | Class / Parser | Target Room Entity | Derived Metric Output | UI Presentation |
|---|---|---|---|---|
| `0x0056 0x0001` | `ActivityDecoder` | `ActivityEntity` | Steps, Distance, Active Calories | Daily Progress Ring Widget |
| `0x0053 0x0001` | `HeartRateDecoder` | `HeartRateEntity` | Real-time BPM, Daily HR Curve | Heart Rate Card / Chart |
| `0x00DA 0x0001` | `RestingHrDecoder` | `RestingHrEntity` | Resting HR Trend | Health Overview Card |
| `0x0055 0x0001` | `SpO2Decoder` | `SpO2Entity` | Blood Oxygen Level % | SpO₂ Card |
| `0x009D 0x0001` | `StressDecoder` | `StressEntity` | Stress Index (1-99) & Category | Stress Card / Score Gauge |
| `0x0058 0x0001` | `SleepDecoder` | `SleepEntity` & `StageEntity` | Deep/Light/REM/Awake Durations | Sleep Stage Timeline Bar |
| `0x0057 0x0001` | `WorkoutDecoder` | `WorkoutEntity` | Training Load, VO₂ Max, Recovery | Workout Session Summary Detail |
