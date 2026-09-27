package com.cmfwatch.companion.ble

import com.cmfwatch.companion.domain.models.HeartRateSample
import com.cmfwatch.companion.domain.models.SleepEpoch
import com.cmfwatch.companion.domain.models.SleepSession
import com.cmfwatch.companion.domain.models.SleepStage
import com.cmfwatch.companion.domain.models.SpO2Sample
import com.cmfwatch.companion.domain.models.StepInterval
import com.cmfwatch.companion.domain.models.StressSample
import com.cmfwatch.companion.storage.SavedWorkout
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
     * Decode SpO2 sample payload (supports 8-byte, 4-byte, or raw byte push payloads).
     */
    fun decodeSpO2(payload: ByteArray): SpO2Sample? {
        if (payload.isEmpty()) return null

        // 1. Try standard 8-byte payload (epochSec + percentage)
        if (payload.size >= 8) {
            try {
                val buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
                val epochSec = buffer.int.toLong() and 0xFFFFFFFFL
                val percentage = buffer.int and 0xFF
                if (percentage in 50..100 && epochSec > 1600000000L) {
                    return SpO2Sample(timestamp = Instant.ofEpochSecond(epochSec), percentage = percentage)
                }
            } catch (e: Exception) {
                // Fallthrough to fallback scanner
            }
        }

        // 2. Fallback scanner: Find any byte in 50..100 range representing SpO2 %
        for (b in payload) {
            val valU8 = b.toInt() and 0xFF
            if (valU8 in 50..100) {
                return SpO2Sample(timestamp = Instant.now(), percentage = valU8)
            }
        }

        return null
    }

    /**
     * Decode SLEEP_DATA payload (opcode 0x0058 0x0001):
     * 18-byte Header (LE) + N x 8-byte Stage records.
     */
    fun decodeSleepData(payload: ByteArray): SleepSession? {
        if (payload.size < 18) return null
        try {
            val buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
            val sessionStartSec = buffer.int.toLong() and 0xFFFFFFFFL
            val wakeupTimeSec = buffer.int.toLong() and 0xFFFFFFFFL
            val totalDeepSec = buffer.short.toInt() and 0xFFFF
            val totalLightSec = buffer.short.toInt() and 0xFFFF
            val totalRemSec = buffer.short.toInt() and 0xFFFF
            val totalAwakeSec = buffer.short.toInt() and 0xFFFF
            buffer.short // metadata skip

            val startTime = Instant.ofEpochSecond(sessionStartSec)
            val endTime = Instant.ofEpochSecond(wakeupTimeSec)
            val totalSleepMinutes = ((totalDeepSec + totalLightSec + totalRemSec) / 60).coerceAtLeast(0)

            val epochs = mutableListOf<SleepEpoch>()
            var offset = 18
            while (offset + 8 <= payload.size) {
                val stageBuffer = ByteBuffer.wrap(payload, offset, 8).order(ByteOrder.LITTLE_ENDIAN)
                val stageStartSec = stageBuffer.int.toLong() and 0xFFFFFFFFL
                val durationVal = stageBuffer.short.toInt() and 0xFFFF
                val stageCode = stageBuffer.short.toInt() and 0xFFFF

                val stage = when (stageCode) {
                    1 -> SleepStage.DEEP
                    2 -> SleepStage.LIGHT
                    3 -> SleepStage.REM
                    else -> SleepStage.AWAKE
                }
                val durationMins = (durationVal / 60).coerceAtLeast(1)
                epochs.add(
                    SleepEpoch(
                        stage = stage,
                        startTime = Instant.ofEpochSecond(stageStartSec),
                        durationMinutes = durationMins
                    )
                )
                offset += 8
            }

            return SleepSession(
                startTime = startTime,
                endTime = endTime,
                totalSleepMinutes = totalSleepMinutes,
                epochs = epochs
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Decode WORKOUT_SUMMARY payload (opcode 0x0057 0x0001 or 0x0160 0x0001):
     * Layout: start_time(u32), duration_sec(u32), distance_m(u32), calories(u32), avg_hr(u32), max_hr(u32), avg_pace(u32), type_id(u32).
     */
    fun decodeWorkoutSummary(payload: ByteArray): SavedWorkout? {
        if (payload.size < 28) return null
        try {
            val buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
            val startSec = buffer.int.toLong() and 0xFFFFFFFFL
            val durationSec = buffer.int.toLong() and 0xFFFFFFFFL
            val distanceMeters = buffer.int.toLong() and 0xFFFFFFFFL
            val caloriesVal = buffer.int.toLong() and 0xFFFFFFFFL
            val avgHr = buffer.int and 0xFF
            val maxHr = buffer.int and 0xFF
            val avgPaceSec = if (buffer.remaining() >= 4) buffer.int else 0
            val typeId = if (buffer.remaining() >= 4) buffer.int else 1

            val typeName = when (typeId) {
                1 -> "Outdoor Run"
                2 -> "Outdoor Walk"
                3 -> "Outdoor Cycle"
                4 -> "Indoor Run"
                5 -> "HIIT"
                6 -> "Yoga"
                else -> "Freestyle Workout"
            }

            val startTime = Instant.ofEpochSecond(startSec)
            val durationMins = (durationSec / 60).toInt().coerceAtLeast(1)
            val distKm = distanceMeters / 1000.0f
            val caloriesKcal = caloriesVal.toInt().coerceAtLeast(0)

            return SavedWorkout(
                type = typeName,
                startTime = startTime,
                durationMinutes = durationMins,
                distanceKm = distKm,
                caloriesKcal = caloriesKcal,
                avgHr = avgHr,
                maxHr = maxHr,
                avgPaceSec = avgPaceSec
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
