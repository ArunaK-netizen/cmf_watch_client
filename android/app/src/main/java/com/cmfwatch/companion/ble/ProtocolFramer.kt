package com.cmfwatch.companion.ble

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 0xF5 Binary Protocol Framer for CMF Watch.
 * Frame Layout:
 * [Magic: 0xF5] [Seq: u8] [Flags: u8] [PayloadLen: u16_be] [Cmd1: u16_be] [Cmd2: u16_be] [Payload: N bytes] [CRC16: u16_be]
 */
data class CmfFrame(
    val seq: Int,
    val isEncrypted: Boolean,
    val cmd1: Int,
    val cmd2: Int,
    val payload: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CmfFrame
        if (seq != other.seq) return false
        if (isEncrypted != other.isEncrypted) return false
        if (cmd1 != other.cmd1) return false
        if (cmd2 != other.cmd2) return false
        if (!payload.contentEquals(other.payload)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = seq
        result = 31 * result + isEncrypted.hashCode()
        result = 31 * result + cmd1
        result = 31 * result + cmd2
        result = 31 * result + payload.contentHashCode()
        return result
    }
}

object ProtocolFramer {

    const val MAGIC_BYTE: Byte = 0xF5.toByte()

    /**
     * Build packed 0xF5 protocol frame byte array.
     */
    fun buildFrame(
        seq: Int,
        cmd1: Int,
        cmd2: Int,
        payload: ByteArray,
        sessionKey: ByteArray? = null,
        iv: ByteArray? = null
    ): ByteArray {
        val isEncrypted = sessionKey != null && iv != null
        val flags = if (isEncrypted) 0x01 else 0x00

        val finalPayload = if (isEncrypted) {
            CryptoEngine.encryptAes128Cbc(payload, sessionKey!!, iv!!)
        } else {
            payload
        }

        val headerAndPayload = ByteBuffer.allocate(8 + finalPayload.size).order(ByteOrder.BIG_ENDIAN)
        headerAndPayload.put(MAGIC_BYTE)
        headerAndPayload.put((seq and 0xFF).toByte())
        headerAndPayload.put(flags.toByte())
        headerAndPayload.putShort(finalPayload.size.toShort())
        headerAndPayload.putShort(cmd1.toShort())
        headerAndPayload.putShort(cmd2.toShort())
        headerAndPayload.put(finalPayload)

        val frameWithoutCrc = headerAndPayload.array()
        val crc = CryptoEngine.calculateCrc16(frameWithoutCrc)

        val completeFrame = ByteBuffer.allocate(frameWithoutCrc.size + 2).order(ByteOrder.BIG_ENDIAN)
        completeFrame.put(frameWithoutCrc)
        completeFrame.putShort(crc.toShort())

        return completeFrame.array()
    }

    /**
     * Parse incoming 0xF5 byte array into CmfFrame object.
     */
    fun parseFrame(
        frameBytes: ByteArray,
        sessionKey: ByteArray? = null,
        iv: ByteArray? = null
    ): CmfFrame? {
        if (frameBytes.size < 10) return null
        if (frameBytes[0] != MAGIC_BYTE) return null

        val buffer = ByteBuffer.wrap(frameBytes).order(ByteOrder.BIG_ENDIAN)
        buffer.get() // magic
        val seq = buffer.get().toInt() and 0xFF
        val flags = buffer.get().toInt() and 0xFF
        val payloadLen = buffer.short.toInt() and 0xFFFF
        val cmd1 = buffer.short.toInt() and 0xFFFF
        val cmd2 = buffer.short.toInt() and 0xFFFF

        val isEncrypted = (flags and 0x01) != 0

        val rawPayload = ByteArray(payloadLen)
        if (buffer.remaining() < payloadLen) return null
        buffer.get(rawPayload)

        val decryptedPayload = if (isEncrypted && sessionKey != null && iv != null) {
            try {
                CryptoEngine.decryptAes128Cbc(rawPayload, sessionKey, iv)
            } catch (e: Exception) {
                return null
            }
        } else {
            rawPayload
        }

        return CmfFrame(
            seq = seq,
            isEncrypted = isEncrypted,
            cmd1 = cmd1,
            cmd2 = cmd2,
            payload = decryptedPayload
        )
    }
}
