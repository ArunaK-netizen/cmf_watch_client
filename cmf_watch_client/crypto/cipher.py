"""
AES-128-CBC Cipher Module with fixed IV and PKCS7 padding for CMF / Nothing Smartwatches.
"""

from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.primitives import padding
from ..exceptions import CmfProtocolError

# Fixed initialization vector extracted from Nothing X APK decompilation & firmware
AES_IV = bytes([
    0x50, 0x51, 0x52, 0x53, 0x54, 0x55, 0x56, 0x57,
    0x60, 0x61, 0x62, 0x63, 0x64, 0x65, 0x66, 0x5A
])


def pad_pkcs7(data: bytes, block_size: int = 128) -> bytes:
    """Apply PKCS7 padding to data buffer."""
    padder = padding.PKCS7(block_size).padder()
    return padder.update(data) + padder.finalize()


def unpad_pkcs7(padded_data: bytes, block_size: int = 128) -> bytes:
    """Remove PKCS7 padding from data buffer."""
    try:
        unpadder = padding.PKCS7(block_size).unpadder()
        return unpadder.update(padded_data) + unpadder.finalize()
    except Exception as e:
        raise CmfProtocolError(f"Failed to unpad PKCS7 payload: {e}") from e


def encrypt_aes_cbc(data: bytes, key: bytes, iv: bytes = AES_IV) -> bytes:
    """
    Encrypt plaintext data using AES-128-CBC with PKCS7 padding and fixed IV.

    Args:
        data: Plaintext payload to encrypt.
        key: 16-byte AES secret key.
        iv: 16-byte initialization vector (defaults to AES_IV).

    Returns:
        Encrypted ciphertext bytes.
    """
    if len(key) != 16:
        raise ValueError(f"AES key must be 16 bytes, got {len(key)} bytes")
    if len(iv) != 16:
        raise ValueError(f"AES IV must be 16 bytes, got {len(iv)} bytes")

    padded_data = pad_pkcs7(data)
    cipher = Cipher(algorithms.AES(key), modes.CBC(iv))
    encryptor = cipher.encryptor()
    return encryptor.update(padded_data) + encryptor.finalize()


def decrypt_aes_cbc(ciphertext: bytes, key: bytes, iv: bytes = AES_IV) -> bytes:
    """
    Decrypt ciphertext using AES-128-CBC with PKCS7 padding and fixed IV.

    Args:
        ciphertext: Ciphertext bytes to decrypt.
        key: 16-byte AES secret key.
        iv: 16-byte initialization vector (defaults to AES_IV).

    Returns:
        Decrypted plaintext payload.
    """
    if len(key) != 16:
        raise ValueError(f"AES key must be 16 bytes, got {len(key)} bytes")
    if len(iv) != 16:
        raise ValueError(f"AES IV must be 16 bytes, got {len(iv)} bytes")
    if len(ciphertext) == 0 or len(ciphertext) % 16 != 0:
        raise CmfProtocolError(f"Ciphertext length must be a non-zero multiple of 16, got {len(ciphertext)} bytes")

    try:
        cipher = Cipher(algorithms.AES(key), modes.CBC(iv))
        decryptor = cipher.decryptor()
        padded_plaintext = decryptor.update(ciphertext) + decryptor.finalize()
        return unpad_pkcs7(padded_plaintext)
    except Exception as e:
        raise CmfProtocolError(f"AES-128-CBC decryption failed: {e}") from e
