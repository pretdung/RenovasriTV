package com.example.renovasritv

import android.util.Base64
import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object SecurityUtils {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val ITERATION_COUNT = 65536
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12 // GCM standard
    private const val TAG_LENGTH = 128

    /**
     * Derives a secret key from a user PIN and salt.
     */
    fun deriveKey(pin: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec: KeySpec = PBEKeySpec(pin.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
    }

    /**
     * Encrypts a string using AES-GCM with a PIN-derived key.
     * Returns a Base64 string containing: SALT (16) + IV (12) + CIPHERTEXT
     */
    fun encrypt(plainText: String, pin: String): String {
        val salt = ByteArray(SALT_LENGTH).apply { SecureRandom().nextBytes(this) }
        val iv = ByteArray(IV_LENGTH).apply { SecureRandom().nextBytes(this) }
        
        val key = deriveKey(pin, salt)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH, iv))
        
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        
        val combined = salt + iv + cipherText
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /**
     * Decrypts a string using AES-GCM with a PIN-derived key.
     */
    fun decrypt(encryptedBase64: String, pin: String): String {
        val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
        
        val salt = combined.sliceArray(0 until SALT_LENGTH)
        val iv = combined.sliceArray(SALT_LENGTH until SALT_LENGTH + IV_LENGTH)
        val cipherText = combined.sliceArray(SALT_LENGTH + IV_LENGTH until combined.size)
        
        val key = deriveKey(pin, salt)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH, iv))
        
        return String(cipher.doFinal(cipherText), Charsets.UTF_8)
    }
}
