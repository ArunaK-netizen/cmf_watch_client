# CMF / Nothing Watch GATT Architecture & Protocol Reference

## 1. GATT Layout & Services

The phone acts as the GATT client; the smartwatch operates as a peripheral advertising as `CMF Watch Pro 2-XXXX` (or `Watch Pro`).

| Service Name | Service UUID | Characteristic UUID | Properties | Purpose |
|---|---|---|---|---|
| **Command Service** | `0000fff0-0000-1000-8000-00805f9b34fb` | `0000fff1-0000-1000-8000-00805f9b34fb` | Notify | All notification streams, response frames, and health data arrive here. CCCD (`0x2902`) must be set to `01 00`. |
| **Command Service** | `0000fff0-0000-1000-8000-00805f9b34fb` | `0000fff2-0000-1000-8000-00805f9b34fb` | Write | All framed request packets from phone are sent here. |
| **Shell Service** | `77d4e67c-2fe2-2334-0d35-9ccd078f529c` | `77d4ff01-2fe2-2334-0d35-9ccd078f529c` | Write | Plaintext AT command channel used during initial pairing (`AT GETSECRET`). |
| **Shell Service** | `77d4e67c-2fe2-2334-0d35-9ccd078f529c` | `77d4ff02-2fe2-2334-0d35-9ccd078f529c` | Notify | Receives `GETSECRET:<32-hex-chars>,OK`. |
| **Data Service** | `02f00000-0000-0000-0000-00000000ffe0` | `02f00000-0000-0000-0000-00000000ffe1` | Write | Bulk binary chunk transfers (watchface, firmware OTA, AGPS). |
| **Data Service** | `02f00000-0000-0000-0000-00000000ffe0` | `02f00000-0000-0000-0000-00000000ffe2` | Notify | Chunk requests (`DATA_CHUNK_REQUEST_*`) and transfer ACKs. |

---

## 2. Authentication & Session Handshake Sequence

### 2.1 First-Time Pairing Sequence
1. Phone sends ASCII `AT GETSECRET\r\n` to shell write characteristic (`77d4ff01`).
2. Watch responds with `GETSECRET:<32-hex-chars>,OK` on shell notify characteristic (`77d4ff02`). Secret = 16 bytes.
3. Phone generates 16 random bytes `rnd1`, computes signature `signed1 = SHA256(rnd1 || secret)`.
4. Phone sends `AUTH_PAIR_REQUEST` (`FFFF 8047`, plaintext, payload = `rnd1[16] || signed1[32]`).
5. Watch responds with `AUTH_PAIR_REPLY` (`FFFF 0048`, plaintext, payload = `rnd2[16] || signed2[32]`).
6. Phone verifies `signed2 == SHA256(rnd2 || secret)`.
7. Phone derives `authkey = SHA256(rnd1 || rnd2 || secret)[0..16]` and persists it.

### 2.2 Reconnection / Session Authentication Sequence
1. Phone sends `AUTH_PHONE_NAME` (`FFFF 8049`, encrypted with `authkey`, payload = `0xA5 || model_utf8`).
2. Watch responds with `AUTH_WATCH_MAC` (`FFFF 0049`).
3. Phone sends `AUTH_NONCE_REQUEST` (`FFFF 804B`, encrypted with `authkey`, payload = `0xA5`).
4. Watch responds with `AUTH_NONCE_REPLY` (`FFFF 004C`, payload = `nonce`).
5. Phone derives `sessionKey = SHA256(nonce || authkey)[0..16]` and switches encryption key to `sessionKey`.
6. Phone sends `AUTHENTICATED_CONFIRM_REQUEST` (`FFFF 804D`, encrypted with `sessionKey`, payload = `0xA5`).
7. Watch responds with `AUTHENTICATED_CONFIRM_REPLY` (`FFFF 0004`). Session state becomes `Initialized`.

### 2.3 Post-Authentication Time Initialization
Immediately after `Initialized`, phone sends `TIME` (`FFFF 8004`, payload = `epochSeconds(i32 BE) || utcOffsetMillis(i32 BE)`).
*Sending `TIME` is mandatory to unblock health data queries.*
