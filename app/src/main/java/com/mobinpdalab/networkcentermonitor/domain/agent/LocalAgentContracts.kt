package com.mobinpdalab.networkcentermonitor.domain.agent

import com.mobinpdalab.networkcentermonitor.domain.model.CenterId
import com.mobinpdalab.networkcentermonitor.domain.model.DeviceId
import com.mobinpdalab.networkcentermonitor.domain.model.FieldSource
import com.mobinpdalab.networkcentermonitor.domain.model.NetworkId

@JvmInline
value class LocalAgentId(val value: String) {
    init { require(value.isNotBlank()) }
}

@JvmInline
value class AgentEnrollmentId(val value: String) {
    init { require(value.isNotBlank()) }
}

@JvmInline
value class AgentCredentialRef(val value: String) {
    init { require(value.isNotBlank()) }
}

enum class AgentCapability {
    LAN_DISCOVERY,
    ONVIF_DISCOVERY,
    CAMERA_DISCOVERY,
    DEVICE_HEALTH,
    PC_INVENTORY,
    REMOTE_SOFTWARE_DETECTION,
}

enum class AgentStatus {
    UNENROLLED,
    ENROLLED,
    ONLINE,
    OFFLINE,
    DISABLED,
}

data class AgentScope(
    val centerId: CenterId,
    val networkIds: Set<NetworkId>,
) {
    init { require(networkIds.isNotEmpty()) { "Agent must be scoped to at least one canonical network" } }
}

data class AgentEnrollment(
    val id: AgentEnrollmentId,
    val agentId: LocalAgentId,
    val credentialRef: AgentCredentialRef,
    val enrolledAtEpochMillis: Long,
) {
    init { require(enrolledAtEpochMillis >= 0) }
}

data class AgentRetryPolicy(
    val initialBackoffMillis: Long = 1_000,
    val maxBackoffMillis: Long = 60_000,
    val maxBufferedBatches: Int = 100,
) {
    init {
        require(initialBackoffMillis > 0)
        require(maxBackoffMillis >= initialBackoffMillis)
        require(maxBufferedBatches in 1..10_000)
    }
}

data class LocalScannerAgent(
    val id: LocalAgentId,
    val scope: AgentScope,
    val capabilities: Set<AgentCapability>,
    val status: AgentStatus = AgentStatus.UNENROLLED,
    val enrollment: AgentEnrollment? = null,
    val retryPolicy: AgentRetryPolicy = AgentRetryPolicy(),
    val lastSeenEpochMillis: Long? = null,
) {
    init {
        require(capabilities.isNotEmpty()) { "Agent must expose at least one metadata/health capability" }
        require(lastSeenEpochMillis == null || lastSeenEpochMillis >= 0)
        require(status == AgentStatus.UNENROLLED || enrollment != null || status == AgentStatus.DISABLED) {
            "Active/enrolled agent states require secure enrollment"
        }
        require(enrollment == null || enrollment.agentId == id) { "Enrollment must belong to the agent" }
    }
}

data class AgentMetadataFact(
    val key: String,
    val value: String,
    val source: FieldSource = FieldSource.AUTO,
    val observedAtEpochMillis: Long,
) {
    init {
        require(key.isNotBlank())
        require(observedAtEpochMillis >= 0)
        require(source == FieldSource.AUTO) {
            "Local agent observations are discovery facts and cannot impersonate manual/imported data"
        }
    }
}

data class AgentDeviceStatusPayload(
    val agentId: LocalAgentId,
    val centerId: CenterId,
    val networkId: NetworkId,
    val deviceId: DeviceId?,
    val candidateKey: String?,
    val online: Boolean,
    val observedAtEpochMillis: Long,
    val facts: List<AgentMetadataFact> = emptyList(),
) {
    init {
        require(deviceId != null || !candidateKey.isNullOrBlank()) {
            "Payload must reference a canonical Device or a discovery candidate key"
        }
        require(observedAtEpochMillis >= 0)
    }
}

data class AgentBatch(
    val agentId: LocalAgentId,
    val sequence: Long,
    val createdAtEpochMillis: Long,
    val payloads: List<AgentDeviceStatusPayload>,
) {
    init {
        require(sequence >= 0)
        require(createdAtEpochMillis >= 0)
        require(payloads.isNotEmpty())
        require(payloads.all { it.agentId == agentId }) { "Batch payloads must belong to the same agent" }
    }
}

/**
 * Agent is a bridge into the canonical application. It may discover and report metadata/status only.
 * It intentionally exposes no Incident repository, independent Device store, or streaming capability.
 */
interface AgentMetadataSink {
    suspend fun submit(batch: AgentBatch)
}
