# Sensor → Metric Dependency Map

This document maps physical smartwatch sensors to derived health and fitness metrics in the **CMF / Nothing Smartwatch Ecosystem**, based on static APK decompilation (Nothing X 3.7.3), `libGoMoreEdgeKit.so` symbol analysis, ATS3089C firmware RE, and BLE notification payload decoders.

---

## 1. Physical Sensor Inventory

The CMF Watch Pro / Pro 2 hardware contains the following physical sensors:

1. **Optical PPG Sensor (Green LED + Photodiode)**: Measures blood volume pulse for heart rate and HRV.
2. **Optical SpO₂ Sensor (Red / Infrared LED + Photodiode)**: Measures oxygenated vs deoxygenated hemoglobin light absorption ratio.
3. **3-Axis Accelerometer (IMU)**: Measures linear acceleration and wrist movement vectors.
4. **3-Axis Gyroscope (IMU)**: Measures rotational velocity (Watch Pro 2).
5. **GNSS / GPS Receiver**: Dual-satellite positioning (GPS / GLONASS / Galileo / Beidou).
6. **Capacitive Wear Detection Sensor**: Contact sensor detecting off-wrist state.

---

## 2. Sensor → Metric Dependency Graph

```text
                  PHYSICAL SENSORS                                  DERIVED METRICS
                  ────────────────                                  ───────────────

  [ Accelerometer ] ─────────┬───────────────────────────────────►  Steps  [CONFIRMED]
                             ├───────────────────────────────────►  Distance  [CONFIRMED]
                             ├───────────────────────────────────►  Active Calories  [CONFIRMED]
                             ├───────────────────────────────────►  Cadence / Pace  [HIGH CONFIDENCE]
                             ├───────────────────────────────────►  Sleep Detection & Motion  [CONFIRMED]
                             └───────────────────────────────────►  Off-Wrist State  [CONFIRMED]

  [ Optical PPG (Green) ] ───┬───────────────────────────────────►  Real-time Heart Rate  [CONFIRMED]
                             ├───────────────────────────────────►  Resting Heart Rate  [CONFIRMED]
                             ├───────────────────────────────────►  HRV (rMSSD / SDNN)  [HIGH CONFIDENCE]
                             ├───────────────────────────────────►  Stress Score  [CONFIRMED]
                             ├───────────────────────────────────►  Sleep Stage (REM/Deep)  [CONFIRMED]
                             └───────────────────────────────────►  Personalized HR Zones  [CONFIRMED]

  [ Optical SpO₂ (Red/IR) ] ─┬───────────────────────────────────►  SpO₂ Percentage  [CONFIRMED]
                             └───────────────────────────────────►  Sleep Apnea / Desat  [POSSIBLE]

  [ GNSS / GPS ] ────────────┬───────────────────────────────────►  Workout GPS Tracks  [CONFIRMED]
                             ├───────────────────────────────────►  Outdoor Distance / Speed  [CONFIRMED]
                             └───────────────────────────────────►  VO₂ Max (GoMore Engine)  [CONFIRMED]

  [ User Profile ] ──────────┬───────────────────────────────────►  Basal Metabolic Rate (BMR)  [CONFIRMED]
  (Age/Sex/Weight/Height)    ├───────────────────────────────────►  Exercise Load  [CONFIRMED]
                             └───────────────────────────────────►  Recovery Time  [CONFIRMED]
```

---

## 3. Relationship Classification Matrix

| Metric | Primary Inputs | Derived Via | Confidence Level | Evidence Base |
|---|---|---|---|---|
| **Steps** | Accelerometer (IMU) | Watch MCU Firmware | **CONFIRMED** | `ACTIVITY_DATA` (`0x0056`) 32B LE records |
| **Distance** | Accelerometer + Stride Model | Watch MCU / App Profile | **CONFIRMED** | Stride length computed from height in `USER_INFO_SET` (`0x0095`) |
| **Real-time HR** | Green PPG | Watch MCU Hardware Filter | **CONFIRMED** | `HEART_RATE_MANUAL_AUTO` (`0x0053`) 8B LE records |
| **Resting HR** | Green PPG + Lowest Sleep HR | Watch MCU + GoMore | **CONFIRMED** | `HEART_RATE_RESTING` (`0x00DA`) & `restHRBound` in `libGoMoreEdgeKit.so` |
| **SpO₂** | Red / IR PPG | Watch MCU Opticals | **CONFIRMED** | `SPO2` (`0x0055`) 8B LE records |
| **Stress** | HRV (rMSSD) + PPG | Watch MCU / GoMore | **CONFIRMED** | `STRESS` (`0x009D`) 8B LE records (score 1-99) |
| **Sleep Stages** | Accelerometer + PPG + Time | Watch MCU Firmware | **CONFIRMED** | `SLEEP_DATA` (`0x0058`) 18B header + 8B stage records |
| **VO₂ Max** | GPS Speed + HR + User Profile | `libGoMoreEdgeKit.so` | **CONFIRMED** | `vo2Max` symbol in GoMore JNI library |
| **Exercise Load** | HR + Workout Duration + Profile | `libGoMoreEdgeKit.so` | **CONFIRMED** | `get_training_load` in `libGoMoreEdgeKit.so` |
| **Recovery Time** | Exercise Load + VO₂ Max | `libGoMoreEdgeKit.so` | **CONFIRMED** | `get_phrz` & `staminaLevel` in GoMore library |
| **HR Zones** | Max HR + Resting HR + Age | `libGoMoreEdgeKit.so` | **CONFIRMED** | `RPE2HRZ_lb_t`, `rpeTohr_0321_v2` in GoMore library |
