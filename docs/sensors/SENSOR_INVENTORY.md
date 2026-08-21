# CMF Smartwatch Physical Sensor Inventory & Accessibility Matrix

This document provides a comprehensive inventory of all physical sensors on the **CMF Watch Pro** and **CMF Watch Pro 2**, evaluating physical presence, BLE protocol accessibility, signal fidelity (Raw vs Pre-processed), sampling rates, packet formats, and evidence confidence levels based on static APK decompilation (Nothing X 3.7.3), firmware reverse-engineering, and live BLE notification captures.

---

## 1. Primary Sensor Inventory Matrix

| Physical Sensor | Present on Hardware? | Accessible over BLE? | Raw Waveform Accessible? | Pre-Processed Data Accessible? | Sampling Frequency | Resolution & Scale | Primary BLE Opcode | Evidence Base | Confidence Level |
|---|---|---|---|---|---|---|---|---|---|
| **3-Axis Accelerometer** | ✅ **YES** | 🟡 **INDIRECT** | ❌ **NO** | ✅ **YES** | 100 Hz (Internal DSP) | 16-bit $\pm 8g$ (Internal) | `0x0056` (`ACTIVITY_DATA`) | Step counts & intervals in BLE stream | **CONFIRMED** |
| **3-Axis Gyroscope** | ✅ **YES** (Pro 2) | ❌ **NO** | ❌ **NO** | ❌ **NO** | 100 Hz (Internal DSP) | 16-bit $\pm 2000^\circ/s$ | None | Used internally by ATS3089C motion filter | **CONFIRMED** |
| **Green PPG Sensor** | ✅ **YES** (525nm) | 🟡 **INDIRECT** | ❌ **NO** | ✅ **YES** | 50 Hz (Internal DSP) | 10-bit AFE (Internal) | `0x0053` / `0x00E0` | Discrete 8B BPM & Resting HR records | **CONFIRMED** |
| **Red / IR SpO₂ Sensor** | ✅ **YES** (660/940nm) | 🟡 **INDIRECT** | ❌ **NO** | ✅ **YES** | Intermittent / Spot | 1% SpO₂ resolution | `0x0055` (`SPO2`) | 8B discrete SpO₂ % records | **CONFIRMED** |
| **GNSS / GPS Receiver** | ✅ **YES** (Dual Sat) | ✅ **YES** | ✅ **YES** | ✅ **YES** | 1 Hz during outdoor activity | $10^{-7}$ deg ($1.1\text{ cm}$) | `0xA05A` (`WORKOUT_GPS`) | 12B coordinate records (`ts`, `lon`, `lat`) | **CONFIRMED** |
| **Capacitive Wear Sensor** | ✅ **YES** | 🟡 **INDIRECT** | ❌ **NO** | ✅ **YES** | Continuous | Binary (On-wrist / Off-wrist) | `0x0053` (Dropouts) | HR dropouts when off-wrist | **CONFIRMED** |
| **Skin Temperature** | ❌ **NO** | ❌ **NO** | ❌ **NO** | ❌ **NO** | N/A | N/A | None | No hardware thermistor exposed | **CONFIRMED** |
| **Barometer / Altimeter** | ❌ **NO** | ❌ **NO** | ❌ **NO** | ❌ **NO** | N/A | N/A | None | Altitude derived via GPS coordinates | **CONFIRMED** |
| **Magnetometer / Compass**| ❌ **NO** | ❌ **NO** | ❌ **NO** | ❌ **NO** | N/A | N/A | None | Bearing derived from GPS velocity vectors | **CONFIRMED** |

---

## 2. Detailed Breakdown by Sensor Domain

### 2.1 3-Axis Accelerometer (IMU)
- **Hardware**: InvenSense / Bosch 3-Axis MEMS accelerometer sampled at 100 Hz by the ATS3089C MCU.
- **BLE Stream Status**: Raw 100Hz 3-axis $(X, Y, Z)$ accelerometry vectors are **NOT** streamed over BLE.
- **Transmitted Data**: The watch firmware runs on-device actigraphy peak detection and transmits 32-byte interval records containing accumulated step counts, estimated walking distance, and active calories over `ACTIVITY_DATA` (`0x0056`).

### 2.2 Optical PPG Sensor (Green LED + Photodiode)
- **Hardware**: Dual 525nm Green LEDs + Analog Front-End (AFE) photodiode sampled at 50 Hz.
- **BLE Stream Status**: Raw photodiode voltage AC/DC waveforms are **NOT** streamed over BLE.
- **Transmitted Data**: Transmits discrete 8-byte Heart Rate records `(epoch_sec: u32, bpm: u32)` over `0x0053` (Auto/Manual) and `0x00E0` (Workout), and 5B/8B Resting HR over `0x00DA`.

### 2.3 Red / IR SpO₂ Sensor
- **Hardware**: 660nm Red LED + 940nm Infrared LED photodiode assembly.
- **BLE Stream Status**: Raw optical ratio-of-ratios photodiode signals ($R$-curve) are **NOT** streamed over BLE.
- **Transmitted Data**: Transmits discrete 8-byte SpO₂ percentage records `(epoch_sec: u32, percentage: u32)` over `0x0055`.

### 2.4 GNSS / GPS Receiver
- **Hardware**: Actions ATS3089C internal dual-band GNSS receiver (GPS / GLONASS / Galileo / Beidou).
- **BLE Stream Status**: **RAW & ACCESSIBLE**. Streams 1 Hz coordinate points during outdoor workout sessions.
- **Transmitted Data**: Transmits 12-byte records `(timestamp: i32_le, lon_1e7: i32_le, lat_1e7: i32_le)` over `WORKOUT_GPS` (`0xA05A`).
