# CMF Watch Companion Navigation & Information Architecture

This document specifies the screen hierarchy, primary bottom navigation destinations, deep-linking structure, and screen transitions for the **CMF Watch Companion Android Application**.

---

## 1. Information Architecture Overview

```text
                               [ MAIN APP SHELL ]
                                       │
            ┌──────────────┬───────────┼───────────┬──────────────┐
            ▼              ▼           ▼           ▼              ▼
        [ HOME ]       [ HEALTH ]  [ ACTIVITY ] [ SLEEP ]     [ DEVICE ]
      (Dashboard)     (HR/HRV/SpO2)  (Workouts)  (Stages)   (Watch/Battery)
            │              │           │           │              │
            └──────────────┴───────────┼───────────┴──────────────┘
                                       │
                                       ▼
                                  [ HISTORY ]
                             (Calendar & Telemetry)
```

---

## 2. Primary Destinations

### 1. HOME (`/home`)
- **Primary Goal**: Immediate "How am I doing today?" overview.
- **Widgets**: Today's Greeting, Daily Steps progress circle, Distance, Active Calories, Heart Rate summary card, Sleep architecture bar, Stress badge, Watch battery pill.

### 2. HEALTH (`/health`)
- **Primary Goal**: In-depth cardiovascular & autonomic health analysis.
- **Sections**:
  - Heart Rate (Current, Resting, Min/Max, 24h interactive graph).
  - Heart Rate Variability (HRV ms $rMSSD$, baseline trend, nocturnal deviation).
  - Stress Index (Normalized 1–99 index, 24h stress timeline).
  - SpO₂ Saturation (Spot readings, nocturnal saturation graph).

### 3. ACTIVITY (`/activity`)
- **Primary Goal**: Daily actigraphy, step metrics, active minutes, and workouts.
- **Sections**: Step distribution, Cadence analysis, Workout history list, Workout detail view (GPS track map, Heart Rate zones, Pace split table).

### 4. SLEEP (`/sleep`)
- **Primary Goal**: Sleep quality and nocturnal recovery analysis.
- **Sections**: Sleep duration vs target, Horizontal Sleep Stage Timeline (Deep, Light, REM, Awake), Sleep consistency score, Nocturnal HR & SpO₂ overlays.

### 5. DEVICE (`/device`)
- **Primary Goal**: Watch hardware management, battery telemetry, sync control.
- **Sections**: Watch connection card, Battery level % & charging state, Firmware version, Last sync timestamp, "Sync Now" action button, Diagnostic logs (redacted keys).
