package com.cmfwatch.companion.ble

import org.junit.Assert.*
import org.junit.Test

class BleUnitTest {

    @Test
    fun testCrc32Calculation() {
        val testData = "123456789".toByteArray(Charsets.UTF_8)
        val crcBytes = CryptoEngine.calculateCrc32Bytes(testData)
        // CRC32 for "123456789" is 0xcbf43926 in Little-Endian: [0x26, 0x39, 0xf4, 0xcb]
        assertEquals(4, crcBytes.size)
        val expected = byteArrayOf(0x26.toByte(), 0x39.toByte(), 0xF4.toByte(), 0xCB.toByte())
        assertArrayEquals(expected, crcBytes)
    }

    @Test
    fun testSessionKeyDerivation() {
        val watchNonce = ByteArray(16) { 0x0A }
        val authKey = ByteArray(16) { 0x0B }
        val sessionKey = CryptoEngine.deriveSessionKey(watchNonce, authKey)
        assertEquals(16, sessionKey.size)
    }

    @Test
    fun testProtocolFramerBuildAndParsePlaintext() {
        val payload = "Hello CMF".toByteArray(Charsets.UTF_8)
        val frames = ProtocolFramer.buildFrames(
            cmd1 = 0xFFFF,
            cmd2 = 0x8049,
            payload = payload,
            sessionKey = null
        )

        assertNotNull(frames)
        assertEquals(1, frames.size)
        val frameBytes = frames[0]
        assertTrue(frameBytes.size >= 11)
        assertEquals(0xF5.toByte(), frameBytes[0])

        val assembler = FrameAssembler()
        val result = assembler.processRawNotification(frameBytes, sessionKey = null)
        assertNotNull(result)
        assertEquals(Pair(0xFFFF, 0x8049), result!!.first)
        val body = result.second
        // Body contains payload + 4B CRC32
        val payloadExtracted = body.copyOfRange(0, body.size - 4)
        assertArrayEquals(payload, payloadExtracted)
    }

    @Test
    fun testBatteryDecoder() {
        val payload = byteArrayOf(86.toByte(), 0.toByte())
        val battery = TelemetryDecoders.decodeBattery(payload)
        assertNotNull(battery)
        assertEquals(86, battery!!.level)
        assertFalse(battery.isCharging)
    }

    @Test
    fun testHeartRateBatchDecoderTakesNewestSample() {
        // Build payload with 3 samples:
        // Sample 1 (oldest, 1 month ago): 86 BPM at epoch 1700000000
        // Sample 2: 72 BPM at epoch 1700001000
        // Sample 3 (newest, modern): 68 BPM at epoch 1700002000
        val buffer = java.nio.ByteBuffer.allocate(24).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(1700000000)
        buffer.putInt(86)
        buffer.putInt(1700001000)
        buffer.putInt(72)
        buffer.putInt(1700002000)
        buffer.putInt(68)

        val batch = TelemetryDecoders.decodeHeartRateBatch(buffer.array())
        assertEquals(3, batch.size)
        assertEquals(86, batch[0].bpm)
        assertEquals(72, batch[1].bpm)
        assertEquals(68, batch[2].bpm)

        // decodeHeartRate MUST return the newest sample (68), NOT the oldest sample (86)
        val latest = TelemetryDecoders.decodeHeartRate(buffer.array())
        assertNotNull(latest)
        assertEquals(68, latest!!.bpm)
    }

    @Test
    fun testHeartRateDecoderLivePushByte() {
        // Single byte live pulse stream push from watch (e.g. 75 BPM)
        val payload = byteArrayOf(75.toByte())
        val sample = TelemetryDecoders.decodeHeartRate(payload)
        assertNotNull(sample)
        assertEquals(75, sample!!.bpm)
    }

    @Test
    fun testRestingHeartRateDecoder5ByteFormat() {
        val buffer = java.nio.ByteBuffer.allocate(10).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        // Record 1: ts = 1700000000, hr = 62
        buffer.putInt(1700000000)
        buffer.put(62.toByte())
        // Record 2: ts = 1700001000, hr = 59
        buffer.putInt(1700001000)
        buffer.put(59.toByte())

        val samples = TelemetryDecoders.decodeRestingHeartRateBatch(buffer.array())
        assertEquals(2, samples.size)
        assertEquals(62, samples[0].bpm)
        assertEquals(59, samples[1].bpm)
    }

    @Test
    fun testStepIntervalBatchDecoder() {
        val buffer = java.nio.ByteBuffer.allocate(64).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        // Interval 1 (32 bytes: 16-byte data + 16-byte padding)
        buffer.putInt(1700000000)
        buffer.putInt(120)
        buffer.putInt(90000)
        buffer.putInt(4500)
        buffer.put(ByteArray(16))
        // Interval 2 (32 bytes: 16-byte data + 16-byte padding)
        buffer.putInt(1700000900)
        buffer.putInt(250)
        buffer.putInt(180000)
        buffer.putInt(9000)
        buffer.put(ByteArray(16))

        val intervals = TelemetryDecoders.decodeStepIntervalBatch(buffer.array())
        assertEquals(2, intervals.size)
        assertEquals(120, intervals[0].steps)
        assertEquals(90.0f, intervals[0].distanceMeters, 0.01f)
        assertEquals(45.0f, intervals[0].caloriesKcal, 0.01f)
        assertEquals(250, intervals[1].steps)
        assertEquals(180.0f, intervals[1].distanceMeters, 0.01f)
        assertEquals(90.0f, intervals[1].caloriesKcal, 0.01f)
    }

    @Test
    fun testLiveWorkoutStatusDecoderCricketAndCycling() {
        // Watch sends [action=1 (START), sportId=17 (Cricket)]
        val cricketStartPayload = byteArrayOf(0x01.toByte(), 17.toByte())
        val cricketStatus = TelemetryDecoders.decodeLiveWorkoutStatus(cricketStartPayload)
        assertNotNull(cricketStatus)
        assertTrue(cricketStatus!!.first) // isActive == true
        assertEquals("Cricket", cricketStatus.second.displayName)

        // Watch sends [action=1 (START), sportId=3 (Cycling)]
        val cycleStartPayload = byteArrayOf(0x01.toByte(), 3.toByte())
        val cycleStatus = TelemetryDecoders.decodeLiveWorkoutStatus(cycleStartPayload)
        assertNotNull(cycleStatus)
        assertTrue(cycleStatus!!.first) // isActive == true
        assertEquals("Cycling", cycleStatus.second.displayName)

        // Watch sends [action=0 (STOP), sportId=17]
        val stopPayload = byteArrayOf(0x00.toByte(), 17.toByte())
        val stopStatus = TelemetryDecoders.decodeLiveWorkoutStatus(stopPayload)
        assertNotNull(stopStatus)
        assertFalse(stopStatus!!.first) // isActive == false
    }

    @Test
    fun testDecodeWorkoutSummaryPackedStructFreeTraining() {
        val buffer = java.nio.ByteBuffer.allocate(24).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(1700000000) // startSec
        buffer.putShort(96.toShort()) // duration: 96 seconds
        buffer.put(11.toByte()) // sport type: Free Training (11)
        buffer.put(0x49.toByte()) // flags
        buffer.putInt(0) // distance: 0m
        buffer.putInt(12) // calories: 12 kcal
        buffer.put(105.toByte()) // avg HR: 105
        buffer.put(120.toByte()) // max HR: 120
        buffer.putShort(0.toShort()) // avg pace: 0

        val workout = TelemetryDecoders.decodeWorkoutSummary(buffer.array())
        assertNotNull(workout)
        assertEquals("Free Training", workout!!.type)
        assertEquals(96, workout.durationSeconds)
        assertEquals(1, workout.durationMinutes)
        assertEquals(0.0f, workout.distanceKm, 0.001f)
        assertEquals(12, workout.caloriesKcal)
        assertEquals(105, workout.avgHr)
        assertEquals(120, workout.maxHr)
    }

    @Test
    fun testDecodeWorkoutSummaryRunning() {
        val buffer = java.nio.ByteBuffer.allocate(24).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(1700000000) // startSec
        buffer.putShort(1800.toShort()) // duration: 1800 seconds (30 min)
        buffer.put(1.toByte()) // sport type: Outdoor Run (1)
        buffer.put(0x01.toByte()) // flags
        buffer.putInt(5000) // distance: 5000m = 5.0 km
        buffer.putInt(320) // calories: 320 kcal
        buffer.put(145.toByte()) // avg HR: 145
        buffer.put(165.toByte()) // max HR: 165
        buffer.putShort(360.toShort()) // avg pace: 360 sec/km (6 min/km)

        val workout = TelemetryDecoders.decodeWorkoutSummary(buffer.array())
        assertNotNull(workout)
        assertEquals("Outdoor Run", workout!!.type)
        assertEquals(1800, workout.durationSeconds)
        assertEquals(30, workout.durationMinutes)
        assertEquals(5.0f, workout.distanceKm, 0.001f)
        assertEquals(320, workout.caloriesKcal)
        assertEquals(145, workout.avgHr)
    }
}
