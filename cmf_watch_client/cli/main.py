"""
CLI Entry Point for CMF Watch Client (`cmf-watch-client` / `python -m cmf_watch_client.cli`).
"""

import argparse
import asyncio
import json
import logging
import os
import sys
from typing import Optional

from ..ble.client import CmfWatchClient
from ..storage.json_exporter import JSONStorageExporter

CONFIG_FILE = "cmf_config.json"

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger("cmf_watch_cli")


def load_config() -> dict:
    if os.path.exists(CONFIG_FILE):
        try:
            with open(CONFIG_FILE, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            logger.warning(f"Could not load config file '{CONFIG_FILE}': {e}")
    return {}


def save_config(cfg: dict) -> None:
    with open(CONFIG_FILE, "w", encoding="utf-8") as f:
        json.dump(cfg, f, indent=2)
    logger.info(f"Saved configuration to '{CONFIG_FILE}'")


async def cmd_scan(args) -> None:
    """Scan for CMF / Nothing smartwatch peripherals."""
    device = await CmfWatchClient.discover(timeout=args.timeout, name_prefix=args.prefix)
    if device:
        print(f"\n[+] Found Watch: Name='{device.name}', Address='{device.address}'")
        cfg = load_config()
        cfg["mac_address"] = device.address
        cfg["name"] = device.name
        save_config(cfg)
    else:
        print("\n[-] No CMF smartwatch found during scan window.")


async def cmd_pair(args) -> None:
    """Perform first-time pairing via shell AT GETSECRET and save authkey."""
    cfg = load_config()
    mac = args.mac or cfg.get("mac_address")
    if not mac:
        print("[-] MAC address required. Run 'cmf-watch-client scan' first or specify --mac MAC")
        sys.exit(1)

    client = CmfWatchClient(mac)
    connected = await client.connect(timeout=15.0)
    if not connected:
        print(f"[-] Failed to connect to {mac}")
        sys.exit(1)

    try:
        authkey = await client.pair_first_time()
        authkey_hex = authkey.hex()
        print(f"\n[+] Pairing successful! Derived authkey: {authkey_hex}")

        cfg["mac_address"] = mac
        cfg["authkey"] = authkey_hex
        save_config(cfg)

        await client.authenticate_session()
        level, charging = await client.fetch_battery()
        print(f"[+] Connected session active. Battery: {level}%")

    finally:
        await client.disconnect()


async def cmd_sync(args) -> None:
    """Connect to watch, authenticate session, set time, and trigger health sync."""
    cfg = load_config()
    mac = args.mac or cfg.get("mac_address")
    authkey_hex = args.authkey or cfg.get("authkey")

    if not mac:
        print("[-] MAC address required. Run 'cmf-watch-client scan' first or specify --mac MAC")
        sys.exit(1)
    if not authkey_hex:
        print("[-] Authkey required. Run 'cmf-watch-client pair' first or specify --authkey KEY")
        sys.exit(1)

    authkey = bytes.fromhex(authkey_hex)
    client = CmfWatchClient(mac, authkey=authkey)

    connected = await client.connect(timeout=15.0)
    if not connected:
        print(f"[-] Failed to connect to {mac}")
        sys.exit(1)

    try:
        await client.authenticate_session()
        level, charging = await client.fetch_battery()
        if level > 0:
            print(f"[+] Authenticated session established. Battery: {level}%")
        else:
            print("[+] Authenticated session established.")

        telemetry = await client.sync_health_data(timeout=args.duration)

        out_path = args.out or "health_sync_latest.json"
        exporter = JSONStorageExporter(filepath=out_path)

        metadata = {
            "mac_address": mac,
            "device_name": cfg.get("name", "CMF Watch"),
            "synced_at": telemetry["activity"][0].timestamp.isoformat() if telemetry["activity"] else None,
            "battery": {"level": level, "charging": charging},
        }

        saved_path = exporter.export(telemetry, metadata)

        print(f"\n[+] Health Synchronization Complete! Telemetry exported to '{saved_path}'")
        print(f"    - Activity Intervals: {len(telemetry['activity'])}")
        print(f"    - Heart Rate Samples: {len(telemetry['heart_rate'])}")
        print(f"    - SpO2 Samples:       {len(telemetry['spo2'])}")
        print(f"    - Stress Samples:     {len(telemetry['stress'])}")
        print(f"    - Sleep Sessions:     {len(telemetry['sleep'])}")
        print(f"    - Workouts:           {len(telemetry['workout_summary'])}")

    finally:
        await client.disconnect()


def main():
    parser = argparse.ArgumentParser(description="CMF / Nothing Smartwatch Production Client")
    subparsers = parser.add_subparsers(dest="command", required=True)

    # Scan command
    p_scan = subparsers.add_parser("scan", help="Scan for nearby CMF Watch peripherals")
    p_scan.add_argument("--timeout", type=float, default=10.0, help="Scan timeout in seconds")
    p_scan.add_argument("--prefix", type=str, default="CMF Watch", help="Device name prefix filter")

    # Pair command
    p_pair = subparsers.add_parser("pair", help="First-time pairing via shell AT GETSECRET")
    p_pair.add_argument("--mac", type=str, help="Target Bluetooth MAC address")

    # Sync command
    p_sync = subparsers.add_parser("sync", help="Synchronize historical health & workout telemetry")
    p_sync.add_argument("--mac", type=str, help="Target Bluetooth MAC address")
    p_sync.add_argument("--authkey", type=str, help="16-byte pairing key in hex")
    p_sync.add_argument("--duration", type=float, default=20.0, help="Sync notification window duration in seconds")
    p_sync.add_argument("--out", type=str, default="health_sync_latest.json", help="Output JSON filepath")

    args = parser.parse_args()

    if args.command == "scan":
        asyncio.run(cmd_scan(args))
    elif args.command == "pair":
        asyncio.run(cmd_pair(args))
    elif args.command == "sync":
        asyncio.run(cmd_sync(args))


if __name__ == "__main__":
    main()
