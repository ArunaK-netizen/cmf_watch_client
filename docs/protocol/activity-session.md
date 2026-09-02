# Watch Activity Session Lifecycle & Synchronization Specification

This document details the state machine, buffering behavior, and synchronization pipeline for native watch activity/workout sessions on **CMF / Nothing Smartwatches**.

---

## 1. Root Cause Analysis: Why Real-Time Polling Yielded Zero Steps

### 🔴 Problem Identified
In passive monitoring mode, reading `ACTIVITY_DATA` (`0x0056`) without triggering an active workout or telemetry fetch resulted in `watch_steps = 0` and `watch_distance = 0`.

### 🟢 Protocol Reality
1. **On-Device Flash Buffering**: The Actions Semi ATS3089C MCU does **not** stream un-finalized step counters every second over BLE during ambient walking. Steps and distance are accumulated in internal MCU RAM/flash buffer blocks.
2. **Workout Session Finalization**: When a native workout session (e.g., *Outdoor Walk*, *Indoor Walk*, *Outdoor Run*) is started on the watch screen, the MCU switches to continuous workout mode.
3. **Summary & Flush Trigger**: Upon tapping **Stop Workout** on the watch, the MCU finalizes the session, computes total steps, distance, active calories, average heart rate, and creates a `WORKOUT_SUMMARY` (`0x0057` / `0x0160`) record.
4. **Fetch Signal**: Executing `ACTIVITY_FETCH_1` (`FFFF 8005`) and `ACTIVITY_FETCH_2` (`FFFF 9057`) prompts the watch to flush all finalized workout summaries and updated activity intervals over GATT channel `0000fff1`.

---

## 2. Activity Session State Machine

```text
               +----------------------------------+
               |            IDLE STATE            |
               +----------------------------------+
                                │
                                │ User starts workout on watch screen
                                ▼
               +----------------------------------+
               |        ACTIVE WORKOUT MODE       |
               |  (Continuous PPG & GPS Enabled)  |
               +----------------------------------+
                                │
                                │ User walks / runs arbitrary distance
                                │
                                │ User taps "Stop Workout" on watch
                                ▼
               +----------------------------------+
               |      FINALIZED SESSION STATE     |
               |  (Summary 0x0057 / 0x0160 generated)|
               +----------------------------------+
                                │
                                │ Client issues ACTIVITY_FETCH_1 & 2
                                ▼
               +----------------------------------+
               |       TELEMETRY SYNC BURST       |
               | (0x0057, 0x0056, 0x00E0, 0xA05A)  |
               +----------------------------------+
                                │
                                ▼
               +----------------------------------+
               |  DATASET SAVED & PROCESSED       |
               +----------------------------------+
```

---

## 3. Data Available Before vs. After Workout Finalization

| Telemetry Element | Available Live During Workout? | Available Finalized After Workout? | Primary BLE Opcode |
|---|:---:|:---:|---|
| **Real-time Heart Rate (BPM)** | ✅ **YES** (1 Hz) | ✅ **YES** | `HEART_RATE_WORKOUT` (`0x00E0`) |
| **GPS Track Coordinates** | ✅ **YES** (1 Hz) | ✅ **YES** | `WORKOUT_GPS` (`0xA05A`) |
| **Final Accumulated Steps** | ❌ **NO** (Buffered) | ✅ **YES** | `WORKOUT_SUMMARY` / `ACTIVITY_DATA` |
| **Final Workout Distance (m)** | ❌ **NO** (Buffered) | ✅ **YES** | `WORKOUT_SUMMARY` (`0x0057` / `0x0160`) |
| **Workout Active Duration** | ❌ **NO** | ✅ **YES** | `WORKOUT_SUMMARY` (`0x0057` / `0x0160`) |
| **Calories Burned (kcal)** | ❌ **NO** | ✅ **YES** | `WORKOUT_SUMMARY` (`0x0057` / `0x0160`) |
| **Average Heart Rate** | ❌ **NO** | ✅ **YES** | `WORKOUT_SUMMARY` (`0x0057` / `0x0160`) |
