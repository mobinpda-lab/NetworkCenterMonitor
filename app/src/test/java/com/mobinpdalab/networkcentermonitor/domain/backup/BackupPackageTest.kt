package com.mobinpdalab.networkcentermonitor.domain.backup

import java.security.SecureRandom
import javax.crypto.KeyGenerator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupPackageTest {
    @Test
    fun `full package round trips every canonical section`() {
        val archive = sampleArchive("backup-1")
        val decoded = BackupPackageCodec.decode(BackupPackageCodec.encode(archive), maximumSchemaVersion = 1)

        assertEquals(archive.manifest, decoded.manifest)
        BackupSection.entries.forEach { section ->
            assertArrayEquals(archive.payloads.getValue(section), decoded.payloads.getValue(section))
        }
    }

    @Test
    fun `encrypted package round trips and rejects tampering`() {
        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val plain = BackupPackageCodec.encode(sampleArchive("backup-secure"))
        val encrypted = BackupCrypto.encrypt(plain, key, SecureRandom())

        assertArrayEquals(plain, BackupCrypto.decrypt(encrypted, key))
        val tampered = encrypted.copyOf().also { it[it.lastIndex] = (it.last() + 1).toByte() }
        val failed = runCatching { BackupCrypto.decrypt(tampered, key) }.isFailure
        assertTrue(failed)
    }

    @Test
    fun `restore creates emergency encrypted backup before applying target`() = runBlocking {
        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val before = sampleArchive("before")
        val target = sampleArchive("target")
        val store = FakeStore(before)
        val encryptedTarget = BackupCrypto.encrypt(BackupPackageCodec.encode(target), key)

        val result = FullRestoreCoordinator(store, key).restore(encryptedTarget)

        assertEquals("target", result.restoredBackupId)
        assertEquals("target", store.current.manifest.backupId)
        val emergency = BackupPackageCodec.decode(BackupCrypto.decrypt(result.emergencyBackupEncrypted, key))
        assertEquals("before", emergency.manifest.backupId)
    }

    @Test
    fun `jalali filename is filesystem safe`() {
        assertEquals("Backup_۱۴۰۵-۰۶-۰۹_21-15.backup", BackupFilename.create("۱۴۰۵/۰۶/۰۹ 21:15"))
    }

    private fun sampleArchive(id: String): BackupArchive {
        val payloads = BackupSection.entries.associateWith { section ->
            "${section.wireName}:$id".toByteArray()
        }
        return BackupArchive(
            manifest = BackupManifest(
                backupId = id,
                createdAtEpochMillis = 100,
                jalaliDateTime = "۱۴۰۵/۰۶/۰۹ 21:15",
                appVersion = "0.1.0-alpha",
                schemaVersion = 1,
                entityCounts = BackupSection.entries.associateWith { 1L },
            ),
            payloads = payloads,
        )
    }

    private class FakeStore(initial: BackupArchive) : BackupStateStore {
        var current: BackupArchive = initial

        override suspend fun exportCanonicalState(): BackupArchive = current

        override suspend fun restoreCanonicalState(archive: BackupArchive) {
            current = archive
        }
    }
}
