package com.mobinpdalab.networkcentermonitor.domain.preset

import com.mobinpdalab.networkcentermonitor.domain.discovery.DiscoveryProbeMethod
import com.mobinpdalab.networkcentermonitor.domain.model.DeviceType
import com.mobinpdalab.networkcentermonitor.domain.model.NetworkPort
import com.mobinpdalab.networkcentermonitor.domain.model.ServiceCriticality

@JvmInline
value class EquipmentPresetId(val value: String) {
    init { require(value.isNotBlank()) }
}

enum class PresetOrigin {
    BUILT_IN,
    USER,
    IMPORTED,
}

enum class PresetProtocol {
    TCP,
    UDP,
}

data class PresetPort(
    val serviceName: String,
    val protocol: PresetProtocol,
    val port: NetworkPort,
    val criticality: ServiceCriticality = ServiceCriticality.NORMAL,
    val monitoringEnabledByDefault: Boolean = true,
    val discoveryEnabled: Boolean = true,
) {
    init { require(serviceName.isNotBlank()) }
}

data class EquipmentPreset(
    val id: EquipmentPresetId,
    val name: String,
    val deviceType: DeviceType,
    val origin: PresetOrigin,
    val brandHint: String? = null,
    val modelHint: String? = null,
    val probeMethods: Set<DiscoveryProbeMethod> = emptySet(),
    val ports: List<PresetPort> = emptyList(),
    val fingerprintHints: Map<String, String> = emptyMap(),
    val tags: Set<String> = emptySet(),
    val archived: Boolean = false,
) {
    init {
        require(name.isNotBlank())
        require(brandHint == null || brandHint.isNotBlank())
        require(modelHint == null || modelHint.isNotBlank())
        require(fingerprintHints.keys.none { it.isBlank() })
        require(ports.distinctBy { it.protocol to it.port.value }.size == ports.size) {
            "A preset cannot contain duplicate protocol/port pairs"
        }
    }

    fun archive(): EquipmentPreset = copy(archived = true)
}

data class PresetDiscoveryPlan(
    val methods: Set<DiscoveryProbeMethod>,
    val ports: Set<NetworkPort>,
)

fun buildPresetDiscoveryPlan(presets: Collection<EquipmentPreset>): PresetDiscoveryPlan {
    val active = presets.filterNot { it.archived }
    val portEntries = active.flatMap { it.ports }.filter { it.discoveryEnabled }
    val methods = buildSet {
        active.forEach { addAll(it.probeMethods) }
        if (portEntries.any { it.protocol == PresetProtocol.TCP }) add(DiscoveryProbeMethod.TCP)
        if (portEntries.any { it.protocol == PresetProtocol.UDP }) add(DiscoveryProbeMethod.UDP)
    }
    return PresetDiscoveryPlan(
        methods = methods,
        ports = portEntries.map { it.port }.toSortedSet(compareBy { it.value }),
    )
}

/**
 * Executable built-in data used by Discovery and service suggestions. These are hints/defaults only;
 * they never overwrite protected Manual/Imported Device fields.
 */
object BuiltInEquipmentPresets {
    val generalNetworkDevice = EquipmentPreset(
        id = EquipmentPresetId("builtin-network-device"),
        name = "Network Device",
        deviceType = DeviceType.UNKNOWN,
        origin = PresetOrigin.BUILT_IN,
        probeMethods = setOf(
            DiscoveryProbeMethod.ICMP,
            DiscoveryProbeMethod.HTTP,
            DiscoveryProbeMethod.HTTPS,
            DiscoveryProbeMethod.HOSTNAME,
            DiscoveryProbeMethod.SERVICE_FINGERPRINT,
        ),
        ports = listOf(
            PresetPort("HTTP", PresetProtocol.TCP, NetworkPort(80)),
            PresetPort("HTTPS", PresetProtocol.TCP, NetworkPort(443)),
            PresetPort("SSH", PresetProtocol.TCP, NetworkPort(22)),
            PresetPort("SNMP", PresetProtocol.UDP, NetworkPort(161), monitoringEnabledByDefault = false),
        ),
    )

    val camera = EquipmentPreset(
        id = EquipmentPresetId("builtin-camera"),
        name = "IP Camera",
        deviceType = DeviceType.CAMERA,
        origin = PresetOrigin.BUILT_IN,
        probeMethods = setOf(
            DiscoveryProbeMethod.ICMP,
            DiscoveryProbeMethod.ONVIF,
            DiscoveryProbeMethod.RTSP,
            DiscoveryProbeMethod.HTTP,
            DiscoveryProbeMethod.HTTPS,
            DiscoveryProbeMethod.SERVICE_FINGERPRINT,
        ),
        ports = listOf(
            PresetPort("HTTP", PresetProtocol.TCP, NetworkPort(80)),
            PresetPort("HTTPS", PresetProtocol.TCP, NetworkPort(443)),
            PresetPort("RTSP", PresetProtocol.TCP, NetworkPort(554), ServiceCriticality.IMPORTANT),
        ),
        tags = setOf("camera", "onvif"),
    )

    val recorder = EquipmentPreset(
        id = EquipmentPresetId("builtin-recorder"),
        name = "NVR / DVR",
        deviceType = DeviceType.NVR,
        origin = PresetOrigin.BUILT_IN,
        probeMethods = setOf(
            DiscoveryProbeMethod.ICMP,
            DiscoveryProbeMethod.ONVIF,
            DiscoveryProbeMethod.RTSP,
            DiscoveryProbeMethod.HTTP,
            DiscoveryProbeMethod.HTTPS,
            DiscoveryProbeMethod.SERVICE_FINGERPRINT,
        ),
        ports = listOf(
            PresetPort("HTTP", PresetProtocol.TCP, NetworkPort(80)),
            PresetPort("HTTPS", PresetProtocol.TCP, NetworkPort(443)),
            PresetPort("RTSP", PresetProtocol.TCP, NetworkPort(554), ServiceCriticality.IMPORTANT),
        ),
        tags = setOf("recorder"),
    )

    val windowsPc = EquipmentPreset(
        id = EquipmentPresetId("builtin-windows-pc"),
        name = "Windows PC",
        deviceType = DeviceType.PC,
        origin = PresetOrigin.BUILT_IN,
        probeMethods = setOf(
            DiscoveryProbeMethod.ICMP,
            DiscoveryProbeMethod.HOSTNAME,
            DiscoveryProbeMethod.AUTHENTICATED_INVENTORY,
            DiscoveryProbeMethod.SERVICE_FINGERPRINT,
        ),
        ports = listOf(
            PresetPort("RDP", PresetProtocol.TCP, NetworkPort(3389), ServiceCriticality.IMPORTANT),
            PresetPort("VNC", PresetProtocol.TCP, NetworkPort(5900), monitoringEnabledByDefault = false),
            PresetPort("Radmin", PresetProtocol.TCP, NetworkPort(4899), monitoringEnabledByDefault = false),
        ),
        tags = setOf("pc", "windows"),
    )

    val mikrotik = EquipmentPreset(
        id = EquipmentPresetId("builtin-mikrotik"),
        name = "MikroTik Router",
        deviceType = DeviceType.MIKROTIK,
        origin = PresetOrigin.BUILT_IN,
        brandHint = "MikroTik",
        probeMethods = setOf(
            DiscoveryProbeMethod.ICMP,
            DiscoveryProbeMethod.HTTP,
            DiscoveryProbeMethod.HTTPS,
            DiscoveryProbeMethod.SERVICE_FINGERPRINT,
        ),
        ports = listOf(
            PresetPort("WinBox", PresetProtocol.TCP, NetworkPort(8291), ServiceCriticality.IMPORTANT),
            PresetPort("SSH", PresetProtocol.TCP, NetworkPort(22)),
            PresetPort("HTTP", PresetProtocol.TCP, NetworkPort(80)),
            PresetPort("HTTPS", PresetProtocol.TCP, NetworkPort(443)),
        ),
        tags = setOf("router", "mikrotik"),
    )

    val all: List<EquipmentPreset> = listOf(generalNetworkDevice, camera, recorder, windowsPc, mikrotik)
}
