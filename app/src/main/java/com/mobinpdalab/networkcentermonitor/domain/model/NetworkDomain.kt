package com.mobinpdalab.networkcentermonitor.domain.model

@JvmInline
value class ProvinceId(val value: String) {
    init { require(value.isNotBlank()) }
}

@JvmInline
value class CenterId(val value: String) {
    init { require(value.isNotBlank()) }
}

@JvmInline
value class IpEndpointId(val value: String) {
    init { require(value.isNotBlank()) }
}

@JvmInline
value class ServiceId(val value: String) {
    init { require(value.isNotBlank()) }
}

@JvmInline
value class IncidentId(val value: String) {
    init { require(value.isNotBlank()) }
}

@JvmInline
value class FollowUpId(val value: String) {
    init { require(value.isNotBlank()) }
}

@JvmInline
value class Ipv4Address(val value: String) {
    init {
        require(isValidIpv4(value)) { "Invalid IPv4 address: $value" }
    }

    override fun toString(): String = value
}

@JvmInline
value class NetworkPort(val value: Int) {
    init {
        require(value in 1..65535) { "Port must be in 1..65535" }
    }
}

sealed interface ServiceProbe {
    data object Icmp : ServiceProbe
    data class Tcp(val port: NetworkPort) : ServiceProbe
    data class Udp(val port: NetworkPort) : ServiceProbe
}

enum class MonitoringStatus {
    CONNECTED,
    DISCONNECTED,
    SUSPECTED,
    DISABLED,
    UNVERIFIED,
}

enum class ServiceCriticality {
    NORMAL,
    IMPORTANT,
    CRITICAL,
}

enum class FollowUpStatus {
    NEW,
    OPEN,
    IN_PROGRESS,
    WAITING_FOR_RESPONSE,
    REFERRED,
    OVERDUE,
    DONE,
    CLOSED,
}

data class Center(
    val id: CenterId,
    val provinceId: ProvinceId,
    val name: String,
    val address: String? = null,
) {
    init { require(name.isNotBlank()) }
}

data class IpEndpoint(
    val id: IpEndpointId,
    val centerId: CenterId,
    val address: Ipv4Address,
    val monitoringEnabled: Boolean = true,
    val pingEnabled: Boolean = true,
    val asset: AssetMetadata = AssetMetadata(),
)

data class MonitoredService(
    val id: ServiceId,
    val endpointId: IpEndpointId,
    val name: String,
    val probe: ServiceProbe,
    val criticality: ServiceCriticality = ServiceCriticality.NORMAL,
    val monitoringEnabled: Boolean = true,
) {
    init { require(name.isNotBlank()) }
}

data class OutageIncident(
    val id: IncidentId,
    val endpointId: IpEndpointId,
    val serviceId: ServiceId? = null,
    val startedAtEpochMillis: Long,
    val recoveredAtEpochMillis: Long? = null,
) {
    init {
        require(startedAtEpochMillis >= 0)
        require(recoveredAtEpochMillis == null || recoveredAtEpochMillis >= startedAtEpochMillis)
    }

    val isOpen: Boolean get() = recoveredAtEpochMillis == null
    val durationMillis: Long? get() = recoveredAtEpochMillis?.minus(startedAtEpochMillis)
}

data class FollowUp(
    val id: FollowUpId,
    val incidentId: IncidentId?,
    val title: String,
    val status: FollowUpStatus,
    val createdAtEpochMillis: Long,
    val dueAtEpochMillis: Long? = null,
    val notes: String? = null,
) {
    init {
        require(title.isNotBlank())
        require(createdAtEpochMillis >= 0)
    }
}

data class AssetMetadata(
    val emergencyPhone: String? = null,
    val assetNumber: String? = null,
    val imei: String? = null,
    val simSerialNumber: String? = null,
    val deviceSerialNumber: String? = null,
    val dedicatedNumber: String? = null,
    val operatorName: String? = null,
    val manufacturer: String? = null,
    val model: String? = null,
    val customFieldValues: Map<String, String> = emptyMap(),
)

private fun isValidIpv4(value: String): Boolean {
    val parts = value.split('.')
    if (parts.size != 4) return false
    return parts.all { part ->
        if (part.isEmpty() || part.length > 3) return@all false
        if (!part.all(Char::isDigit)) return@all false
        if (part.length > 1 && part.first() == '0') return@all false
        part.toIntOrNull() in 0..255
    }
}
