# Experimental Roadmap & First Experiment Recommendation

This document prioritizes algorithm improvement opportunities and details the experimental design for our **first empirical experiment**.

---

## 1. Single Best First Experiment Recommendation

### 🏆 Recommended First Experiment: Cadence-Adaptive Stride Length & Distance Scaling

#### Why This Is the Best First Experiment:
1. **Available Data**: `ACTIVITY_DATA` (`0x0056`) already provides step counts and interval timestamps over BLE.
2. **Clear & Unambiguous Ground Truth**: 100% accurate ground truth can be established using a physical surveyor's wheel or calibrated 400m athletics track.
3. **Measurable Baseline**: Nothing's current static stride multiplier ($\text{Stride} = \text{Height} \times 0.415$) is easy to compute and benchmark against.
4. **Low Implementation Complexity**: Pure mathematical model requiring no neural network training or complex JNI dependencies.
5. **High Immediate Practical Value**: Directly improves outdoor walking, jogging, and running distance accuracy when GPS is unavailable or disabled.

---

## 2. Detailed Experimental Designs

---

### Experiment 01: Cadence-Adaptive Stride Length Scaling

- **Target Metric**: Distance (meters) & Stride Length (cm).
- **Current Nothing Approach**: Static height multiplier ($\text{Distance} = \text{Steps} \times (\text{Height} \times 0.415)$).
- **Candidate Algorithm**: Pace-dependent dynamic stride length equation:
  $$\text{Cadence } f = \frac{\text{Steps}}{\Delta t_{\text{minutes}}}$$
  $$\text{Stride Length}(f) = \text{Height}_{\text{m}} \times \left( a \cdot f + b \right)$$
- **Ground Truth**: Calibrated 400m athletics track (4 laps = 1,600m) + Surveyor's wheel.
- **Evaluation Metrics**: MAE (meters), MAPE (%), and Bland-Altman Limits of Agreement.
- **Success Criteria**: Reduce distance MAPE from $> 8.0\%$ (baseline static model) to $< 3.5\%$ (adaptive model).
- **Priority**: 🔥 **HIGH PRIORITY**

---

### Experiment 02: Personalized $z$-Score Stress Normalization

- **Target Metric**: Stress Score Index (1–99) & Stress Categories.
- **Current Nothing Approach**: Fixed population-wide $rMSSD$ thresholds.
- **Candidate Algorithm**: Individualized 14-day rolling $z$-score normalization:
  $$z = \frac{rMSSD_i - \mu_{14d}}{\sigma_{14d}}, \quad \text{Stress Index} = \text{Clamp}\left(50 + 15 \cdot z, 1, 99\right)$$
- **Ground Truth**: ECG Chest Strap $rMSSD$ baseline recording + Self-reported perceived stress log.
- **Evaluation Metrics**: Pearson correlation coefficient ($r$), Cohen's Kappa ($\kappa$) for stress categories.
- **Success Criteria**: Increase correlation with validated ECG $rMSSD$ stress index from $r = 0.65$ to $r > 0.85$.
- **Priority**: 🔥 **HIGH PRIORITY**

---

### Experiment 03: Open Banister TRIMP & ACWR Training Load Engine

- **Target Metric**: Training Load Index & Acute-to-Chronic Workload Ratio.
- **Current Nothing Approach**: Proprietary closed binary `libGoMoreEdgeKit.so` (`get_training_load`).
- **Candidate Algorithm**: Open Banister TRIMP equation implemented in Python:
  $$\text{TRIMP} = D_{\text{min}} \cdot \Delta HR_{\text{ratio}} \cdot 0.64 \cdot e^{1.92 \cdot \Delta HR_{\text{ratio}}}$$
- **Ground Truth**: Calculated TRIMP benchmark from raw chest strap workout HR logs.
- **Evaluation Metrics**: MAE, RMSE, Pearson correlation $r$.
- **Success Criteria**: Match GoMore training load outputs with $r > 0.95$ while providing full source code transparency.
- **Priority**: 🟡 **MEDIUM PRIORITY**

---

### Experiment 04: Hidden Markov Model (HMM) Sleep Stage Sequence Smoothing

- **Target Metric**: Sleep Stage Sequence (Deep, Light, REM, Awake) & Sleep Architecture Durations.
- **Current Nothing Approach**: Raw 30-second epoch actigraphy/HRV classification.
- **Candidate Algorithm**: 4-State Hidden Markov Model with Viterbi decoding to eliminate physiologically impossible stage jumps.
- **Ground Truth**: Sleep stage timeline logged by EEG sleep headband / Polysomnography.
- **Evaluation Metrics**: Multi-class $F_1$-score, Confusion Matrix.
- **Success Criteria**: Improve overall sleep stage classification $F_1$-score by $\ge 8\%$.
- **Priority**: 🟡 **MEDIUM PRIORITY**

---

## 3. Priority Summary Table

| Experiment | Target Metric | Required Data | Ground Truth | Implementation Complexity | Impact Potential | Overall Priority |
|---|---|---|---|---|---|---|
| **Exp 01** | Stride Length / Distance | Step intervals, Height | Surveyor Wheel / Track | **Low** | **High** | 🔥 **HIGH** |
| **Exp 02** | Stress Normalization | Stress / HRV stream | ECG Chest Strap $rMSSD$ | **Medium** | **High** | 🔥 **HIGH** |
| **Exp 03** | Training Load (TRIMP) | Workout HR & Duration | Banister TRIMP benchmark | **Medium** | **Medium** | 🟡 **MEDIUM** |
| **Exp 04** | Sleep Stage Smoothing | Decoded Sleep Stages | EEG Sleep Headband / PSG | **High** | **High** | 🟡 **MEDIUM** |
