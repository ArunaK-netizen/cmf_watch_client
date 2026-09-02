# CMF Watch Platform Architectural Specification

This document defines the system architecture, component relationships, data flows, and protocol synchronization standards for the **CMF Watch Client Ecosystem**.

---

## 1. System Vision & Component Roles

```text
 ┌─────────────────────────────────────────────────────────────────────────────┐
 │                    PYTHON CMF WATCH CLIENT (cmf_watch_client)              │
 │  - Primary Protocol Source of Truth & Reverse-Engineering Engine           │
 │  - Asynchronous Bleak GATT Client, Decoders, CLI & Experiment Recorder     │
 └──────────────────────────────────────┬──────────────────────────────────────┘
                                        │
                         Shared Protocol Specification
                                        │
 ┌──────────────────────────────────────▼──────────────────────────────────────┐
 │                     ANDROID COMPANION APPLICATION (android/)                │
 │  - Consumer Companion App & Background Telemetry Sync Host                  │
 │  - Native Android BluetoothGatt, Room SQLite DB, Jetpack Compose UI        │
 └─────────────────────────────────────────────────────────────────────────────┘
```

### 1.1 Python CMF Watch Client (`cmf_watch_client/`)
- **Role**: The **Authoritative Source of Truth** for all BLE GATT protocols, opcodes, frame packing, AES-128 cryptographic key derivations, and telemetry decoders.
- **Capabilities**: Full pairing, session handshake, time setting, 1-min step intervals, 24h heart rate, SpO₂, stress, sleep architecture, workout summaries, GPS tracks, and real-world experiment trial dataset recorder.

### 1.2 Android Companion Application (`android/`)
- **Role**: High-performance consumer Android companion client delivering offline-first local persistence (Room DB), background sync (`WorkManager`), and Nothing `NType82` visual presentation.
- **Protocol Contract**: Must strictly mirror the exact protocol framing, AES IVs, CRC32 algorithms, and opcodes established by the Python reference implementation.

---

## 2. Comprehensive Protocol Source of Truth Mapping

To guarantee that the Python and Android clients never silently diverge, every Kotlin component is strictly mapped to its Python reference:

| Functional Area | Python Source of Truth (`cmf_watch_client`) | Kotlin Equivalent (`com.cmfwatch.companion`) | Equivalence Status |
|---|---|---|:---:|
| **GATT Service UUID** | `0000fff0-0000-1000-8000-00805f9b34fb` | `CmfBleManager.CMF_SERVICE_UUID` | Identical |
| **Command Write/Notify** | `0000fff1-0000-1000-8000-00805f9b34fb` | `CmfBleManager.CMF_CMD_CHAR_UUID` | Identical |
| **Shell Write/Notify** | `77d4ff01` / `77d4ff02` | `CmfBleManager.CMF_SHELL_CHAR_UUID` | Identical |
| **Fixed AES-128 IV** | `0x50 51 52 53 54 55 56 57 60...` (`cipher.py`) | `CryptoEngine.AES_IV` | Aligned |
| **CRC Algorithm** | `CRC32` (IEEE 802.3, LE `struct.pack("<I")`) | `CryptoEngine.calculateCrc32` | Aligned |
| **AuthKey Derivation** | `SHA256(rnd1 + rnd2 + secret)[0..16]` | `CryptoEngine.deriveAuthKey` | Aligned |
| **SessionKey Derivation** | `SHA256(watchNonce + authkey)[0..16]` | `CryptoEngine.deriveSessionKey` | Aligned |
| **0xF5 Frame Header** | `>BHHHHH` (11B: Magic, Len, Cmd1, Chunks, Index, Cmd2) | `ProtocolFramer.buildFrames` | Aligned |
| **Handshake Sequence** | `AUTH_PHONE_NAME` (`8049`) $\to$ `NONCE` (`804B`) $\to$ `CONFIRM` (`804D`) | `CmfBleManager.startSessionHandshake` | Aligned |
| **Post-Auth Mandate** | `TIME` (`FFFF 8004`, 8-byte `[epoch, offset_ms]`) | `CmfBleManager.setDeviceTime` | Aligned |

---

## 3. End-to-End Telemetry Pipeline Architecture

$$\text{CMF Watch Pro 2} \xrightarrow{\text{BLE 0xF5}} \text{CmfBleManager} \xrightarrow{\text{Decoders}} \text{Room DB} \xrightarrow{\text{SyncWorker}} \text{Backend API} \xrightarrow{\text{MongoDB}} \text{Compose UI}$$

1. **BLE Acquisition**: `BluetoothGatt` receives raw 0xF5 notification chunks over characteristic `0000fff1`.
2. **Frame Deframing & Decryption**: `FrameAssembler` verifies 4-byte CRC32, decrypts payload with `SessionKey` and `AES_IV`, and reassembles multi-chunk payloads.
3. **Telemetry Decoding**: `TelemetryDecoders` convert binary byte arrays into domain objects (`HeartRateSample`, `StepInterval`, `BatteryState`).
4. **Local Persistence**: Telemetry records persist immediately into Room SQLite (`cmf_health.db`).
5. **UI State Observation**: ViewModels observe Room DB `StateFlow` streams. When watch is disconnected, UI renders truthful unavailable (`"--"`) states without inventing dummy metrics.

---

## 4. Production Architecture Evaluation & Decision

We evaluated three potential strategies for the Android Companion Application:

| Criterion | Option A: Native Kotlin Port (Strict Alignment) | Option B: Embedded Python (Chaquopy) | Option C: Shared C/C++ JNI Engine |
|---|---|---|---|
| **BLE Integration** | ✅ Direct Android `BluetoothGatt` | ❌ Complex Bleak-to-Android BLE bridging | 🟡 Complex JNI BLE wrapper |
| **App Size** | ✅ Minimal (< 8 MB APK) | ❌ Large (> 45 MB APK) | 🟡 Moderate (~ 15 MB) |
| **Background Lifecycle** | ✅ Native Android `WorkManager` | ❌ Unreliable background daemon | 🟡 Requires Native Threads |
| **Protocol Parity** | ✅ Guaranteed via strict test mapping | ✅ Uses Python code directly | ❌ Separate C implementation |
| **Recommendation** | **SELECTED** | Rejected | Rejected |

### Decision Rationale:
We select **Option A (Native Kotlin Engine strictly aligned with Python Source of Truth)**. It provides minimal APK overhead, zero BLE bridging latency, full Android background support (`WorkManager`), and native Compose integration, backed by explicit protocol unit tests verifying 100% equivalence with Python outputs.

---

## 5. Audit & File Classification Matrix (`android/`)

Every existing Android file has been audited against the Python Source of Truth:

| File Path | Python Reference | Classification | Discrepancy & Action |
|---|---|---|---|
| `CryptoEngine.kt` | `crypto/cipher.py`, `crc.py`, `keys.py` | `REWORK` | Align session key derivation to `SHA256(nonce + authkey)`, set fixed `AES_IV`, switch CRC to CRC32. |
| `ProtocolFramer.kt` | `protocol/frame.py` | `REWORK` | Update header to 11-byte `>BHHHHH` layout and implement multi-chunk frame reassembly. |
| `CmfBleManager.kt` | `ble/client.py` | `REWORK` | Align opcode constants (`8049` phone name, `804B` nonce request, `804D` confirm) and post-auth `TIME` (`8004`) command. |
| `TelemetryDecoders.kt` | `health/decoders.py` | `KEEP` | Decoders matching binary layouts. |
| `HomeViewModel.kt` | N/A | `PRODUCTION-READY` | Nullable StateFlow metrics for truthful disconnected states. |
| `HomeScreen.kt` | N/A | `PRODUCTION-READY` | Truthful UI rendering (`"--"` for un-synced metrics). |
| `DeviceScreen.kt` | N/A | `PRODUCTION-READY` | Real battery & connection state rendering. |
