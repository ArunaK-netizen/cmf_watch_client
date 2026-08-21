"""
Key derivation and signature functions for Nothing X / CMF Watch authentication.
"""

import hashlib
import os


def generate_random_bytes(length: int = 16) -> bytes:
    """Generate cryptographically secure random bytes."""
    return os.urandom(length)


def sign_challenge(rnd_bytes: bytes, secret: bytes) -> bytes:
    """
    Compute SHA-256 signature over random bytes concatenated with secret.
    used in AUTH_PAIR_REQUEST and AUTH_PAIR_REPLY verification.

    signature = SHA256(rnd_bytes || secret)
    """
    hasher = hashlib.sha256()
    hasher.update(rnd_bytes)
    hasher.update(secret)
    return hasher.digest()


def derive_auth_key(rnd1: bytes, rnd2: bytes, secret: bytes) -> bytes:
    """
    Derive 16-byte long-term pairing key (K1 / authkey) persisted across sessions.

    authkey = SHA256(rnd1 || rnd2 || secret)[0..16]
    """
    if len(rnd1) != 16 or len(rnd2) != 16:
        raise ValueError(f"rnd1 and rnd2 must be 16 bytes each (got {len(rnd1)}, {len(rnd2)})")
    
    hasher = hashlib.sha256()
    hasher.update(rnd1)
    hasher.update(rnd2)
    hasher.update(secret)
    return hasher.digest()[:16]


def derive_session_key(nonce: bytes, authkey: bytes) -> bytes:
    """
    Derive per-connection 16-byte session key from watch nonce and authkey.

    sessionKey = SHA256(nonce || authkey)[0..16]
    """
    if len(authkey) != 16:
        raise ValueError(f"authkey must be 16 bytes (got {len(authkey)})")
    
    hasher = hashlib.sha256()
    hasher.update(nonce)
    hasher.update(authkey)
    return hasher.digest()[:16]
