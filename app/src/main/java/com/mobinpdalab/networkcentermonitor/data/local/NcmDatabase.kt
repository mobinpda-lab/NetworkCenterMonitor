package com.mobinpdalab.networkcentermonitor.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Dao
abstract class CanonicalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertCenter(value: CenterEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertNetwork(value: NetworkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertDevice(value: DeviceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertDeviceInterfaces(values: List<DeviceInterfaceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertInterfaceIps(values: List<InterfaceIpEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertEndpoint(value: IpEndpointEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertService(value: ServiceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertRelation(value: DeviceRelationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertSourcedFields(values: List<SourcedFieldEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertTags(values: List<DeviceTagEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertCustomFields(values: List<CustomFieldValueEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertCustomFieldDefinition(value: CustomFieldDefinitionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertCameraProfile(value: CameraProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertRecorderProfile(value: RecorderProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertPcProfile(value: PcProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertRemoteProfile(value: RemoteProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertLocalAgent(value: LocalAgentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertIncident(value: IncidentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertFollowUp(value: FollowUpEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertMonitoringState(value: MonitoringStateEntity)

    @Query("DELETE FROM device_interfaces WHERE deviceId = :deviceId")
    abstract suspend fun deleteInterfacesForDevice(deviceId: String)

    @Query("DELETE FROM device_tags WHERE deviceId = :deviceId")
    abstract suspend fun deleteTagsForDevice(deviceId: String)

    @Query("DELETE FROM custom_field_values WHERE ownerType = 'DEVICE' AND ownerId = :deviceId")
    abstract suspend fun deleteCustomFieldsForDevice(deviceId: String)

    @Query("DELETE FROM sourced_fields WHERE ownerType = 'DEVICE' AND ownerId = :deviceId")
    abstract suspend fun deleteSourcedFieldsForDevice(deviceId: String)

    @Query("SELECT * FROM devices WHERE id = :deviceId LIMIT 1")
    abstract suspend fun getDevice(deviceId: String): DeviceEntity?

    @Query("SELECT * FROM device_interfaces WHERE deviceId = :deviceId ORDER BY id")
    abstract suspend fun getInterfaces(deviceId: String): List<DeviceInterfaceEntity>

    @Query("SELECT * FROM interface_ips WHERE interfaceId IN (:interfaceIds) ORDER BY interfaceId, address")
    abstract suspend fun getInterfaceIps(interfaceIds: List<String>): List<InterfaceIpEntity>

    @Query("SELECT * FROM device_tags WHERE deviceId = :deviceId ORDER BY tag")
    abstract suspend fun getTags(deviceId: String): List<DeviceTagEntity>

    @Query("SELECT * FROM custom_field_values WHERE ownerType = 'DEVICE' AND ownerId = :deviceId ORDER BY fieldKey")
    abstract suspend fun getDeviceCustomFields(deviceId: String): List<CustomFieldValueEntity>

    @Query("SELECT * FROM custom_field_definitions ORDER BY sortOrder, title, id")
    abstract suspend fun getCustomFieldDefinitions(): List<CustomFieldDefinitionEntity>

    @Query("SELECT * FROM custom_field_definitions WHERE enabled = 1 ORDER BY sortOrder, title, id")
    abstract suspend fun getEnabledCustomFieldDefinitions(): List<CustomFieldDefinitionEntity>

    @Query("UPDATE custom_field_definitions SET enabled = 0 WHERE id = :definitionId")
    abstract suspend fun archiveCustomFieldDefinition(definitionId: String): Int

    @Query("SELECT COUNT(*) FROM custom_field_values WHERE fieldKey = :definitionId")
    abstract suspend fun countCustomFieldValues(definitionId: String): Int

    @Query("DELETE FROM custom_field_definitions WHERE id = :definitionId")
    protected abstract suspend fun hardDeleteCustomFieldDefinitionUnchecked(definitionId: String): Int

    @Transaction
    open suspend fun hardDeleteUnusedCustomFieldDefinition(definitionId: String): Boolean {
        if (countCustomFieldValues(definitionId) != 0) return false
        return hardDeleteCustomFieldDefinitionUnchecked(definitionId) == 1
    }

    @Query("SELECT * FROM sourced_fields WHERE ownerType = 'DEVICE' AND ownerId = :deviceId ORDER BY fieldKey")
    abstract suspend fun getDeviceSourcedFields(deviceId: String): List<SourcedFieldEntity>

    @Query("SELECT * FROM incidents WHERE recoveredAtEpochMillis IS NULL ORDER BY startedAtEpochMillis")
    abstract suspend fun getOpenIncidents(): List<IncidentEntity>

    @Query("SELECT * FROM monitoring_state WHERE targetKey = :targetKey LIMIT 1")
    abstract suspend fun getMonitoringState(targetKey: String): MonitoringStateEntity?

    @Transaction
    open suspend fun replaceDeviceAggregate(aggregate: PersistedDeviceAggregate) {
        upsertDevice(aggregate.device)
        deleteInterfacesForDevice(aggregate.device.id)
        deleteTagsForDevice(aggregate.device.id)
        deleteCustomFieldsForDevice(aggregate.device.id)
        deleteSourcedFieldsForDevice(aggregate.device.id)
        if (aggregate.interfaces.isNotEmpty()) upsertDeviceInterfaces(aggregate.interfaces)
        if (aggregate.interfaceIps.isNotEmpty()) upsertInterfaceIps(aggregate.interfaceIps)
        if (aggregate.tags.isNotEmpty()) upsertTags(aggregate.tags)
        if (aggregate.customFields.isNotEmpty()) upsertCustomFields(aggregate.customFields)
        if (aggregate.sourcedFields.isNotEmpty()) upsertSourcedFields(aggregate.sourcedFields)
    }

    @Transaction
    open suspend fun loadDeviceAggregate(deviceId: String): PersistedDeviceAggregate? {
        val device = getDevice(deviceId) ?: return null
        val interfaces = getInterfaces(deviceId)
        val ips = if (interfaces.isEmpty()) emptyList() else getInterfaceIps(interfaces.map { it.id })
        return PersistedDeviceAggregate(
            device = device,
            interfaces = interfaces,
            interfaceIps = ips,
            tags = getTags(deviceId),
            customFields = getDeviceCustomFields(deviceId),
            sourcedFields = getDeviceSourcedFields(deviceId),
        )
    }
}

data class PersistedDeviceAggregate(
    val device: DeviceEntity,
    val interfaces: List<DeviceInterfaceEntity> = emptyList(),
    val interfaceIps: List<InterfaceIpEntity> = emptyList(),
    val tags: List<DeviceTagEntity> = emptyList(),
    val customFields: List<CustomFieldValueEntity> = emptyList(),
    val sourcedFields: List<SourcedFieldEntity> = emptyList(),
)

@Database(
    entities = [
        CenterEntity::class,
        NetworkEntity::class,
        DeviceEntity::class,
        DeviceInterfaceEntity::class,
        InterfaceIpEntity::class,
        IpEndpointEntity::class,
        ServiceEntity::class,
        DeviceRelationEntity::class,
        SourcedFieldEntity::class,
        DeviceTagEntity::class,
        CustomFieldValueEntity::class,
        CustomFieldDefinitionEntity::class,
        CameraProfileEntity::class,
        RecorderProfileEntity::class,
        PcProfileEntity::class,
        RemoteProfileEntity::class,
        LocalAgentEntity::class,
        IncidentEntity::class,
        FollowUpEntity::class,
        MonitoringStateEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class NcmDatabase : RoomDatabase() {
    abstract fun canonicalDao(): CanonicalDao

    companion object {
        const val DATABASE_NAME = "network-center-monitor.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `custom_field_definitions` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `dataType` TEXT NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        `required` INTEGER NOT NULL,
                        `enabled` INTEGER NOT NULL,
                        `description` TEXT,
                        `selectOptions` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
            }
        }

        fun build(context: Context): NcmDatabase = Room.databaseBuilder(
            context.applicationContext,
            NcmDatabase::class.java,
            DATABASE_NAME,
        ).addMigrations(MIGRATION_1_2).build()
    }
}
