package com.cmfwatch.companion.domain.models

import java.time.Instant

enum class DeviceConnectionState {
    IDLE,
    SCANNING,
    DEVICES_FOUND,
    CONNECTING,
    CONNECTED,
    DISCOVERING_SERVICES,
    SUBSCRIBING,
    AUTHENTICATING,
    CONNECTED_PAIRED, // READY
    SYNCING,
    DISCONNECTING,
    DISCONNECTED,
    ERROR
}

data class DiscoveredDevice(
    val name: String,
    val address: String,
    val rssi: Int
)

data class HeartRateSample(
    val timestamp: Instant,
    val bpm: Int,
    val opcode: String
)

data class StepInterval(
    val timestamp: Instant,
    val steps: Int,
    val distanceMeters: Float,
    val caloriesKcal: Float
)

enum class SleepStage {
    DEEP,
    LIGHT,
    REM,
    AWAKE
}

data class SleepEpoch(
    val stage: SleepStage,
    val startTime: Instant,
    val durationMinutes: Int
)

data class SleepSession(
    val startTime: Instant,
    val endTime: Instant,
    val totalSleepMinutes: Int,
    val epochs: List<SleepEpoch>
)

data class StressSample(
    val timestamp: Instant,
    val score: Int
)

data class SpO2Sample(
    val timestamp: Instant,
    val percentage: Int
)

data class DashboardSummary(
    val latestHeartRate: Int?,
    val restingHeartRate: Int?,
    val todaySteps: Int?,
    val todayDistanceKm: Float?,
    val todayCaloriesKcal: Int?,
    val lastSleepMinutes: Int?,
    val latestStressScore: Int?,
    val deviceBatteryLevel: Int?,
    val connectionState: DeviceConnectionState,
    val lastSyncedAt: Instant?,
    val discoveredDevices: List<DiscoveredDevice> = emptyList()
)
