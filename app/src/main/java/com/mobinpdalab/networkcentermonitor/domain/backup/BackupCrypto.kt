package com.mobinpdalab.networkcentermonitor.domain.backup

import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object BackupCrypto {
    private const val GCM_TAG_BITS = 128
    private const val IV_SIZE = 12
    private val magic = byteArrayOf('N'.code.toByte(), 'C'.code.toByte(), 'M'.code.toByte(), 'B'.code.toByte())

    fun encrypt(plainArchive: ByteArray, key: SecretKey, random: SecureRandom = SecureRandom()): ByteArray {
        require(plainArchive.isNotEmpty())
        val iv = ByteArray(IV_SIZE).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(magic)
        val cipherText = cipher.doFinal(plainArchive)
        return ByteBuffer.allocate(magic.size + 1 + iv.size + cipherText.size)
            .put(magic)
            .put(ENVELOPE_VERSION.toByte())
            .put(iv)
            .put(cipherText)
            .array()
    }

    fun decrypt(envelope: ByteArray, key: SecretKey): ByteArray {
        require(envelope.size > magic.size + 1 + IV_SIZE)
        val buffer = ByteBuffer.wrap(envelope)
        val foundMagic = ByteArray(magic.size).also(buffer::get)
        require(foundMagic.contentEquals(magic)) { "Not a NetworkCenterMonitor encrypted backup" }
        val version = buffer.get().toInt() and 0xFF
        require(version == ENVELOPE_VERSION) { "Unsupported encrypted backup envelope version: $version" }
        val iv = ByteArray(IV_SIZE).also(buffer::get)
        val cipherText = ByteArray(buffer.remaining()).also(buffer::get)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(magic)
        return cipher.doFinal(cipherText)
    }

    const val ENVELOPE_VERSION = 1
}

interface BackupStateStore {
    /** Export every canonical section. Credential payloads must contain references, never plaintext secrets. */
    suspend fun exportCanonicalState(): BackupArchive

    /** Must restore atomically: on failure, the pre-restore state remains valid. */
    suspend fun restoreCanonicalState(archive: BackupArchive)
}

data class RestoreResult(
    val restoredBackupId: String,
    val emergencyBackupEncrypted: ByteArray,
)

class FullRestoreCoordinator(
    private val store: BackupStateStore,
    private val key: SecretKey,
) {
    suspend fun restore(encryptedBackup: ByteArray): RestoreResult {
        val emergencyArchive = store.exportCanonicalState()
        val emergencyEncrypted = BackupCrypto.encrypt(BackupPackageCodec.encode(emergencyArchive), key)

        val decoded = BackupPackageCodec.decode(BackupCrypto.decrypt(encryptedBackup, key))
        CanonicalBackupCoverage.verify(decoded.payloads)
        store.restoreCanonicalState(decoded)
        return RestoreResult(
            restoredBackupId = decoded.manifest.backupId,
            emergencyBackupEncrypted = emergencyEncrypted,
        )
    }
}
