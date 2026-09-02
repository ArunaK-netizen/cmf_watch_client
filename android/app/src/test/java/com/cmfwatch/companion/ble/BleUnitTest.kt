package com.cmfwatch.companion.ble

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class BleUnitTest {

    @Test
    fun testCrc16Calculation() {
        val testData = "123456789".toByteArray(Charsets.UTF_8)
        val crc = CryptoEngine.calculateCrc16(testData)
        // CRC16-CCITT check for "123456789" is 0x29B1 (10673)
        assertEquals(0x29B1, crc)
    }

    @Test
    fun testAuthKeyDerivation() {
        val rnd1 = ByteArray(16) { 0x01 }
        val rnd2 = ByteArray(16) { 0x02 }
        val secret = ByteArray(16) { 0x03 }
        val authKey = CryptoEngine.deriveAuthKey(rnd1, rnd2, secret)
        assertEquals(16, authKey.size)
    }

    @Test
    fun testProtocolFramerBuildAndParsePlaintext() {
        val payload = "Hello CMF".toByteArray(Charsets.UTF_8)
        val frameBytes = ProtocolFramer.buildFrame(
            seq = 1,
            cmd1 = 0xFFFF,
            cmd2 = 0x804B,
            payload = payload
        )

        assertNotNull(frameBytes)
        assertTrue(frameBytes.size >= 10)
        assertEquals(0xF5.toByte(), frameBytes[0])

        val parsed = ProtocolFramer.parseFrame(frameBytes)
        assertNotNull(parsed)
        assertEquals(1, parsed!!.seq)
        assertEquals(0xFFFF, parsed.cmd1)
        assertEquals(0x804B, parsed.cmd2)
        assertFalse(parsed.isEncrypted)
        assertArrayEquals(payload, parsed.payload)
    }

    @Test
    fun testHeartRateDecoder() {
        // Payload: 8 bytes LE (epoch_sec: 1787333684, bpm: 72)
        val payload = byteArrayOf(
            0x34.toByte(), 0xA1.toByte(), 0x86.toByte(), 0x6A.toByte(), // 1787333684
            0x48.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()  // 72 BPM
        )
        val hr = TelemetryDecoders.decodeHeartRate(payload, "0x0053")
        assertNotNull(hr)
        assertEquals(72, hr!!.bpm)
        assertEquals(1787333684L, hr.timestamp.epochSecond)
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
