# Deep-Dive Analysis of `libGoMoreEdgeKit.so`

`libGoMoreEdgeKit.so` is a proprietary C/C++ native shared library bundled in Nothing X, developed by **Bomdic / GoMore Inc.** (a Taiwanese sports science and physiological algorithm provider).

---

## 1. JNI Exports & Java Wrappers

`libGoMoreEdgeKit.so` exposes standard JNI native method entry points called by `com.bomdic.gomoreedgekit.GMBase` and `com.bomdic.gomoreedgekit.GMRunCoach`:

```cpp
// JNI Exported Functions in libGoMoreEdgeKit.so
JNIEXPORT void JNICALL Java_com_bomdic_gomoreedgekit_GMBase_jniCoachUserInfoInit(JNIEnv *env, jobject thiz, jobject user_info);
JNIEXPORT void JNICALL Java_com_bomdic_gomoreedgekit_GMBase_jniSetLeave(JNIEnv *env, jobject thiz, jint level);
JNIEXPORT jobject JNICALL Java_com_bomdic_gomoreedgekit_GMRunCoach_jniGetRunCoach(JNIEnv *env, jobject thiz, jobject lesson);
JNIEXPORT jstring JNICALL Java_com_bomdic_gomoreedgekit_GMBase_jniWellnessSdkVersion(JNIEnv *env, jobject thiz);
JNIEXPORT jint JNICALL Java_com_bomdic_gomoreedgekit_GMBase_jniWellnessSdkBuildSubVersion(JNIEnv *env, jobject thiz);
```

---

## 2. Internal Sports Science & Physiological Algorithm Symbols

Decompilation of string pools and C++ mangled symbols in `libGoMoreEdgeKit.so` reveals the core sports science algorithms:

### 2.1 Personalized Heart Rate Zones (`get_phrz`)
- **Symbol**: `get_phrz`
- **Function**: Calculates custom Heart Rate Zone (HRZ 1 to HRZ 5) boundaries based on resting HR (`restingHR`, `restHRBound`), max HR (`maxHR`), and Rating of Perceived Exertion (`RPE2HRZ_lb_t`, `RPE2HRZ_ub_t`, `rpeTohr_0321_v2`).
- **Zone Boundaries**:
  - Warm Up / Cool Down: `warmUp2HRBound`, `coolDownHRBound`
  - Steady / Aerobic: `hrBounds`
  - Intense / Anaerobic: `intenseHRBound`

### 2.2 Training Load Calculation (`get_training_load`)
- **Symbol**: `get_training_load`
- **Function**: Computes cumulative physiological strain (TRIMP / Training Impulse model) by combining heart rate time-series during a workout with total active workout duration and user profile metrics.

### 2.3 Aerobic Capacity (`vo2Max`)
- **Symbol**: `vo2Max`
- **Function**: Estimates maximal oxygen uptake (mL/kg/min) by analyzing heart rate vs speed/pace ratio during outdoor GPS running workouts.

### 2.4 Stamina Level & Recovery (`staminaLevel`)
- **Symbol**: `staminaLevel`
- **Function**: Computes remaining energy/stamina percentage during workout sessions and total required recovery time (hours) post-workout.

---

## 3. Data Structures & Lesson Models

`libGoMoreEdgeKit.so` interacts with Java data transfer objects:
- `com/bomdic/gomoreedgekit/data/GMCoachUserInfo`: User age, sex, weight (kg), height (cm), max HR, resting HR.
- `com/bomdic/gomoreedgekit/data/GMCoachBound`: HR zone upper and lower bounds.
- `com/bomdic/gomoreedgekit/data/GMRunPlan`: Running training schedules.
- `com/bomdic/gomoreedgekit/data/GMTempoLesson`, `GMProgressionLesson`, `GMIntervalLesson`, `GMSteadyLesson`.
