"""
Unit tests for health telemetry payload decoders using sanitized fixtures.
"""

import pytest
from cmf_watch_client.health import (
    decode_activity_data,
    decode_heart_rate,
    decode_resting_heart_rate,
    decode_sleep_data,
    decode_spo2,
    decode_stress,
    decode_workout_gps,
)
from tests.fixtures.sample_payloads import (
    SYNTHETIC_ACTIVITY_RECORD,
    SYNTHETIC_GPS_RECORD,
    SYNTHETIC_HEART_RATE_RECORD,
    SYNTHETIC_RESTING_HR_RECORD,
    SYNTHETIC_SLEEP_PAYLOAD,
    SYNTHETIC_SPO2_RECORD,
    SYNTHETIC_STRESS_RECORD,
)


def test_decode_activity_data():
    samples = decode_activity_data(SYNTHETIC_ACTIVITY_RECORD)
    assert len(samples) == 1
    assert samples[0].steps == 1000
    assert samples[0].distance_meters == 800
    assert samples[0].calories_kcal == 35.0


def test_decode_heart_rate():
    samples = decode_heart_rate(SYNTHETIC_HEART_RATE_RECORD, sample_type="auto")
    assert len(samples) == 1
    assert samples[0].bpm == 75
    assert samples[0].sample_type == "auto"


def test_decode_resting_heart_rate():
    samples = decode_resting_heart_rate(SYNTHETIC_RESTING_HR_RECORD)
    assert len(samples) == 1
    assert samples[0].bpm == 60


def test_decode_spo2():
    samples = decode_spo2(SYNTHETIC_SPO2_RECORD)
    assert len(samples) == 1
    assert samples[0].percentage == 99


def test_decode_stress():
    samples = decode_stress(SYNTHETIC_STRESS_RECORD)
    assert len(samples) == 1
    assert samples[0].score == 20
    assert samples[0].category.value == "relaxed"


def test_decode_sleep_data():
    sessions = decode_sleep_data(SYNTHETIC_SLEEP_PAYLOAD)
    assert len(sessions) == 1
    session = sessions[0]
    assert session.total_deep_seconds == 7200
    assert len(session.stages) == 1
    assert session.stages[0].stage.value == "deep"


def test_decode_workout_gps():
    points = decode_workout_gps(SYNTHETIC_GPS_RECORD)
    assert len(points) == 1
    assert points[0].longitude == 0.0
    assert points[0].latitude == 0.0
