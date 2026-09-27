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
            latestSleepSession = null
        )
    )
    val uiState: StateFlow<DashboardSummary> = _uiState.asStateFlow()

    init {
        // Load initial persistent snapshots & local telemetry history
        val cached = snapshotStore.load()
        val allHR = telemetryStore.getHeartRateSamples()
        val todayHR = telemetryStore.getHeartRateSamplesToday()
        val allSteps = telemetryStore.getStepIntervals()
        val todaySteps = telemetryStore.getStepIntervalsToday()
        val allWorkouts = telemetryStore.getSavedWorkouts()
        val todayWorkouts = telemetryStore.getSavedWorkoutsToday()
        val latestSleep = telemetryStore.getLatestSleepSession()
        val stands = telemetryStore.computeStandHoursToday()

        val initialBpm = todayHR.lastOrNull()?.bpm ?: cached.latestHeartRate
        val sleepMins = latestSleep?.totalSleepMinutes ?: cached.lastSleepMinutes

        _uiState.value = cached.copy(
            latestHeartRate = initialBpm,
            lastSleepMinutes = sleepMins,
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
                snapshotStore.save(updated)
            }
        }

        // Observe Step Intervals
        viewModelScope.launch {
            bleManager.stepFlow.collect { step ->
                telemetryStore.saveStepInterval(step)
                val allStepList = telemetryStore.getStepIntervals()
                val todayStepList = telemetryStore.getStepIntervalsToday()
                val standsCount = telemetryStore.computeStandHoursToday()

                val newSteps = (_uiState.value.todaySteps ?: 0) + step.steps
                val newDist = (_uiState.value.todayDistanceKm ?: 0.0f) + (step.distanceMeters / 1000.0f)
                val newKcal = (_uiState.value.todayCaloriesKcal ?: 0) + step.caloriesKcal.toInt()

                val updated = _uiState.value.copy(
                    todaySteps = newSteps,
                    todayDistanceKm = newDist,
                    todayCaloriesKcal = newKcal,
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
        snapshotStore.preferredDeviceAddress()?.let { mac ->
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

        // Smart periodic sampling timer (every 2 minutes)
        startPeriodicSamplingTimer()
    }

    private fun startPeriodicSamplingTimer() {
        viewModelScope.launch {
            while (true) {
                delay(120_000) // 2 minutes smart sync
                if (_uiState.value.connectionState == DeviceConnectionState.CONNECTED_PAIRED) {
                    bleManager.fetchBattery()
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

    fun triggerSync() {
        bleManager.triggerSync()
    }
}
