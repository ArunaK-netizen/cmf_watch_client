# Cloud vs Device vs App Responsibility Matrix

This matrix establishes where each processing step occurs across the hardware, app, library, and cloud layers, backed by static APK decompilation and live device BLE captures.

---

## 1. System Responsibility Matrix

| Metric / Function | Watch MCU (ATS3089C) | Nothing X App (Java/Kotlin) | GoMore Native Engine (`libGoMoreEdgeKit.so`) | Cloud Backend | Status |
|---|:---:|:---:|:---:|:---:|---|
| **Step Counting** | **PRIMARY** | Storage only | — | — | **CONFIRMED** |
| **Real-time Heart Rate** | **PRIMARY** | Storage & UI | — | — | **CONFIRMED** |
| **Resting Heart Rate** | **PRIMARY** | Storage & UI | Secondary Filter | — | **CONFIRMED** |
| **SpO₂ Measurements** | **PRIMARY** | Storage & UI | — | — | **CONFIRMED** |
| **Stress Level (1-99)** | **PRIMARY** | Storage & UI | — | — | **CONFIRMED** |
| **Sleep Stage Classification** | **PRIMARY** | Score Math & UI | — | — | **CONFIRMED** |
| **Sleep Score Index** | — | **PRIMARY** | — | — | **CONFIRMED** |
| **Basal Calories (BMR)** | — | **PRIMARY** | — | — | **CONFIRMED** |
| **Active Calories** | **PRIMARY** | Aggregation | — | — | **CONFIRMED** |
| **Personalized HR Zones** | — | Configuration | **PRIMARY** | — | **CONFIRMED** |
| **Training Load** | — | Input Data Provider | **PRIMARY** | — | **CONFIRMED** |
| **VO₂ Max** | — | GPS/HR Provider | **PRIMARY** | — | **CONFIRMED** |
| **Recovery Hours** | — | Input Data Provider | **PRIMARY** | — | **CONFIRMED** |
| **Cloud Telemetry Backup** | — | Sync Client | — | **PRIMARY** | **CONFIRMED** |
