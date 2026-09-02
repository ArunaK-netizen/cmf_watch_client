# CMF Watch Companion Application Architecture

This document defines the high-level system architecture, software layering, data flow, reactive state management, and domain separation for the **CMF Watch Companion Android Application**.

---

## 1. Architectural Layers & Separation of Concerns

The application follows **Clean Architecture** principles and **Android Unidirectional Data Flow (UDF)** recommendations.

```text
 ┌─────────────────────────────────────────────────────────────────────────────┐
 │                         PRESENTATION LAYER (UI)                             │
 │   - Jetpack Compose Screens (Home, Health, Activity, Sleep, Device, History)│
 │   - ViewModels (StateFlow / UI State management)                            │
 │   - Material 3 Design System & Nothing NType82 Typography                   │
 └─────────────────────────────────────────────────────────────────────────────┘
                                       │
                                       ▼  (Observes UI State / Triggers Events)
 ┌─────────────────────────────────────────────────────────────────────────────┐
 │                           DOMAIN LAYER (CORE)                               │
 │   - Domain Models (HealthSummary, HeartRateSample, SleepSession, Workout)   │
 │   - Use Cases (SyncHealthDataUseCase, GetDashboardSummaryUseCase, etc.)     │
 │   - Replaceable Algorithm Engines (StrideEngine, StressNormalizer, Sleep)   │
 └─────────────────────────────────────────────────────────────────────────────┘
                                       │
                                       ▼  (Calls Repositories)
 ┌─────────────────────────────────────────────────────────────────────────────┐
 │                            DATA LAYER (STORAGE)                             │
 │   - Repositories (HealthRepository, DeviceRepository, SyncRepository)       │
 │   - Local Data Source: Room SQLite Database (`cmf_health.db`)              │
 │   - Remote Data Source: Ktor / Retrofit MongoDB REST API Client             │
 └─────────────────────────────────────────────────────────────────────────────┘
                                       │
                                       ▼  (Communicates over GATT)
 ┌─────────────────────────────────────────────────────────────────────────────┐
 │                             BLE PROTOCOL LAYER                              │
 │   - CMF Watch BLE GATT Driver & Connection Manager                         │
 │   - Protocol Framer, AES-128-CBC Decryptor, Nonce Handshake (0xFFFF 8047)  │
 │   - Telemetry Decoders (0x0053, 0x0056, 0x0058, 0x009D, 0x0055, 0xA05A)     │
 └─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Unidirectional Data Flow (UDF) & State Management

Each screen is powered by a dedicated `ViewModel` maintaining a single immutable `UI State` data class emitted via Kotlin `StateFlow`.

### Key UI States:
- **`Loading`**: Fetching local cached data or authenticating session.
- **`Connected`**: GATT peripheral connected, session key established.
- **`Disconnected`**: Watch disconnected, displaying last cached telemetry.
- **`Syncing`**: Active `ACTIVITY_FETCH_1/2` burst in progress.
- **`NoData`**: Fresh installation or no watch records recorded yet.
- **`Error`**: Connection timeout or BLE error state.

---

## 3. Replaceable Algorithm Engine Layer

To fulfill our long-term goal of improving upon proprietary Nothing X calculations, all algorithmic transformations operate behind strict interface contracts in `domain/algorithms/`:

```kotlin
interface StrideLengthEngine {
    fun calculateStrideLength(cadenceSpm: Float, userHeightCm: Float): Float
    fun calculateDistance(stepIntervals: List<StepInterval>, userHeightCm: Float): Float
}

interface StressNormalizationEngine {
    fun normalizeStress(rawRmssd: Float, userBaseline: HrvBaseline): Int
}

interface SleepStageFilterEngine {
    fun smoothStageSequence(rawStages: List<SleepStageInterval>): List<SleepStageInterval>
}
```

This guarantees that we can plug in custom statistical models, machine learning algorithms, or physiological equations without modifying the UI layer or data storage layer.
