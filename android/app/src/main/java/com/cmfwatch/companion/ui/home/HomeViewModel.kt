package com.cmfwatch.companion.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cmfwatch.companion.ble.CmfBleManager
import com.cmfwatch.companion.ble.CmfBleService
import com.cmfwatch.companion.domain.models.DashboardSummary
import com.cmfwatch.companion.domain.models.DeviceConnectionState
import com.cmfwatch.companion.storage.HealthSnapshotStore
import com.cmfwatch.companion.storage.LocalTelemetryStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val snapshotStore = HealthSnapshotStore(application.applicationContext)
    private val telemetryStore = LocalTelemetryStore(application.applicationContext)
    private val bleManager = CmfBleManager.getInstance(application.applicationContext)

    private val _uiState = MutableStateFlow(
        DashboardSummary(
            latestHeartRate = null,
            restingHeartRate = null,
            latestSpO2 = null,
            todaySteps = null,
            todayDistanceKm = null,
            todayCaloriesKcal = null,
            lastSleepMinutes = null,
            latestStressScore = null,
            deviceBatteryLevel = null,
            connectionState = bleManager.connectionState.value,
            lastSyncedAt = null,
            discoveredDevices = emptyList(),
            hrSamplesToday = emptyList(),
            hrSamplesAll = emptyList(),
            stepIntervalsToday = emptyList(),
            stepIntervalsAll = emptyList(),
            workoutsToday = emptyList(),
            workoutsAll = emptyList(),
            activeStandHours = 0,
            latestSleepSession = null,
            isPairedWithWatch = bleManager.hasSavedPairing()
        )
    )
    val uiState: StateFlow<DashboardSummary> = _uiState.asStateFlow()

    init {
        // Load initial persistent snapshots & local telemetry history
        telemetryStore.pruneOutdatedHeartRateSamples()
        val cached = snapshotStore.load()
        val allHR = telemetryStore.getHeartRateSamples()
        val todayHR = telemetryStore.getHeartRateSamplesToday()
        val allSteps = telemetryStore.getStepIntervals()
        val todaySteps = telemetryStore.getStepIntervalsToday()
        val allWorkouts = telemetryStore.getSavedWorkouts()
        val todayWorkouts = telemetryStore.getSavedWorkoutsToday()
        val latestSleep = telemetryStore.getLatestSleepSession()
        val stands = telemetryStore.computeStandHoursToday()

        // Authoritative production validation: only display today's latest heart rate
        val initialBpm = todayHR.lastOrNull()?.bpm ?: cached.latestHeartRate?.takeIf {
            cached.lastSyncedAt != null &&
            cached.lastSyncedAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate() == java.time.LocalDate.now()
        }
        val sleepMins = latestSleep?.totalSleepMinutes ?: cached.lastSleepMinutes

        // Compute today's step totals from the authoritative telemetry store
        val initialSteps = todaySteps.sumOf { it.steps }.let { if (it > 0) it else cached.todaySteps }
        val initialDist = todaySteps.sumOf { it.distanceMeters.toDouble() }.toFloat().let { d ->
            val km = d / 1000f
            if (km > 0f) km else cached.todayDistanceKm
        }
        val initialKcal = todaySteps.sumOf { it.caloriesKcal.toDouble() }.toInt().let { if (it > 0) it else cached.todayCaloriesKcal }

        _uiState.value = cached.copy(
            latestHeartRate = initialBpm,
            lastSleepMinutes = sleepMins,
            todaySteps = initialSteps,
            todayDistanceKm = initialDist,
            todayCaloriesKcal = initialKcal,
            connectionState = bleManager.connectionState.value,
            hrSamplesToday = todayHR,
            hrSamplesAll = allHR,
            stepIntervalsToday = todaySteps,
            stepIntervalsAll = allSteps,
            workoutsToday = todayWorkouts,
            workoutsAll = allWorkouts,
            activeStandHours = stands,
            latestSleepSession = latestSleep
        )

        // Observe Connection State & Auto-save MAC
        viewModelScope.launch {
            bleManager.connectionState.collect { state ->
                _uiState.value = _uiState.value.copy(connectionState = state)
                if (state == DeviceConnectionState.CONNECTED_PAIRED) {
                    bleManager.enableRealtimeStream()
                }
            }
        }

        // Observe Pairing State
        viewModelScope.launch {
            bleManager.isPairedWithWatch.collect { isPaired ->
                _uiState.value = _uiState.value.copy(isPairedWithWatch = isPaired)
            }
        }

        // Observe Discovered Peripherals
        viewModelScope.launch {
            bleManager.discoveredDevices.collect { devices ->
                _uiState.value = _uiState.value.copy(discoveredDevices = devices)
            }
        }

        // Observe Battery Telemetry
        viewModelScope.launch {
            bleManager.batteryState.collect { battery ->
                if (battery != null) {
                    val updated = _uiState.value.copy(deviceBatteryLevel = battery.level)
                    _uiState.value = updated
                    snapshotStore.save(updated)
                }
            }
        }

        // Observe Live Heart Rate Telemetry
        viewModelScope.launch {
            bleManager.heartRateFlow.collect { sample ->
                telemetryStore.saveHeartRateSample(sample)
                val allHRList = telemetryStore.getHeartRateSamples()
                val todayHRList = telemetryStore.getHeartRateSamplesToday()

                val updated = _uiState.value.copy(
                    latestHeartRate = sample.bpm,
                    hrSamplesToday = todayHRList,
                    hrSamplesAll = allHRList,
                    lastSyncedAt = Instant.now()
                )
                _uiState.value = updated
                snapshotStore.save(updated, sample.timestamp)
            }
        }

        // Observe Resting Heart Rate Telemetry
        viewModelScope.launch {
            bleManager.restingHeartRate.collect { resting ->
                if (resting != null) {
                    val updated = _uiState.value.copy(restingHeartRate = resting)
                    _uiState.value = updated
                    snapshotStore.save(updated)
                }
            }
        }

        // Observe Live Workout Active Status & Live Workout Heart Rate
        var workoutTimerJob: kotlinx.coroutines.Job? = null
        viewModelScope.launch {
            bleManager.isWorkoutActive.collect { active ->
                _uiState.value = _uiState.value.copy(isWorkoutActive = active)
                if (active) {
                    workoutTimerJob?.cancel()
                    workoutTimerJob = viewModelScope.launch {
                        while (_uiState.value.isWorkoutActive) {
                            delay(1000)
                            _uiState.value = _uiState.value.copy(
                                liveWorkoutDurationSec = _uiState.value.liveWorkoutDurationSec + 1
                            )
                        }
                    }
                } else {
                    workoutTimerJob?.cancel()
                    _uiState.value = _uiState.value.copy(
                        liveWorkoutDurationSec = 0,
                        liveWorkoutBpm = null
                    )
                }
            }
        }

        viewModelScope.launch {
            bleManager.liveWorkoutHeartRate.collect { bpm ->
                if (bpm != null) {
                    _uiState.value = _uiState.value.copy(
                        liveWorkoutBpm = bpm,
                        latestHeartRate = bpm
                    )
                }
            }
        }

        // Observe Step Intervals
        viewModelScope.launch {
            bleManager.stepFlow.collect { step ->
                telemetryStore.saveStepInterval(step)
                val allStepList = telemetryStore.getStepIntervals()
                val todayStepList = telemetryStore.getStepIntervalsToday()
                val standsCount = telemetryStore.computeStandHoursToday()

                // Compute totals from the authoritative store to prevent double-counting.
                // Each step interval has absolute values; we sum ALL today's intervals.
                val totalSteps = todayStepList.sumOf { it.steps }
                val totalDist = todayStepList.sumOf { it.distanceMeters.toDouble() }.toFloat() / 1000.0f
                val totalKcal = todayStepList.sumOf { it.caloriesKcal.toDouble() }.toInt()

                val updated = _uiState.value.copy(
                    todaySteps = totalSteps,
                    todayDistanceKm = totalDist,
                    todayCaloriesKcal = totalKcal,
                    stepIntervalsToday = todayStepList,
                    stepIntervalsAll = allStepList,
                    activeStandHours = standsCount,
                    lastSyncedAt = Instant.now()
                )
                _uiState.value = updated
                snapshotStore.save(updated)
            }
        }

        // Observe SpO2 Telemetry
        viewModelScope.launch {
            bleManager.spO2Flow.collect { sample ->
                val updated = _uiState.value.copy(
                    latestSpO2 = sample.percentage,
                    lastSyncedAt = Instant.now()
                )
                _uiState.value = updated
                snapshotStore.save(updated)
            }
        }

        // Observe Stress Telemetry
        viewModelScope.launch {
            bleManager.stressFlow.collect { sample ->
                val updated = _uiState.value.copy(
                    latestStressScore = sample.score,
                    lastSyncedAt = Instant.now()
                )
                _uiState.value = updated
                snapshotStore.save(updated)
            }
        }

        // Observe Sleep Telemetry
        viewModelScope.launch {
            bleManager.sleepFlow.collect { session ->
                telemetryStore.saveSleepSession(session)
                val updated = _uiState.value.copy(
                    lastSleepMinutes = session.totalSleepMinutes,
                    latestSleepSession = session,
                    lastSyncedAt = Instant.now()
                )
                _uiState.value = updated
                snapshotStore.save(updated)
            }
        }

        // Observe Workout Telemetry
        viewModelScope.launch {
            bleManager.workoutFlow.collect { workout ->
                telemetryStore.saveWorkout(workout)
                val todayW = telemetryStore.getSavedWorkoutsToday()
                val allW = telemetryStore.getSavedWorkouts()

                val updated = _uiState.value.copy(
                    workoutsToday = todayW,
                    workoutsAll = allW,
                    lastSyncedAt = Instant.now()
                )
                _uiState.value = updated
                snapshotStore.save(updated)
            }
        }

        // Auto-reconnect to preferred saved watch on app launch if not already connected
        if (bleManager.hasSavedPairing()) {
            val mac = bleManager.getSavedDeviceMac() ?: snapshotStore.preferredDeviceAddress()
            if (mac != null) {
                try {
                    CmfBleService.start(application.applicationContext)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                val currentState = bleManager.connectionState.value
                if (currentState != DeviceConnectionState.CONNECTED_PAIRED &&
                    currentState != DeviceConnectionState.CONNECTED &&
                    currentState != DeviceConnectionState.SUBSCRIBING
                ) {
                    connectToWatch(mac)
                }
            }
        }

        // Smart periodic sampling timer (every 2 minutes)
        startPeriodicSamplingTimer()
    }

    private fun startPeriodicSamplingTimer() {
        viewModelScope.launch {
            while (true) {
                delay(120_000) // 2 minutes smart sync
                if (_uiState.value.connectionState == DeviceConnectionState.CONNECTED_PAIRED) {
                    bleManager.fetchBattery()
                    bleManager.enableRealtimeStream()
                    bleManager.triggerSync()
                }
            }
        }
    }

    fun startScan() {
        bleManager.startScan()
    }

    fun stopScan() {
        bleManager.stopScan()
    }

    fun connectToWatch(macAddress: String) {
        snapshotStore.savePreferredDevice(macAddress)
        try {
            CmfBleService.start(getApplication())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        bleManager.connect(macAddress)
    }

    fun disconnect() {
        try {
            CmfBleService.stop(getApplication())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        bleManager.disconnect()
    }

    fun unpairWatch() {
        try {
            CmfBleService.stop(getApplication())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        bleManager.unpair()
        telemetryStore.clearAll()
        // Reset dashboard summary to clean empty state
        _uiState.value = DashboardSummary(
            latestHeartRate = null,
            restingHeartRate = null,
            latestSpO2 = null,
            todaySteps = null,
            todayDistanceKm = null,
            todayCaloriesKcal = null,
            lastSleepMinutes = null,
            latestStressScore = null,
            deviceBatteryLevel = null,
            connectionState = DeviceConnectionState.IDLE,
            lastSyncedAt = null,
            isPairedWithWatch = false
        )
    }

    fun startWorkout(type: String = "Outdoor Run") {
        _uiState.value = _uiState.value.copy(
            activeWorkoutType = type,
            isWorkoutActive = true,
            liveWorkoutDurationSec = 0
        )
        bleManager.startWorkoutMode()
    }

    fun stopWorkout() {
        bleManager.stopWorkoutMode()
        _uiState.value = _uiState.value.copy(
            isWorkoutActive = false,
            liveWorkoutDurationSec = 0,
            liveWorkoutBpm = null
        )
    }

    fun triggerSync() {
        bleManager.triggerSync()
    }
}
