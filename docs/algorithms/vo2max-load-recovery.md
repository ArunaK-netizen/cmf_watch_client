# VO₂ Max, Exercise Load & Recovery Pipeline

This document details the sports science algorithms integrated via **Bomdic / GoMore (`libGoMoreEdgeKit.so`)**.

---

## 1. VO₂ Max (Maximal Oxygen Uptake)

- **Definition**: Maximum volume of oxygen (in mL/kg/min) that the body can utilize during incremental exercise.
- **Engine Location**: `libGoMoreEdgeKit.so` (`vo2Max` symbol).
- **Required Inputs**:
  - Outdoor Running Workout GPS Track (Speed / Pace).
  - Continuous Workout Heart Rate Stream (`HEART_RATE_WORKOUT`).
  - User Biometrics (Age, Sex, Weight, Height, Max HR).
- **Algorithm**: Submaximal Heart Rate vs Speed Linear Regression model.
- **Minimum Criteria**: Requires at least 10 minutes of uninterrupted outdoor running above 70% HRR with valid GPS lock.

---

## 2. Exercise Load (Training Load)

- **Definition**: Cumulative physiological strain imparted on the body by a workout session.
- **Engine Location**: `libGoMoreEdgeKit.so` (`get_training_load` symbol).
- **Algorithm**: Modified Banister TRIMP (Training Impulse) equation:

$$\text{Training Load} = D_{\text{min}} \times \left( \frac{HR_{\text{avg}} - HR_{\text{rest}}}{HR_{\text{max}} - HR_{\text{rest}}} \right) \times 0.64 \times e^{1.92 \times \left( \frac{HR_{\text{avg}} - HR_{\text{rest}}}{HR_{\text{max}} - HR_{\text{rest}}} \right)}$$

---

## 3. Recovery Time (Hours)

- **Definition**: Recommended rest duration (0 to 96 hours) before undertaking another high-intensity training session.
- **Engine Location**: `libGoMoreEdgeKit.so` (`staminaLevel` & `get_phrz`).
- **Algorithm**: Exponential decay function of cumulative 7-day acute training load vs chronic training load ratio.
