package com.mobinpdalab.networkcentermonitor.domain.discovery

import com.mobinpdalab.networkcentermonitor.domain.model.CenterId
import com.mobinpdalab.networkcentermonitor.domain.model.DeviceType
import com.mobinpdalab.networkcentermonitor.domain.model.FieldSource
import com.mobinpdalab.networkcentermonitor.domain.model.Ipv4Address
import com.mobinpdalab.networkcentermonitor.domain.model.MacAddress
import com.mobinpdalab.networkcentermonitor.domain.model.NetworkId
import com.mobinpdalab.networkcentermonitor.domain.model.NetworkPort
import com.mobinpdalab.networkcentermonitor.domain.model.ProvinceId
import com.mobinpdalab.networkcentermonitor.domain.model.ServiceProbe
import com.mobinpdalab.networkcentermonitor.domain.model.SourcedValue

@JvmInline
value class DiscoverySessionId(val value: String) {
    init { require(value.isNotBlank()) }
}

@JvmInline
value class DiscoveryCandidateId(val value: String) {
    init { require(value.isNotBlank()) }
}

sealed interface DiscoveryAddressScope {
    val estimatedAddressCount: Long

    data class Cidr(
        val networkAddress: Ipv4Address,
        val prefixLength: Int,
    ) : DiscoveryAddressScope {
        init { require(prefixLength in 0..32) { "CIDR prefix must be in 0..32" } }
        override val estimatedAddressCount: Long = 1L shl (32 - prefixLength)
    }

    data class Range(
        val from: Ipv4Address,
        val to: Ipv4Address,
    ) : DiscoveryAddressScope {
        init { require(to.asIpv4Long() >= from.asIpv4Long()) { "Range end must not precede start" } }
        override val estimatedAddressCount: Long = to.asIpv4Long() - from.asIpv4Long() + 1L
    }
}

enum class DiscoveryProbeMethod {
    ARP,
    ICMP,
    TCP,
    UDP,
    HTTP,
    HTTPS,
    RTSP,
    ONVIF,
    HOSTNAME,
    MAC_VENDOR,
    SERVICE_FINGERPRINT,
    VENDOR_API,
    AUTHENTICATED_INVENTORY,
    LOCAL_AGENT,
}

enum class DiscoverySpeed {
    SLOW,
    NORMAL,
    FAST,
    CUSTOM,
}

data class DiscoveryScanConfig(
    val speed: DiscoverySpeed = DiscoverySpeed.NORMAL,
    val timeoutMillis: Long = 2_000,
    val retryCount: Int = 1,
    val rateLimitPerSecond: Int = 64,
    val maxConcurrency: Int = 32,
) {
    init {
        require(timeoutMillis > 0)
        require(retryCount in 0..10)
        require(rateLimitPerSecond > 0)
        require(maxConcurrency > 0)
    }
}

data class DiscoveryRequest(
    val provinceId: ProvinceId?,
    val centerId: CenterId,
    val networkId: NetworkId,
    val addressScope: DiscoveryAddressScope,
    val methods: Set<DiscoveryProbeMethod>,
    val ports: Set<NetworkPort> = emptySet(),
    val autoDetectPorts: Boolean = true,
    val config: DiscoveryScanConfig = DiscoveryScanConfig(),
) {
    init { require(methods.isNotEmpty()) { "At least one discovery method is required" } }

    fun preview(): DiscoveryPreview {
        val addressCount = addressScope.estimatedAddressCount
        val methodProbeCount = methods.size.toLong().coerceAtLeast(1)
        val portMultiplier = ports.size.toLong().coerceAtLeast(1)
        val estimatedProbeCount = safeMultiply(safeMultiply(addressCount, methodProbeCount), portMultiplier)
        return DiscoveryPreview(
            addressCount = addressCount,
            estimatedProbeCount = estimatedProbeCount,
            requiresBoundedExecution = addressCount >= LARGE_RANGE_THRESHOLD,
        )
    }
}

data class DiscoveryPreview(
    val addressCount: Long,
    val estimatedProbeCount: Long,
    val requiresBoundedExecution: Boolean,
)

enum class DiscoverySessionStatus {
    CREATED,
    RUNNING,
    PAUSED,
    STOPPED,
    COMPLETED,
    FAILED,
}

enum class DiscoveryCommand {
    START,
    PAUSE,
    RESUME,
    STOP,
    COMPLETE,
    FAIL,
}

data class DiscoveryProgress(
    val totalAddresses: Long,
    val scannedAddresses: Long = 0,
    val candidatesFound: Int = 0,
) {
    init {
        require(totalAddresses >= 0)
        require(scannedAddresses in 0..totalAddresses)
        require(candidatesFound >= 0)
    }

    val fraction: Double
        get() = if (totalAddresses == 0L) 1.0 else scannedAddresses.toDouble() / totalAddresses
}

data class DiscoverySession(
    val id: DiscoverySessionId,
    val request: DiscoveryRequest,
    val status: DiscoverySessionStatus = DiscoverySessionStatus.CREATED,
    val progress: DiscoveryProgress = DiscoveryProgress(request.addressScope.estimatedAddressCount),
) {
    fun apply(command: DiscoveryCommand): DiscoverySession {
        val nextStatus = when (command) {
            DiscoveryCommand.START -> requireStatus(DiscoverySessionStatus.CREATED, DiscoverySessionStatus.RUNNING)
            DiscoveryCommand.PAUSE -> requireStatus(DiscoverySessionStatus.RUNNING, DiscoverySessionStatus.PAUSED)
            DiscoveryCommand.RESUME -> requireStatus(DiscoverySessionStatus.PAUSED, DiscoverySessionStatus.RUNNING)
            DiscoveryCommand.STOP -> {
                require(status == DiscoverySessionStatus.RUNNING || status == DiscoverySessionStatus.PAUSED) {
                    "Stop is only valid for active sessions"
                }
                DiscoverySessionStatus.STOPPED
            }
            DiscoveryCommand.COMPLETE -> requireStatus(DiscoverySessionStatus.RUNNING, DiscoverySessionStatus.COMPLETED)
            DiscoveryCommand.FAIL -> {
                require(status == DiscoverySessionStatus.RUNNING || status == DiscoverySessionStatus.PAUSED) {
                    "Fail is only valid for active sessions"
                }
                DiscoverySessionStatus.FAILED
            }
        }
        return copy(status = nextStatus)
    }

    private fun requireStatus(expected: DiscoverySessionStatus, next: DiscoverySessionStatus): DiscoverySessionStatus {
        require(status == expected) { "Command is invalid while session is $status" }
        return next
    }
}

data class DiscoveryCandidate(
    val id: DiscoveryCandidateId,
    val networkId: NetworkId,
    val ipAddresses: List<Ipv4Address>,
    val macAddress: MacAddress? = null,
    val hostname: String? = null,
    val detectedType: DeviceType = DeviceType.UNKNOWN,
    val confidence: Double = 0.0,
    val detectedServices: Set<ServiceProbe> = emptySet(),
    val attributes: Map<String, SourcedValue<String>> = emptyMap(),
) {
    init {
        require(ipAddresses.isNotEmpty()) { "A discovery candidate must have at least one IP" }
        require(confidence in 0.0..1.0)
        require(attributes.values.all { it.source == FieldSource.AUTO }) {
            "Discovery candidates may only contain AUTO sourced attributes"
        }
    }

    val duplicateKey: String
        get() = macAddress?.toString()?.let { "mac:$it" }
            ?: "ip:${ipAddresses.map { it.value }.sorted().joinToString(",")}" 
}

data class DiscoveryImportPlan(
    val selectedCandidateIds: Set<DiscoveryCandidateId>,
) {
    init { require(selectedCandidateIds.isNotEmpty()) { "Select at least one candidate to import" } }
}

data class ProbeObservation(
    val method: DiscoveryProbeMethod,
    val target: Ipv4Address,
    val successful: Boolean,
    val observedAtEpochMillis: Long,
    val attributes: Map<String, String> = emptyMap(),
) {
    init { require(observedAtEpochMillis >= 0) }
}

/** Adapter contract used by the one shared discovery engine. Camera/PC workers plug into this contract. */
interface DiscoveryProbeAdapter {
    val method: DiscoveryProbeMethod
    suspend fun probe(target: Ipv4Address, request: DiscoveryRequest): ProbeObservation
}

fun deduplicateCandidates(candidates: List<DiscoveryCandidate>): List<DiscoveryCandidate> =
    candidates.distinctBy { it.duplicateKey }

private fun Ipv4Address.asIpv4Long(): Long = value.split('.').fold(0L) { acc, part ->
    (acc shl 8) or part.toLong()
}

private fun safeMultiply(left: Long, right: Long): Long =
    if (left == 0L || right == 0L) 0L
    else if (left > Long.MAX_VALUE / right) Long.MAX_VALUE
    else left * right

private const val LARGE_RANGE_THRESHOLD = 65_536L
