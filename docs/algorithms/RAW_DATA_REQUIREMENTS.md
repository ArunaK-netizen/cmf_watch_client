# Raw Sensor Data Stream Availability & Requirements

This document evaluates the raw physical sensor data streams required for candidate algorithm improvements, compares them against what the CMF Smartwatch BLE protocol actually exposes, and establishes feasibility boundaries.

---

## 1. Raw Sensor Stream Availability Inventory

| Sensor Stream | Target Sampling Rate | Raw Signal Format | BLE Command / Opcode | Protocol Availability Status | Confidence Level |
|---|---|---|---|---|---|
| **Raw PPG Waveform** | 50 Hz – 100 Hz | 16-bit Photodiode Array | None Discovered | ❌ **NOT AVAILABLE** | **CONFIRMED** |
| **Raw Accelerometer (XYZ)** | 50 Hz – 100 Hz | $m/s^2$ or $g$ 3-Axis Vectors | None Discovered | ❌ **NOT AVAILABLE** | **CONFIRMED** |
| **Raw Gyroscope (XYZ)** | 50 Hz – 100 Hz | $deg/s$ Rotational Velocity | None Discovered | ❌ **NOT AVAILABLE** | **CONFIRMED** |
| **Inter-Beat Intervals (IBI)** | Per-beat ($ms$) | Millisecond $RR$-Intervals | Embedded in Stress (`0x009D`) | 🟡 **INDIRECT / PRE-PROCESSED** | **CONFIRMED** |
| **Discrete Heart Rate (BPM)** | 1 Hz / 1-min | 8-byte LE (`epoch`, `bpm`) | `HEART_RATE_*` (`0x0053`, `0x00E0`) | ✅ **AVAILABLE** | **CONFIRMED** |
| **Resting Heart Rate (BPM)** | Daily / Sleep Min | 5-byte / 8-byte LE | `HEART_RATE_RESTING` (`0x00DA`) | ✅ **AVAILABLE** | **CONFIRMED** |
| **Step Interval Counts** | 1-min / Hourly | 32-byte LE (`steps`, `dist`) | `ACTIVITY_DATA` (`0x0056`) | ✅ **AVAILABLE** | **CONFIRMED** |
| **SpO₂ Samples** | Spot / Periodic | 8-byte LE (`epoch`, `spo2`) | `SPO2` (`0x0055`) | ✅ **AVAILABLE** | **CONFIRMED** |
| **Stress Index Samples** | Periodic | 8-byte LE (`epoch`, `score`) | `STRESS` (`0x009D`) | ✅ **AVAILABLE** | **CONFIRMED** |
| **Sleep Session & Stages** | Per sleep session | 18-byte Header + 8-byte Stages | `SLEEP_DATA` (`0x0058`) | ✅ **AVAILABLE** | **CONFIRMED** |
| **Workout GPS Coordinates** | 1 Hz during GPS | 12-byte LE (`ts`, `lon`, `lat`) | `WORKOUT_GPS` (`0xA05A`) | ✅ **AVAILABLE** | **CONFIRMED** |

---

## 2. Requirement Assessment for Candidate Improvements

### 2.1 Candidate 1: Cadence-Based Adaptive Stride Length & Distance
- **Required Inputs**: Interval step counts + interval elapsed time + user height.
- **Available Inputs**: `ACTIVITY_DATA` (`0x0056`) provides step counts and timestamps per interval. User height is sent in `USER_INFO_SET` (`0x0095`).
- **Feasibility Determination**: ✅ **FEASIBLE**. Does NOT require raw 100Hz IMU arrays. Step cadence can be derived directly from interval timestamps:
  $$\text{Cadence (steps/min)} = \frac{\Delta \text{Steps}}{\Delta t_{\text{minutes}}}$$

---

### 2.2 Candidate 2: Individualized $z$-Score HRV Stress Normalization
- **Required Inputs**: Historical time-series of continuous stress / $rMSSD$ readings over a multi-day baseline period.
- **Available Inputs**: `STRESS` (`0x009D`) provides periodic 8-byte stress index records.
- **Feasibility Determination**: ✅ **FEASIBLE**. Does NOT require raw ECG/PPG waveforms. Can be computed on the host by building a rolling 14-day mean ($\mu_{14d}$) and standard deviation ($\sigma_{14d}$) of daily stress scores.

---

### 2.3 Candidate 3: Open Banister TRIMP & ACWR Training Load Engine
- **Required Inputs**: Active workout duration, workout average heart rate stream, resting heart rate, and max heart rate.
- **Available Inputs**: `WORKOUT_SUMMARY` (`0x0057`), `HEART_RATE_WORKOUT` (`0x00E0`), `HEART_RATE_RESTING` (`0x00DA`), and user profile parameters.
- **Feasibility Determination**: ✅ **FEASIBLE**. Does NOT depend on closed binary `libGoMoreEdgeKit.so`. All required physiological inputs are accessible over BLE.

---

### 2.4 Candidate 4: Hidden Markov Model (HMM) Sleep Stage Sequence Smoothing
- **Required Inputs**: Decoded sleep stage interval sequence `(stage_code, start_time, duration)`.
- **Available Inputs**: `SLEEP_DATA` (`0x0058`) header and stage array.
- **Feasibility Determination**: ✅ **FEASIBLE**. Post-processes the decoded stage sequence to eliminate physiologically invalid rapid stage transitions (e.g., Deep Sleep $\to$ REM Sleep).

---

### 2.5 Candidate 5: Raw PPG Motion Artifact Cancellation
- **Required Inputs**: 50Hz – 100Hz continuous raw dual-channel photodiode optical waveforms + simultaneous 3-axis accelerometer signals.
- **Available Inputs**: None. The watch MCU does not stream raw PPG signals over BLE.
- **Feasibility Determination**: ❌ **NOT FEASIBLE**. We must not attempt to design host-side optical PPG motion cancellation filters without access to raw photodiode waveforms.

---

## 3. Summary Conclusion

Host-side algorithm improvements must focus strictly on **higher-level data fusion, physiological modeling, adaptive stride length scaling, baseline normalization, and temporal sequence smoothing** operating on the discrete telemetry streams provided by the watch protocol.
