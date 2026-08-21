"""
Geospatial calculation utilities for GPS tracks (Haversine formula).
"""

import math
from typing import List, Tuple


def haversine_distance(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    """
    Calculate the great-circle distance between two points on the Earth in meters
    using the Haversine formula.
    """
    R = 6371000.0  # Earth radius in meters

    phi1 = math.radians(lat1)
    phi2 = math.radians(lat2)
    delta_phi = math.radians(lat2 - lat1)
    delta_lambda = math.radians(lon2 - lon1)

    a = (
        math.sin(delta_phi / 2.0) ** 2
        + math.cos(phi1) * math.cos(phi2) * math.sin(delta_lambda / 2.0) ** 2
    )
    c = 2.0 * math.atan2(math.sqrt(a), math.sqrt(1.0 - a))

    return R * c


def calculate_gps_track_distance(coords: List[Tuple[float, float]]) -> float:
    """
    Calculate cumulative distance in meters along an ordered list of (latitude, longitude) tuples.
    """
    if len(coords) < 2:
        return 0.0

    total_dist = 0.0
    for i in range(1, len(coords)):
        lat1, lon1 = coords[i - 1]
        lat2, lon2 = coords[i]
        # Ignore static jitter points (< 0.5m)
        step_dist = haversine_distance(lat1, lon1, lat2, lon2)
        if step_dist >= 0.5:
            total_dist += step_dist

    return total_dist
