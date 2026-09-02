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
}
