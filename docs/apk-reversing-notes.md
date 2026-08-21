# Nothing X APK Static Reverse Engineering Findings

This document summarizes the static decompilation analysis performed on the official **Nothing X** Android application (version 3.7.3, package `com.nothing.smartcenter`).

---

## 1. Package Structure & Mapping

Decompilation of all 8 DEX files (`classes.dex` .. `classes8.dex`) revealed the internal Java/Kotlin package hierarchy:

```text
com.nothing.link.bluetooth.sdk
├── XBluetoothManager                 # Primary BLE lifecycle & GATT connection manager
├── util.BleUtil                      # GATT characteristic & MTU negotiation utilities
├── scan.parser.NothingWatchParser    # Advertised BLE name & service parser (CMF Watch Pro 2 / Watch Pro)
└── connect.tranform
    ├── XCommand                      # Opcode pair (cmd1, cmd2) & 0xF5 11-byte frame encoder
    └── XByteArrayParser              # Payload deframer, CRC32 verifier & AES chunk assembler

com.nothing.xservicecore
└── CMFWatchServiceManager            # Cross-process Android IPC manager for CMF Watch service

com.nothing.xhost
└── BindWatchXServiceHandler          # IPC Service binder bridging Nothing X host with Watch service

com.nothing.nt_sync_health
└── NtSyncHealthPlugin                # Flutter Pigeon bridge (NtSyncHealthFlutterApi) for sync UI

com.nothing.database.old
└── DeviceDao                         # Room SQLite database DAO accessing local cached health & device state (ntwatch.db)
```

---

## 2. Flutter & Native Library Integrations

- **UI Framework**: Nothing X 3.7.3 uses Flutter for UI screens, communicating with native BLE code via Flutter Pigeon IPC interfaces (`NtSyncHealthFlutterApi`).
- **Fitness Engine**: The app incorporates `libGoMoreEdgeKit.so` (GoMore algorithms) to process exercise load, VO2 max, and training effect metrics.
