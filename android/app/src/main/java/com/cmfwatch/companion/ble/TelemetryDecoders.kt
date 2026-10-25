package com.cmfwatch.companion.ble

import com.cmfwatch.companion.domain.models.CmfSport
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
     * Decode Heart Rate sample payload: returns the latest sample in the payload.
     */
    fun decodeHeartRate(payload: ByteArray, opcode: String = "0x0053"): HeartRateSample? {
        val batch = decodeHeartRateBatch(payload, opcode)
        return batch.lastOrNull()
    }

    /**
     * Decode N x 8-byte Heart Rate sample records: (epoch_sec: u32_le, bpm: u32_le)
     * or 5-byte records: (epoch_sec: u32_le, bpm: u8)
     * or short 1-4 byte live push notifications: (bpm: u8).
     * Returns all valid samples in chronological order.
     */
    fun decodeHeartRateBatch(payload: ByteArray, opcode: String = "0x0053"): List<HeartRateSample> {
        val samples = mutableListOf<HeartRateSample>()
        if (payload.isEmpty()) return samples

        // Case 1: Multiples of 8 bytes: (epoch_sec: u32_le, bpm: u32_le)
        if (payload.size >= 8 && payload.size % 8 == 0) {
            val buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
            val count = payload.size / 8
            for (i in 0 until count) {
                val epochSec = buffer.int.toLong() and 0xFFFFFFFFL
                val bpm = buffer.int and 0xFF
                // Valid human physiological heart rate (30 to 240 BPM) and modern Unix timestamp
                if (bpm in 30..240 && epochSec > 1600000000L) {
                    samples.add(
                        HeartRateSample(
                            timestamp = Instant.ofEpochSecond(epochSec),
                            bpm = bpm,
                            opcode = opcode
                        )
                    )
                }
            }
            if (samples.isNotEmpty()) {
                return samples.sortedBy { it.timestamp }
            }
        }

        // Case 2: Multiples of 5 bytes: (epoch_sec: u32_le, bpm: u8)
        if (payload.size >= 5 && payload.size % 5 == 0) {
            val buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
            val count = payload.size / 5
            for (i in 0 until count) {
                val epochSec = buffer.int.toLong() and 0xFFFFFFFFL
                val bpm = buffer.get().toInt() and 0xFF
                if (bpm in 30..240 && epochSec > 1600000000L) {
                    samples.add(
                        HeartRateSample(
                            timestamp = Instant.ofEpochSecond(epochSec),
                            bpm = bpm,
                            opcode = opcode
                        )
                    )
                }
            }
            if (samples.isNotEmpty()) {
                return samples.sortedBy { it.timestamp }
            }
        }

        // Case 3: Arbitrary buffer containing 8-byte chunks (even if trailing padding exists)
        if (payload.size >= 8) {
            val buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
            val count = payload.size / 8
            for (i in 0 until count) {
                val epochSec = buffer.int.toLong() and 0xFFFFFFFFL
                val bpm = buffer.int and 0xFF
                if (bpm in 30..240 && epochSec > 1600000000L) {
                    samples.add(
                        HeartRateSample(
                            timestamp = Instant.ofEpochSecond(epochSec),
                            bpm = bpm,
                            opcode = opcode
                        )
                    )
                }
            }
            if (samples.isNotEmpty()) {
                return samples.sortedBy { it.timestamp }
            }
        }

        // Case 4: Real-time 1-4 byte live pulse push notification from watch
        // When realtime stream is active, the watch pushes the live BPM directly
        if (payload.size in 1..4) {
            for (b in payload) {
                val bpm = b.toInt() and 0xFF
                if (bpm in 35..230) {
                    samples.add(
                        HeartRateSample(
                            timestamp = Instant.now(),
                            bpm = bpm,
                            opcode = opcode
                        )
                    )
                    break
                }
            }
        }

        return samples
    }

    /**
     * Decode resting heart rate payload (opcode 0x00DA 0x0001).
     * Supports 5-byte records (ts: u32 LE, hr: u8) or 8-byte records (ts: u32 LE, hr: u32 LE).
     */
    fun decodeRestingHeartRateBatch(payload: ByteArray): List<HeartRateSample> {
        val samples = mutableListOf<HeartRateSample>()
        if (payload.isEmpty()) return samples
        if (payload.size % 5 == 0 && payload.isNotEmpty()) {
            val buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
            val count = payload.size / 5
            for (i in 0 until count) {
                val epochSec = buffer.int.toLong() and 0xFFFFFFFFL
                val hr = buffer.get().toInt() and 0xFF
                if (hr in 30..200 && epochSec > 1600000000L) {
                    samples.add(
                        HeartRateSample(
                            timestamp = Instant.ofEpochSecond(epochSec),
                            bpm = hr,
                            opcode = "0x00DA"
                        )
                    )
                }
            }
            return samples.sortedBy { it.timestamp }
        }
        return decodeHeartRateBatch(payload, "0x00DA")
    }

    /**
     * Decode Step Interval payload: returns the latest interval in the payload.
     */
    fun decodeStepInterval(payload: ByteArray): StepInterval? {
        val batch = decodeStepIntervalBatch(payload)
        return batch.lastOrNull()
    }

    /**
     * Decode N x 32-byte or 16-byte Step Interval records:
     * (epoch_sec: u32_le, steps: u32_le, dist_mm: u32_le, kcal_100: u32_le).
     */
    fun decodeStepIntervalBatch(payload: ByteArray): List<StepInterval> {
        val intervals = mutableListOf<StepInterval>()
        if (payload.size < 16) return intervals
        val recordSize = if (payload.size % 32 == 0) 32 else 16
        val count = payload.size / recordSize
        val buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)

        for (i in 0 until count) {
            val epochSec = buffer.int.toLong() and 0xFFFFFFFFL
            val steps = buffer.int
            val distMm = buffer.int
            val kcal100 = buffer.int
            if (recordSize == 32) {
                // Skip 16 reserved padding bytes
                buffer.position(buffer.position() + 16)
            }

            if (epochSec > 1600000000L && steps >= 0) {
                intervals.add(
                    StepInterval(
                        timestamp = Instant.ofEpochSecond(epochSec),
                        steps = steps.coerceAtLeast(0),
                        distanceMeters = (distMm / 1000.0f).coerceAtLeast(0.0f),
                        caloriesKcal = (kcal100 / 100.0f).coerceAtLeast(0.0f)
                    )
                )
            }
        }
        return intervals.sortedBy { it.timestamp }
    }

    /**
     * Decode Stress sample payload: returns latest sample.
     */
    fun decodeStress(payload: ByteArray): StressSample? {
        val batch = decodeStressBatch(payload)
        return batch.lastOrNull()
    }

    /**
     * Decode N x 8-byte Stress sample records: (epoch_sec: u32_le, score: u32_le).
     */
    fun decodeStressBatch(payload: ByteArray): List<StressSample> {
        val samples = mutableListOf<StressSample>()
        if (payload.size < 8) return samples
        val buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
        val count = payload.size / 8
        for (i in 0 until count) {
            val epochSec = buffer.int.toLong() and 0xFFFFFFFFL
            val score = buffer.int and 0xFF
            if (score in 1..99 && epochSec > 1600000000L) {
                samples.add(StressSample(timestamp = Instant.ofEpochSecond(epochSec), score = score))
            }
        }
        return samples.sortedBy { it.timestamp }
    }

    /**
     * Decode SpO2 sample payload (supports 8-byte batch, 4-byte, or raw byte push payloads).
     */
    fun decodeSpO2(payload: ByteArray): SpO2Sample? {
        val batch = decodeSpO2Batch(payload)
        return batch.lastOrNull()
    }

    /**
     * Decode N x 8-byte SpO2 sample records: (epoch_sec: u32_le, percentage: u32_le).
     */
    fun decodeSpO2Batch(payload: ByteArray): List<SpO2Sample> {
        val samples = mutableListOf<SpO2Sample>()
        if (payload.size >= 8) {
            val buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
            val count = payload.size / 8
            for (i in 0 until count) {
                val epochSec = buffer.int.toLong() and 0xFFFFFFFFL
                val percentage = buffer.int and 0xFF
                if (percentage in 50..100 && epochSec > 1600000000L) {
                    samples.add(SpO2Sample(timestamp = Instant.ofEpochSecond(epochSec), percentage = percentage))
                }
            }
            if (samples.isNotEmpty()) return samples.sortedBy { it.timestamp }
        }

        // Fallback scanner for single raw byte push
        for (b in payload) {
            val valU8 = b.toInt() and 0xFF
            if (valU8 in 50..100) {
                samples.add(SpO2Sample(timestamp = Instant.now(), percentage = valU8))
                break
            }
        }
        return samples
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
     * Decode WORKOUT_SUMMARY batch payload (opcode 0x0057 0x0001 or 0x0160 0x0001):
     * Supports N x 32-byte records (standard) or N x 54-byte records (extended).
     * Layout: start_time(u32), duration_sec(u16), type_id(u8), flags(u8), distance_m(u32), calories(u32), avg_hr(u8), max_hr(u8), avg_pace(u16)...
     */
    fun decodeWorkoutSummaryBatch(payload: ByteArray): List<SavedWorkout> {
        val list = mutableListOf<SavedWorkout>()
        if (payload.size < 20) return list

        val recordSize = when {
            payload.size % 54 == 0 -> 54
            payload.size % 32 == 0 -> 32
            payload.size >= 54 -> 54
            payload.size >= 32 -> 32
            else -> payload.size
        }

        val count = (payload.size / recordSize).coerceAtLeast(1)
        val buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)

        for (i in 0 until count) {
            val recordStartPos = i * recordSize
            buffer.position(recordStartPos)

            val startSec = buffer.int.toLong() and 0xFFFFFFFFL
            val durationShort = buffer.short.toInt() and 0xFFFF
            val byte6 = if (buffer.hasRemaining()) buffer.get().toInt() and 0xFF else 1
            val byte7 = if (buffer.hasRemaining()) buffer.get().toInt() and 0xFF else 0

            val (durationSec, typeId) = if (byte6 in 1..35) {
                Pair(durationShort, byte6)
            } else if (byte7 in 1..35) {
                Pair(durationShort, byte7)
            } else {
                val d = (durationShort.toLong()) or ((byte6.toLong()) shl 16) or ((byte7.toLong()) shl 24)
                if (d in 1..86400L) {
                    val fallbackType = if (buffer.remaining() >= 4) buffer.int else 1
                    Pair(d.toInt(), fallbackType)
                } else {
                    Pair(durationShort, 1)
                }
            }

            val typeName = CmfSport.fromId(typeId).displayName
            val startTime = Instant.ofEpochSecond(startSec)
            val durationMins = (durationSec / 60).coerceAtLeast(if (durationSec > 0) 1 else 0)

            val distanceMeters = if (buffer.remaining() >= 4) (buffer.int.toLong() and 0xFFFFFFFFL) else 0L
            val caloriesVal = if (buffer.remaining() >= 4) (buffer.int.toLong() and 0xFFFFFFFFL) else 0L
            val avgHr = if (buffer.remaining() >= 1) (buffer.get().toInt() and 0xFF) else 0
            val maxHr = if (buffer.remaining() >= 1) (buffer.get().toInt() and 0xFF) else 0
            val avgPaceSec = if (buffer.remaining() >= 2) (buffer.short.toInt() and 0xFFFF) else 0

            val distKm = distanceMeters / 1000.0f
            val caloriesKcal = caloriesVal.toInt().coerceAtLeast(0)

            if (startSec > 1600000000L && durationSec > 0) {
                list.add(
                    SavedWorkout(
                        type = typeName,
                        startTime = startTime,
                        durationMinutes = durationMins,
                        distanceKm = distKm,
                        caloriesKcal = caloriesKcal,
                        avgHr = avgHr,
                        maxHr = maxHr,
                        avgPaceSec = avgPaceSec,
                        durationSeconds = durationSec
                    )
                )
            }
        }
        return list.sortedBy { it.startTime }
    }

    /**
     * Backward-compatible decode for single summary payload or latest summary.
     */
    fun decodeWorkoutSummary(payload: ByteArray): SavedWorkout? {
        val list = decodeWorkoutSummaryBatch(payload)
        return list.lastOrNull()
    }

    /**
     * Decode live workout status packet from watch (opcode 0x0057 or 0x0160).
     * Returns Pair(isActive: Boolean, sport: CmfSport)?
     */
    fun decodeLiveWorkoutStatus(payload: ByteArray): Pair<Boolean, CmfSport>? {
        if (payload.isEmpty()) return null

        // If it's a batch summary payload (>= 28 bytes), check if durationSec == 0 (initial start) or > 0 (completed)
        if (payload.size >= 28) {
            try {
                val buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
                val startSec = buffer.int.toLong() and 0xFFFFFFFFL
                val durationShort = buffer.short.toInt() and 0xFFFF
                val sportCode = buffer.get().toInt() and 0xFF
                val sport = CmfSport.fromId(sportCode)
                return if (durationShort == 0) Pair(true, sport) else Pair(false, sport)
            } catch (_: Exception) {
                // fall through to short packet heuristic
            }
        }

        // Short status packets (1 to 24 bytes)
        // Format A: [state: u8, sportId: u8, ...] (1=start, 2=pause, 3=resume, 0/4=stop)
        // Format B: [sportId: u8, state: u8, ...]
        if (payload.size >= 2) {
            val b0 = payload[0].toInt() and 0xFF
            val b1 = payload[1].toInt() and 0xFF

            if (b0 in listOf(0, 1, 2, 3, 4) && b1 in 1..35) {
                val isActive = b0 in listOf(1, 2, 3)
                return Pair(isActive, CmfSport.fromId(b1))
            }

            if (b1 in listOf(0, 1, 2, 3, 4) && b0 in 1..35) {
                val isActive = b1 in listOf(1, 2, 3)
                return Pair(isActive, CmfSport.fromId(b0))
            }
        }

        if (payload.size == 1) {
            val b = payload[0].toInt() and 0xFF
            return when (b) {
                0, 4 -> Pair(false, CmfSport.FREE_TRAINING)
                1 -> Pair(true, CmfSport.FREE_TRAINING)
                in 2..35 -> Pair(true, CmfSport.fromId(b))
                else -> null
            }
        }

        return null
    }
}
