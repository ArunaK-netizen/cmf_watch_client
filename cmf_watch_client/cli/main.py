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
from ..experiment.models import ActivityType, GroundTruthMethod, IntensityLevel
from ..experiment.recorder import ExperimentRecorder
from ..experiment.storage import TrialStorageManager
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
        print("\n[+] Pairing successful! Authkey derived and saved securely.")

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


async def cmd_experiment_start(args) -> None:
    """Execute live interactive experiment data collection session."""
    cfg = load_config()
    mac = args.mac or cfg.get("mac_address")
    authkey_hex = args.authkey or cfg.get("authkey")

    if not mac or not authkey_hex:
        print("[-] Target watch MAC address and authkey required in config or flags.")
        sys.exit(1)

    storage = TrialStorageManager()
    trial_id = storage.get_next_trial_id()

    activity = ActivityType(args.activity) if args.activity else ActivityType.WALK
    intensity = IntensityLevel(args.intensity) if args.intensity else IntensityLevel.NORMAL
    method = GroundTruthMethod(args.method) if args.method else GroundTruthMethod.TRACK

    print("\n========================================================")
    print("      CMF Watch Client — Experiment 01 Recorder          ")
    print("========================================================")
    print(f"Trial ID:              {trial_id}")
    print(f"Activity Type:         {activity.value.upper()}")
    print(f"Pace Intensity:        {intensity.value.upper()}")
    print(f"Ground Truth Method:   {method.value.upper()}")
    if args.target:
        print(f"Target Distance:       {args.target} m")
    print("========================================================\n")

    authkey = bytes.fromhex(authkey_hex)
    client = CmfWatchClient(mac, authkey=authkey)

    print(f"[*] Connecting to watch ({mac})...")
    connected = await client.connect(timeout=15.0)
    if not connected:
        print("[-] Failed to establish GATT connection.")
        sys.exit(1)

    try:
        await client.authenticate_session()
        level, _ = await client.fetch_battery()
        print(f"[+] Authenticated session active. Watch Battery: {level}%\n")

        recorder = ExperimentRecorder(
            client=client,
            trial_id=trial_id,
            activity=activity,
            intensity=intensity,
            ground_truth_method=method,
            target_distance_m=args.target,
            user_height_cm=args.height,
            notes=args.notes,
        )

        input(">>> Press ENTER to START trial recording...")
        recorder.start_recording()
        print(f"\n[+] Recording trial '{trial_id}'... Walk/run your target distance now.")

        input("\n>>> Press ENTER when the physical trial is COMPLETE...")
        
        gt_dist_str = input("\n[?] Enter actual physical Ground-Truth Distance (in meters) [e.g. 400.0]: ").strip()
        gt_dist = float(gt_dist_str) if gt_dist_str else None

        notes_input = input("[?] Enter optional observational notes (or press ENTER to skip): ").strip()
        if notes_input:
            recorder.notes = notes_input

        trial = recorder.stop_recording(ground_truth_distance_m=gt_dist)

        saved_path = storage.save_trial(trial)

        derived = trial.derived_metrics
        raw = trial.raw_observations

        print("\n========================================================")
        print("                  TRIAL SUMMARY RESULT                  ")
        print("========================================================")
        print(f"Trial ID:              {trial.metadata.trial_id}")
        print(f"Active Duration:       {raw.total_duration_seconds:.1f} seconds")
        print(f"Watch Final Steps:     {raw.watch_final_steps} steps")
        print(f"Watch Distance:        {derived.watch_reported_distance_m:.1f} m")
        print(f"Average Cadence:       {derived.average_cadence_spm:.1f} spm")
        print(f"Calculated Mean Stride:{derived.calculated_mean_stride_m:.3f} m")
        if derived.ground_truth_distance_m is not None:
            print(f"Ground-Truth Distance: {derived.ground_truth_distance_m:.1f} m")
            print(f"Baseline Error:        {derived.baseline_error_m:+.2f} m ({derived.baseline_error_percentage:+.2f}%)")
        print(f"\n[+] Trial successfully validated and saved to '{saved_path}'")
        print("========================================================\n")

    except Exception as e:
        logger.error(f"Error during trial recording: {e}")
        raise
    finally:
        await client.disconnect()


def cmd_experiment_list(args) -> None:
    """List all recorded experiment trials."""
    storage = TrialStorageManager()
    trials = storage.list_trials()
    if not trials:
        print("[-] No recorded trials found in experiments directory.")
        return

    print("\n================ RECORDED EXPERIMENT TRIALS ================")
    print(f"{'Trial ID':<12} {'Date':<12} {'Activity':<8} {'Steps':<7} {'Watch (m)':<10} {'Ground Truth':<14} {'Error %':<8}")
    print("-" * 75)
    for t in trials:
        m = t.metadata
        r = t.raw_observations
        d = t.derived_metrics
        dt_str = m.date_time.strftime("%Y-%m-%d")
        gt_str = f"{d.ground_truth_distance_m:.1f} m" if d.ground_truth_distance_m is not None else "N/A"
        err_str = f"{d.baseline_error_percentage:+.1f}%" if d.baseline_error_percentage is not None else "N/A"
        print(f"{m.trial_id:<12} {dt_str:<12} {m.activity.value:<8} {r.watch_final_steps:<7} {d.watch_reported_distance_m:<10.1f} {gt_str:<14} {err_str:<8}")
    print("============================================================\n")


def main():
    parser = argparse.ArgumentParser(description="CMF / Nothing Smartwatch Production Client & Experiment Subsystem")
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

    # Experiment command group
    p_exp = subparsers.add_parser("experiment", help="Experiment data collection subsystem")
    exp_subparsers = p_exp.add_subparsers(dest="exp_command", required=True)

    # experiment start
    p_exp_start = exp_subparsers.add_parser("start", help="Start interactive real-time data collection trial")
    p_exp_start.add_argument("--mac", type=str, help="Target Bluetooth MAC address")
    p_exp_start.add_argument("--authkey", type=str, help="16-byte pairing key in hex")
    p_exp_start.add_argument("--activity", type=str, choices=["walk", "jog", "run", "other"], default="walk")
    p_exp_start.add_argument("--intensity", type=str, choices=["slow", "normal", "fast", "other"], default="normal")
    p_exp_start.add_argument("--method", type=str, choices=["track", "survey_wheel", "measured_route", "other"], default="track")
    p_exp_start.add_argument("--target", type=float, help="Target trial distance in meters")
    p_exp_start.add_argument("--height", type=float, help="User height in cm")
    p_exp_start.add_argument("--notes", type=str, help="Free-form trial notes")

    # experiment list
    p_exp_list = exp_subparsers.add_parser("list", help="List all recorded experiment trials")

    args = parser.parse_args()

    if args.command == "scan":
        asyncio.run(cmd_scan(args))
    elif args.command == "pair":
        asyncio.run(cmd_pair(args))
    elif args.command == "sync":
        asyncio.run(cmd_sync(args))
    elif args.command == "experiment":
        if args.exp_command == "start":
            asyncio.run(cmd_experiment_start(args))
        elif args.exp_command == "list":
            cmd_experiment_list(args)


if __name__ == "__main__":
    main()
