"""
Decoders for raw CMF Watch health, activity, and workout binary notification payloads.
"""

from datetime import datetime, timezone
import struct
from typing import List

from .models import (
    ActivitySample,
    HeartRateSample,
    RestingHeartRateSample,
    SleepSession,
    SleepStage,
    SleepStageType,
    SpO2Sample,
    StressCategory,
    StressSample,
    WorkoutGpsPoint,
    WorkoutSummary,
)


def _to_datetime(epoch_seconds: int) -> datetime:
    """Convert Unix epoch seconds to UTC datetime."""
    return datetime.fromtimestamp(epoch_seconds, tz=timezone.utc)


def decode_activity_data(payload: bytes) -> List[ActivitySample]:
    """
    Decode ACTIVITY_DATA payload (0x0056 / 0x0001).
    Payload consists of N x 32-byte Little-Endian records.
    """
    record_size = 32
    if len(payload) % record_size != 0:
        payload = payload[: (len(payload) // record_size) * record_size]

    samples: List[ActivitySample] = []
    for offset in range(0, len(payload), record_size):
        ts, steps, distance_m, calories_cal = struct.unpack(
            "<IIII", payload[offset : offset + 16]
        )
        samples.append(
            ActivitySample(
                timestamp=_to_datetime(ts),
                steps=steps,
                distance_meters=distance_m,
                calories_kcal=calories_cal / 1000.0,
            )
        )
    return samples


def decode_heart_rate(payload: bytes, sample_type: str = "auto") -> List[HeartRateSample]:
    """
    Decode HEART_RATE_MANUAL_AUTO (0x0053 / 0x0001) or HEART_RATE_WORKOUT (0x00E0 / 0x0001).
    Payload consists of N x 8-byte Little-Endian records (ts: u32, bpm: u32).
    """
    record_size = 8
    samples: List[HeartRateSample] = []
    num_records = len(payload) // record_size

    for i in range(num_records):
        offset = i * record_size
        ts, bpm = struct.unpack("<II", payload[offset : offset + 8])
        if 30 <= bpm <= 240:
            samples.append(
                HeartRateSample(
                    timestamp=_to_datetime(ts),
                    bpm=bpm,
                    sample_type=sample_type,
                )
            )
    return samples


def decode_resting_heart_rate(payload: bytes) -> List[RestingHeartRateSample]:
    """
    Decode HEART_RATE_RESTING payload (0x00DA / 0x0001).
    Supports 5-byte records (ts: u32 LE, hr: u8) or 8-byte records (ts: u32 LE, hr: u32 LE).
    """
    samples: List[RestingHeartRateSample] = []
    if len(payload) % 5 == 0 and len(payload) > 0:
        for offset in range(0, len(payload), 5):
            ts, hr = struct.unpack("<IB", payload[offset : offset + 5])
            if 30 <= hr <= 200:
                samples.append(
                    RestingHeartRateSample(timestamp=_to_datetime(ts), bpm=hr)
                )
    elif len(payload) % 8 == 0 and len(payload) > 0:
        for offset in range(0, len(payload), 8):
            ts, hr = struct.unpack("<II", payload[offset : offset + 8])
            if 30 <= hr <= 200:
                samples.append(
                    RestingHeartRateSample(timestamp=_to_datetime(ts), bpm=hr)
                )
    return samples


def decode_spo2(payload: bytes) -> List[SpO2Sample]:
    """
    Decode SPO2 payload (0x0055 / 0x0001).
    Payload consists of N x 8-byte Little-Endian records (ts: u32, spo2: u32).
    """
    record_size = 8
    samples: List[SpO2Sample] = []
    num_records = len(payload) // record_size

    for i in range(num_records):
        offset = i * record_size
        ts, spo2 = struct.unpack("<II", payload[offset : offset + 8])
        if 50 <= spo2 <= 100:
            samples.append(
                SpO2Sample(
                    timestamp=_to_datetime(ts),
                    percentage=spo2,
                )
            )
    return samples


def decode_stress(payload: bytes) -> List[StressSample]:
    """
    Decode STRESS payload (0x009D / 0x0001).
    Payload consists of N x 8-byte Little-Endian records (ts: u32, score: u32).
    """
    record_size = 8
    samples: List[StressSample] = []
    num_records = len(payload) // record_size

    for i in range(num_records):
        offset = i * record_size
        ts, score = struct.unpack("<II", payload[offset : offset + 8])
        if 1 <= score <= 100:
            samples.append(
                StressSample(
                    timestamp=_to_datetime(ts),
                    score=score,
                    category=StressCategory.from_score(score),
                )
            )
    return samples


def decode_sleep_data(payload: bytes) -> List[SleepSession]:
    """
    Decode SLEEP_DATA payload (0x0058 / 0x0001).
    Header (18 bytes LE):
        0..4: session_start (u32 LE)
        4..8: wakeup_time (u32 LE)
        8..10: total_deep_s (u16 LE)
        10..12: total_core_s (u16 LE)
        12..14: total_rem_s (u16 LE)
        14..16: total_awake_s (u16 LE)
        16..18: metadata (2B)
    Stage Records (8 bytes LE each):
        0..4: stage_start (u32 LE)
        4..6: duration_s (u16 LE)
        6..8: stage_type (u16 LE: 1=Deep, 2=Light, 3=REM, 4=Awake)
    """
    if len(payload) < 18:
        return []

    (
        start_ts,
        wakeup_ts,
        deep_s,
        core_s,
        rem_s,
        awake_s,
        _,
    ) = struct.unpack("<IIHHHHH", payload[:18])

    stage_records: List[SleepStage] = []
    stage_data = payload[18:]
    record_size = 8
    num_stages = len(stage_data) // record_size

    for i in range(num_stages):
        offset = i * record_size
        st_ts, dur_raw, stage_code = struct.unpack("<IHH", stage_data[offset : offset + 8])
        duration_sec = dur_raw * 60 if dur_raw < 500 else dur_raw
        stage_records.append(
            SleepStage(
                timestamp=_to_datetime(st_ts),
                duration_seconds=duration_sec,
                stage=SleepStageType.from_code(stage_code),
            )
        )

    session = SleepSession(
        start_time=_to_datetime(start_ts),
        end_time=_to_datetime(wakeup_ts),
        total_deep_seconds=deep_s,
        total_light_seconds=core_s,
        total_rem_seconds=rem_s,
        total_awake_seconds=awake_s,
        stages=stage_records,
    )
    return [session]


def decode_workout_summary(payload: bytes) -> List[WorkoutSummary]:
    """
    Decode WORKOUT_SUMMARY payload (0x0057 / 0x0001).
    Supports 32-byte records (v1) and 54-byte records (v2/v3).
    """
    summaries: List[WorkoutSummary] = []
    if len(payload) == 0:
        return summaries

    if len(payload) % 32 == 0:
        rec_size = 32
    elif len(payload) % 54 == 0:
        rec_size = 54
    else:
        rec_size = 54 if len(payload) >= 54 else 32

    num_records = len(payload) // rec_size
    for i in range(num_records):
        offset = i * rec_size
        data = payload[offset : offset + rec_size]
        if len(data) >= 31:
            start_ts, dur_s, workout_type = struct.unpack("<IH B", data[:7])
            end_ts = struct.unpack("<I", data[26:30])[0]
            has_gps = data[30] == 1

            summaries.append(
                WorkoutSummary(
                    start_time=_to_datetime(start_ts),
                    end_time=_to_datetime(end_ts),
                    duration_seconds=dur_s,
                    workout_type_code=workout_type,
                    has_gps=has_gps,
                )
            )
    return summaries


def decode_workout_gps(payload: bytes) -> List[WorkoutGpsPoint]:
    """
    Decode WORKOUT_GPS payload (0xFFFF / 0xA05A).
    Each record is 12 bytes Little-Endian: ts(i32), lon_1e7(i32), lat_1e7(i32).
    """
    record_size = 12
    points: List[WorkoutGpsPoint] = []
    num_records = len(payload) // record_size

    for i in range(num_records):
        offset = i * record_size
        ts, lon_1e7, lat_1e7 = struct.unpack("<iii", payload[offset : offset + 12])
        points.append(
            WorkoutGpsPoint(
                timestamp=_to_datetime(ts),
                longitude=lon_1e7 / 1e7,
                latitude=lat_1e7 / 1e7,
            )
        )
    return points
