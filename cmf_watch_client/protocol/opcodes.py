"""
Protocol command opcodes (cmd1, cmd2) for CMF / Nothing smartwatch BLE communication.
"""

from enum import Enum
from typing import Tuple, Union


class CmfOpcode(Enum):
    """Command pair (cmd1, cmd2) enumeration."""
    # Session / Device Info
    TIME = (0xFFFF, 0x8004)
    FIRMWARE_VERSION_GET = (0xFFFF, 0x8006)
    FIRMWARE_VERSION_RET = (0xFFFF, 0x0006)
    SERIAL_NUMBER_GET = (0x00DE, 0x0002)
    SERIAL_NUMBER_RET = (0x00DE, 0x0001)
    BATTERY = (0x005C, 0x0001)
    BATTERY_GET = (0x005C, 0x0002)
    TRIGGER_SYNC = (0x005C, 0x0002)
    USER_INFO_SET = (0x0095, 0x0001)
    GOALS_SET = (0x005E, 0x0001)

    # Authentication Handshake
    AUTH_PAIR_REQUEST = (0xFFFF, 0x8047)
    AUTH_PAIR_REPLY = (0xFFFF, 0x0048)
    AUTH_PHONE_NAME = (0xFFFF, 0x8049)
    AUTH_WATCH_MAC = (0xFFFF, 0x0049)
    AUTH_NONCE_REQUEST = (0xFFFF, 0x804B)
    AUTH_NONCE_REPLY = (0xFFFF, 0x004C)
    AUTHENTICATED_CONFIRM_REQUEST = (0xFFFF, 0x804D)
    AUTHENTICATED_CONFIRM_REPLY = (0xFFFF, 0x0004)
    AUTH_FAILED = (0xFFFF, 0xA061)

    # Health & Activity Data Synchronization
    ACTIVITY_FETCH_1 = (0xFFFF, 0x8005)
    ACTIVITY_FETCH_2 = (0xFFFF, 0x9057)
    ACTIVITY_FETCH_ACK_1 = (0xFFFF, 0x0005)
    ACTIVITY_FETCH_ACK_2 = (0xFFFF, 0xA057)
    ACTIVITY_DATA = (0x0056, 0x0001)
    HEART_RATE_MANUAL_AUTO = (0x0053, 0x0001)
    HEART_RATE_WORKOUT = (0x00E0, 0x0001)
    HEART_RATE_RESTING = (0x00DA, 0x0001)
    SPO2 = (0x0055, 0x0001)
    STRESS = (0x009D, 0x0001)
    SLEEP_DATA = (0x0058, 0x0001)
    WORKOUT_SUMMARY = (0x0057, 0x0001)
    WORKOUT_SUMMARY_V3 = (0x0160, 0x0001)
    WORKOUT_GPS = (0xFFFF, 0xA05A)
    GPS_COORDS = (0xFFFF, 0x906A)

    # Bulk Data Channel (Watchface / Firmware / AGPS)
    DATA_TRANSFER_WATCHFACE_INIT_1_REQUEST = (0xFFFF, 0x8052)
    DATA_TRANSFER_WATCHFACE_INIT_1_REPLY = (0xFFFF, 0x0052)
    DATA_TRANSFER_WATCHFACE_INIT_2_REQUEST = (0xFFFF, 0x9063)
    DATA_TRANSFER_WATCHFACE_INIT_2_REPLY = (0xFFFF, 0xA063)
    DATA_CHUNK_REQUEST_WATCHFACE = (0xFFFF, 0xA064)
    DATA_CHUNK_WRITE_WATCHFACE = (0xFFFF, 0x9064)
    DATA_TRANSFER_WATCHFACE_FINISH_ACK_1 = (0xFFFF, 0xA065)
    DATA_TRANSFER_WATCHFACE_FINISH_ACK_2 = (0xFFFF, 0x9065)

    DATA_TRANSFER_AGPS_INIT_REQUEST = (0xFFFF, 0x905E)
    DATA_TRANSFER_AGPS_INIT_REPLY = (0xFFFF, 0xA05E)
    DATA_CHUNK_REQUEST_AGPS = (0xFFFF, 0xA05F)
    DATA_CHUNK_WRITE_AGPS = (0xFFFF, 0x905F)
    DATA_TRANSFER_AGPS_FINISH_ACK_1 = (0xFFFF, 0xA060)
    DATA_TRANSFER_AGPS_FINISH_ACK_2 = (0xFFFF, 0x9060)

    @property
    def cmd1(self) -> int:
        return self.value[0]

    @property
    def cmd2(self) -> int:
        return self.value[1]

    @property
    def is_plaintext(self) -> bool:
        """
        Check if opcode pair travels in plaintext.
        Plaintext opcodes: AUTH_PAIR_REQUEST, AUTH_PAIR_REPLY,
        DATA_CHUNK_WRITE_WATCHFACE, DATA_CHUNK_WRITE_AGPS.
        """
        return self in (
            CmfOpcode.AUTH_PAIR_REQUEST,
            CmfOpcode.AUTH_PAIR_REPLY,
            CmfOpcode.DATA_CHUNK_WRITE_WATCHFACE,
            CmfOpcode.DATA_CHUNK_WRITE_AGPS,
        )

    @classmethod
    def from_codes(cls, cmd1: int, cmd2: int) -> Union['CmfOpcode', Tuple[int, int]]:
        """Resolve CmfOpcode enum from cmd1, cmd2 integers."""
        for op in cls:
            if op.cmd1 == cmd1 and op.cmd2 == cmd2:
                return op
        return (cmd1, cmd2)
