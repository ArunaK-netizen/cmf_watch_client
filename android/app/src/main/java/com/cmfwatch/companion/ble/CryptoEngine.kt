package com.cmfwatch.companion.ble

import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Cryptographic operations for CMF Watch BLE protocol.
 * Implements AES-128-CBC encryption/decryption, SHA-256 key derivation,
 * and CRC16-CCITT checksum calculation matching reverse-engineered specs.
 */
object CryptoEngine {

    /**
     * Calculate CRC16-CCITT (polynomial 0x1021, init 0x0000) over input bytes.
     */
    fun calculateCrc16(data: ByteArray, offset: Int = 0, length: Int = data.size): Int {
        var crc = 0x0000
        for (i in offset until (offset + length)) {
            val b = data[i].toInt() and 0xFF
            crc = crc xor (b shl 8)
            for (j in 0 until 8) {
                if ((crc and 0x8000) != 0) {
                    crc = ((crc shl 1) xor 0x1021) and 0xFFFF
                } else {
                    crc = (crc shl 1) and 0xFFFF
                }
            }
        }
        return crc and 0xFFFF
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
     * Derive 16-byte Session Key using AES-128-ECB or XOR nonce combination.
     */
    fun deriveSessionKey(clientNonce: ByteArray, watchNonce: ByteArray, authKey: ByteArray): ByteArray {
        val combined = ByteArray(16)
        for (i in 0 until 16) {
            combined[i] = (clientNonce[i].toInt() xor watchNonce[i].toInt()).toByte()
        }
        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
        val keySpec = SecretKeySpec(authKey, "AES")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec)
        return cipher.doFinal(combined)
    }

    /**
     * AES-128-CBC Encrypt payload with PKCS7 padding.
     */
    fun encryptAes128Cbc(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val keySpec = SecretKeySpec(key, "AES")
        val ivSpec = IvParameterSpec(iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
        return cipher.doFinal(data)
    }

    /**
     * AES-128-CBC Decrypt ciphertext with PKCS7 padding.
     */
    fun decryptAes128Cbc(cipherText: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val keySpec = SecretKeySpec(key, "AES")
        val ivSpec = IvParameterSpec(iv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
        return cipher.doFinal(cipherText)
    }
}
