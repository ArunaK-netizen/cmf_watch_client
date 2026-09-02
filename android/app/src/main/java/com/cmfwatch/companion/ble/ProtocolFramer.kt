package com.cmfwatch.companion.ble

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.ceil

/**
 * 0xF5 Binary Protocol Framer matching Python Source of Truth (frame.py).
 * Header Layout (11 Bytes):
 * [0xF5] [PayloadLen: u16_be] [Cmd1: u16_be] [ChunkCount: u16_be] [ChunkIndex: u16_be] [Cmd2: u16_be]
 */
data class CmfFrame(
    val cmd1: Int,
    val cmd2: Int,
    val chunkCount: Int,
    val chunkIndex: Int,
    val payload: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CmfFrame
        if (cmd1 != other.cmd1) return false
        if (cmd2 != other.cmd2) return false
        if (chunkCount != other.chunkCount) return false
        if (chunkIndex != other.chunkIndex) return false
        return payload.contentEquals(other.payload)
    }

    override fun hashCode(): Int {
        var result = cmd1
        result = 31 * result + cmd2
        result = 31 * result + chunkCount
        result = 31 * result + chunkIndex
        result = 31 * result + payload.contentHashCode()
        return result
    }
}

object ProtocolFramer {

    const val PAYLOAD_HEADER: Byte = 0xF5.toByte()

    /**
     * Construct framed BLE write packets (one or more chunks) for command (cmd1, cmd2).
     */
    fun buildFrames(
        cmd1: Int,
        cmd2: Int,
        payload: ByteArray,
        sessionKey: ByteArray? = null,
        mtu: Int = 247
    ): List<ByteArray> {
        val maxWrite = maxOf(23, mtu - 3)
        val headerLen = 11

        val isPlaintext = sessionKey == null

        val maxChunkPayload = if (isPlaintext) {
            maxOf(1, maxWrite - headerLen - 4)
        } else {
            val maxEncryptedBlock = ((maxWrite - headerLen) / 16) * 16
            maxOf(1, maxEncryptedBlock - 4 - 1)
        }

        val numChunks = if (payload.isNotEmpty()) {
            ceil(payload.size.toDouble() / maxChunkPayload).toInt()
        } else {
            1
        }

        val frames = mutableListOf<ByteArray>()

        if (payload.isEmpty()) {
            val header = ByteBuffer.allocate(11).order(ByteOrder.BIG_ENDIAN)
            header.put(PAYLOAD_HEADER)
            header.putShort(0.toShort())
            header.putShort(cmd1.toShort())
            header.putShort(1.toShort())
            header.putShort(1.toShort())
            header.putShort(cmd2.toShort())
            frames.add(header.array())
            return frames
        }

        for (i in 0 until numChunks) {
            val start = i * maxChunkPayload
            val end = minOf(start + maxChunkPayload, payload.size)
            val chunkSlice = payload.copyOfRange(start, end)
            val crcBytes = CryptoEngine.calculateCrc32Bytes(chunkSlice)

            val rawBody = chunkSlice + crcBytes

            val chunkBody = if (isPlaintext) {
                rawBody
            } else {
                CryptoEngine.encryptAes128Cbc(rawBody, sessionKey!!)
            }

            val header = ByteBuffer.allocate(11 + chunkBody.size).order(ByteOrder.BIG_ENDIAN)
            header.put(PAYLOAD_HEADER)
            header.putShort(chunkBody.size.toShort())
            header.putShort(cmd1.toShort())
            header.putShort(numChunks.toShort())
            header.putShort((i + 1).toShort()) // 1-based index
            header.putShort(cmd2.toShort())
            header.put(chunkBody)

            frames.add(header.array())
        }

        return frames
    }
}

class FrameAssembler {
    private val buffers = mutableMapOf<Pair<Int, Int>, MutableList<Byte>>()

    /**
     * Process incoming raw notification byte packet.
     */
    fun processRawNotification(
        rawBytes: ByteArray,
        sessionKey: ByteArray? = null
    ): Pair<Pair<Int, Int>, ByteArray>? {
        if (rawBytes.size < 11) return null
        if (rawBytes[0] != ProtocolFramer.PAYLOAD_HEADER) return null

        val buffer = ByteBuffer.wrap(rawBytes).order(ByteOrder.BIG_ENDIAN)
        buffer.get() // header byte 0xF5
        val payloadLen = buffer.short.toInt() and 0xFFFF
        val cmd1 = buffer.short.toInt() and 0xFFFF
        val chunkCount = buffer.short.toInt() and 0xFFFF
        val chunkIndex = buffer.short.toInt() and 0xFFFF
        val cmd2 = buffer.short.toInt() and 0xFFFF

        val rawChunkData = ByteArray(payloadLen)
        if (buffer.remaining() < payloadLen) return null
        buffer.get(rawChunkData)

        val isPlaintext = sessionKey == null

        val payload = if (payloadLen > 0) {
            if (isPlaintext) {
                rawChunkData
            } else {
                val decrypted = CryptoEngine.decryptAes128Cbc(rawChunkData, sessionKey!!)
                if (decrypted.size < 4) return null
                val body = decrypted.copyOfRange(0, decrypted.size - 4)
                val expectedCrc = decrypted.copyOfRange(decrypted.size - 4, decrypted.size)
                if (!CryptoEngine.verifyCrc32(body, expectedCrc)) {
                    return null
                }
                body
            }
        } else {
            ByteArray(0)
        }

        val key = Pair(cmd1, cmd2)

        if (chunkCount == 1) {
            return Pair(key, payload)
        }

        if (!buffers.containsKey(key) || chunkIndex == 1) {
            buffers[key] = mutableListOf()
        }

        val buf = buffers[key]!!
        buf.addAll(payload.toList())

        if (chunkIndex == chunkCount) {
            val completeData = buf.toByteArray()
            buffers.remove(key)
            return Pair(key, completeData)
        }

        return null
    }
}
