# Calories Calculation Pipeline (Basal vs Active)

This document details the mathematical formulas used by the CMF Smartwatch ecosystem to calculate **Basal Metabolic Rate (BMR)**, **Active Calories**, and **Total Daily Caloric Expenditure**.

---

## 1. Calorie Classification

- **Basal / Resting Calories (BMR)**: Energy expended at rest to maintain vital metabolic processes (respiration, circulation, cellular homeostasis).
- **Active Calories**: Energy expended above baseline BMR due to physical motion (steps, workouts, exercise).
- **Total Daily Calories**: $\text{Total} = \text{BMR} + \text{Active Calories}$.

---

## 2. Basal Metabolic Rate (BMR) Formula

Nothing X calculates daily baseline BMR using the revised **Mifflin-St Jeor Equation**:

### For Males:
$$\text{BMR (kcal/day)} = (10 \times \text{Weight}_{\text{kg}}) + (6.25 \times \text{Height}_{\text{cm}}) - (5 \times \text{Age}_{\text{years}}) + 5$$

### For Females:
$$\text{BMR (kcal/day)} = (10 \times \text{Weight}_{\text{kg}}) + (6.25 \times \text{Height}_{\text{cm}}) - (5 \times \text{Age}_{\text{years}}) - 161$$

---

## 3. Active Calorie Calculation

Active calories are computed dynamically during workouts and daily activity using MET (Metabolic Equivalent of Task) values scaled by Heart Rate Ratio:

$$\text{Active Calories (kcal/min)} = \text{MET} \times 3.5 \times \frac{\text{Weight}_{\text{kg}}}{200} \times \left( \frac{HR_{\text{current}}}{HR_{\text{rest}}} \right)$$

- **Walking MET**: 3.3 METs
- **Running MET**: 8.0 – 11.5 METs
- **Cycling MET**: 6.8 – 10.0 METs
