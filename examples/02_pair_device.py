"""
Example 02: Initial First-Time Shell Pairing.
"""

import asyncio
import logging
import sys
from cmf_watch_client import CmfWatchClient

logging.basicConfig(level=logging.INFO)


async def main():
    if len(sys.argv) < 2:
        print("Usage: python 02_pair_device.py <MAC_ADDRESS>")
        sys.exit(1)

    mac_address = sys.argv[1]
    client = CmfWatchClient(mac_address)

    print(f"[*] Connecting to {mac_address}...")
    await client.connect()

    try:
        print("[*] Initiating AT shell pairing...")
        authkey = await client.pair_first_time()
        print(f"\n[+] Pairing Successful!")
        print(f"    Derived 16-byte Authkey (HEX): {authkey.hex()}")

        await client.authenticate_session()
        level, charging = await client.fetch_battery()
        print(f"    Battery Level: {level}% (Charging: {charging})")

    finally:
        await client.disconnect()


if __name__ == "__main__":
    asyncio.run(main())
