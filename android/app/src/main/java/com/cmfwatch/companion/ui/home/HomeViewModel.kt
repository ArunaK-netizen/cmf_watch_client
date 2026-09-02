package com.cmfwatch.companion.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cmfwatch.companion.ble.CmfBleManager
import com.cmfwatch.companion.domain.models.DashboardSummary
import com.cmfwatch.companion.domain.models.DeviceConnectionState
import com.cmfwatch.companion.domain.models.DiscoveredDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val bleManager = CmfBleManager(application.applicationContext)

    private val _uiState = MutableStateFlow(
        DashboardSummary(
            latestHeartRate = null,
            restingHeartRate = null,
            todaySteps = null,
            todayDistanceKm = null,
            todayCaloriesKcal = null,
            lastSleepMinutes = null,
            latestStressScore = null,
            deviceBatteryLevel = null,
            connectionState = DeviceConnectionState.IDLE,
            lastSyncedAt = null,
            discoveredDevices = emptyList()
        )
    )
    val uiState: StateFlow<DashboardSummary> = _uiState.asStateFlow()

    init {
        // Observe Connection State
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
                    _uiState.value = _uiState.value.copy(deviceBatteryLevel = battery.level)
                }
            }
        }

        // Observe Live Heart Rate Samples
        viewModelScope.launch {
            bleManager.heartRateFlow.collect { hr ->
                _uiState.value = _uiState.value.copy(
                    latestHeartRate = hr.bpm,
                    lastSyncedAt = Instant.now()
                )
            }
        }

        // Observe Step Intervals
        viewModelScope.launch {
            bleManager.stepFlow.collect { step ->
                val newSteps = (_uiState.value.todaySteps ?: 0) + step.steps
                val newDist = (_uiState.value.todayDistanceKm ?: 0.0f) + (step.distanceMeters / 1000.0f)
                val newKcal = (_uiState.value.todayCaloriesKcal ?: 0) + step.caloriesKcal.toInt()

                _uiState.value = _uiState.value.copy(
                    todaySteps = newSteps,
                    todayDistanceKm = newDist,
                    todayCaloriesKcal = newKcal
                )
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
        bleManager.connect(macAddress)
    }

    fun disconnect() {
        bleManager.disconnect()
    }

    fun triggerSync() {
        bleManager.triggerSync()
    }
}
