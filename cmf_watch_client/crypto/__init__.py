"""
Cryptographic module for CMF / Nothing Watch client.
"""

from .cipher import AES_IV, decrypt_aes_cbc, encrypt_aes_cbc, pad_pkcs7, unpad_pkcs7
from .crc import calculate_crc32, calculate_crc32_bytes, verify_crc32
from .keys import derive_auth_key, derive_session_key, generate_random_bytes, sign_challenge

__all__ = [
    "AES_IV",
    "encrypt_aes_cbc",
    "decrypt_aes_cbc",
    "pad_pkcs7",
    "unpad_pkcs7",
    "calculate_crc32",
    "calculate_crc32_bytes",
    "verify_crc32",
    "generate_random_bytes",
    "sign_challenge",
    "derive_auth_key",
    "derive_session_key",
]
