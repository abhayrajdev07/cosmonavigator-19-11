
package com.cosmotrinity.cosmonavigator.data.backup

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CosmoBackupCrypto {

    private const val MAGIC = "COSMONAV"
    private const val FILE_VERSION = 1

    private const val SALT_SIZE = 16
    private const val IV_SIZE = 12
    private const val KEY_SIZE = 256
    private const val GCM_TAG_SIZE = 128

    private const val PBKDF2_ITERATIONS = 210_000

    private const val MIN_PASSWORD_LENGTH = 8

    private const val MAX_CIPHERTEXT_SIZE = 100 * 1024 * 1024

    private val secureRandom = SecureRandom()

    class InvalidBackupException(
        message: String,
        cause: Throwable? = null
    ) : Exception(message, cause)

    /**
     * Encrypts backup data using a password.
     *
     * Returned bytes contain:
     * Magic header + version + iterations + salt + IV + ciphertext.
     */
    fun encrypt(
        plainData: ByteArray,
        password: CharArray
    ): ByteArray {

        require(password.size >= MIN_PASSWORD_LENGTH) {
            "Password must contain at least 8 characters."
        }

        require(plainData.isNotEmpty()) {
            "Backup data is empty."
        }

        require(plainData.size <= MAX_CIPHERTEXT_SIZE) {
            "Backup data is too large."
        }

        val salt = ByteArray(SALT_SIZE)
        val iv = ByteArray(IV_SIZE)

        secureRandom.nextBytes(salt)
        secureRandom.nextBytes(iv)

        val key = deriveKey(
            password = password,
            salt = salt,
            iterations = PBKDF2_ITERATIONS
        )

        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")

            cipher.init(
                Cipher.ENCRYPT_MODE,
                key,
                GCMParameterSpec(GCM_TAG_SIZE, iv)
            )

            cipher.updateAAD(createHeader())

            val encryptedData = cipher.doFinal(plainData)

            ByteArrayOutputStream().use { output ->
                DataOutputStream(output).use { data ->

                    data.write(createHeader())
                    data.writeInt(PBKDF2_ITERATIONS)

                    data.writeInt(SALT_SIZE)
                    data.write(salt)

                    data.writeInt(IV_SIZE)
                    data.write(iv)

                    data.writeInt(encryptedData.size)
                    data.write(encryptedData)
                }

                output.toByteArray()
            }

        } finally {
            key.encoded?.fill(0)
        }
    }

    /**
     * Decrypts a backup file using the supplied password.
     */
    fun decrypt(
        encryptedBackup: ByteArray,
        password: CharArray
    ): ByteArray {

        require(password.isNotEmpty()) {
            "Please enter your backup password."
        }

        if (encryptedBackup.size > MAX_CIPHERTEXT_SIZE + 1024) {
            throw InvalidBackupException("Backup file is too large.")
        }

        try {
            DataInputStream(
                ByteArrayInputStream(encryptedBackup)
            ).use { input ->

                val magic = ByteArray(MAGIC.length)
                input.readFully(magic)

                if (!magic.contentEquals(MAGIC.toByteArray(Charsets.UTF_8))) {
                    throw InvalidBackupException(
                        "This is not a valid CosmoNavigator backup."
                    )
                }

                val version = input.readInt()

                if (version != FILE_VERSION) {
                    throw InvalidBackupException(
                        "Unsupported backup file version: $version"
                    )
                }

                val iterations = input.readInt()

                if (iterations !in 100_000..1_000_000) {
                    throw InvalidBackupException(
                        "Invalid backup encryption parameters."
                    )
                }

                val saltSize = input.readInt()

                if (saltSize != SALT_SIZE) {
                    throw InvalidBackupException(
                        "Invalid backup salt."
                    )
                }

                val salt = ByteArray(saltSize)
                input.readFully(salt)

                val ivSize = input.readInt()

                if (ivSize != IV_SIZE) {
                    throw InvalidBackupException(
                        "Invalid backup initialization vector."
                    )
                }

                val iv = ByteArray(ivSize)
                input.readFully(iv)

                val encryptedSize = input.readInt()

                if (
                    encryptedSize < 16 ||
                    encryptedSize > MAX_CIPHERTEXT_SIZE ||
                    encryptedSize != input.available()
                ) {
                    throw InvalidBackupException(
                        "Backup file is incomplete or corrupted."
                    )
                }

                val encryptedData = ByteArray(encryptedSize)
                input.readFully(encryptedData)

                val key = deriveKey(
                    password = password,
                    salt = salt,
                    iterations = iterations
                )

                try {
                    val cipher = Cipher.getInstance("AES/GCM/NoPadding")

                    cipher.init(
                        Cipher.DECRYPT_MODE,
                        key,
                        GCMParameterSpec(GCM_TAG_SIZE, iv)
                    )

                    cipher.updateAAD(createHeader())

                    return cipher.doFinal(encryptedData)

                } catch (error: AEADBadTagException) {
                    throw InvalidBackupException(
                        "Incorrect password or corrupted backup file.",
                        error
                    )
                } finally {
                    key.encoded?.fill(0)
                }
            }

        } catch (error: InvalidBackupException) {
            throw error
        } catch (error: Exception) {
            throw InvalidBackupException(
                "Unable to read or decrypt this backup.",
                error
            )
        }
    }

    private fun deriveKey(
        password: CharArray,
        salt: ByteArray,
        iterations: Int
    ): SecretKeySpec {

        val spec = PBEKeySpec(
            password,
            salt,
            iterations,
            KEY_SIZE
        )

        return try {
            val factory = SecretKeyFactory.getInstance(
                "PBKDF2WithHmacSHA256"
            )

            val keyBytes = factory.generateSecret(spec).encoded

            try {
                SecretKeySpec(keyBytes, "AES")
            } finally {
                keyBytes.fill(0)
            }

        } finally {
            spec.clearPassword()
        }
    }

    private fun createHeader(): ByteArray {
        return ByteArrayOutputStream().use { output ->
            DataOutputStream(output).use { data ->
                data.write(MAGIC.toByteArray(Charsets.UTF_8))
                data.writeInt(FILE_VERSION)
            }
            output.toByteArray()
        }
    }
}
