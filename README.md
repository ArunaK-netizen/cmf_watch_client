# CMF Watch Client (`cmf-watch-client`)

[![Python Version](https://img.shields.io/badge/python-3.9%20%7C%203.10%20%7C%203.11%20%7C%203.12-blue.svg)](https://www.python.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Code Style: PEP8](https://img.shields.io/badge/code%20style-pep8-green.svg)](https://www.python.org/dev/peps/pep-0008/)

**`cmf-watch-client`** is a production-grade Python client, BLE driver, and reverse-engineered protocol library for **CMF by Nothing** smartwatches (including **CMF Watch Pro 2** and **CMF Watch Pro**).

It enables direct, local, encrypted Bluetooth Low Energy communication with your smartwatch—allowing you to pair your device, synchronize historical health/workout telemetry, decode metrics, and export data without relying on Nothing's cloud backend.

---

## 🌟 Key Features

- **Direct BLE Communication**: Connects directly to the watch peripheral over GATT (`0xFFF0`, `77D4E67C`) via `Bleak`.
- **First-Time Pairing**: Automated shell AT channel pairing (`AT GETSECRET`), challenge verification (`SHA256(rnd || secret)`), and long-term `authkey` derivation.
- **Encrypted Session Handshake**: AES-128-CBC encryption with fixed IV (`5051525354555657606162636465665A`), dynamic `sessionKey` derivation via nonce exchange, and `0xF5` 11-byte frame reassembly.
- **Telemetry Decoders**: Decodes raw binary stream buffers into normalized **Pydantic v2** models:
  - 🏃 **Activity & Steps**: Timestamped step counts, distance (meters), and calories (kcal).
  - ❤️ **Heart Rate**: Auto-monitoring, manual spot checks, workout heart rates, and daily resting HR.
  - 🩸 **Blood Oxygen (SpO₂)**: Oxygen saturation level percentages.
  - 🧘 **Stress Levels**: Stress index scores (1–99) categorized into Relaxed, Normal, Medium, and High.
  - 🌙 **Sleep Telemetry**: Sleep session headers with total Deep, Light, REM, and Awake durations + ordered stage intervals.
  - 🚴 **Workouts & GPS**: Active workout summaries and 12-byte coordinate GPS tracks.
- **Extensible Storage**: Pluggable storage architecture (`BaseStorageExporter`) with built-in `JSONStorageExporter`.
- **CLI Utility**: Command-line tool (`cmf-watch-client`) with subcommands `scan`, `pair`, and `sync`.

---

## 📱 Supported Watch Models

| Device Model | Advertised Name Filter | Status | MCU / Platform |
|---|---|---|---|
| **CMF Watch Pro 2** | `CMF Watch Pro 2-XXXX` | **CONFIRMED (FULL SUPPORT)** | Actions Semi ATS3089C |
| **CMF Watch Pro** | `Watch Pro` | **CONFIRMED (FULL SUPPORT)** | Actions Semi ATS3085 |
| **CMF Watch 3 Pro** | `CMF Watch 3 Pro` | **SUPPORTED (Protocol Compatible)** | Actions Semi ATS3089C |

---

## 🚀 Installation

Install from local source:

```bash
git clone https://github.com/joshuapassos/CMF-Watch-Pro-2-BLE-Protocol.git
cd CMF-Watch-Pro-2-BLE-Protocol
pip install -e .
```

For development and test dependencies:

```bash
pip install -e ".[dev]"
```

---

## 💻 Command Line Interface (CLI)

The package installs the `cmf-watch-client` command line tool (also accessible via `python -m cmf_watch_client.cli`).

### 1. Scan for Nearby Smartwatches
```bash
cmf-watch-client scan
```
Scans for advertising watches over BLE and saves the discovered MAC address to `cmf_config.json`.

### 2. Pair Device (First-Time Only)
```bash
cmf-watch-client pair
```
Executes the `AT GETSECRET` shell pairing exchange, verifies the watch challenge signature, derives the 16-byte `authkey`, and saves credentials to `cmf_config.json`.

### 3. Synchronize Telemetry Data
```bash
cmf-watch-client sync --out health_sync_latest.json
```
Establishes an encrypted BLE session, initializes watch time, requests historical telemetry streams (`ACTIVITY_FETCH_1/2`), decodes metrics, and exports a JSON report.

---

## 🐍 Python API Usage

```python
import asyncio
from cmf_watch_client import CmfWatchClient, JSONStorageExporter

async def main():
    mac_address = "3C:B0:ED:3F:BA:70"
    authkey_bytes = bytes.fromhex("cc78921a307df9095405f67d0132a7f4")

    # 1. Initialize Client
    client = CmfWatchClient(mac_address, authkey=authkey_bytes)
    
    # 2. Connect & Authenticate Session
    await client.connect()
    await client.authenticate_session()

    # 3. Fetch Battery State
    battery_level, is_charging = await client.fetch_battery()
    print(f"Watch Battery: {battery_level}% (Charging: {is_charging})")

    # 4. Synchronize Health Telemetry
    telemetry = await client.sync_health_data(timeout=20.0)

    # 5. Process Pydantic Metric Models
    print(f"Decoded {len(telemetry['activity'])} activity interval samples.")
    print(f"Decoded {len(telemetry['heart_rate'])} heart rate samples.")
    print(f"Decoded {len(telemetry['sleep'])} sleep sessions.")

    # 6. Export to Storage Exporter
    exporter = JSONStorageExporter("latest_health_export.json")
    exporter.export(telemetry, {"mac_address": mac_address, "battery_level": battery_level})

    await client.disconnect()

if __name__ == "__main__":
    asyncio.run(main())
```

---

## 🛠️ Protocol Architecture Overview

For full technical specifications, inspect the [`docs/`](docs/) directory:

- [docs/protocol-spec.md](docs/protocol-spec.md): GATT service UUIDs, pairing sequence, and session handshake.
- [docs/packets-and-opcodes.md](docs/packets-and-opcodes.md): `0xF5` 11-byte frame header layout and opcode pair reference.
- [docs/crypto-spec.md](docs/crypto-spec.md): AES-128-CBC fixed IV cipher, CRC32 LE, `authkey`, and `sessionKey` formulas.
- [docs/health-decoders-spec.md](docs/health-decoders-spec.md): Binary Little-Endian metric payload encodings.
- [docs/apk-reversing-notes.md](docs/apk-reversing-notes.md): Static reverse-engineering analysis of Nothing X (v3.7.3).

---

## 🧪 Testing

Run the automated unit test suite with synthetic test vectors:

```bash
python -m pytest tests/
```

---

## 🔒 Security & Privacy

- **Zero Secret Commits**: Real MAC addresses, secret keys, and personal health data are strictly excluded via `.gitignore`.
- **Sanitized Fixtures**: Unit tests operate exclusively on synthetic binary test vectors (`tests/fixtures/sample_payloads.py`).

See [SECURITY.md](SECURITY.md) for full privacy policy details.

---

## 📚 References & Prior Art

This project acknowledges prior reverse-engineering research and open-source contributions:
1. **CMF Watch Pro 2 BLE Protocol Repository**: [joshuapassos/CMF-Watch-Pro-2-BLE-Protocol](https://github.com/joshuapassos/CMF-Watch-Pro-2-BLE-Protocol)
2. **fmc_go — Independent Go Implementation**: [freethinkel/fmc_go](https://github.com/freethinkel/fmc_go)
3. **Gadgetbridge CMF Implementation**: `nodomain.freeyourgadget.gadgetbridge`
4. **CMF Watch Firmware RE**: [whatotter/cmf-watch-firmware](https://github.com/whatotter/cmf-watch-firmware)
5. **Nothing X Smartwatch App RE Write-up**: [Ambraglow Blog](https://ambraglow.org/blog/nothing-x/a-look-into-the-nothing-x-smartwatch-app/)
6. **Official Nothing Product Data Info**: [Nothing Product Data Information](https://nothing.tech/pages/product-data-information)

---

## 📄 License

This project is open-source software licensed under the [MIT License](LICENSE).
