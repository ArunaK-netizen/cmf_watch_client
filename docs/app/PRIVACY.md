# CMF Watch Companion Privacy & Security Architecture

This document defines privacy controls, zero-secret logging rules, credential storage standards, GPS data isolation, and user data export features for the **CMF Watch Companion Android Application**.

---

## 1. Zero-Secret Logging Rule

> [!CAUTION]
> **Strict Security Constraint**: BLE pairing authkeys, session nonces, derived AES-128 keys, device secrets, and authorization tokens must **NEVER** be printed to system `Logcat`, file logs, or console outputs.

### Redaction Rules in Code:
```kotlin
// WRONG: Logs raw key bytes
Log.d(TAG, "Derived session key: ${sessionKey.toHex()}")

// CORRECT: Redacts key bytes
Log.d(TAG, "Derived session key: [REDACTED]")
```

---

## 2. Sensitive Personal Data Boundaries

1. **GPS Coordinates**: Raw workout GPS track points (`0xA05A`) are stored exclusively in local encrypted database storage. GPS coordinates are **NEVER** committed to Git repositories or sent to unauthenticated third-party telemetry services.
2. **Local Credential Storage**: Pairing secrets (`cmf_config.json` / Android `EncryptedSharedPreferences`) are protected using Android Keystore hardware-backed keys.
3. **Data Export Controls**: Users retain 100% ownership of their data. The application provides an explicit **"Export Health Data"** feature allowing users to download their complete local dataset as standardized JSON or CSV files.
