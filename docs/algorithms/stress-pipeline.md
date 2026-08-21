# Stress Level Pipeline (HRV → Stress Index)

This document details how autonomic nervous system stress is measured and categorized (1–99) in the CMF smartwatch ecosystem.

---

## 1. Stress Processing Architecture

```text
[ PPG Optical Sensor ] ───► Inter-Beat Intervals (IBI / RR-Intervals)
                                       │
                                       ▼
                       [ Root Mean Square of Successive Differences ]
                       $rMSSD = \sqrt{\frac{1}{N-1} \sum_{i=1}^{N-1} (RR_{i+1} - RR_i)^2}$
                                       │
                                       │ Motion Masking (IMU check: if steps > 0, suppress stress)
                                       ▼
                       [ Normalization & Stress Score Scaling ]
                       Score = Clamp(100 - Scale(rMSSD), 1, 99)
                                       │
                                       ▼
                       [ Categorization (Opcode 0x009D) ]
```

---

## 2. Stress Score Categorization

| Stress Index Range | Category Enum | Sympathetic / Parasympathetic State |
|---|---|---|
| **1 – 29** | `RELAXED` | Parasympathetic dominance (high HRV, low heart rate, rest) |
| **30 – 59** | `NORMAL` | Autonomic balance (baseline daily activity) |
| **60 – 79** | `MEDIUM` | Sympathetic elevation (mild stress / mental exertion) |
| **80 – 99** | `HIGH` | High Sympathetic activation (acute stress / physical fatigue) |
