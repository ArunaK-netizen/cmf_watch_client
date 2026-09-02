# CMF Watch Companion Sync & Offline Architecture

This document defines the offline-first synchronization pipeline, background BLE worker lifecycle, local Room DB caching, queue management, and MongoDB cloud sync.

---

## 1. Offline-First Architecture Blueprint

The application guarantees full operational capability regardless of internet connectivity:

```text
 ┌─────────────────────────────────────────────────────────────────────────────┐
 │                            CMF WATCH PRO 2                                  │
 └─────────────────────────────────────────────────────────────────────────────┘
                                       │
                                       │ BLE GATT Telemetry Sync (ACTIVITY_FETCH_1/2)
                                       ▼
 ┌─────────────────────────────────────────────────────────────────────────────┐
 │                         LOCAL ROOM SQLITE DATABASE                          │
 │  - Stores all received raw telemetry records immediately                     │
 │  - Application UI renders directly from Room DB Flows                       │
 └─────────────────────────────────────────────────────────────────────────────┘
                                       │
                                       │ Enqueue Pending Records
                                       ▼
 ┌─────────────────────────────────────────────────────────────────────────────┐
 │                          OFFLINE SYNC QUEUE                                 │
 │  - WorkManager background task monitors network availability               │
 └─────────────────────────────────────────────────────────────────────────────┘
                                       │
                                       │ HTTPS REST POST (When Online)
                                       ▼
 ┌─────────────────────────────────────────────────────────────────────────────┐
 │                           MONGODB BACKEND API                               │
 └─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Key Synchronization Guarantees

1. **Zero Data Loss on Offline Execution**: Syncing with the watch over Bluetooth requires **zero internet connection**. Telemetry is cached locally in Room DB first.
2. **Background Auto-Reconnect**: Android `WorkManager` monitors GATT connection status and handles background reconnection without blocking the main UI looper.
3. **Idempotent Cloud Ingestion**: Cloud sync endpoints process payloads using deterministic record UUIDs/timestamps to prevent duplicate record insertion.
