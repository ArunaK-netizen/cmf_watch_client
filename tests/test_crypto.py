"""
Unit tests for CMF Watch cryptography module.
"""

import pytest
from cmf_watch_client.crypto import (
    AES_IV,
    calculate_crc32,
    calculate_crc32_bytes,
    decrypt_aes_cbc,
    derive_auth_key,
    derive_session_key,
    encrypt_aes_cbc,
    generate_random_bytes,
    sign_challenge,
    verify_crc32,
)


def test_aes_cbc_roundtrip():
    key = b"1234567890123456"
    data = b"Hello, CMF Watch Pro 2! Testing AES-128-CBC encryption."

    ciphertext = encrypt_aes_cbc(data, key)
    assert len(ciphertext) > 0
    assert len(ciphertext) % 16 == 0

    decrypted = decrypt_aes_cbc(ciphertext, key)
    assert decrypted == data


def test_crc32():
    data = b"123456789"
    crc_int = calculate_crc32(data)
    assert crc_int == 0xCBF43926

    crc_bytes = calculate_crc32_bytes(data)
    assert len(crc_bytes) == 4
    assert verify_crc32(data, crc_bytes) is True


def test_key_derivation():
    rnd1 = b"\x01" * 16
    rnd2 = b"\x02" * 16
    secret = b"\x03" * 16

    authkey = derive_auth_key(rnd1, rnd2, secret)
    assert len(authkey) == 16

    nonce = b"\x04" * 16
    session_key = derive_session_key(nonce, authkey)
    assert len(session_key) == 16


def test_challenge_signing():
    rnd = generate_random_bytes(16)
    secret = b"testsecret123456"
    sig = sign_challenge(rnd, secret)
    assert len(sig) == 32
