# Real BLE Integration Audit & Source of Truth Mapping

This document provides a line-by-line comparison between the **Python CMF Watch Client (`cmf_watch_client`)** (Authoritative Reference) and the **Android Companion Application (`android/`)**.

---

## 1. Python Execution Path Trace (SOURCE OF TRUTH)

The following table traces the exact execution path used by the Python CLI (`cmf-watch-client`) when communicating with the physical CMF Watch Pro 2:

| Stage | Operation | Python Module & Function | Byte Payload / Opcode | Expected Watch Response |
|---|---|---|---|---|
| **1. Scan** | Peripheral Discovery | `CmfWatchClient.discover` (`ble/client.py:93`) | Bleak Scanner filter `"CMF Watch"` | `BLEDevice(address, name)` |
| **2. GATT Connect** | Connect & Subscribe | `CmfWatchClient.connect` (`ble/client.py:103`) | Notify `0000fff1` & `77d4ff02` | `is_connected == True` |
| **3. Pairing (First Time)**| Secret Retrieval | `CmfWatchClient.pair_first_time` (`ble/client.py:136`)| `b"AT GETSECRET\r\n"` over `77d4ff01` | `GETSECRET: <32-hex>,OK` |
| **3. Pairing (Phase 1)** | Handshake Challenge | `CmfWatchClient.pair_first_time` (`ble/client.py:168`)| `AUTH_PAIR_REQUEST` (`FFFF 8047`) | `AUTH_PAIR_REPLY` (`FFFF 0047`) |
| **4. Auth Handshake (1)** | Phone Name | `CmfWatchClient.authenticate_session` (`ble/client.py:206`)| `AUTH_PHONE_NAME` (`FFFF 8049`, `b"\xa5" + model`) | `AUTH_WATCH_MAC` (`FFFF 0049`) |
| **4. Auth Handshake (2)** | Nonce Request | `CmfWatchClient.authenticate_session` (`ble/client.py:214`)| `AUTH_NONCE_REQUEST` (`FFFF 804B`, `b"\xa5"`) | `AUTH_NONCE_REPLY` (`FFFF 004C`, 16B nonce) |
| **4. Auth Handshake (3)** | Key Derivation | `derive_session_key` (`crypto/keys.py:43`) | `SHA256(nonce + authkey)[0..16]` | `session_key` |
| **4. Auth Handshake (4)** | Confirm Session | `CmfWatchClient.authenticate_session` (`ble/client.py:228`)| `AUTHENTICATED_CONFIRM_REQUEST` (`FFFF 804D`) | `AUTHENTICATED_CONFIRM_REPLY` (`FFFF 0004`)|
| **5. Post-Auth Mandate** | Set Watch Time | `CmfWatchClient.set_device_time` (`ble/client.py:243`)| `TIME` (`FFFF 8004`, `struct.pack(">ii", epoch, offset)`) | None (Unblocks MCU sync) |
| **6. Query Battery** | Battery Percentage | `CmfWatchClient.fetch_battery` (`ble/client.py:259`) | `BATTERY_GET` (`0x005C 0x0002`, `b"\xa5"`) | `BATTERY` (`0x005C 0x0001`, `[level, charging]`) |
| **7. Fetch Telemetry** | Stream Trigger | `CmfWatchClient.sync_health_data` (`ble/client.py:280`)| `ACTIVITY_FETCH_1` (`FFFF 8005`) & `2` (`FFFF 9057`) | Stream notification bursts |
| **8. Decode Telemetry** | Heart Rate Parser | `decode_heart_rate` (`health/decoders.py:44`) | `0x0053` / `0x00E0` (8B chunks: `epoch_sec` + `bpm`) | `List[HeartRateSample]` |

---

## 2. Comparative Audit: Python vs. Android

```text
                                COMPARATIVE AUDIT
                                
   FUNCTIONAL AREA               PYTHON (REFERENCE)               ANDROID (CURRENT STATE)
 ──────────────────────────────────────────────────────────────────────────────────────────
 1. Peripheral Scan              Dynamic BleakScanner            🔴 HARDCODED MAC Address
                                 Filters "CMF Watch"             Missing dynamic scan UI
 
 2. Device Selection             Command line / config           🔴 No device selection UI
                                                                 Implicit target MAC
 
 3. GATT Connection State        Explicit BleakClient            🟡 BluetoothGattCallback
                                 State machine                   State enum created
 
 4. 0xF5 Frame Header            11 Bytes: >BHHHHH               ✅ 11 Bytes: >BHHHHH
                                 0xF5 + len + cmd1 + chunks      ProtocolFramer.kt aligned
 
 5. CRC Checksum                 4-Byte CRC32 (LE)               ✅ 4-Byte CRC32 (LE)
                                                                 CryptoEngine.kt aligned
 
 6. Session Key Derivation       SHA256(nonce + authkey)         ✅ SHA256(nonce + authkey)
                                 Fixed AES_IV                    CryptoEngine.kt aligned
 
 7. Session Auth Opcodes         8049 -> 804B -> 804D            ✅ 8049 -> 804B -> 804D
                                 Set TIME (8004) mandate         CmfBleManager.kt aligned
 
 8. Telemetry Data Truth         Zero mock fallback              ✅ Truthful StateFlow
                                 Returns None when empty         HomeScreen renders "--"
```

---

## 3. Identified Gaps & Required Implementation Changes

### Gap 1: Hardcoded MAC Address in Android (CRITICAL)
- **Problem**: `CmfBleManager.kt` and `DeviceScreen.kt` previously assumed a hardcoded MAC address (`3C:B0:ED:3F:BA:70`).
- **Required Change**: Implement dynamic `BluetoothLeScanner` in `CmfBleManager.kt` scanning for advertised devices matching `"CMF Watch"`, and create a **Device Scan & Selection Modal/Screen** in Jetpack Compose allowing the user to select their watch peripheral from real scan results.

### Gap 2: Connection State Machine Granularity
- **Problem**: Android UI needs explicit states for dynamic scan and selection (`IDLE`, `SCANNING`, `DEVICES_FOUND`, `CONNECTING`, `CONNECTED`, `DISCOVERING_SERVICES`, `SUBSCRIBING`, `AUTHENTICATING`, `READY`, `SYNCING`, `DISCONNECTED`, `ERROR`).
- **Required Change**: Update `DeviceConnectionState` enum in `HealthMetrics.kt` to include `IDLE`, `SCANNING`, `DEVICES_FOUND`, `DISCOVERING_SERVICES`, `SUBSCRIBING`, and `READY`.

### Gap 3: Discovered Peripheral Data Model
- **Problem**: Android lacks a data model for scanned BLE peripherals.
- **Required Change**: Create `DiscoveredDevice(val name: String, val address: String, val rssi: Int)` data model.

---

## 4. Minimal Target Integration Milestone

We will implement the smallest end-to-end integration path:

$$\text{User taps "Scan for CMF Watches"} \to \text{Android BLE Scan} \to \text{Display Discovered Peripherals} \to \text{User Selects Device}$$
$$\downarrow$$
$$\text{GATT Connect} \to \text{Subscribe CCCD} \to \text{Handshake 8049/804B/804D} \to \text{Set Time 8004} \to \text{Query Battery 0x005C} \to \text{Fetch Live HR 0x0053} \to \text{Compose UI}$$
