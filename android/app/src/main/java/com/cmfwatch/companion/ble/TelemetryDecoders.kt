package com.cmfwatch.companion.ble

import com.cmfwatch.companion.domain.models.HeartRateSample
import com.cmfwatch.companion.domain.models.SpO2Sample
import com.cmfwatch.companion.domain.models.StepInterval
import com.cmfwatch.companion.domain.models.StressSample
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Instant

data class BatteryState(
    val level: Int,
    val isCharging: Boolean
)

object TelemetryDecoders {

    /**
     * Decode battery level response payload from opcode 0x005C 0x0001.
     */
    fun decodeBattery(payload: ByteArray): BatteryState? {
        if (payload.isEmpty()) return null
        val level = payload[0].toInt() and 0xFF
        val charging = if (payload.size > 1) (payload[1].toInt() != 0) else false
        return BatteryState(level = level.coerceIn(0, 100), isCharging = charging)
    }

    /**
     * Decode 8-byte Heart Rate sample payload: (epoch_sec: u32_le, bpm: u32_le).
     */
    fun decodeHeartRate(payload: ByteArray, opcode: String = "0x0053"): HeartRateSample? {
        if (payload.size < 8) return null
        val buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
        val epochSec = buffer.int.toLong() and 0xFFFFFFFFL
        val bpm = buffer.int and 0xFF
        if (bpm <= 0 || bpm > 255) return null
        val timestamp = Instant.ofEpochSecond(epochSec)
        return HeartRateSample(timestamp = timestamp, bpm = bpm, opcode = opcode)
    }

    /**
     * Decode 32-byte Step Interval payload: (epoch_sec: u32_le, steps: u32_le, dist_mm: u32_le, kcal_100: u32_le).
     */
    fun decodeStepInterval(payload: ByteArray): StepInterval? {
        if (payload.size < 16) return null
        val buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
        val epochSec = buffer.int.toLong() and 0xFFFFFFFFL
        val steps = buffer.int
        val distMm = buffer.int
        val kcal100 = buffer.int

        val timestamp = Instant.ofEpochSecond(epochSec)
        val distMeters = distMm / 1000.0f
        val caloriesKcal = kcal100 / 100.0f

        return StepInterval(
            timestamp = timestamp,
            steps = steps.coerceAtLeast(0),
            distanceMeters = distMeters.coerceAtLeast(0.0f),
            caloriesKcal = caloriesKcal.coerceAtLeast(0.0f)
        )
    }

    /**
     * Decode 8-byte Stress sample payload: (epoch_sec: u32_le, score: u32_le).
     */
    fun decodeStress(payload: ByteArray): StressSample? {
        if (payload.size < 8) return null
        val buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
        val epochSec = buffer.int.toLong() and 0xFFFFFFFFL
        val score = buffer.int and 0xFF
        val timestamp = Instant.ofEpochSecond(epochSec)
        return StressSample(timestamp = timestamp, score = score.coerceIn(1, 99))
    }

    /**
     * Decode 8-byte SpO2 sample payload: (epoch_sec: u32_le, percentage: u32_le).
     */
    fun decodeSpO2(payload: ByteArray): SpO2Sample? {
        if (payload.size < 8) return null
        val buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
        val epochSec = buffer.int.toLong() and 0xFFFFFFFFL
        val percentage = buffer.int and 0xFF
        if (percentage < 50 || percentage > 100) return null
        val timestamp = Instant.ofEpochSecond(epochSec)
        return SpO2Sample(timestamp = timestamp, percentage = percentage)
    }
}
