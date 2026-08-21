"""
Exception hierarchy for CMF Watch Client.
"""


class CmfError(Exception):
    """Base exception for all CMF Watch Client errors."""
    pass


class CmfConnectionError(CmfError):
    """Raised when Bluetooth LE scanning, GATT connection, or service discovery fails."""
    pass


class CmfAuthError(CmfError):
    """Raised when pairing, challenge signature verification, or session authentication fails."""
    pass


class CmfProtocolError(CmfError):
    """Raised when frame deframing, CRC32 verification, or header parsing fails."""
    pass


class CmfTimeoutError(CmfError):
    """Raised when a command response or notification stream times out."""
    pass
