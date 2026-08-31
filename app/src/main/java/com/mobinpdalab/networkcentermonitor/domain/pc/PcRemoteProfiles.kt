package com.mobinpdalab.networkcentermonitor.domain.pc

import com.mobinpdalab.networkcentermonitor.domain.model.DeviceId
import com.mobinpdalab.networkcentermonitor.domain.model.DeviceProfile
import com.mobinpdalab.networkcentermonitor.domain.model.IpEndpointId
import com.mobinpdalab.networkcentermonitor.domain.model.NetworkPort
import com.mobinpdalab.networkcentermonitor.domain.model.SourcedValue

@JvmInline
value class PcProfileId(val value: String) {
    init { require(value.isNotBlank()) }
}

@JvmInline
value class RemoteProfileId(val value: String) {
    init { require(value.isNotBlank()) }
}

@JvmInline
value class SecureCredentialRef(val value: String) {
    init { require(value.isNotBlank()) }
}

data class StorageInventory(
    val totalBytes: Long,
    val freeBytes: Long,
) {
    init {
        require(totalBytes >= 0)
        require(freeBytes in 0..totalBytes)
    }
}

data class PcProfile(
    val id: PcProfileId,
    override val deviceId: DeviceId,
    val computerName: SourcedValue<String>? = null,
    val manufacturer: SourcedValue<String>? = null,
    val model: SourcedValue<String>? = null,
    val serialNumber: SourcedValue<String>? = null,
    val windowsEdition: SourcedValue<String>? = null,
    val windowsVersion: SourcedValue<String>? = null,
    val windowsBuild: SourcedValue<String>? = null,
    val cpu: SourcedValue<String>? = null,
    val ramBytes: SourcedValue<Long>? = null,
    val storage: SourcedValue<StorageInventory>? = null,
    val uptimeSeconds: SourcedValue<Long>? = null,
    val domainOrWorkgroup: SourcedValue<String>? = null,
    val installedSoftware: List<SourcedValue<String>> = emptyList(),
    val notes: String? = null,
) : DeviceProfile {
    init {
        require(ramBytes == null || ramBytes.value >= 0)
        require(uptimeSeconds == null || uptimeSeconds.value >= 0)
    }
}

enum class RemoteMethod {
    RDP,
    RADMIN,
    RUSTDESK_DIRECT,
    VNC,
    TEAMVIEWER_LAN,
    CUSTOM,
}

enum class RemoteReachabilityStatus {
    REACHABLE,
    CONFIGURED_UNAVAILABLE,
    UNKNOWN,
    DISABLED,
}

data class RemotePortConfig(
    val defaultPort: NetworkPort,
    val customPort: NetworkPort? = null,
) {
    val effectivePort: NetworkPort get() = customPort ?: defaultPort
}

data class RemoteCredentialPolicy(
    val secureCredentialRef: SecureCredentialRef? = null,
    val askOnConnect: Boolean = true,
) {
    init {
        require(askOnConnect || secureCredentialRef != null) {
            "A remote profile must ask on connect or reference secure storage"
        }
    }
}

data class RemoteProfile(
    val id: RemoteProfileId,
    val deviceId: DeviceId,
    val endpointId: IpEndpointId? = null,
    val method: RemoteMethod,
    val displayName: String? = null,
    val port: RemotePortConfig,
    val status: RemoteReachabilityStatus = RemoteReachabilityStatus.UNKNOWN,
    val enabled: Boolean = true,
    val priority: Int = 100,
    val credentials: RemoteCredentialPolicy = RemoteCredentialPolicy(),
    val customLaunchTemplate: String? = null,
) {
    init {
        require(displayName == null || displayName.isNotBlank())
        require(priority >= 0)
        require(method == RemoteMethod.CUSTOM || customLaunchTemplate == null) {
            "Launch templates are only valid for custom remote methods"
        }
        require(method != RemoteMethod.CUSTOM || !customLaunchTemplate.isNullOrBlank()) {
            "Custom remote method requires a launch template"
        }
    }

    val healthCheckPort: NetworkPort get() = port.effectivePort
    val connectionTestPort: NetworkPort get() = port.effectivePort
    val launchPort: NetworkPort get() = port.effectivePort
}

sealed interface RemoteLaunchDecision {
    data object NoneAvailable : RemoteLaunchDecision
    data class Direct(val profile: RemoteProfile) : RemoteLaunchDecision
    data class Pick(val profiles: List<RemoteProfile>) : RemoteLaunchDecision
}

fun chooseRemoteLaunch(profiles: List<RemoteProfile>): RemoteLaunchDecision {
    val eligible = profiles
        .filter { it.enabled }
        .sortedWith(compareBy<RemoteProfile> { it.priority }.thenBy { it.method.name })

    return when (eligible.size) {
        0 -> RemoteLaunchDecision.NoneAvailable
        1 -> RemoteLaunchDecision.Direct(eligible.single())
        else -> RemoteLaunchDecision.Pick(eligible)
    }
}

data class RemoteNetworkPolicy(
    val privateNetworkFirst: Boolean = true,
    val publicInternetDependencyAllowed: Boolean = false,
) {
    init {
        require(privateNetworkFirst) { "Remote access must remain LAN/private-network first" }
        require(!publicInternetDependencyAllowed) { "Public Internet cannot be a core remote dependency" }
    }
}

object StandardRemotePorts {
    val rdp = NetworkPort(3389)
    val vnc = NetworkPort(5900)
}
