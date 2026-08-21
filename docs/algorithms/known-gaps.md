# Map of Knowns vs Unknowns (Known Gaps Analysis)

This document provides a scientifically defensible, rigorous breakdown of confirmed facts versus open research gaps in the CMF Smartwatch data processing pipeline.

---

## 1. Metric-by-Metric Breakdown

### 1.1 Steps & Distance
- **CONFIRMED**: Step counts and distance are calculated on the watch MCU (Actions ATS3089C) and delivered in 32-byte `ACTIVITY_DATA` (`0x0056`) LE records. Height in `USER_INFO_SET` (`0x0095`) configures stride length.
- **UNKNOWN / GAPS**: The exact FIR/IIR filter coefficients and digital peak-detection thresholds embedded inside the closed-source ATS3089C C firmware binary.

### 1.2 Heart Rate & Resting HR
- **CONFIRMED**: Real-time heart rate is sampled by green PPG optical sensors on the watch and sent as 8-byte LE records (`0x0053`). Resting HR is computed as the minimum 10-minute average during deep sleep (`0x00DA`).
- **UNKNOWN / GAPS**: Raw optical PPG waveform samples (50Hz photodiode arrays) are not exposed over standard BLE opcodes.

### 1.3 Sleep & Stage Classification
- **CONFIRMED**: The watch MCU performs sleep detection and classifies stages (1=Deep, 2=Light, 3=REM, 4=Awake), sending 18-byte headers + 8-byte stage records (`0x0058`). Nothing X calculates the 0-100 Sleep Score index.
- **UNKNOWN / GAPS**: The exact decision-tree thresholds (motion index cutoff vs HRV shift) used by ATS3089C firmware to transition between Light and REM sleep.

### 1.4 SpO₂ & Stress
- **CONFIRMED**: SpO2 (50-100%) and Stress (1-99) are computed on the watch MCU and delivered over opcodes `0x0055` and `0x009D`.
- **UNKNOWN / GAPS**: Proprietary optical calibration curves ($R$-curve table) for the red/IR photodiode hardware assembly.

### 1.5 VO₂ Max, Training Load & Recovery
- **CONFIRMED**: Processed by native shared library `libGoMoreEdgeKit.so` using Banister TRIMP equations (`get_training_load`), Karvonen HRR zone models (`get_phrz`), and submaximal speed-to-HR linear regression (`vo2Max`).
- **UNKNOWN / GAPS**: GoMore's internal proprietary weighting factors for non-running sports (e.g. swimming, badminton).
