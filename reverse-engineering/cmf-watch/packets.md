# CMF / Nothing Watch Packet Framing & Opcode Reference

## 1. Frame Structure (`0xF5`)

All command messages travel wrapped in 11-byte Big-Endian header frames:

```text
+------+-----------+--------+-------------+-------------+--------+-------------------+
| 0xF5 | chunkLen  | cmd1   | chunkCount  | chunkIndex  | cmd2   | chunk bytes …     |
| 1 B  | 2 B (BE)  | 2 B BE | 2 B BE      | 2 B BE      | 2 B BE | chunkLen bytes    |
+------+-----------+--------+-------------+-------------+--------+-------------------+
        \__________________________ 11-byte header ____________________________/
```

- `cmd1` (uint16 BE): First byte pair of opcode.
- `chunkCount` (uint16 BE): Total chunk count for fragmented payloads.
- `chunkIndex` (uint16 BE): 1-based index of current chunk.
- `cmd2` (uint16 BE): Second byte pair of opcode.
- `chunkLen` (uint16 BE): Length of chunk payload that follows.

---

## 2. Opcode Command Reference

| Name | Opcode `(cmd1, cmd2)` | Direction | Encryption | Notes |
|---|---|---|---|---|
| `TIME` | `FFFF 8004` | Phone -> Watch | Encrypted | Payload = `epoch(i32 BE) \|\| offset_ms(i32 BE)` |
| `FIRMWARE_VERSION_GET` | `FFFF 8006` | Phone -> Watch | Encrypted | Requests firmware version string |
| `FIRMWARE_VERSION_RET` | `FFFF 0006` | Watch -> Phone | Encrypted | Returns dotted version string |
| `SERIAL_NUMBER_GET` | `00DE 0002` | Phone -> Watch | Encrypted | Requests device serial number |
| `SERIAL_NUMBER_RET` | `00DE 0001` | Watch -> Phone | Encrypted | Returns ASCII serial number |
| `BATTERY` | `005C 0001` | Both | Encrypted | Payload = `level(u8) \|\| charging(u8)` |
| `AUTH_PAIR_REQUEST` | `FFFF 8047` | Phone -> Watch | **Plaintext** | Payload = `rnd1[16] \|\| signed1[32]` |
| `AUTH_PAIR_REPLY` | `FFFF 0048` | Watch -> Phone | **Plaintext** | Payload = `rnd2[16] \|\| signed2[32]` |
| `AUTH_PHONE_NAME` | `FFFF 8049` | Phone -> Watch | Encrypted | Payload = `0xA5 \|\| model_name` |
| `AUTH_WATCH_MAC` | `FFFF 0049` | Watch -> Phone | Encrypted | Returns watch MAC address |
| `AUTH_NONCE_REQUEST` | `FFFF 804B` | Phone -> Watch | Encrypted | Requests session nonce |
| `AUTH_NONCE_REPLY` | `FFFF 004C` | Watch -> Phone | Encrypted | Returns 16-byte nonce |
| `AUTHENTICATED_CONFIRM_REQUEST` | `FFFF 804D` | Phone -> Watch | Encrypted | Final auth confirm (`0xA5`) |
| `AUTHENTICATED_CONFIRM_REPLY` | `FFFF 0004` | Watch -> Phone | Encrypted | Session auth ACK |
| `ACTIVITY_FETCH_1` | `FFFF 8005` | Phone -> Watch | Encrypted | Initiates health data sync phase 1 |
| `ACTIVITY_FETCH_2` | `FFFF 9057` | Phone -> Watch | Encrypted | Initiates health data sync phase 2 |
| `ACTIVITY_DATA` | `0056 0001` | Watch -> Phone | Encrypted | 32-byte activity/step records |
| `HEART_RATE_MANUAL_AUTO` | `0053 0001` | Watch -> Phone | Encrypted | 8-byte heart rate records |
| `HEART_RATE_WORKOUT` | `00E0 0001` | Watch -> Phone | Encrypted | 8-byte workout HR records |
| `HEART_RATE_RESTING` | `00DA 0001` | Watch -> Phone | Encrypted | 5-byte/8-byte resting HR records |
| `SPO2` | `0055 0001` | Watch -> Phone | Encrypted | 8-byte blood oxygen records |
| `STRESS` | `009D 0001` | Watch -> Phone | Encrypted | 8-byte stress score records |
| `SLEEP_DATA` | `0058 0001` | Watch -> Phone | Encrypted | 18-byte header + 8-byte stage records |
| `WORKOUT_SUMMARY` | `0057 0001` | Watch -> Phone | Encrypted | 32-byte / 54-byte workout session summary |
| `WORKOUT_GPS` | `FFFF A05A` | Watch -> Phone | Encrypted | 12-byte GPS coordinate records |
