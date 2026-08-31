package com.mobinpdalab.networkcentermonitor.domain.backup

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

enum class BackupSection(val wireName: String) {
    PROVINCES("provinces"),
    GROUPS("groups"),
    CENTERS("centers"),
    NETWORKS("networks"),
    DEVICES("devices"),
    DEVICE_INTERFACES("device_interfaces"),
    DEVICE_RELATIONS("device_relations"),
    IP_ENDPOINTS("ip_endpoints"),
    SERVICES("services"),
    DISCOVERY("discovery"),
    CAMERA_PROFILES("camera_profiles"),
    RECORDER_PROFILES("recorder_profiles"),
    PC_INVENTORY("pc_inventory"),
    REMOTE_PROFILES("remote_profiles"),
    AGENT_CONFIGURATION("agent_configuration"),
    MONITORING_SETTINGS("monitoring_settings"),
    MONITORING_STATE("monitoring_state"),
    HISTORY("history"),
    INCIDENTS("incidents"),
    FOLLOW_UPS("follow_ups"),
    CUSTOM_FIELDS("custom_fields"),
    EQUIPMENT_PRESETS("equipment_presets"),
    MANUAL_OVERRIDES("manual_overrides"),
    REPORT_SETTINGS("report_settings"),
    APPLICATION_SETTINGS("application_settings"),
    ATTACHMENT_INDEX("attachment_index"),
    SECURE_CREDENTIAL_REFERENCES("secure_credential_references"),
}

object CanonicalBackupCoverage {
    val requiredSections: Set<BackupSection> = BackupSection.entries.toSet()

    fun verify(payloads: Map<BackupSection, ByteArray>) {
        val missing = requiredSections - payloads.keys
        require(missing.isEmpty()) {
            "Full backup is missing canonical sections: ${missing.joinToString { it.wireName }}"
        }
    }
}

data class BackupManifest(
    val packageVersion: Int = CURRENT_PACKAGE_VERSION,
    val backupId: String,
    val createdAtEpochMillis: Long,
    val jalaliDateTime: String,
    val appVersion: String,
    val schemaVersion: Int,
    val entityCounts: Map<BackupSection, Long>,
) {
    init {
        require(packageVersion > 0)
        require(backupId.isNotBlank())
        require(createdAtEpochMillis >= 0)
        require(jalaliDateTime.isNotBlank())
        require(appVersion.isNotBlank())
        require(schemaVersion > 0)
        require(entityCounts.values.all { it >= 0 })
    }
}

data class BackupArchive(
    val manifest: BackupManifest,
    val payloads: Map<BackupSection, ByteArray>,
) {
    init {
        CanonicalBackupCoverage.verify(payloads)
        require(manifest.entityCounts.keys.containsAll(CanonicalBackupCoverage.requiredSections)) {
            "Backup manifest must contain a count for every canonical section"
        }
    }
}

object BackupFilename {
    fun create(jalaliDateTime: String, prefix: String = "Backup"): String {
        require(prefix.isNotBlank())
        val normalized = jalaliDateTime
            .trim()
            .replace('/', '-')
            .replace(':', '-')
            .replace(Regex("\\s+"), "_")
            .replace(Regex("[^0-9۰-۹_-]"), "")
            .replace(Regex("_+"), "_")
            .trim('_', '-')
        require(normalized.isNotBlank()) { "Jalali date/time must create a valid filename" }
        return "${prefix}_${normalized}.backup"
    }
}

object BackupPackageCodec {
    fun encode(archive: BackupArchive): ByteArray = ByteArrayOutputStream().use { output ->
        encode(archive, output)
        output.toByteArray()
    }

    fun encode(archive: BackupArchive, output: OutputStream) {
        CanonicalBackupCoverage.verify(archive.payloads)
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
            zip.write(encodeManifest(archive.manifest))
            zip.closeEntry()

            BackupSection.entries.forEach { section ->
                zip.putNextEntry(ZipEntry(sectionEntry(section)))
                zip.write(requireNotNull(archive.payloads[section]))
                zip.closeEntry()
            }
        }
    }

    fun decode(
        bytes: ByteArray,
        supportedPackageVersion: Int = CURRENT_PACKAGE_VERSION,
        maximumSchemaVersion: Int = Int.MAX_VALUE,
    ): BackupArchive = decode(ByteArrayInputStream(bytes), supportedPackageVersion, maximumSchemaVersion)

    fun decode(
        input: InputStream,
        supportedPackageVersion: Int = CURRENT_PACKAGE_VERSION,
        maximumSchemaVersion: Int = Int.MAX_VALUE,
    ): BackupArchive {
        var manifestBytes: ByteArray? = null
        val payloads = mutableMapOf<BackupSection, ByteArray>()
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val content = zip.readBytes()
                when {
                    entry.name == MANIFEST_ENTRY -> manifestBytes = content
                    entry.name.startsWith(SECTION_PREFIX) -> {
                        val wireName = entry.name.removePrefix(SECTION_PREFIX).removeSuffix(".bin")
                        val section = BackupSection.entries.firstOrNull { it.wireName == wireName }
                            ?: throw IllegalArgumentException("Unknown backup section: $wireName")
                        require(payloads.put(section, content) == null) { "Duplicate backup section: $wireName" }
                    }
                }
                zip.closeEntry()
            }
        }

        val manifest = decodeManifest(requireNotNull(manifestBytes) { "Backup manifest is missing" })
        require(manifest.packageVersion == supportedPackageVersion) {
            "Unsupported backup package version ${manifest.packageVersion}; supported=$supportedPackageVersion"
        }
        require(manifest.schemaVersion <= maximumSchemaVersion) {
            "Backup schema ${manifest.schemaVersion} is newer than supported schema $maximumSchemaVersion"
        }
        CanonicalBackupCoverage.verify(payloads)
        return BackupArchive(manifest, payloads)
    }

    private fun encodeManifest(manifest: BackupManifest): ByteArray {
        val lines = buildList {
            add("packageVersion=${manifest.packageVersion}")
            add("backupId=${encodeText(manifest.backupId)}")
            add("createdAtEpochMillis=${manifest.createdAtEpochMillis}")
            add("jalaliDateTime=${encodeText(manifest.jalaliDateTime)}")
            add("appVersion=${encodeText(manifest.appVersion)}")
            add("schemaVersion=${manifest.schemaVersion}")
            BackupSection.entries.forEach { section ->
                add("count.${section.wireName}=${manifest.entityCounts[section] ?: 0L}")
            }
        }
        return (lines.joinToString("\n") + "\n").toByteArray(StandardCharsets.UTF_8)
    }

    private fun decodeManifest(bytes: ByteArray): BackupManifest {
        val values = bytes.toString(StandardCharsets.UTF_8)
            .lineSequence()
            .filter { it.isNotBlank() }
            .associate { line ->
                val separator = line.indexOf('=')
                require(separator > 0) { "Malformed backup manifest line" }
                line.substring(0, separator) to line.substring(separator + 1)
            }
        val counts = BackupSection.entries.associateWith { section ->
            values["count.${section.wireName}"]?.toLongOrNull()
                ?: throw IllegalArgumentException("Missing count for ${section.wireName}")
        }
        return BackupManifest(
            packageVersion = values.getValue("packageVersion").toInt(),
            backupId = decodeText(values.getValue("backupId")),
            createdAtEpochMillis = values.getValue("createdAtEpochMillis").toLong(),
            jalaliDateTime = decodeText(values.getValue("jalaliDateTime")),
            appVersion = decodeText(values.getValue("appVersion")),
            schemaVersion = values.getValue("schemaVersion").toInt(),
            entityCounts = counts,
        )
    }

    private fun encodeText(value: String): String = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun decodeText(value: String): String = String(
        Base64.getUrlDecoder().decode(value),
        StandardCharsets.UTF_8,
    )

    private fun sectionEntry(section: BackupSection): String = "$SECTION_PREFIX${section.wireName}.bin"

    private const val MANIFEST_ENTRY = "manifest.ncm"
    private const val SECTION_PREFIX = "sections/"
}

const val CURRENT_PACKAGE_VERSION = 1
