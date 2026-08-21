"""
Framing, fragmentation, MTU chunking, and deframing for CMF Watch BLE protocol.
"""

import math
import struct
from typing import Dict, List, Optional, Tuple, Union

from ..crypto.cipher import decrypt_aes_cbc, encrypt_aes_cbc
from ..crypto.crc import calculate_crc32_bytes, verify_crc32
from ..exceptions import CmfProtocolError
from .opcodes import CmfOpcode

PAYLOAD_HEADER = 0xF5


class CmfFrame:
    """Represents a decoded protocol frame chunk."""

    def __init__(
        self,
        cmd1: int,
        cmd2: int,
        chunk_count: int,
        chunk_index: int,
        payload: bytes,
    ):
        self.cmd1 = cmd1
        self.cmd2 = cmd2
        self.chunk_count = chunk_count
        self.chunk_index = chunk_index
        self.payload = payload
        self.opcode = CmfOpcode.from_codes(cmd1, cmd2)

    def __repr__(self) -> str:
        return (
            f"<CmfFrame opcode={self.opcode} ({self.cmd1:#06x}, {self.cmd2:#06x}) "
            f"chunk={self.chunk_index}/{self.chunk_count} len={len(self.payload)}>"
        )


def build_frames(
    cmd1: int,
    cmd2: int,
    payload: bytes,
    session_key: Optional[bytes] = None,
    mtu: int = 247,
) -> List[bytes]:
    """
    Construct framed BLE write packets (one or more chunks) for command (cmd1, cmd2).

    Args:
        cmd1: 16-bit uint opcode prefix.
        cmd2: 16-bit uint opcode suffix.
        payload: Command payload bytes.
        session_key: 16-byte session key for encrypted commands (None for plaintext).
        mtu: Link Layer MTU (default 247).

    Returns:
        List of raw bytes to write sequentially to the BLE write characteristic.
    """
    opcode = CmfOpcode.from_codes(cmd1, cmd2)
    is_plaintext = isinstance(opcode, CmfOpcode) and opcode.is_plaintext or (session_key is None)

    max_write = max(23, mtu - 3)
    header_len = 11

    if is_plaintext or session_key is None:
        max_chunk_payload = max_write - header_len - 4
        if max_chunk_payload <= 0:
            max_chunk_payload = 1
        num_chunks = math.ceil(len(payload) / max_chunk_payload) if payload else 1
    else:
        # Encrypted chunks must land on 16-byte AES block boundaries
        max_encrypted_block = ((max_write - header_len) // 16) * 16
        max_chunk_payload = max_encrypted_block - 4 - 1  # 4B CRC + >=1B PKCS7 padding
        if max_chunk_payload <= 0:
            max_chunk_payload = 1
        num_chunks = math.ceil(len(payload) / max_chunk_payload) if payload else 1

    frames = []

    if len(payload) == 0:
        header = struct.pack(">BHHHHH", PAYLOAD_HEADER, 0, cmd1, 1, 1, cmd2)
        frames.append(header)
        return frames

    for i in range(num_chunks):
        start = i * max_chunk_payload
        end = min(start + max_chunk_payload, len(payload))
        chunk_slice = payload[start:end]
        crc_bytes = calculate_crc32_bytes(chunk_slice)
        raw_body = chunk_slice + crc_bytes

        if is_plaintext or session_key is None:
            chunk_body = raw_body
        else:
            chunk_body = encrypt_aes_cbc(raw_body, session_key)

        chunk_len = len(chunk_body)
        header = struct.pack(
            ">BHHHHH",
            PAYLOAD_HEADER,
            chunk_len,
            cmd1,
            num_chunks,
            i + 1,  # 1-based index
            cmd2,
        )
        frames.append(header + chunk_body)

    return frames


class FrameAssembler:
    """
    Buffers and reassembles incoming notification frame chunks for multi-part commands.
    """

    def __init__(self):
        self._buffers: Dict[Tuple[int, int], Dict[str, Union[int, bytearray]]] = {}

    def process_raw_notification(
        self,
        raw_bytes: bytes,
        session_key: Optional[bytes] = None,
    ) -> Optional[Tuple[Union[CmfOpcode, Tuple[int, int]], bytes]]:
        """
        Process incoming notification packet from FFF1 characteristic.

        Returns:
            Tuple of (opcode, full_reassembled_payload) when a complete command message
            has arrived, or None if awaiting further chunks.
        """
        if len(raw_bytes) < 11:
            raise CmfProtocolError(f"Raw notification byte length {len(raw_bytes)} is less than 11-byte header")

        header, payload_len, cmd1, chunk_count, chunk_index, cmd2 = struct.unpack(
            ">BHHHHH", raw_bytes[:11]
        )

        if header != PAYLOAD_HEADER:
            raise CmfProtocolError(f"Invalid frame header byte {header:#04x}, expected {PAYLOAD_HEADER:#04x}")

        chunk_data = raw_bytes[11 : 11 + payload_len]
        opcode = CmfOpcode.from_codes(cmd1, cmd2)
        is_plaintext = isinstance(opcode, CmfOpcode) and opcode.is_plaintext or (session_key is None)

        if payload_len > 0:
            if is_plaintext or session_key is None:
                # Plaintext frame payload (the watch does not transmit trailing CRC bytes in plaintext notifications)
                payload = chunk_data
            else:
                # Encrypted chunk frame
                decrypted = decrypt_aes_cbc(chunk_data, session_key)
                if len(decrypted) < 4:
                    raise CmfProtocolError(f"Decrypted payload length {len(decrypted)} is smaller than 4-byte CRC")
                payload = decrypted[:-4]
                expected_crc = decrypted[-4:]
                if not verify_crc32(payload, expected_crc):
                    raise CmfProtocolError(f"CRC verification failed for opcode ({cmd1:#06x}, {cmd2:#06x})")
        else:
            payload = b""

        # Single chunk command shortcut
        if chunk_count == 1:
            return opcode, payload

        # Multi-chunk reassembly
        key = (cmd1, cmd2)
        if key not in self._buffers or chunk_index == 1:
            self._buffers[key] = {"expected": 1, "data": bytearray()}

        buf = self._buffers[key]
        if chunk_index != buf["expected"]:
            # Desynchronized chunk sequence; reset buffer
            buf["expected"] = 1
            buf["data"] = bytearray()
            if chunk_index != 1:
                return None

        buf["data"].extend(payload)
        buf["expected"] = chunk_index + 1

        if chunk_index == chunk_count:
            complete_data = bytes(buf["data"])
            del self._buffers[key]
            return opcode, complete_data

        return None
