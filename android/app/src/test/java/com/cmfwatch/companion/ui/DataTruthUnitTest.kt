package com.cmfwatch.companion.ui

import com.cmfwatch.companion.domain.models.DashboardSummary
import com.cmfwatch.companion.domain.models.DeviceConnectionState
import com.cmfwatch.companion.domain.models.HeartRateSample
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class DataTruthUnitTest {

    @Test
    fun testDisconnectedStateDisplaysUnavailableValues() {
        val summary = DashboardSummary(
            latestHeartRate = null,
            restingHeartRate = null,
            todaySteps = null,
            todayDistanceKm = null,
            todayCaloriesKcal = null,
            lastSleepMinutes = null,
            latestStressScore = null,
            deviceBatteryLevel = null,
            connectionState = DeviceConnectionState.DISCONNECTED,
            lastSyncedAt = null
        )

        assertEquals(DeviceConnectionState.DISCONNECTED, summary.connectionState)
        assertNull(summary.latestHeartRate)
        assertNull(summary.todaySteps)
        assertNull(summary.todayDistanceKm)
        assertNull(summary.todayCaloriesKcal)
        assertNull(summary.deviceBatteryLevel)
        assertNull(summary.lastSleepMinutes)
        assertNull(summary.latestStressScore)
    }

    @Test
    fun testRealHeartRateSamplePopulatesState() {
        val initial = DashboardSummary(
            latestHeartRate = null,
            restingHeartRate = null,
            todaySteps = null,
            todayDistanceKm = null,
            todayCaloriesKcal = null,
            lastSleepMinutes = null,
            latestStressScore = null,
            deviceBatteryLevel = 86,
            connectionState = DeviceConnectionState.CONNECTED_PAIRED,
            lastSyncedAt = null
        )

        val hr = HeartRateSample(timestamp = Instant.now(), bpm = 74, opcode = "0x0053")
        val updated = initial.copy(latestHeartRate = hr.bpm, lastSyncedAt = hr.timestamp)

        assertEquals(74, updated.latestHeartRate)
        assertEquals(86, updated.deviceBatteryLevel?.toInt())
        assertNull(updated.todaySteps)
    }
}
