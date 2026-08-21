# Ground-Truth Validation Methodology & Instrumentation

This document defines the reference instrumentation, experimental protocols, error metrics, and validation procedures required to measure algorithm accuracy against empirical ground truth.

---

## 1. Reference Instrumentation & Validation Matrix

| Metric Domain | Target Metric | Gold-Standard Reference Instrument | Practical Field Reference | Reference Error Margin |
|---|---|---|---|---|
| **Actigraphy** | Steps & Cadence | High-Frame-Rate Video Annotation (1080p 60fps) | Manual Digital Tally Counter | $\pm 0.0\%$ (Zero error) |
| **Actigraphy** | Distance | Calibrated Surveyors Wheel / Track | GPS Differential Dual-Band | $\pm 0.1\%$ |
| **Cardiovascular** | Heart Rate & HRV | 12-Lead Clinical ECG / 3-Lead Chest Strap | Polar H10 Chest Strap | $\pm 1.0\text{ bpm}$ / $\pm 2.0\text{ ms } rMSSD$ |
| **Cardiovascular** | Resting HR | Continuous Nocturnal ECG | Polar H10 Nocturnal Stream | $\pm 1.0\text{ bpm}$ |
| **Oxygenation** | SpO₂ Saturation | Arterial Blood Gas (ABG) Co-Oximetry | Masimo MightySat Pulse Oximeter | $\pm 1.5\%$ SpO₂ |
| **Sleep** | Sleep Stages | Clinical Polysomnography (PSG 16-channel) | SomnoSystems / Dreem 2 EEG | $\pm 85\text{--}90\%$ Stage Agreement |
| **Metabolic** | Energy Expenditure | Indirect Calorimetry (Metabolic Cart) | Cosmed K5 Portable CPET | $\pm 3.0\%$ kcal |
| **Fitness** | VO₂ Max | Incremental Treadmill CPET to Exhaustion | Cosmed K5 Portable CPET | $\pm 1.5\text{ mL/kg/min}$ |

---

## 2. Statistical Evaluation Metrics

To rigorously evaluate candidate algorithms against baseline implementations, the following statistical metrics must be calculated:

### 2.1 Mean Absolute Error (MAE)
$$\text{MAE} = \frac{1}{N} \sum_{i=1}^{N} \left| y_i - \hat{y}_i \right|$$

### 2.2 Root Mean Square Error (RMSE)
$$\text{RMSE} = \sqrt{\frac{1}{N} \sum_{i=1}^{N} \left( y_i - \hat{y}_i \right)^2}$$

### 2.3 Mean Absolute Percentage Error (MAPE)
$$\text{MAPE} = \frac{100\%}{N} \sum_{i=1}^{N} \left| \frac{y_i - \hat{y}_i}{y_i} \right|$$

### 2.4 Bland-Altman Analysis (95% Limits of Agreement)
- **Mean Bias ($\bar{d}$)**: $\bar{d} = \frac{1}{N} \sum (y_i - \hat{y}_i)$
- **Upper Limit of Agreement (ULoA)**: $\bar{d} + 1.96 \cdot SD$
- **Lower Limit of Agreement (LLoA)**: $\bar{d} - 1.96 \cdot SD$

### 2.5 Categorical Classification Metrics (Sleep Stages / Stress Categories)
- **Confusion Matrix**: True Positives ($TP$), False Positives ($FP$), False Negatives ($FN$), True Negatives ($TN$).
- **$F_1$-Score**:
  $$\text{Precision} = \frac{TP}{TP + FP}, \quad \text{Recall} = \frac{TP}{TP + FN}, \quad F_1 = 2 \cdot \frac{\text{Precision} \cdot \text{Recall}}{\text{Precision} + \text{Recall}}$$

---

## 3. Protocol Setup Guidelines

1. **Step Counting Protocol**: 1,000-step continuous indoor track walk + 500-step outdoor run + 15-minute simulated office activity (typing, desk movement). Manual tally counter vs. watch reported steps.
2. **Heart Rate Protocol**: 30-minute incremental treadmill protocol (Rest $\to$ Walk $5\text{km/h}$ $\to$ Jog $9\text{km/h}$ $\to$ Sprint $14\text{km/h}$ $\to$ Cool Down). Polar H10 chest strap vs. watch heart rate.
3. **Distance Protocol**: 400m athletics track walk (4 laps = 1,600m). Surveyor wheel measurement vs. watch calculated distance.
