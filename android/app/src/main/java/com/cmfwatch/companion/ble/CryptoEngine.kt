package com.cmfwatch.companion.ble

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.zip.CRC32
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Cryptographic operations for CMF Watch BLE protocol matching Python Source of Truth.
 * Implements AES-128-CBC encryption/decryption with fixed IV, SHA-256 key derivation,
 * and CRC32 Little-Endian checksum calculation matching reverse-engineered specs.
 */
object CryptoEngine {

    /**
     * Fixed initialization vector extracted from Nothing X APK & firmware.
     */
    val AES_IV = byteArrayOf(
        0x50.toByte(), 0x51.toByte(), 0x52.toByte(), 0x53.toByte(),
        0x54.toByte(), 0x55.toByte(), 0x56.toByte(), 0x57.toByte(),
        0x60.toByte(), 0x61.toByte(), 0x62.toByte(), 0x63.toByte(),
        0x64.toByte(), 0x65.toByte(), 0x66.toByte(), 0x5A.toByte()
    )

    /**
     * Generate cryptographically secure random bytes.
     */
    fun generateRandomBytes(length: Int): ByteArray {
        require(length >= 0) { "Random byte length must be non-negative" }
        val random = java.security.SecureRandom()
        val bytes = ByteArray(length)
        random.nextBytes(bytes)
        return bytes
    }

    /**
     * Calculate 32-bit CRC32 checksum returned as 4 Little-Endian bytes.
     */
    fun calculateCrc32Bytes(data: ByteArray, offset: Int = 0, length: Int = data.size): ByteArray {
        val crc = CRC32()
        crc.update(data, offset, length)
        val crcVal = crc.value and 0xFFFFFFFFL
        val buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(crcVal.toInt())
        return buffer.array()
    }

    /**
     * Verify 4-byte Little-Endian expected CRC32 against calculated payload CRC32.
     */
    fun verifyCrc32(data: ByteArray, expectedCrcBytes: ByteArray): Boolean {
        if (expectedCrcBytes.size != 4) return false
        val actualCrcBytes = calculateCrc32Bytes(data)
        return actualCrcBytes.contentEquals(expectedCrcBytes)
    }

    /**
     * Derive 16-byte long-term AuthKey (K1) using SHA-256 digest of (rnd1 + rnd2 + secret).
     */
    fun deriveAuthKey(rnd1: ByteArray, rnd2: ByteArray, secret: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(rnd1)
        md.update(rnd2)
        md.update(secret)
        val hash = md.digest()
        val authKey = ByteArray(16)
        System.arraycopy(hash, 0, authKey, 0, 16)
        return authKey
    }

    /**
     * Derive 16-byte per-session Key using SHA-256 digest of (watchNonce + authKey)[0..16].
     */
    fun deriveSessionKey(watchNonce: ByteArray, authKey: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(watchNonce)
        md.update(authKey)
        val hash = md.digest()
        val sessionKey = ByteArray(16)
        System.arraycopy(hash, 0, sessionKey, 0, 16)
        return sessionKey
    }

    /**
     * AES-128-CBC Encrypt payload with PKCS7 padding and fixed IV.
     */
    fun encryptAes128Cbc(data: ByteArray, key: ByteArray, iv: ByteArray = AES_IV): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val keySpec = SecretKeySpec(key, "AES")
        val ivSpec = IvParameterSpec(iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
        return cipher.doFinal(data)
    }

    /**
     * AES-128-CBC Decrypt ciphertext with PKCS7 padding and fixed IV.
     */
    fun decryptAes128Cbc(cipherText: ByteArray, key: ByteArray, iv: ByteArray = AES_IV): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val keySpec = SecretKeySpec(key, "AES")
        val ivSpec = IvParameterSpec(iv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
        return cipher.doFinal(cipherText)
    }
}
