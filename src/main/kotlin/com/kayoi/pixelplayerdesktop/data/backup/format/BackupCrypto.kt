package com.kayoi.pixelplayerdesktop.data.backup.format

import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class BackupEncryptedException :
    Exception("This backup is encrypted and requires a password.")

class BackupWrongPassphraseException :
    Exception("Incorrect password, or the backup file is corrupted.")

object BackupCrypto {
    val ENC_MARKER = byteArrayOf('E'.code.toByte(), 'C'.code.toByte(), '0'.code.toByte(), '1'.code.toByte())

    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val KEY_BITS = 256
    private const val GCM_TAG_BITS = 128
    private const val PBKDF2_ITERATIONS = 210_000
    private const val MAX_HEADER_ITERATIONS = 5_000_000

    const val HEADER_BYTES_AFTER_MAGIC = 4 + SALT_BYTES + IV_BYTES + 4

    private val secureRandom = SecureRandom()

    fun encryptingStream(raw: OutputStream, passphrase: String): OutputStream {
        val salt = ByteArray(SALT_BYTES).also(secureRandom::nextBytes)
        val iv = ByteArray(IV_BYTES).also(secureRandom::nextBytes)

        raw.write(ENC_MARKER)
        raw.write(salt)
        raw.write(iv)
        raw.write(ByteBuffer.allocate(4).putInt(PBKDF2_ITERATIONS).array())

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(passphrase, salt, PBKDF2_ITERATIONS), GCMParameterSpec(GCM_TAG_BITS, iv))
        return CipherOutputStream(raw, cipher)
    }

    fun decryptingStream(raw: InputStream, passphrase: String): InputStream {
        val marker = readFully(raw, ENC_MARKER.size)
        require(marker.contentEquals(ENC_MARKER)) { "Not an encrypted PixelPlayer backup." }
        val salt = readFully(raw, SALT_BYTES)
        val iv = readFully(raw, IV_BYTES)
        val iterations = ByteBuffer.wrap(readFully(raw, 4)).int
        require(iterations in 1..MAX_HEADER_ITERATIONS) { "Corrupted encryption header." }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, deriveKey(passphrase, salt, iterations), GCMParameterSpec(GCM_TAG_BITS, iv))
        return CipherInputStream(raw, cipher)
    }

    private fun deriveKey(passphrase: String, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, iterations, KEY_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val key = factory.generateSecret(spec)
        spec.clearPassword()
        return SecretKeySpec(key.encoded, "AES")
    }

    private fun readFully(input: InputStream, count: Int): ByteArray {
        val buffer = ByteArray(count)
        var offset = 0
        while (offset < count) {
            val read = input.read(buffer, offset, count - offset)
            if (read == -1) throw IllegalArgumentException("Encrypted backup header is truncated.")
            offset += read
        }
        return buffer
    }
}
