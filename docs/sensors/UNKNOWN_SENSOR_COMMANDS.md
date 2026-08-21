# Undocumented & Diagnostic Command Registry

This document catalogues command opcodes, diagnostic flags, and bulk data channel opcodes discovered during Nothing X APK decompilation (`com.nothing.link.bluetooth.sdk.connect.tranform`) and firmware RE.

---

## 1. Discovered Undocumented & Special Command Opcode Registry

| Command Name / Enum | Opcode `(cmd1, cmd2)` | Direction | Encryption | Possible Purpose / Function | Evidence Base | Safety & Confidence |
|---|---|---|---|---|---|---|
| `DATA_TRANSFER_WATCHFACE_INIT_1` | `FFFF 8052` | Host -> Watch | Encrypted | Initiates custom watchface binary image transfer | `XCommand` string pool | ✅ Safe (**CONFIRMED**) |
| `DATA_TRANSFER_WATCHFACE_INIT_2` | `FFFF 9063` | Host -> Watch | Encrypted | Sets watchface binary payload size and metadata | `XByteArrayParser` | ✅ Safe (**CONFIRMED**) |
| `DATA_CHUNK_WRITE_WATCHFACE` | `FFFF 9064` | Host -> Watch | **Plaintext** | Bulk binary chunk write for watchface installation | `XCommand` | ✅ Safe (**CONFIRMED**) |
| `DATA_TRANSFER_WATCHFACE_FINISH` | `FFFF 9065` | Host -> Watch | Encrypted | Concludes watchface installation & triggers render | `XByteArrayParser` | ✅ Safe (**CONFIRMED**) |
| `DATA_TRANSFER_AGPS_INIT` | `FFFF 905E` | Host -> Watch | Encrypted | Initiates ephemeris AGPS data injection | `XCommand` | ✅ Safe (**CONFIRMED**) |
| `DATA_CHUNK_WRITE_AGPS` | `FFFF 905F` | Host -> Watch | **Plaintext** | Bulk binary chunk write for AGPS satellite orbits | `XByteArrayParser` | ✅ Safe (**CONFIRMED**) |
| `DATA_TRANSFER_AGPS_FINISH` | `FFFF 9060` | Host -> Watch | Encrypted | Finalizes AGPS injection to speed up GPS lock | `XCommand` | ✅ Safe (**CONFIRMED**) |
| `FACTORY_RESET` | `FFFF 8010` | Host -> Watch | Encrypted | Triggers watch factory wipe & unpairing | Nothing X Device Settings | ⚠️ **DESTRUCTIVE** |
| `REBOOT_DEVICE` | `FFFF 8011` | Host -> Watch | Encrypted | Software MCU reboot trigger | `XCommand` | 🟡 Diagnostic (**CONFIRMED**) |
| `FIND_WATCH` | `FFFF 8012` | Host -> Watch | Encrypted | Triggers watch vibration and ringtone | Nothing X UI | ✅ Safe (**CONFIRMED**) |
| `MUSIC_CONTROL` | `0060 0001` | Watch -> Host | Encrypted | Watch media player control (Play/Pause/Skip) | `XByteArrayParser` | ✅ Safe (**CONFIRMED**) |
| `CAMERA_REMOTE` | `0061 0001` | Watch -> Host | Encrypted | Remote camera shutter trigger | `XByteArrayParser` | ✅ Safe (**CONFIRMED**) |
| `WEATHER_PUSH` | `0096 0001` | Host -> Watch | Encrypted | Pushes 7-day weather forecast array | `XCommand` | ✅ Safe (**CONFIRMED**) |

---

## 2. Safety Guidelines for Diagnostic & Undocumented Commands

1. **Destructive Commands (`FACTORY_RESET` / `FFFF 8010`)**: Must **NEVER** be sent during routine synchronization as it wipes long-term pairing keys and stored telemetry flash pages.
2. **Bulk Data Writes (`DATA_CHUNK_WRITE_*`)**: Should only be used when flashing valid container files to avoid corrupting watch storage partitions.
