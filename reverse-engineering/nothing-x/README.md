# Nothing X / CMF Watch Reverse Engineering & Personal Data API Integration

## 1. Project Objective

The primary objective of this project is to reverse engineer the communication protocol between the **Nothing X Android Application** and **CMF / Nothing Smartwatches** (specifically CMF Watch Pro / CMF Watch Pro 2) over Bluetooth Low Energy (BLE). 

The ultimate goal is **NOT** to modify or patch the official Nothing X application, but to build an independent, lightweight, self-hosted client (Python / Bleak + FastAPI + PostgreSQL) as part of a private **"Personal Life OS"**. This client will:
- Discover and pair with the user's CMF/Nothing smartwatch.
- Establish encrypted BLE communication sessions.
- Trigger and download historical health, activity, and workout telemetry.
- Decode and normalize metrics (steps, heart rate, resting HR, SpO2, stress, sleep stages, workouts, VO2 max, vitality score, battery).
- Store normalized data locally in PostgreSQL and serve it via a clean REST API without relying on Nothing's cloud infrastructure.

---

## 2. Target Watch Model(s)

- **Primary Target Model**: **CMF Watch Pro 2** (Advertised name: `CMF Watch Pro 2-XXXX`, MCU: Actions Semi ATS3089C).
- **Secondary Target Model**: **CMF Watch Pro** (Advertised name: `Watch Pro`).
- **Extensibility Note**: Protocol differences across models (e.g., CMF Watch Pro vs. Pro 2 vs. future models) are isolated into distinct modular drivers (`cmf_watch_pro2`, `cmf_watch_pro`, `common`).

---

## 3. Nothing X APK Inspection & Architecture (Verified Version 3.7.3)

The user provided the official Nothing X APK package: `Nothing+X_3.7.3_APKPure.xapk`.

### 3.1 APK Metadata
- **Package Name**: `com.nothing.smartcenter`
- **Application Name**: `Nothing X`
- **Version Name**: `3.7.3`
- **Version Code**: `3070305`
- **Target SDK**: `36` (Android 16), **Min SDK**: `26` (Android 8.0)
- **Container Structure**: Split APK containing `com.nothing.smartcenter.apk` (152.7 MB base APK with 8 DEX files `classes.dex` .. `classes8.dex`) and `config.armeabi_v7a.apk` (131.5 MB native libraries).

### 3.2 Key Android Permissions
- `android.permission.BLUETOOTH`, `BLUETOOTH_ADMIN`, `BLUETOOTH_CONNECT`, `BLUETOOTH_ADVERTISE`, `BLUETOOTH_SCAN`
- `android.permission.ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`
- `android.permission.health.WRITE_HEART_RATE`, `WRITE_STEPS`, `WRITE_SLEEP`, `WRITE_OXYGEN_SATURATION`, `READ_EXERCISE`, `WRITE_EXERCISE`, `WRITE_RESTING_HEART_RATE`, `WRITE_VO2_MAX`, `WRITE_DISTANCE`, `WRITE_TOTAL_CALORIES_BURNED`
- Custom permissions: `com.nothing.xservice.permission.ACCESS_XSERVICE`, `com.nothing.cmf.watch.permission.PROVIDER_XVIEW_DATA`

### 3.3 Core Class Architecture & Mapping (Nothing X v3.7.3)

| Component Layer | Class / Package Name | Primary Role & Description | Evidence |
|---|---|---|---|
| **BLE SDK Core** | `com.nothing.link.bluetooth.sdk.XBluetoothManager` | Primary Bluetooth lifecycle manager for discovery, GATT connection, and state maintenance. | **CONFIRMED (APK)** |
| **GATT Scanner/Parser** | `com.nothing.link.bluetooth.sdk.scan.parser.NothingWatchParser` | Filters and decodes BLE advertising packets (`CMF Watch Pro 2-XXXX` & `Watch Pro`). | **CONFIRMED (APK)** |
| **BLE Utility & Handshake** | `com.nothing.link.bluetooth.sdk.util.BleUtil` | Utilities for GATT characteristic operations and MTU negotiation. | **CONFIRMED (APK)** |
| **Command Framer** | `com.nothing.link.bluetooth.sdk.connect.tranform.XCommand` | Encapsulates framed opcodes `(cmd1, cmd2)`, 11-byte `0xF5` header generation, and packet payload serialization. | **CONFIRMED (APK)** |
| **ByteArray Framer** | `com.nothing.link.bluetooth.sdk.connect.tranform.XByteArrayParser` | Low-level payload parser and CRC32 / AES block chunk deframer. | **CONFIRMED (APK)** |
| **Service Host Manager** | `com.nothing.xservicecore.CMFWatchServiceManager` | Cross-process Android IPC manager coordinating CMF Watch background service operations. | **CONFIRMED (APK)** |
| **Watch Service Handler** | `com.nothing.xhost.BindWatchXServiceHandler` | Handles binding and lifecycle events between Nothing X UI host and Watch X background service. | **CONFIRMED (APK)** |
| **Flutter Health Bridge** | `com.nothing.nt_sync_health.NtSyncHealthPlugin` | Pigeon interface plugin (`NtSyncHealthFlutterApi`) bridging Flutter UI with native health sync pipelines (`notifySyncData`, `getSyncData`). | **CONFIRMED (APK)** |
| **Local Database DAO** | `com.nothing.database.old.DeviceDao` / `DeviceDatabase` | Room SQLite database access layer managing local cached device state and health telemetry (`ntwatch.db`). | **CONFIRMED (APK)** |
| **Network & OTA Repo** | `com.nothing.network.core.load.NetworkLoadRepo` / `NtNetPlugin` | Retrofit & Flutter network plugins managing Nothing cloud synchronization endpoints and firmware OTA downloads. | **CONFIRMED (APK)** |

---

## 4. Known Protocol Information

The protocol has been extensively analyzed and validated across decrypted BLE captures, decompiled firmware, and open-source implementations (Gadgetbridge, `CMF-Watch-Pro-2-BLE-Protocol`, `fmc_go`).

### 4.1 GATT Architecture & Service Layout
The watch acts as a BLE peripheral advertising with service `0xFFF0`. The phone acts as the GATT client.

| Purpose | Service UUID | Characteristic UUID | Properties | Notes |
|---|---|---|---|---|
| **Command Read (Notify)** | `0000fff0-0000-1000-8000-00805f9b34fb` | `0000fff1-0000-1000-8000-00805f9b34fb` | Notify | All command responses, notifications, and health streams arrive here. CCCD: `0x2902` set to `01 00`. |
| **Command Write** | `0000fff0-0000-1000-8000-00805f9b34fb` | `0000fff2-0000-1000-8000-00805f9b34fb` | Write | All framed requests from phone are sent here. |
| **Shell Write (AT)** | `77d4e67c-2fe2-2334-0d35-9ccd078f529c` | `77d4ff01-2fe2-2334-0d35-9ccd078f529c` | Write | Plaintext AT command channel used during initial pairing (`AT GETSECRET`). |
| **Shell Read (AT)** | `77d4e67c-2fe2-2334-0d35-9ccd078f529c` | `77d4ff02-2fe2-2334-0d35-9ccd078f529c` | Notify | Receives `GETSECRET:<32-hex-chars>,OK`. |
| **Bulk Data Write** | `02f00000-0000-0000-0000-00000000ffe0` | `02f00000-0000-0000-0000-00000000ffe1` | Write | Used for watchface, firmware OTA, and AGPS binary chunk transfers. |
| **Bulk Data Read** | `02f00000-0000-0000-0000-00000000ffe0` | `02f00000-0000-0000-0000-00000000ffe2` | Notify | Chunk requests (`DATA_CHUNK_REQUEST_*`) and transfer ACKs from watch. |

### 4.2 Packet Framing (`0xF5`)
Every message on the command channel is wrapped in an 11-byte Big-Endian header frame:

```text
+------+-----------+--------+-------------+-------------+--------+-------------------+
| 0xF5 | chunkLen  | cmd1   | chunkCount  | chunkIndex  | cmd2   | chunk bytes …     |
| 1 B  | 2 B (BE)  | 2 B BE | 2 B BE      | 2 B BE      | 2 B BE | chunkLen bytes    |
+------+-----------+--------+-------------+-------------+--------+-------------------+
        \__________________________ 11-byte header ____________________________/
```

- `cmd1` + `cmd2`: 16-bit uint Big-Endian values identifying the opcode command pair `(cmd1, cmd2)`.
- `chunkCount`: Total chunk count for fragmented payloads.
- `chunkIndex`: 1-based index of current chunk.
- **Chunk Body**: `payloadSlice ‖ CRC32_LE(payloadSlice)`. For encrypted commands, this body is encrypted using AES-128-CBC with PKCS7 padding.
- **CRC Algorithm**: Standard IEEE 802.3 CRC32 (`0xEDB88320`), appended as 4 bytes Little-Endian.

### 4.3 Cryptographic Handshake & Key Derivation
- **Cipher**: AES-128-CBC with PKCS7 padding.
- **Fixed IV**: `50 51 52 53 54 55 56 57 60 61 62 63 64 65 66 5A` (hex: `5051525354555657606162636465665a`).
- **Pairing Key (`authkey`)**:
  - Derived during first-time pairing via shell `AT GETSECRET` → returns `secret` (16 bytes).
  - Phone generates `rnd1` (16 random bytes); watch generates `rnd2` (16 random bytes).
  - `authkey = SHA256(rnd1 ‖ rnd2 ‖ secret)[0..16]`. Persisted across connections.
- **Session Key (`sessionKey`)**:
  - On every connection, phone requests nonce (`AUTH_NONCE_REQUEST`, opcode `FFFF 804B`).
  - Watch replies with `nonce` (`AUTH_NONCE_REPLY`, opcode `FFFF 004C`).
  - `sessionKey = SHA256(nonce ‖ authkey)[0..16]`. Used to encrypt/decrypt all subsequent frames.

### 4.4 Key Opcode Reference
- `TIME` (`FFFF 8004`): Payload = `epochSeconds(i32 BE) ‖ utcOffsetMillis(i32 BE)`. Mandatory post-auth command required to unblock health data queries.
- `ACTIVITY_FETCH_1` (`FFFF 8005`) & `ACTIVITY_FETCH_2` (`FFFF 9057`): Initiates health sync sequence.
- `ACTIVITY_DATA` (`0056 0001`): 32-byte records Little-Endian (`ts(u32)`, `steps(u32)`, `distance_m(u32)`, `calories_cal(u32)`, `reserved(16B)`).
- `HEART_RATE_MANUAL_AUTO` (`0053 0001`) / `HEART_RATE_WORKOUT` (`00E0 0001`): 8-byte LE records (`ts(u32)`, `bpm(u32)`).
- `HEART_RATE_RESTING` (`00DA 0001`): 5-byte LE record (`ts(u32)`, `hr(u8)`).
- `SPO2` (`0055 0001`): 8-byte LE records (`ts(u32)`, `spo2_pct(u32)`).
- `STRESS` (`009D 0001`): 8-byte LE records (`ts(u32)`, `stress_score(u32)`).
- `SLEEP_DATA` (`0058 0001`): 18-byte header (`session_start`, `wakeup_time`, stage totals) + 8-byte stage records (`ts(u32)`, `duration_s(u16)`, `stage_code(u16)`: 1=Deep, 2=Light/Core, 3=REM, 4=Awake).
- `WORKOUT_SUMMARY` (`0057 0001`) / `WORKOUT_SUMMARY_V3` (`0160 0001`): Detailed session summaries (54-byte v1 layout and extended v3 layout).
- `WORKOUT_GPS` (`FFFF A05A`): 12-byte LE records (`ts(i32)`, `lon_1e7(i32)`, `lat_1e7(i32)`).

---

## 5. Unknown Protocol Information & Gaps

1. **`WORKOUT_SUMMARY_V3` (`0160 0001`) Extended 40-byte Block Layout**: Exact byte offsets for exercise load, aerobic/anaerobic training effect, VO2 max, recovery time, and vitality score within the v3 summary payload.
2. **Flutter Pigeon Event Payloads**: Specific schema for `NtSyncHealthFlutterApi.getSyncData` / `updateDataById` data serializations.
3. **Multi-device protocol variations**: Protocol nuances between CMF Watch Pro, CMF Watch Pro 2, and CMF Watch 3 Pro.

---

## 6. References

1. **CMF Watch Pro 2 BLE Protocol RE Repository**: [joshuapassos/CMF-Watch-Pro-2-BLE-Protocol](https://github.com/joshuapassos/CMF-Watch-Pro-2-BLE-Protocol)
2. **`fmc_go` Protocol Documentation**: [freethinkel/fmc_go/docs/cmf-protocol.md](https://github.com/freethinkel/fmc_go/blob/main/docs/cmf-protocol.md)
3. **Gadgetbridge CMF Implementation**: `nodomain.freeyourgadget.gadgetbridge.service.devices.cmfwatchpro`
4. **CMF Watch Firmware RE**: [whatotter/cmf-watch-firmware](https://github.com/whatotter/cmf-watch-firmware)
5. **Nothing X APK Reversing Write-up**: [Ambraglow - A look into the Nothing X smartwatch app](https://ambraglow.org/blog/nothing-x/a-look-into-the-nothing-x-smartwatch-app/)
6. **Official Nothing Product Data Info**: [Nothing Product Data Information](https://nothing.tech/pages/product-data-information)

---

## 7. Reverse Engineering & Execution Plan

- **Phase 1 — APK Reconnaissance**: Complete. Decompiled Nothing X APK v3.7.3, mapped package name, DEX files, Flutter bridge, and `com.nothing.link.bluetooth.sdk` classes.
- **Phase 2 — BLE Architecture Mapping**: Mapped GATT connection flow from `XBluetoothManager` -> `NothingWatchParser` -> `XCommand` -> AES Crypto -> `NtSyncHealthPlugin` -> `DeviceDao`.
- **Phase 3 — GATT & Opcode Verification**: Confirmed GATT service UUIDs and command opcode specifications.
- **Phase 4 — Auth & Key Derivation**: Verified AES-128-CBC setup, fixed IV usage, SHA-256 key derivation (`authkey` and `sessionKey`).
- **Phase 5 — Packet Parsing & Frame Assembly**: Implement binary packet framer/deframer with CRC32 verification and MTU-aware AES block chunking.
- **Phase 6 — Health Sync Pipeline**: Implement `ACTIVITY_FETCH` handshake, session timestamp initialization via `TIME`, and stream decoders.
- **Phase 7 — Telemetry Metric Mapping**: Map each health metric (steps, heart rate, resting HR, SpO2, stress, sleep stages, workouts, GPS tracks, battery).
- **Phase 8 — Storage & DB Investigation**: Analyze Nothing X local SQLite/Room schema (`ntwatch.db`) and stored credential keychains.
- **Phase 9 — Cloud API Boundary**: Document HTTP REST endpoints used by Nothing X to distinguish local BLE device data from cloud-synced data.
- **Phase 10 — Frida Dynamic Analysis**: Script Frida hooks for runtime logging of `BluetoothGattCallback`, `Cipher.doFinal`, and loggers.
- **Phase 11 — Proof of Concept Client**: Implement standalone Python Bleak client connecting to watch, authenticating, requesting health data, and storing in PostgreSQL.
- **Phase 12 — Validation & Verification**: Validate all findings against static analysis, live captures, and reference implementation suites.

---

## 8. Evidence & Confidence Methodology

Every claim in this project is categorized by confidence level based on empirical evidence:

- **CONFIRMED (✅)**: Verified via live BLE capture / active device testing or matching multi-source implementation.
- **HIGH CONFIDENCE (🔎)**: Extracted directly from decompiled Nothing X APK v3.7.3, firmware binary analysis, or Gadgetbridge master.
- **POSSIBLE (🟡)**: Structurally inferred based on layout patterns, but pending live packet capture validation.
- **UNKNOWN (⚠️)**: Unverified hypothesis requiring further APK/Frida inspection or live capture.
