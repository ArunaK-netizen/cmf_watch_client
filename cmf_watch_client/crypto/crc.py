"""
CRC-32 checksum calculation for CMF Watch protocol.
Uses standard zlib/IEEE 802.3 polynomial (0xEDB88320), formatted Little-Endian.
"""

import struct
import zlib


def calculate_crc32(data: bytes) -> int:
    """Calculate 32-bit CRC32 checksum as integer."""
    return zlib.crc32(data) & 0xFFFFFFFF


def calculate_crc32_bytes(data: bytes) -> bytes:
    """Calculate 32-bit CRC32 checksum returned as 4 Little-Endian bytes."""
    crc_int = calculate_crc32(data)
    return struct.pack("<I", crc_int)


def verify_crc32(data: bytes, expected_crc_bytes: bytes) -> bool:
    """
    Verify expected 4-byte Little-Endian CRC32 against calculated CRC32.

    Args:
        data: Payload buffer.
        expected_crc_bytes: 4-byte Little-Endian expected CRC.

    Returns:
        True if CRC matches, False otherwise.
    """
    if len(expected_crc_bytes) != 4:
        return False
    expected_crc = struct.unpack("<I", expected_crc_bytes)[0]
    actual_crc = calculate_crc32(data)
    return actual_crc == expected_crc
