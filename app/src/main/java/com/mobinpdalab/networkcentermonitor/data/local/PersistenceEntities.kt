package com.mobinpdalab.networkcentermonitor.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "centers")
data class CenterEntity(
    @androidx.room.PrimaryKey val id: String,
    val provinceId: String,
    val name: String,
    val address: String? = null,
)

@Entity(
    tableName = "networks",
    foreignKeys = [ForeignKey(
        entity = CenterEntity::class,
        parentColumns = ["id"],
        childColumns = ["centerId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("centerId")],
)
data class NetworkEntity(
    @androidx.room.PrimaryKey val id: String,
    val centerId: String,
    val name: String,
    val type: String,
    val cidr: String? = null,
    val fromIp: String? = null,
    val toIp: String? = null,
    val gateway: String? = null,
    val vlanId: Int? = null,
    val accessMethod: String,
    val monitoringEnabled: Boolean,
    val discoveryEnabled: Boolean,
    val notes: String? = null,
)

@Entity(
    tableName = "devices",
    foreignKeys = [
        ForeignKey(
            entity = CenterEntity::class,
            parentColumns = ["id"],
            childColumns = ["centerId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = NetworkEntity::class,
            parentColumns = ["id"],
            childColumns = ["networkId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("centerId"), Index("networkId")],
)
data class DeviceEntity(
    @androidx.room.PrimaryKey val id: String,
    val centerId: String,
    val networkId: String? = null,
    val displayName: String,
    val type: String,
    val monitoringEnabled: Boolean,
    val status: String,
    val lastSeenEpochMillis: Long? = null,
    val lastStatusChangeEpochMillis: Long? = null,
    val lastDiscoveryEpochMillis: Long? = null,
    val notes: String? = null,
    val emergencyPhone: String? = null,
    val assetNumber: String? = null,
    val imei: String? = null,
    val simSerialNumber: String? = null,
    val deviceSerialNumber: String? = null,
    val dedicatedNumber: String? = null,
    val operatorName: String? = null,
    val manufacturer: String? = null,
    val model: String? = null,
)

@Entity(
    tableName = "device_interfaces",
    foreignKeys = [ForeignKey(
        entity = DeviceEntity::class,
        parentColumns = ["id"],
        childColumns = ["deviceId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("deviceId"), Index(value = ["macAddress"], unique = false)],
)
data class DeviceInterfaceEntity(
    @androidx.room.PrimaryKey val id: String,
    val deviceId: String,
    val name: String? = null,
    val macAddress: String? = null,
)

@Entity(
    tableName = "interface_ips",
    primaryKeys = ["interfaceId", "address"],
    foreignKeys = [ForeignKey(
        entity = DeviceInterfaceEntity::class,
        parentColumns = ["id"],
        childColumns = ["interfaceId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("interfaceId"), Index("address")],
)
data class InterfaceIpEntity(
    val interfaceId: String,
    val address: String,
)

@Entity(
    tableName = "ip_endpoints",
    foreignKeys = [
        ForeignKey(
            entity = CenterEntity::class,
            parentColumns = ["id"],
            childColumns = ["centerId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = NetworkEntity::class,
            parentColumns = ["id"],
            childColumns = ["networkId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = DeviceEntity::class,
            parentColumns = ["id"],
            childColumns = ["deviceId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = DeviceInterfaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["deviceInterfaceId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("centerId"), Index("networkId"), Index("deviceId"), Index("deviceInterfaceId"), Index("address")],
)
data class IpEndpointEntity(
    @androidx.room.PrimaryKey val id: String,
    val centerId: String,
    val address: String,
    val networkId: String? = null,
    val deviceId: String? = null,
    val deviceInterfaceId: String? = null,
    val monitoringEnabled: Boolean,
    val pingEnabled: Boolean,
)

@Entity(
    tableName = "services",
    foreignKeys = [ForeignKey(
        entity = IpEndpointEntity::class,
        parentColumns = ["id"],
        childColumns = ["endpointId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("endpointId")],
)
data class ServiceEntity(
    @androidx.room.PrimaryKey val id: String,
    val endpointId: String,
    val name: String,
    val probeType: String,
    val port: Int? = null,
    val criticality: String,
    val monitoringEnabled: Boolean,
)

@Entity(
    tableName = "device_relations",
    foreignKeys = [
        ForeignKey(
            entity = DeviceEntity::class,
            parentColumns = ["id"],
            childColumns = ["fromDeviceId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = DeviceEntity::class,
            parentColumns = ["id"],
            childColumns = ["toDeviceId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("fromDeviceId"), Index("toDeviceId")],
)
data class DeviceRelationEntity(
    @androidx.room.PrimaryKey val id: String,
    val fromDeviceId: String,
    val toDeviceId: String,
    val type: String,
    val channelNumber: Int? = null,
    val notes: String? = null,
)

@Entity(tableName = "sourced_fields", primaryKeys = ["ownerType", "ownerId", "fieldKey"], indices = [Index("ownerId")])
data class SourcedFieldEntity(
    val ownerType: String,
    val ownerId: String,
    val fieldKey: String,
    val value: String,
    val source: String,
    val lastUpdatedEpochMillis: Long,
    val lastDiscoveryEpochMillis: Long? = null,
)

@Entity(
    tableName = "device_tags",
    primaryKeys = ["deviceId", "tag"],
    foreignKeys = [ForeignKey(
        entity = DeviceEntity::class,
        parentColumns = ["id"],
        childColumns = ["deviceId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("deviceId")],
)
data class DeviceTagEntity(val deviceId: String, val tag: String)

@Entity(tableName = "custom_field_values", primaryKeys = ["ownerType", "ownerId", "fieldKey"], indices = [Index("ownerId")])
data class CustomFieldValueEntity(
    val ownerType: String,
    val ownerId: String,
    val fieldKey: String,
    val value: String,
)

@Entity(
    tableName = "camera_profiles",
    foreignKeys = [
        ForeignKey(entity = DeviceEntity::class, parentColumns = ["id"], childColumns = ["deviceId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = DeviceEntity::class, parentColumns = ["id"], childColumns = ["recorderDeviceId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index(value = ["deviceId"], unique = true), Index("recorderDeviceId")],
)
data class CameraProfileEntity(
    @androidx.room.PrimaryKey val id: String,
    val deviceId: String,
    val environmentName: String? = null,
    val installationLocation: String? = null,
    val cameraType: String,
    val recorderDeviceId: String? = null,
    val channelNumber: Int? = null,
    val httpPort: Int? = null,
    val httpsPort: Int? = null,
    val rtspPort: Int? = null,
    val onvifPort: Int? = null,
    val capabilities: String,
    val notes: String? = null,
)

@Entity(
    tableName = "recorder_profiles",
    foreignKeys = [ForeignKey(entity = DeviceEntity::class, parentColumns = ["id"], childColumns = ["deviceId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["deviceId"], unique = true)],
)
data class RecorderProfileEntity(
    @androidx.room.PrimaryKey val id: String,
    val deviceId: String,
    val recordingSupported: Boolean,
    val hddHealthSupported: Boolean,
    val onvifProfiles: String,
    val notes: String? = null,
)

@Entity(
    tableName = "pc_profiles",
    foreignKeys = [ForeignKey(entity = DeviceEntity::class, parentColumns = ["id"], childColumns = ["deviceId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["deviceId"], unique = true)],
)
data class PcProfileEntity(
    @androidx.room.PrimaryKey val id: String,
    val deviceId: String,
    val notes: String? = null,
)

@Entity(
    tableName = "remote_profiles",
    foreignKeys = [
        ForeignKey(entity = DeviceEntity::class, parentColumns = ["id"], childColumns = ["deviceId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = IpEndpointEntity::class, parentColumns = ["id"], childColumns = ["endpointId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("deviceId"), Index("endpointId")],
)
data class RemoteProfileEntity(
    @androidx.room.PrimaryKey val id: String,
    val deviceId: String,
    val endpointId: String? = null,
    val method: String,
    val displayName: String? = null,
    val defaultPort: Int,
    val customPort: Int? = null,
    val status: String,
    val enabled: Boolean,
    val priority: Int,
    val secureCredentialRef: String? = null,
    val askOnConnect: Boolean,
    val customLaunchTemplate: String? = null,
)

@Entity(
    tableName = "local_agents",
    foreignKeys = [ForeignKey(entity = CenterEntity::class, parentColumns = ["id"], childColumns = ["centerId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("centerId")],
)
data class LocalAgentEntity(
    @androidx.room.PrimaryKey val id: String,
    val centerId: String,
    val networkIds: String,
    val capabilities: String,
    val status: String,
    val enrollmentId: String? = null,
    val credentialRef: String? = null,
    val enrolledAtEpochMillis: Long? = null,
    val initialBackoffMillis: Long,
    val maxBackoffMillis: Long,
    val maxBufferedBatches: Int,
    val lastSeenEpochMillis: Long? = null,
)

@Entity(
    tableName = "incidents",
    foreignKeys = [
        ForeignKey(entity = IpEndpointEntity::class, parentColumns = ["id"], childColumns = ["endpointId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ServiceEntity::class, parentColumns = ["id"], childColumns = ["serviceId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("endpointId"), Index("serviceId")],
)
data class IncidentEntity(
    @androidx.room.PrimaryKey val id: String,
    val endpointId: String,
    val serviceId: String? = null,
    val startedAtEpochMillis: Long,
    val recoveredAtEpochMillis: Long? = null,
)

@Entity(
    tableName = "follow_ups",
    foreignKeys = [ForeignKey(entity = IncidentEntity::class, parentColumns = ["id"], childColumns = ["incidentId"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index("incidentId")],
)
data class FollowUpEntity(
    @androidx.room.PrimaryKey val id: String,
    val incidentId: String? = null,
    val title: String,
    val status: String,
    val createdAtEpochMillis: Long,
    val dueAtEpochMillis: Long? = null,
    val notes: String? = null,
)

@Entity(tableName = "monitoring_state")
data class MonitoringStateEntity(
    @androidx.room.PrimaryKey val targetKey: String,
    val status: String,
    val consecutiveFailures: Int,
    val consecutiveSuccesses: Int,
    val lastProbeAtEpochMillis: Long? = null,
    val lastStatusChangeEpochMillis: Long? = null,
    val activeIncidentId: String? = null,
    val maintenanceUntilEpochMillis: Long? = null,
)

/** Shared registry used by Backup/Restore coverage checks. */
object CanonicalPersistenceTables {
    val names: Set<String> = setOf(
        "centers", "networks", "devices", "device_interfaces", "interface_ips",
        "ip_endpoints", "services", "device_relations", "sourced_fields", "device_tags",
        "custom_field_values", "camera_profiles", "recorder_profiles", "pc_profiles",
        "remote_profiles", "local_agents", "incidents", "follow_ups", "monitoring_state",
    )
}
