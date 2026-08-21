# Sleep Processing & Stage Classification Pipeline

This document details how sleep detection and stage classification (Awake, Light, Deep, REM) operate in the CMF / Nothing Smartwatch ecosystem.

---

## 1. Sleep Pipeline Architecture

```text
  [ 3-Axis Accelerometer (IMU) ]       [ Green PPG (Heart Rate / HRV) ]
                │                                    │
                │ 100Hz Motion Signals               │ Inter-Beat Intervals (IBI)
                ▼                                    ▼
       ┌──────────────────────────────────────────────────┐
       │     Watch MCU Firmware (Actions ATS3089C)        │
       │                                                  │
       │  1. Sleep Onset / Offset Detection (Actigraphy) │
       │  2. Movement Index Windowing (30-second epochs)  │
       │  3. HRV Autonomic Tone Analysis (Sympathetic)    │
       │  4. Sleep Stage Classification Engine            │
       └──────────────────────────────────────────────────┘
                                │
                                │ Encrypted BLE Stream (Opcode 0x0058)
                                ▼
                       [ Nothing X App ]
                                │
                                ▼
       ┌──────────────────────────────────────────────────┐
       │             Database & UI Presentation           │
       │                                                  │
       │  - Calculate Total Duration (Deep, Core, REM)    │
       │  - Compute Sleep Score Index (0 - 100)           │
       │  - Render Sleep Stage Timeline Bar               │
       └──────────────────────────────────────────────────┘
```

---

## 2. Stage Classification Logic & Features

| Sleep Stage | Code (`u16`) | Primary Input Signals | Heuristic Characteristics |
|---|---|---|---|
| **Deep Sleep** | `1` | Zero Motion + Low HR + High HRV | Low wrist acceleration variance, minimal movement index over 15-30 min windows, parasympathetic dominance. |
| **Light (Core) Sleep** | `2` | Micro Motion + Moderate HR | Periodic small wrist movements, baseline resting heart rate. |
| **REM Sleep** | `3` | Zero Body Motion + Variable HR/HRV | Muscle atonia (zero accelerometer variance) accompanied by heart rate variability spikes (vivid dreaming state). |
| **Awake** | `4` | High Accelerometer Variance | Sustained wrist rotation and acceleration above actigraphy threshold. |

---

## 3. Sleep Score Computation (`0 - 100`)

The Sleep Score displayed in Nothing X is computed locally in the app using weighted sleep architecture parameters:

$$\text{Sleep Score} = W_{\text{dur}} \cdot S_{\text{dur}} + W_{\text{deep}} \cdot S_{\text{deep}} + W_{\text{rem}} \cdot S_{\text{rem}} + W_{\text{eff}} \cdot S_{\text{eff}}$$

Where:
- **Total Duration Score ($S_{\text{dur}}$)**: Optimal target = 7 to 9 hours (420–540 minutes).
- **Deep Sleep Proportion ($S_{\text{deep}}$)**: Target = 15% to 25% of total sleep duration.
- **REM Sleep Proportion ($S_{\text{rem}}$)**: Target = 20% to 25% of total sleep duration.
- **Sleep Efficiency ($S_{\text{eff}}$)**: Ratio of total asleep time to total time in bed ($\frac{\text{Sleep Time}}{\text{Time in Bed}}$).
