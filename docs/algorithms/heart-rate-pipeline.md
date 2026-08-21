# Heart Rate & HRV Processing Pipeline

This document explains PPG optical signal processing, resting heart rate determination, HRV calculation, and Heart Rate Zones.

---

## 1. Heart Rate Processing Pipeline

```text
[ Green LED (525nm) ] ───► [ Wrist Tissue ] ───► [ Photodiode Sensor ]
                                                       │
                                                       │ Analog Raw PPG Signal
                                                       ▼
                                          [ ATS3089C Hardware AFE ]
                                                       │
                                                       │ Bandpass Filter (0.5Hz - 4.0Hz)
                                                       ▼
                                          [ Motion Artifact Rejection ]
                                          (IMU Accelerometer Compensation)
                                                       │
                                                       │ Systolic Peak Detection
                                                       ▼
                                          [ Inter-Beat Intervals (IBI) ]
                                                       │
                                   ┌───────────────────┴───────────────────┐
                                   ▼                                       ▼
                         [ Real-time BPM ]                       [ HRV (rMSSD) ]
                         (Opcode 0x0053)                         (Opcode 0x009D / Stress)
```

---

## 2. Resting Heart Rate Determination

Resting Heart Rate ($HR_{rest}$) is transmitted via `HEART_RATE_RESTING` (`0x00DA`).

**Calculation**:
$$HR_{rest} = \min \left( \text{PPG Mean during Deep Sleep} \right)$$

The watch identifies the lowest continuous 10-minute average heart rate measured while the user is in a confirmed **Deep Sleep** stage.

---

## 3. Personalized Heart Rate Zones (GoMore Engine)

Heart Rate Zones in Nothing X are calculated by `libGoMoreEdgeKit.so` using the **Karvonen Heart Rate Reserve (HRR)** formula:

$$HRR = HR_{max} - HR_{rest}$$
$$HR_{target} = (HRR \times \% \text{Intensity}) + HR_{rest}$$

Where:
- $HR_{max} = 220 - \text{Age}$ (or custom user max HR from `GMCoachUserInfo`).
- $HR_{rest}$ = Measured Resting Heart Rate.

| Zone | Name | % HRR | Purpose |
|---|---|---|---|
| **Zone 1** | Warm Up | 50% – 60% HRR | Active recovery & warmup |
| **Zone 2** | Fat Burn / Aerobic | 60% – 70% HRR | Endurance building & lipid oxidation |
| **Zone 3** | Aerobic Endurance | 70% – 80% HRR | Cardiovascular capacity |
| **Zone 4** | Anaerobic Threshold | 80% – 90% HRR | Lactate threshold & speed endurance |
| **Zone 5** | VO₂ Max / Peak | 90% – 100% HRR | Maximum performance & sprinting |
