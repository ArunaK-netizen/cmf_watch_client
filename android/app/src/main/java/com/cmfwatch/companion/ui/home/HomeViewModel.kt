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
            workoutsToday = emptyList()
        )
    )
    val uiState: StateFlow<DashboardSummary> = _uiState.asStateFlow()

    init {
        // Load initial persistent snapshots & local telemetry history
        val cached = snapshotStore.load()
        val savedHR = telemetryStore.getHeartRateSamples()
        val savedSteps = telemetryStore.getStepIntervals()
        val savedWorkouts = telemetryStore.getSavedWorkouts()

        val initialBpm = savedHR.lastOrNull()?.bpm ?: cached.latestHeartRate

        _uiState.value = cached.copy(
            latestHeartRate = initialBpm,
            connectionState = bleManager.connectionState.value,
            hrSamplesToday = savedHR,
            stepIntervalsToday = savedSteps,
            workoutsToday = savedWorkouts
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
                val allHR = telemetryStore.getHeartRateSamples()

                val updated = _uiState.value.copy(
                    latestHeartRate = sample.bpm,
                    hrSamplesToday = allHR,
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
                val allSteps = telemetryStore.getStepIntervals()
                val newSteps = (_uiState.value.todaySteps ?: 0) + step.steps
                val newDist = (_uiState.value.todayDistanceKm ?: 0.0f) + (step.distanceMeters / 1000.0f)
                val newKcal = (_uiState.value.todayCaloriesKcal ?: 0) + step.caloriesKcal.toInt()

                val updated = _uiState.value.copy(
                    todaySteps = newSteps,
                    todayDistanceKm = newDist,
                    todayCaloriesKcal = newKcal,
                    stepIntervalsToday = allSteps,
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
