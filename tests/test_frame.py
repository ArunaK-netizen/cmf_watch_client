"""
Unit tests for protocol framing and chunk reassembly.
"""

import pytest
from cmf_watch_client.protocol import CmfOpcode, FrameAssembler, build_frames


def test_build_and_parse_plaintext_frame():
    opcode = CmfOpcode.AUTH_PAIR_REQUEST
    payload = b"SamplePlaintextPayloadForPairingChallenge"

    frames = build_frames(opcode.cmd1, opcode.cmd2, payload, session_key=None, mtu=247)
    assert len(frames) == 1

    assembler = FrameAssembler()
    res = assembler.process_raw_notification(frames[0], session_key=None)
    assert res is not None
    res_opcode, res_payload = res
    assert res_opcode == opcode
    assert res_payload[:len(payload)] == payload


def test_build_and_parse_encrypted_frame():
    opcode = CmfOpcode.TIME
    payload = b"\x00\x00\x00\x01\x00\x00\x00\x00"
    session_key = b"1234567890123456"

    frames = build_frames(opcode.cmd1, opcode.cmd2, payload, session_key=session_key, mtu=247)
    assert len(frames) == 1

    assembler = FrameAssembler()
    res = assembler.process_raw_notification(frames[0], session_key=session_key)
    assert res is not None
    res_opcode, res_payload = res
    assert res_opcode == opcode
    assert res_payload == payload


def test_multi_chunk_fragmentation():
    opcode = CmfOpcode.ACTIVITY_DATA
    large_payload = b"X" * 300
    session_key = b"1234567890123456"

    frames = build_frames(opcode.cmd1, opcode.cmd2, large_payload, session_key=session_key, mtu=100)
    assert len(frames) > 1

    assembler = FrameAssembler()
    result = None
    for frame in frames:
        res = assembler.process_raw_notification(frame, session_key=session_key)
        if res is not None:
            result = res

    assert result is not None
    res_opcode, res_payload = result
    assert res_opcode == opcode
    assert res_payload == large_payload
