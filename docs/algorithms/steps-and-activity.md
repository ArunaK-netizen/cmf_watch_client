# Step Detection & Activity Tracking Pipeline

This document details accelerometer motion processing, step counting, stride length math, and walking/running classification.

---

## 1. Step Detection Pipeline

```text
[ 3-Axis Accelerometer ] ───► [ Vector Magnitude Calculation ]
                              $A_{net} = \sqrt{A_x^2 + A_y^2 + A_z^2}$
                                          │
                                          │ High-Pass Filter (Remove Gravity 1g)
                                          ▼
                              [ Peak Threshold Detection ]
                              (Frequency window: 1.0Hz - 3.5Hz)
                                          │
                                          │ Minimum Step Buffer (Regulator: >= 10 consecutive steps)
                                          ▼
                              [ Step Count Accumulation ]
```

---

## 2. Stride Length & Distance Calculation

Distance is computed locally on the watch MCU and transmitted in 32-byte `ACTIVITY_DATA` (`0x0056`) records:

$$\text{Stride Length (Walking)} = \text{Height (cm)} \times 0.415$$
$$\text{Stride Length (Running)} = \text{Height (cm)} \times 0.450$$
$$\text{Distance (meters)} = \text{Steps} \times \text{Stride Length (meters)}$$

When GPS is enabled during outdoor workouts, distance is updated directly from GPS track points.
