# Machine Learning & Algorithm Model Inventory

This document evaluates whether Nothing X and the CMF smartwatch platform utilize Machine Learning (ML) models (TensorFlow Lite, ONNX, NNAPI) or deterministic mathematical/heuristic models for health metrics.

---

## 1. Machine Learning vs Rule-Based Inventory

| Health Metric | Model Type | Engine / Location | Model Artifact File | Status |
|---|---|---|---|---|
| **Steps** | Heuristic / Threshold Filter | Watch MCU Firmware (ATS3089C) | Embedded in C Firmware | **CONFIRMED (No ML)** |
| **Heart Rate** | DSP Peak Detection | Watch MCU Hardware DSP | Embedded in MCU Firmware | **CONFIRMED (No ML)** |
| **SpO₂** | Optical Ratio Calculation ($R = \frac{AC_{red}/DC_{red}}{AC_{ir}/DC_{ir}}$) | Watch MCU Opticals | Lookup Calibration Curve | **CONFIRMED (No ML)** |
| **Stress** | HRV Statistical Model (rMSSD) | Watch MCU Firmware | Mathematical Formula | **CONFIRMED (No ML)** |
| **Sleep Stages** | Heuristic Decision Tree | Watch MCU Firmware | State Machine | **CONFIRMED (No ML)** |
| **HR Zones** | Physiological Formulas | `libGoMoreEdgeKit.so` | Karvonen / HRR Formulas | **CONFIRMED (No ML)** |
| **Training Load** | TRIMP Physiological Model | `libGoMoreEdgeKit.so` | Banister TRIMP Equation | **CONFIRMED (No ML)** |
| **VO₂ Max** | Linear Regression | `libGoMoreEdgeKit.so` | Firstbeat/GoMore Submaximal Model | **CONFIRMED (No ML)** |

---

## 2. APK Model File Search Results

A comprehensive file audit across `com.nothing.smartcenter.apk` (Base APK) and `config.armeabi_v7a.apk` (Split APK) revealed:

- **`.tflite` files**: 0 found.
- **`.onnx` files**: 0 found.
- **`.pb` / `.model` files**: 0 found.
- **ML Libraries**:
  - `libggml.so` & `libwhisper.so`: Used exclusively for speech-to-text / voice assistant features (Nothing Ear integration), **not** used for health metrics.
  - `libgoogle_mlkit_*`: Used for QR code scanning and camera barcode recognition in Nothing X host app.

---

## 3. Conclusion

**No neural networks or Machine Learning models are used for health or fitness metrics in the CMF smartwatch ecosystem.**

All health metrics (steps, heart rate, SpO2, stress, sleep stages, training load, VO2 max) are computed using **deterministic digital signal processing (DSP), physiological mathematical formulas (Karvonen HRR, Banister TRIMP), and heuristic state machines**.
