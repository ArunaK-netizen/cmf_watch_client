"""
Unit tests for high-level CmfWatchClient initialization and error states.
"""

import asyncio
import pytest
from cmf_watch_client import CmfWatchClient
from cmf_watch_client.exceptions import CmfAuthError


def test_client_initialization():
    client = CmfWatchClient("3C:B0:ED:3F:BA:70", phone_model="TestClient")
    assert client.target == "3C:B0:ED:3F:BA:70"
    assert client.phone_model == "TestClient"
    assert client.is_authenticated is False


def test_authenticate_without_authkey():
    client = CmfWatchClient("3C:B0:ED:3F:BA:70", authkey=None)
    with pytest.raises(CmfAuthError):
        asyncio.run(client.authenticate_session())
