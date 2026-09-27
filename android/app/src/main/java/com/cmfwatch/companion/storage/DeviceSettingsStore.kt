package com.cmfwatch.companion.storage

import android.content.Context

data class DeviceSettings(
    val continuousHeartRate: Boolean = true,
    val allDaySpO2: Boolean = true,
    val sleepApneaBreathing: Boolean = true,
    val highStressReminders: Boolean = true,
    val hapticLevel: String = "Firm",
    val alwaysOnDisplay: Boolean = true
)

class DeviceSettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences("cmf_device_settings", Context.MODE_PRIVATE)

    fun loadSettings(): DeviceSettings {
        return DeviceSettings(
            continuousHeartRate = preferences.getBoolean(KEY_HR_TOGGLE, true),
            allDaySpO2 = preferences.getBoolean(KEY_SPO2_TOGGLE, true),
            sleepApneaBreathing = preferences.getBoolean(KEY_SLEEP_APNEA_TOGGLE, true),
            highStressReminders = preferences.getBoolean(KEY_STRESS_TOGGLE, true),
            hapticLevel = preferences.getString(KEY_HAPTIC_LEVEL, "Firm") ?: "Firm",
            alwaysOnDisplay = preferences.getBoolean(KEY_AOD_TOGGLE, true)
        )
    }

    fun saveSettings(settings: DeviceSettings) {
        preferences.edit()
            .putBoolean(KEY_HR_TOGGLE, settings.continuousHeartRate)
            .putBoolean(KEY_SPO2_TOGGLE, settings.allDaySpO2)
            .putBoolean(KEY_SLEEP_APNEA_TOGGLE, settings.sleepApneaBreathing)
            .putBoolean(KEY_STRESS_TOGGLE, settings.highStressReminders)
            .putString(KEY_HAPTIC_LEVEL, settings.hapticLevel)
            .putBoolean(KEY_AOD_TOGGLE, settings.alwaysOnDisplay)
            .apply()
    }

    private companion object {
        const val KEY_HR_TOGGLE = "continuous_hr"
        const val KEY_SPO2_TOGGLE = "all_day_spo2"
        const val KEY_SLEEP_APNEA_TOGGLE = "sleep_apnea"
        const val KEY_STRESS_TOGGLE = "high_stress"
        const val KEY_HAPTIC_LEVEL = "haptic_level"
        const val KEY_AOD_TOGGLE = "always_on_display"
    }
}
