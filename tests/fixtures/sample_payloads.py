"""
Sanitized synthetic binary test vectors and fixtures for protocol testing.
Contains zero real user data or sensitive secrets.
"""

import struct

# Sanitized 32-byte activity record (ts=1700000000, steps=1000, dist=800m, cal=35000)
SYNTHETIC_ACTIVITY_RECORD = struct.pack("<IIII", 1700000000, 1000, 800, 35000) + (b"\x00" * 16)

# Sanitized 8-byte heart rate record (ts=1700000000, bpm=75)
SYNTHETIC_HEART_RATE_RECORD = struct.pack("<II", 1700000000, 75)

# Sanitized 5-byte resting heart rate record (ts=1700000000, hr=60)
SYNTHETIC_RESTING_HR_RECORD = struct.pack("<IB", 1700000000, 60)

# Sanitized 8-byte SpO2 record (ts=1700000000, spo2=99%)
SYNTHETIC_SPO2_RECORD = struct.pack("<II", 1700000000, 99)

# Sanitized 8-byte stress record (ts=1700000000, score=20)
SYNTHETIC_STRESS_RECORD = struct.pack("<II", 1700000000, 20)

# Sanitized sleep session (18-byte header + 8-byte stage record)
SYNTHETIC_SLEEP_HEADER = struct.pack("<IIHHHHH", 1700000000, 1700028800, 7200, 14400, 3600, 3600, 0)
SYNTHETIC_SLEEP_STAGE = struct.pack("<IHH", 1700000000, 120, 1)  # Deep sleep stage
SYNTHETIC_SLEEP_PAYLOAD = SYNTHETIC_SLEEP_HEADER + SYNTHETIC_SLEEP_STAGE

# Sanitized 12-byte workout GPS coordinate record (ts=1700000000, lon=0.0, lat=0.0)
SYNTHETIC_GPS_RECORD = struct.pack("<iii", 1700000000, 0, 0)
