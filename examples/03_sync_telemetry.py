"""
Example 03: Synchronize Health & Workout Telemetry.
"""

import asyncio
import logging
import sys
from cmf_watch_client import CmfWatchClient, JSONStorageExporter

logging.basicConfig(level=logging.INFO)


async def main():
    if len(sys.argv) < 3:
        print("Usage: python 03_sync_telemetry.py <MAC_ADDRESS> <AUTHKEY_HEX>")
        sys.exit(1)

    mac_address = sys.argv[1]
    authkey_bytes = bytes.fromhex(sys.argv[2])

    client = CmfWatchClient(mac_address, authkey=authkey_bytes)
    print(f"[*] Connecting to {mac_address}...")
    await client.connect()

    try:
        print("[*] Authenticating session...")
        await client.authenticate_session()

        level, charging = await client.fetch_battery()
        print(f"[+] Session Active. Battery: {level}%")

        print("[*] Fetching health telemetry streams...")
        telemetry = await client.sync_health_data(timeout=20.0)

        metadata = {
            "mac_address": mac_address,
            "synced_at": telemetry["activity"][0].timestamp.isoformat() if telemetry["activity"] else None,
            "battery": {"level": level, "charging": charging},
        }

        exporter = JSONStorageExporter("my_health_data.json")
        saved_file = exporter.export(telemetry, metadata)
        print(f"\n[+] Health Telemetry Saved to '{saved_file}'")

    finally:
        await client.disconnect()


if __name__ == "__main__":
    asyncio.run(main())
