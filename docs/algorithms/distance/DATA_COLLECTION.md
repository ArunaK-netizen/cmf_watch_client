# Experiment 01: Distance & Cadence Data Collection Guide

This guide provides practical, step-by-step field instructions for performing **Experiment 01 (Cadence-Adaptive Stride Length & Distance Estimation)** using the `cmf-watch-client` experiment recorder.

---

## 1. Prerequisites & Equipment Setup

### Required Equipment:
1. **CMF Watch Pro / Pro 2** (worn firmly on non-dominant wrist).
2. **Host Device** (Laptop or handheld running Python 3.9+ with Bluetooth enabled).
3. **Gold-Standard Ground-Truth Reference**:
   - Option A: Measured 400m Athletics Track (1 lap = 400.0 m, 4 laps = 1,600.0 m).
   - Option B: Calibrated Surveyor's Measuring Wheel.

---

## 2. Experimental Trial Matrix

To build a statistically representative dataset, collect trials across varying **paces (cadences)** and **activities**:

| Trial Category | Activity | Target Pace / Intensity | Target Distance | Recommended Trials |
|---|---|---|---|---|
| **Slow Walking** | `walk` | `slow` ($\sim 80\text{ spm}$) | 400.0 meters | 3 trials |
| **Normal Walking** | `walk` | `normal` ($\sim 100\text{ spm}$) | 400.0 meters | 3 trials |
| **Brisk Walking** | `walk` | `fast` ($\sim 120\text{ spm}$) | 400.0 meters | 3 trials |
| **Slow Jogging** | `jog` | `slow` ($\sim 140\text{ spm}$) | 400.0 meters | 3 trials |
| **Fast Running** | `run` | `fast` ($\ge 160\text{ spm}$) | 400.0 meters | 3 trials |

---

## 3. Step-by-Step Field Execution Protocol

### Step 1: Pre-Trial Verification
Ensure your watch is paired and `cmf_config.json` is configured:
```bash
cmf-watch-client scan
```

### Step 2: Launch Experiment Recorder
Position yourself exactly at the physical start line of your measured track, then run:
```bash
cmf-watch-client experiment start --activity walk --intensity normal --method track --target 400.0 --height 175.0
```

### Step 3: Start Recording
1. The CLI will establish the GATT connection and display:
   ```text
   >>> Press ENTER to START trial recording...
   ```
2. Press **ENTER** at the precise instant your foot crosses the start line and begin walking/running at a steady pace.

### Step 4: Complete Physical Trial
1. Walk/run continuously without stopping until your foot crosses the target end line (e.g. 400m).
2. Immediately press **ENTER** on your CLI terminal to stop recording.

### Step 5: Log Ground Truth & Notes
1. Prompt for ground truth distance:
   ```text
   [?] Enter actual physical Ground-Truth Distance (in meters) [e.g. 400.0]: 400.0
   ```
2. Enter optional notes (e.g., `"Consistent 102 spm walk, clear weather"`).
3. The trial summary will display watch-reported distance, average cadence, stride length, and baseline percentage error, then save the trial JSON file to `experiments/experiment_01_distance/trials/trial_001.json`.

---

## 4. Guidelines to Prevent Trial Contamination

- **Maintain Steady Cadence**: Avoid stopping to talk or drastically altering speed mid-trial.
- **Natural Wrist Motion**: Do not hold your phone or object in your watch arm during the trial.
- **Accurate Height Metadata**: Ensure `--height` matches your actual height in cm (this drives Nothing's baseline stride math).
- **Trial Isolation**: Rest for 30 seconds between trials to allow watch step buffers to settle.
