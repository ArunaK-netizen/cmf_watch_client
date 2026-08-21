# CMF / Nothing Watch Health Telemetry Payload Formats

All health notification payload fields are encoded in **Little-Endian** byte order (in contrast to Big-Endian frame headers).
All timestamps represent Unix epoch **seconds** (uint32 Little-Endian).

---

## 1. Metric Payload Encodings

### 1.1 Activity Data — `ACTIVITY_DATA` (`0056 0001`)
Payload consists of N x 32-byte records:
- `offset 0..4`: `timestamp` (uint32 LE, epoch seconds)
- `offset 4..8`: `steps` (uint32 LE)
- `offset 8..12`: `distance` (uint32 LE, meters)
- `offset 12..16`: `calories` (uint32 LE, gram-calories). *Divide by 1000 for kcal.*
- `offset 16..32`: Reserved / padding (16 bytes, zero)

### 1.2 Heart Rate — `HEART_RATE_MANUAL_AUTO` (`0053 0001`) & `HEART_RATE_WORKOUT` (`00E0 0001`)
Payload consists of N x 8-byte records:
- `offset 0..4`: `timestamp` (uint32 LE, epoch seconds)
- `offset 4..8`: `bpm` (uint32 LE, 30-240 bpm)

### 1.3 Resting Heart Rate — `HEART_RATE_RESTING` (`00DA 0001`)
Payload consists of 5-byte records:
- `offset 0..4`: `timestamp` (uint32 LE, epoch seconds)
- `offset 4`: `hr` (uint8, bpm)

### 1.4 Blood Oxygen — `SPO2` (`0055 0001`)
Payload consists of N x 8-byte records:
- `offset 0..4`: `timestamp` (uint32 LE, epoch seconds)
- `offset 4..8`: `spo2` (uint32 LE, percentage 50-100%)

### 1.5 Stress Level — `STRESS` (`009D 0001`)
Payload consists of N x 8-byte records:
- `offset 0..4`: `timestamp` (uint32 LE, epoch seconds)
- `offset 4..8`: `stress_score` (uint32 LE, range 1-99)
  - 1–29: Relaxed
  - 30–59: Normal
  - 60–79: Medium
  - 80–99: High

### 1.6 Sleep Telemetry — `SLEEP_DATA` (`0058 0001`)
Payload = 18-byte Header + N x 8-byte Stage Records:

**Header (18 bytes LE)**:
- `offset 0..4`: `session_start` (uint32 LE, epoch seconds)
- `offset 4..8`: `wakeup_time` (uint32 LE, epoch seconds)
- `offset 8..10`: `total_deep_seconds` (uint16 LE)
- `offset 10..12`: `total_light_seconds` (uint16 LE)
- `offset 12..14`: `total_rem_seconds` (uint16 LE)
- `offset 14..16`: `total_awake_seconds` (uint16 LE)
- `offset 16..18`: `metadata` (2 bytes)

**Stage Record (8 bytes LE)**:
- `offset 0..4`: `stage_start` (uint32 LE, epoch seconds)
- `offset 4..6`: `duration` (uint16 LE, seconds or minutes)
- `offset 6..8`: `stage_code` (uint16 LE: 1=Deep, 2=Light, 3=REM, 4=Awake)

### 1.7 Workout GPS Track — `WORKOUT_GPS` (`FFFF A05A`)
Payload consists of N x 12-byte records:
- `offset 0..4`: `timestamp` (int32 LE, epoch seconds)
- `offset 4..8`: `longitude` (int32 LE, deg x 10^7). *Longitude is first.*
- `offset 8..12`: `latitude` (int32 LE, deg x 10^7)
