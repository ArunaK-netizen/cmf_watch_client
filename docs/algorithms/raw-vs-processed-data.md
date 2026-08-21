# Raw Sensor Data vs Pre-Processed Watch Data Analysis

Critical architectural question: **Does the CMF Watch transmit raw sensor wave signals (raw 50Hz PPG photodiode arrays / 100Hz IMU XYZ samples), or does the watch MCU execute local signal processing and transmit pre-calculated metrics to Nothing X?**

---

## 1. Finding & Technical Proof

### 🟢 CONFIRMED FINDING: The Watch MCU performs primary signal processing.

The CMF Smartwatch does **NOT** stream continuous raw optical PPG waveforms or raw 3-axis accelerometer arrays over BLE to Nothing X.

Instead, the **Actions Semi ATS3089C MCU firmware** runs embedded DSP algorithms on-device to filter raw optical and motion sensor signals, and transmits **pre-processed, discrete measurement records** over GATT characteristic `0000fff1-0000-1000-8000-00805f9b34fb`.

---

## 2. Evidence & Payload Structure Breakdown

### 2.1 Heart Rate Stream (`0x0053` / `0x00E0`)
- **Transmitted Data**: 8-byte discrete records `(epoch_seconds: uint32_le, bpm: uint32_le)`.
- **Pre-processing On Watch**: Optical PPG filtering, motion artifact rejection, peak detection, and BPM windowing are performed directly inside ATS3089C firmware.
- **App Role**: Stores BPM time-series data into Room DB (`ntwatch.db`) and passes values to GoMore for HR zone classification.

### 2.2 Activity & Step Stream (`0x0056`)
- **Transmitted Data**: 32-byte interval records `(epoch_seconds: uint32_le, steps: uint32_le, distance_m: uint32_le, calories_cal: uint32_le)`.
- **Pre-processing On Watch**: Accelerometer peak detection, step counting, stride length estimation, and active calorie accumulation occur on the watch.

### 2.3 Sleep Data Stream (`0x0058`)
- **Transmitted Data**: 18-byte session header + 8-byte stage records `(stage_start: uint32_le, duration_s: uint16_le, stage_code: uint16_le)`.
- **Pre-processing On Watch**: The watch firmware calculates sleep start time, wakeup time, and classifies stages (1=Deep, 2=Light, 3=REM, 4=Awake) **before** sending BLE notifications.

### 2.4 Stress Stream (`0x009D`)
- **Transmitted Data**: 8-byte discrete records `(epoch_seconds: uint32_le, stress_score: uint32_le)`.
- **Pre-processing On Watch**: Inter-beat interval (IBI) / HRV calculation and stress index scoring (1–99) are computed by the watch MCU.

---

## 3. Local Host Processing Responsibilities

While basic telemetry metrics arrive pre-processed from the watch, **Nothing X & GoMoreEdgeKit execute secondary processing**:

1. **Personalized HR Zones**: `get_phrz` in `libGoMoreEdgeKit.so` calculates custom HR zone boundaries from user age, max HR, and resting HR.
2. **Exercise Load & VO₂ Max**: `get_training_load` and `vo2Max` in `libGoMoreEdgeKit.so` combine watch workout summaries with GPS pace and user biometric profiles.
3. **Recovery Time & Stamina**: `staminaLevel` computes post-workout recovery hours based on training load.
