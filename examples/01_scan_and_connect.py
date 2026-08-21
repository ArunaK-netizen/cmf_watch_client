"""
Example 01: Scan for CMF Smartwatches over BLE.
"""

import asyncio
import logging
from cmf_watch_client import CmfWatchClient

logging.basicConfig(level=logging.INFO)


async def main():
    print("[*] Scanning for nearby CMF / Nothing smartwatch peripherals...")
    device = await CmfWatchClient.discover(timeout=10.0)

    if device:
        print(f"\n[+] Found Device:")
        print(f"    - Name:    {device.name}")
        print(f"    - Address: {device.address}")
        print(f"    - RSSI:    {device.rssi} dBm")
    else:
        print("\n[-] No CMF smartwatch found within scan window.")


if __name__ == "__main__":
    asyncio.run(main())
