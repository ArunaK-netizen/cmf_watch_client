# CMF / Nothing Smartwatch BLE Protocol Architecture Specification

## 1. Overview & GATT Layout

The CMF / Nothing Smartwatch communicates with the host application using Bluetooth Low Energy (BLE). The phone acts as the GATT client; the smartwatch operates as a peripheral advertising with name `CMF Watch Pro 2-XXXX` or `Watch Pro`.

### Service & Characteristic Table

| Service Name | Service UUID | Characteristic UUID | Properties | Purpose |
|---|---|---|---|---|
| **Command Service** | `0000fff0-0000-1000-8000-00805f9b34fb` | `0000fff1-0000-1000-8000-00805f9b34fb` | Notify | All notification streams, response frames, and health data arrive here. CCCD (`0x2902`) set to `01 00`. |
| **Command Service** | `0000fff0-0000-1000-8000-00805f9b34fb` | `0000fff2-0000-1000-8000-00805f9b34fb` | Write | All framed request packets from host are sent here. |
| **Shell Service** | `77d4e67c-2fe2-2334-0d35-9ccd078f529c` | `77d4ff01-2fe2-2334-0d35-9ccd078f529c` | Write | Plaintext AT command channel used during initial pairing (`AT GETSECRET`). |
| **Shell Service** | `77d4e67c-2fe2-2334-0d35-9ccd078f529c` | `77d4ff02-2fe2-2334-0d35-9ccd078f529c` | Notify | Receives `GETSECRET:<32-hex-chars>,OK`. |
| **Data Service** | `02f00000-0000-0000-0000-00000000ffe0` | `02f00000-0000-0000-0000-00000000ffe1` | Write | Bulk binary chunk transfers (watchface, firmware OTA, AGPS). |
| **Data Service** | `02f00000-0000-0000-0000-00000000ffe0` | `02f00000-0000-0000-0000-00000000ffe2` | Notify | Chunk requests (`DATA_CHUNK_REQUEST_*`) and transfer ACKs. |

---

## 2. Pairing & Session State Machine

### 2.1 First-Time Pairing Sequence
```text
Host App                                                   Smartwatch
   |                                                           |
   |--- Write ASCII "AT GETSECRET\r\n" (77d4ff01) ------------>|
   |<-- Notify ASCII "GETSECRET:<secret_hex>,OK" (77d4ff02) --|
   |                                                           |
   | [Generate rnd1, signed1 = SHA256(rnd1 || secret)]         |
   |--- AUTH_PAIR_REQUEST (FFFF 8047, plaintext) ------------->|
   |<-- AUTH_PAIR_REPLY (FFFF 0048, plaintext, rnd2 + signed2) -|
   |                                                           |
   | [Verify signed2 == SHA256(rnd2 || secret)]                |
   | [Derive authkey = SHA256(rnd1 || rnd2 || secret)[0..16]]  |
   | [Persist authkey]                                         |
```

### 2.2 Reconnection / Session Handshake Sequence
```text
Host App                                                   Smartwatch
   |                                                           |
   | [Set frame cipher key = authkey]                          |
   |--- AUTH_PHONE_NAME (FFFF 8049, encrypted) --------------->|
   |<-- AUTH_WATCH_MAC (FFFF 0049, encrypted) -----------------|
   |                                                           |
   |--- AUTH_NONCE_REQUEST (FFFF 804B, encrypted) ------------>|
   |<-- AUTH_NONCE_REPLY (FFFF 004C, payload = nonce) ---------|
   |                                                           |
   | [Derive sessionKey = SHA256(nonce || authkey)[0..16]]     |
   | [Switch frame cipher key = sessionKey]                    |
   |--- AUTHENTICATED_CONFIRM_REQUEST (FFFF 804D) ------------>|
   |<-- AUTHENTICATED_CONFIRM_REPLY (FFFF 0004) ---------------|
   |                                                           |
   | Session State = INITIALIZED                               |
   |--- TIME (FFFF 8004, epoch + utc_offset) ----------------->|
```

---

## 3. Health Data Synchronization Sequence

```text
Host App                                                   Smartwatch
   |                                                           |
   |--- TIME (FFFF 8004) ------------------------------------->|
   | [await 500ms MCU initialization delay]                    |
   |                                                           |
   |--- ACTIVITY_FETCH_1 (FFFF 8005) ------------------------->|
   |<-- ACTIVITY_FETCH_ACK_1 (FFFF 0005) ----------------------|
   |                                                           |
   |--- ACTIVITY_FETCH_2 (FFFF 9057) ------------------------->|
   |<-- ACTIVITY_FETCH_ACK_2 (FFFF A057) ----------------------|
   |                                                           |
   |<== Telemetry Burst (ACTIVITY_DATA, HR, SpO2, Stress, Sleep) ==|
```
