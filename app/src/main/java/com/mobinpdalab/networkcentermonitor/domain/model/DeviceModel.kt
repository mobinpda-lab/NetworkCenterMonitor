package com.mobinpdalab.networkcentermonitor.domain.model

@JvmInline
value class NetworkId(val value: String) {
    init { require(value.isNotBlank()) }
}

@JvmInline
value class DeviceId(val value: String) {
    init { require(value.isNotBlank()) }
}

@JvmInline
value class DeviceInterfaceId(val value: String) {
    init { require(value.isNotBlank()) }
}

@JvmInline
value class DeviceRelationId(val value: String) {
    init { require(value.isNotBlank()) }
}

@JvmInline
value class MacAddress(val value: String) {
    init { require(MAC_REGEX.matches(value)) { "Invalid MAC address: $value" } }
    override fun toString(): String = value.uppercase().replace('-', ':')
}

enum class NetworkType {
    GENERAL,
    CAMERA,
    MANAGEMENT,
    SERVER,
    VOIP,
    INDUSTRIAL,
    OFFICE,
    CUSTOM,
}

enum class NetworkAccessMethod {
    LAN,
    PRIVATE_APN,
    ROUTED_APN,
    VPN,
    ROUTE,
    PORT_FORWARD,
    LOCAL_AGENT,
    CUSTOM,
}

enum class DeviceType {
    ROUTER,
    FIREWALL,
    MODEM,
    MIKROTIK,
    SWITCH,
    ACCESS_POINT,
    SERVER,
    DATABASE_SERVER,
    PC,
    WORKSTATION,
    CAMERA,
    NVR,
    DVR,
    NETWORK_PRINTER,
    VOIP,
    PBX,
    PLC,
    INDUSTRIAL_DEVICE,
    UPS,
    STORAGE,
    NAS,
    UNKNOWN,
    CUSTOM,
}

enum class DeviceRelationType {
    PARENT_CHILD,
    CONNECTED_TO,
    MANAGED_BY,
    RECORDED_BY,
    CONNECTED_VIA,
    NETWORK_PARENT,
}

enum class FieldSource {
    AUTO,
    MANUAL,
    IMPORTED,
}

data class SourcedValue<T>(
    val value: T,
    val source: FieldSource,
    val lastUpdatedEpochMillis: Long,
    val lastDiscoveryEpochMillis: Long? = null,
) {
    init {
        require(lastUpdatedEpochMillis >= 0)
        require(lastDiscoveryEpochMillis == null || lastDiscoveryEpochMillis >= 0)
    }

    val protectsAgainstAutoOverwrite: Boolean get() = source != FieldSource.AUTO
}

data class DiscoveryMerge<T>(
    val effective: SourcedValue<T>,
    val proposed: SourcedValue<T>? = null,
    val requiresUserConfirmation: Boolean = false,
)

fun <T> mergeDiscoveredValue(
    current: SourcedValue<T>?,
    discoveredValue: T,
    discoveredAtEpochMillis: Long,
): DiscoveryMerge<T> {
    require(discoveredAtEpochMillis >= 0)
    val discovered = SourcedValue(
        value = discoveredValue,
        source = FieldSource.AUTO,
        lastUpdatedEpochMillis = discoveredAtEpochMillis,
        lastDiscoveryEpochMillis = discoveredAtEpochMillis,
    )
    if (current == null || current.source == FieldSource.AUTO) {
        return DiscoveryMerge(effective = discovered)
    }
    if (current.value == discoveredValue) {
        return DiscoveryMerge(effective = current)
    }
    return DiscoveryMerge(
        effective = current,
        proposed = discovered,
        requiresUserConfirmation = true,
    )
}

data class CenterNetwork(
    val id: NetworkId,
    val centerId: CenterId,
    val name: String,
    val type: NetworkType = NetworkType.GENERAL,
    val cidr: String? = null,
    val fromIp: Ipv4Address? = null,
    val toIp: Ipv4Address? = null,
    val gateway: Ipv4Address? = null,
    val vlanId: Int? = null,
    val accessMethod: NetworkAccessMethod = NetworkAccessMethod.LAN,
    val monitoringEnabled: Boolean = true,
    val discoveryEnabled: Boolean = true,
    val notes: String? = null,
) {
    init {
        require(name.isNotBlank())
        require(vlanId == null || vlanId in 0..4094) { "VLAN ID must be in 0..4094" }
        require((fromIp == null) == (toIp == null)) { "From/To IP must be provided together" }
    }
}

data class DeviceInterface(
    val id: DeviceInterfaceId,
    val deviceId: DeviceId,
    val name: String? = null,
    val macAddress: MacAddress? = null,
    val ipAddresses: List<Ipv4Address> = emptyList(),
) {
    init {
        require(name == null || name.isNotBlank())
        require(ipAddresses.distinct().size == ipAddresses.size) { "Duplicate IP on interface" }
    }
}

data class DeviceIdentity(
    val manufacturer: SourcedValue<String>? = null,
    val brand: SourcedValue<String>? = null,
    val model: SourcedValue<String>? = null,
    val hostname: SourcedValue<String>? = null,
    val serialNumber: SourcedValue<String>? = null,
    val firmware: SourcedValue<String>? = null,
    val operatingSystem: SourcedValue<String>? = null,
    val imei: SourcedValue<String>? = null,
    val simSerial: SourcedValue<String>? = null,
    val operatorName: SourcedValue<String>? = null,
    val dedicatedNumber: SourcedValue<String>? = null,
    val phoneNumber: SourcedValue<String>? = null,
)

data class Device(
    val id: DeviceId,
    val centerId: CenterId,
    val networkId: NetworkId?,
    val displayName: String,
    val type: DeviceType = DeviceType.UNKNOWN,
    val identity: DeviceIdentity = DeviceIdentity(),
    val interfaces: List<DeviceInterface> = emptyList(),
    val asset: AssetMetadata = AssetMetadata(),
    val monitoringEnabled: Boolean = true,
    val status: MonitoringStatus = MonitoringStatus.UNVERIFIED,
    val lastSeenEpochMillis: Long? = null,
    val lastStatusChangeEpochMillis: Long? = null,
    val lastDiscoveryEpochMillis: Long? = null,
    val tags: Set<String> = emptySet(),
    val notes: String? = null,
    val customFieldValues: Map<String, String> = emptyMap(),
) {
    init {
        require(displayName.isNotBlank())
        require(interfaces.all { it.deviceId == id }) { "Every interface must belong to the device" }
        require(interfaces.map { it.id }.distinct().size == interfaces.size) { "Duplicate interface ID" }
        require(lastSeenEpochMillis == null || lastSeenEpochMillis >= 0)
        require(lastStatusChangeEpochMillis == null || lastStatusChangeEpochMillis >= 0)
        require(lastDiscoveryEpochMillis == null || lastDiscoveryEpochMillis >= 0)
    }

    val ipAddresses: List<Ipv4Address>
        get() = interfaces.flatMap { it.ipAddresses }.distinct()

    val macAddresses: List<MacAddress>
        get() = interfaces.mapNotNull { it.macAddress }.distinctBy { it.toString() }
}

data class DeviceRelation(
    val id: DeviceRelationId,
    val fromDeviceId: DeviceId,
    val toDeviceId: DeviceId,
    val type: DeviceRelationType,
    val channelNumber: Int? = null,
    val notes: String? = null,
) {
    init {
        require(fromDeviceId != toDeviceId) { "Device cannot relate to itself" }
        require(channelNumber == null || channelNumber > 0) { "Channel number must be positive" }
    }
}

/**
 * Specialized data is attached to the canonical Device through this contract.
 * Camera, Recorder and PC profiles must never create a second device identity.
 */
interface DeviceProfile {
    val deviceId: DeviceId
}

private val MAC_REGEX = Regex("^(?:[0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}$")
