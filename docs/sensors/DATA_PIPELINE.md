# End-to-End Sensor Data Pipeline & GoMore Integration

This document traces the complete physical sensor data pipeline from hardware transducers through watch firmware, BLE GATT characteristics, Nothing X parsers, Room DB storage, and JNI entry points into `libGoMoreEdgeKit.so`.

---

## 1. Complete Physical Data Pipeline Tracing

```text
[ 3-Axis Accelerometer (IMU) ]          [ Optical PPG (Green 525nm) ]          [ Dual-Band GNSS / GPS ]
             │                                        │                                   │
             │ 100Hz XYZ Signal                       │ 50Hz Photodiode AFE               │ 1Hz NMEA / Satellite
             ▼                                        ▼                                   ▼
┌────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                               Actions ATS3089C Watch MCU Firmware                                       │
│                                                                                                        │
│  - IMU Bandpass Filter (1.0-3.5Hz)     - PPG Motion Artifact Rejection       - GPS Coordinate Filter    │
│  - Peak Step Count Accumulator         - Systolic Peak BPM Detector          - 12B Struct (ts, lon, lat)│
│  - Sleep Actigraphy Classification     - rMSSD HRV & Stress Calculation                                 │
└────────────────────────────────────────────────────────────────────────────────────────────────────────┘
                                                     │
                                                     │ Encrypted BLE Notification Packets (0000fff1)
                                                     ▼
┌────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                      Nothing X Android Host App                                        │
│                                                                                                        │
│  - XBluetoothManager -> XByteArrayParser (0xF5 Frame Assembler & AES-128-CBC Decryptor)                 │
│  - Opcode Dispatcher -> Room SQLite Database (`ntwatch.db`)                                            │
│    - ActivityEntity (0x0056), HeartRateEntity (0x0053), SleepEntity (0x0058), StressEntity (0x009D)    │
└────────────────────────────────────────────────────────────────────────────────────────────────────────┘
                                                     │
                                                     │ Java/Kotlin JNI Wrapper (GMBase / GMRunCoach)
                                                     ▼
┌────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                 GoMore Engine (libGoMoreEdgeKit.so)                                    │
│                                                                                                        │
│  - GMCoachUserInfoInit -> Pass User Profile (Age, Sex, Weight, Height, Max HR, Resting HR)            │
│  - get_phrz           -> Compute Personalized Heart Rate Zones (Karvonen HRR Equation)                 │
│  - get_training_load  -> Calculate Workout TRIMP Load (Workout HR Stream + Active Duration)          │
│  - vo2Max             -> Estimate Aerobic Capacity (GPS Pace vs. Workout HR Linear Regression)         │
│  - staminaLevel       -> Compute Post-Workout Recovery Hours & Remaining Energy                        │
└────────────────────────────────────────────────────────────────────────────────────────────────────────┘
                                                     │
                                                     │ Flutter Pigeon IPC (NtSyncHealthPlugin)
                                                     ▼
                                          [ Flutter UI Dashboard ]
```

---

## 2. Real-Time vs. Historical Data Streaming Mechanisms

The BLE protocol supports two distinct data acquisition modes:

### 2.1 Historical Telemetry Sync Mode
- **Mechanism**: Triggered by sending `ACTIVITY_FETCH_1` (`FFFF 8005`) and `ACTIVITY_FETCH_2` (`FFFF 9057`).
- **Behavior**: The watch transmits continuous bursts of stored binary records (`0x0056`, `0x0053`, `0x00DA`, `0x0055`, `0x009D`, `0x0058`, `0x0057`) accumulated in flash memory since the previous sync session.
- **Use Case**: Offline telemetry synchronization, batch analysis, daily health exporting.

### 2.2 Real-Time Stream Mode
- **Mechanism**: Active when CCCD descriptor (`0x2902`) on characteristic `0000fff1` is set to `01 00`.
- **Behavior**: When an active workout session is initiated on the watch, the watch streams real-time 8-byte Heart Rate records (`0x00E0`) at 1 Hz and 12-byte GPS coordinate records (`0xA05A`) at 1 Hz directly to the GATT client.
- **Use Case**: Live workout dashboards, real-time heart rate monitoring, active GPS tracking.

---

## 3. GoMore Native Data Inputs & Call Graph

Java/Kotlin caller mapping into `libGoMoreEdgeKit.so`:

```text
com.bomdic.gomoreedgekit.GMBase.jniCoachUserInfoInit(user_info)
   └── Inputs: GMCoachUserInfo object
       ├── age (int)
       ├── gender (int: 1=Male, 2=Female)
       ├── height_cm (float)
       ├── weight_kg (float)
       ├── max_hr (int)
       └── rest_hr (int)

com.bomdic.gomoreedgekit.GMRunCoach.jniGetRunCoach(lesson)
   ├── Inputs: Workout HR time-series + GPS Speed time-series + User Profile
   ├── Native Processing:
   │   ├── get_phrz(hr_stream, user_info)       -> HR Zone Distribution
   │   ├── get_training_load(hr_stream, dur)    -> TRIMP Training Load
   │   ├── vo2Max(speed_stream, hr_stream)      -> VO₂ Max Score
   │   └── staminaLevel(training_load)          -> Recovery Hours (0-96h)
   └── Output: GMCoachBound & GMRunPlan results
```
