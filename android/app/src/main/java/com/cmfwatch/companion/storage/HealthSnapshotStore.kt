package com.cmfwatch.companion.storage

import android.content.Context
import com.cmfwatch.companion.domain.models.DashboardSummary
import com.cmfwatch.companion.domain.models.DeviceConnectionState
import java.time.Instant
import java.time.LocalDate

class HealthSnapshotStore(context: Context) {
    private val preferences = context.getSharedPreferences("cmf_health_snapshot", Context.MODE_PRIVATE)

    fun load(): DashboardSummary {
        val todayStr = LocalDate.now().toString()
        val savedDate = preferences.getString(KEY_SNAPSHOT_DATE, null)

        val syncedAt = preferences.getString(KEY_LAST_SYNCED_AT, null)?.let { value ->
            runCatching { Instant.parse(value) }.getOrNull()
        }

        val isSameDay = savedDate == todayStr

        return DashboardSummary(
            latestHeartRate = if (isSameDay) preferences.getNullableInt(KEY_HEART_RATE) else null,
            restingHeartRate = if (isSameDay) preferences.getNullableInt(KEY_RESTING_HEART_RATE) else null,
            latestSpO2 = if (isSameDay) preferences.getNullableInt(KEY_SPO2) else null,
            todaySteps = if (isSameDay) preferences.getNullableInt(KEY_STEPS) else null,
            todayDistanceKm = if (isSameDay) preferences.getNullableFloat(KEY_DISTANCE_KM) else null,
            todayCaloriesKcal = if (isSameDay) preferences.getNullableInt(KEY_CALORIES) else null,
            lastSleepMinutes = if (isSameDay) preferences.getNullableInt(KEY_SLEEP_MINUTES) else null,
            latestStressScore = if (isSameDay) preferences.getNullableInt(KEY_STRESS) else null,
            deviceBatteryLevel = preferences.getNullableInt(KEY_BATTERY),
            connectionState = DeviceConnectionState.IDLE,
            lastSyncedAt = syncedAt
        )
    }

    fun save(summary: DashboardSummary) {
        val todayStr = LocalDate.now().toString()
        preferences.edit()
            .putString(KEY_SNAPSHOT_DATE, todayStr)
            .putNullableInt(KEY_HEART_RATE, summary.latestHeartRate)
            .putNullableInt(KEY_RESTING_HEART_RATE, summary.restingHeartRate)
            .putNullableInt(KEY_SPO2, summary.latestSpO2)
            .putNullableInt(KEY_STEPS, summary.todaySteps)
            .putNullableFloat(KEY_DISTANCE_KM, summary.todayDistanceKm)
            .putNullableInt(KEY_CALORIES, summary.todayCaloriesKcal)
            .putNullableInt(KEY_SLEEP_MINUTES, summary.lastSleepMinutes)
            .putNullableInt(KEY_STRESS, summary.latestStressScore)
            .putNullableInt(KEY_BATTERY, summary.deviceBatteryLevel)
            .apply {
                summary.lastSyncedAt?.let { putString(KEY_LAST_SYNCED_AT, it.toString()) }
                    ?: remove(KEY_LAST_SYNCED_AT)
            }
            .apply()
    }

    fun savePreferredDevice(address: String) {
        preferences.edit().putString(KEY_DEVICE_ADDRESS, address).apply()
    }

    fun preferredDeviceAddress(): String? = preferences.getString(KEY_DEVICE_ADDRESS, null)

    private fun android.content.SharedPreferences.getNullableInt(key: String): Int? =
        if (contains(key)) getInt(key, 0) else null

    private fun android.content.SharedPreferences.getNullableFloat(key: String): Float? =
        if (contains(key)) getFloat(key, 0.0f) else null

    private fun android.content.SharedPreferences.Editor.putNullableInt(key: String, value: Int?): android.content.SharedPreferences.Editor =
        if (value == null) remove(key) else putInt(key, value)

    private fun android.content.SharedPreferences.Editor.putNullableFloat(key: String, value: Float?): android.content.SharedPreferences.Editor =
        if (value == null) remove(key) else putFloat(key, value)

    private companion object {
        const val KEY_SNAPSHOT_DATE = "snapshot_date"
        const val KEY_HEART_RATE = "latest_heart_rate"
        const val KEY_RESTING_HEART_RATE = "resting_heart_rate"
        const val KEY_SPO2 = "latest_spo2"
        const val KEY_STEPS = "today_steps"
        const val KEY_DISTANCE_KM = "today_distance_km"
        const val KEY_CALORIES = "today_calories"
        const val KEY_SLEEP_MINUTES = "last_sleep_minutes"
        const val KEY_STRESS = "latest_stress"
        const val KEY_BATTERY = "device_battery"
        const val KEY_LAST_SYNCED_AT = "last_synced_at"
        const val KEY_DEVICE_ADDRESS = "preferred_device_address"
    }
}
