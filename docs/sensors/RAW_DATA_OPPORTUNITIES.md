# Raw Sensor Data Opportunity Map

This document classifies every data stream accessible from the CMF Smartwatch into four accessibility levels and outlines the realistic algorithm research opportunities at each level.

---

## 1. Data Accessibility Levels

### 🟢 LEVEL 1 — Raw Accessible (Direct Physical Signal)
- **Streams**: GPS Coordinates (`WORKOUT_GPS` / `0xA05A`).
- **Characteristics**: Unfiltered $10^{-7}$ degree latitude/longitude coordinate points sampled at 1 Hz during outdoor workouts.
- **Algorithm Opportunities**: Custom pace/speed estimation, elevation contour mapping, track map smoothing, and submaximal VO₂ Max regression.

---

### 🟡 LEVEL 2 — Semi-Processed Accessible (Intermediate Features)
- **Streams**:
  1. Inter-Beat Interval (IBI) / $rMSSD$ Stress Scores (`STRESS` / `0x009D`).
  2. Step Interval Counts (`ACTIVITY_DATA` / `0x0056`).
  3. Sleep Stage Timeline Sequences (`SLEEP_DATA` / `0x0058`).
- **Characteristics**: Data has undergone basic MCU-level DSP filtering or peak counting, but retains granular temporal structure.
- **Algorithm Opportunities**:
  - **Cadence-Based Adaptive Stride Length**: Derive step cadence ($f = \frac{\Delta \text{Steps}}{\Delta t}$) to compute dynamic stride length and distance.
  - **Personalized $z$-Score Stress Normalization**: Scale stress index against individual 14-day $rMSSD$ rolling baseline ($\mu_{14d}, \sigma_{14d}$).
  - **Hidden Markov Model (HMM) Sleep Smoothing**: Post-process sleep stage sequences to eliminate physiologically invalid stage transitions.

---

### 🔴 LEVEL 3 — Final Metric Only (Aggregated Results)
- **Streams**: Daily Step Totals, Active Calorie Accumulation, Resting Heart Rate (`0x00DA`), Workout Session Summaries (`0x0057`).
- **Characteristics**: Fully aggregated summary numbers calculated by watch firmware.
- **Algorithm Opportunities**: Open Banister TRIMP & Acute-to-Chronic Workload Ratio (ACWR) modeling using workout average heart rate and active duration.

---

### ⚪ LEVEL 4 — Unknown / Inaccessible
- **Streams**: 50Hz Raw Optical PPG Waveforms, 100Hz Raw 3-Axis Accelerometer XYZ Arrays, Raw SpO₂ Photodiode Voltage.
- **Characteristics**: Kept strictly inside the ATS3089C MCU firmware execution space; not transmitted over BLE.
- **Algorithm Opportunities**: ❌ **Host-side raw PPG motion artifact cancellation or deep learning IMU gait classification are NOT FEASIBLE** due to protocol data constraints.

---

## 2. Priority Ranking of Extraction Sources for Future Research

| Rank | Data Source | Accessibility Level | Sampling Fidelity | Real-time / Historical | Primary Algorithm Target |
|---|---|---|---|---|---|
| **1** | **GPS Track Points (`0xA05A`)** | **Level 1** | 1 Hz ($10^{-7}$ deg) | Historical / Stream | Outdoor Pace & VO₂ Max Regression |
| **2** | **Step Intervals (`0x0056`)** | **Level 2** | Interval / 1-min | Historical | Cadence-Adaptive Stride Length (Exp 01) |
| **3** | **Stress / HRV Stream (`0x009D`)** | **Level 2** | Periodic | Historical | Personalized $z$-Score Stress Scaling (Exp 02) |
| **4** | **Workout Heart Rate (`0x00E0`)** | **Level 2** | 8B records (1 Hz) | Historical / Stream | Open Banister TRIMP & ACWR Engine (Exp 03) |
| **5** | **Sleep Stage Timeline (`0x0058`)** | **Level 2** | 8B stage records | Historical | HMM Viterbi Sleep Sequence Filter (Exp 04) |
