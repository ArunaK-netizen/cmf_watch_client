# Native Android BLE Driver Specification (`CmfBleManager`)

This document defines the technical architecture, GATT lifecycle, handshake state machine, protocol framing, and security policies for the native Android BLE driver (**`com.cmfwatch.companion.ble.CmfBleManager`**).

---

## 1. BLE Architecture & GATT Parameters

- **Service UUID**: `0000fff0-0000-1000-8000-00805f9b34fb`
- **Command Characteristic**: `0000fff1-0000-1000-8000-00805f9b34fb` (Tx / Rx)
- **CCCD Descriptor**: `00002902-0000-1000-8000-00805f9b34fb`
- **MTU Size**: Requested MTU = 512 bytes
- **Device Prefix Filter**: `"CMF Watch"` (e.g. `CMF Watch Pro 2-BA70`)

---

## 2. Session Authentication & Handshake State Machine

```text
  [ Android App ]                                     [ CMF Watch Pro 2 ]
        │                                                     │
        │ 1. GATT Connect & MTU Request (512 bytes)           │
        ├────────────────────────────────────────────────────►│
        │ 2. Subscribe to CCCD Notifications (0x2902)        │
        ├────────────────────────────────────────────────────►│
        │                                                     │
        │ 3. AUTH_PHONE_NAME (0xFFFF 804B, "CMF Companion")   │
        ├────────────────────────────────────────────────────►│
        │                                                     │
        │ 4. AUTH_NONCE_REQUEST (0xFFFF 804C, ClientNonce)    │
        ├────────────────────────────────────────────────────►│
        │    AUTH_NONCE_REPLY (0xFFFF 0003, WatchNonce)       │
        │◄────────────────────────────────────────────────────┤
        │                                                     │
        │ [ Derive SessionKey = AES128(AuthKey, NonceXOR) ]  │
        │ [ REDACT KEYS IN LOGCAT: "Session key: [REDACTED]" ]│
        │                                                     │
        │ 5. AUTHENTICATED_CONFIRM_REQUEST (0xFFFF 804D)      │
        ├────────────────────────────────────────────────────►│
        │    AUTHENTICATED_CONFIRM_REPLY (0xFFFF 0004)        │
        │◄────────────────────────────────────────────────────┤
        │                                                     │
        ▼                                                     ▼
  State: CONNECTED_PAIRED                             State: INITIALIZED
```

---

## 3. 0xF5 Binary Frame Packing

All command transmissions use the `0xF5` frame header format:

$$\text{Magic (0xF5)} \mid \text{Seq (u8)} \mid \text{Flags (u8)} \mid \text{PayloadLen (u16)} \mid \text{Cmd1 (u16)} \mid \text{Cmd2 (u16)} \mid \text{Payload} \mid \text{CRC16 (u16)}$$

- **Plaintext Frames (`flags = 0x00`)**: Used during initial handshake (`AUTH_PHONE_NAME`, `AUTH_NONCE_REQUEST`).
- **Encrypted Frames (`flags = 0x01`)**: Used for all post-auth telemetry commands (`BATTERY_GET`, `ACTIVITY_FETCH_1/2`). Encrypted using AES-128-CBC with PKCS7 padding.

---

## 4. Security & Logging Redaction Policy

> [!CAUTION]
> **Strict Redaction Requirement**:
> Session keys, auth keys, pairing secrets, client nonces, and decrypted raw auth packets MUST NEVER be output to `android.util.Log` or printed to console.
> All key derivation events log explicit `[REDACTED]` markers.

---

## 5. Supported Telemetry Opcodes & Decoders

| Opcode `(cmd1, cmd2)` | Direction | Encryption | Decoded Domain Model | Function |
|---|---|---|---|---|
| `0x005C 0x0002` | Host -> Watch | Encrypted | N/A | Battery Level Query |
| `0x005C 0x0001` | Watch -> Host | Encrypted | `BatteryState` | Battery Level % & Charging State |
| `0x0053` / `0x00E0` | Watch -> Host | Encrypted | `HeartRateSample` | Continuous / Workout Heart Rate BPM |
| `0x00DA` | Watch -> Host | Encrypted | `HeartRateSample` | Resting Heart Rate Minimum |
| `0x0056` | Watch -> Host | Encrypted | `StepInterval` | 1-min Step Count, Distance & Calories |
| `0x009D` | Watch -> Host | Encrypted | `StressSample` | Periodic 1–99 Stress Index |
| `0x0055` | Watch -> Host | Encrypted | `SpO2Sample` | SpO₂ Saturation Percentage |
| `0xFFFF 8005` / `9057` | Host -> Watch | Encrypted | N/A | `ACTIVITY_FETCH_1` & `2` Telemetry Trigger |
