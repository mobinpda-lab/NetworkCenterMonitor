package com.mobinpdalab.networkcentermonitor.domain.monitoring

import com.mobinpdalab.networkcentermonitor.domain.model.IncidentId
import com.mobinpdalab.networkcentermonitor.domain.model.IpEndpointId
import com.mobinpdalab.networkcentermonitor.domain.model.MonitoringStatus
import com.mobinpdalab.networkcentermonitor.domain.model.ServiceCriticality
import com.mobinpdalab.networkcentermonitor.domain.model.ServiceId

sealed interface MonitoringTarget {
    val endpointId: IpEndpointId

    data class Ping(override val endpointId: IpEndpointId) : MonitoringTarget
    data class Service(
        override val endpointId: IpEndpointId,
        val serviceId: ServiceId,
        val criticality: ServiceCriticality = ServiceCriticality.NORMAL,
    ) : MonitoringTarget
}

data class MonitoringPolicy(
    val timeoutMillis: Long = 2_000,
    val retryCount: Int = 1,
    val retryIntervalMillis: Long = 1_000,
    val outageConfirmationMillis: Long = 60_000,
    val recoveryConfirmationMillis: Long = 5_000,
    val flappingWindowMillis: Long = 5 * 60_000,
    val flappingTransitionThreshold: Int = 4,
) {
    init {
        require(timeoutMillis > 0)
        require(retryCount in 0..10)
        require(retryIntervalMillis >= 0)
        require(outageConfirmationMillis >= 0)
        require(recoveryConfirmationMillis >= 0)
        require(flappingWindowMillis > 0)
        require(flappingTransitionThreshold >= 2)
    }
}

data class MonitoringPolicyOverride(
    val timeoutMillis: Long? = null,
    val retryCount: Int? = null,
    val retryIntervalMillis: Long? = null,
    val outageConfirmationMillis: Long? = null,
    val recoveryConfirmationMillis: Long? = null,
    val flappingWindowMillis: Long? = null,
    val flappingTransitionThreshold: Int? = null,
)

/** Overrides must be supplied from broadest to most specific scope. */
fun resolveMonitoringPolicy(
    global: MonitoringPolicy,
    overrides: List<MonitoringPolicyOverride>,
): MonitoringPolicy = overrides.fold(global) { current, override ->
    MonitoringPolicy(
        timeoutMillis = override.timeoutMillis ?: current.timeoutMillis,
        retryCount = override.retryCount ?: current.retryCount,
        retryIntervalMillis = override.retryIntervalMillis ?: current.retryIntervalMillis,
        outageConfirmationMillis = override.outageConfirmationMillis ?: current.outageConfirmationMillis,
        recoveryConfirmationMillis = override.recoveryConfirmationMillis ?: current.recoveryConfirmationMillis,
        flappingWindowMillis = override.flappingWindowMillis ?: current.flappingWindowMillis,
        flappingTransitionThreshold = override.flappingTransitionThreshold ?: current.flappingTransitionThreshold,
    )
}

data class MaintenanceWindow(
    val startsAtEpochMillis: Long,
    val endsAtEpochMillis: Long,
) {
    init {
        require(startsAtEpochMillis >= 0)
        require(endsAtEpochMillis > startsAtEpochMillis)
    }

    fun contains(epochMillis: Long): Boolean = epochMillis in startsAtEpochMillis until endsAtEpochMillis
}

data class ProbeResult(
    val successful: Boolean,
    val observedAtEpochMillis: Long,
    val attempts: Int = 1,
    val detail: String? = null,
) {
    init {
        require(observedAtEpochMillis >= 0)
        require(attempts > 0)
    }
}

data class ActiveIncident(
    val id: IncidentId,
    val target: MonitoringTarget,
    val startedAtEpochMillis: Long,
    val failureAttemptsAtOpen: Int,
) {
    init {
        require(startedAtEpochMillis >= 0)
        require(failureAttemptsAtOpen > 0)
    }
}

data class MonitoringTargetState(
    val target: MonitoringTarget,
    val status: MonitoringStatus = MonitoringStatus.UNVERIFIED,
    val suspectedSinceEpochMillis: Long? = null,
    val recoverySinceEpochMillis: Long? = null,
    val activeIncident: ActiveIncident? = null,
    val consecutiveFailureAttempts: Int = 0,
    val lastProbeAtEpochMillis: Long? = null,
    val lastSuccessAtEpochMillis: Long? = null,
    val lastStatusChangeEpochMillis: Long? = null,
    val confirmedTransitionsEpochMillis: List<Long> = emptyList(),
    val flapping: Boolean = false,
) {
    init {
        require(consecutiveFailureAttempts >= 0)
        require(activeIncident == null || activeIncident.target == target)
    }
}

sealed interface MonitoringEvent {
    val target: MonitoringTarget
    val occurredAtEpochMillis: Long

    data class Suspected(
        override val target: MonitoringTarget,
        override val occurredAtEpochMillis: Long,
    ) : MonitoringEvent

    data class IncidentOpened(
        override val target: MonitoringTarget,
        override val occurredAtEpochMillis: Long,
        val incident: ActiveIncident,
    ) : MonitoringEvent

    data class IncidentRecovered(
        override val target: MonitoringTarget,
        override val occurredAtEpochMillis: Long,
        val incidentId: IncidentId,
        val durationMillis: Long,
    ) : MonitoringEvent

    data class MaintenanceFailureObserved(
        override val target: MonitoringTarget,
        override val occurredAtEpochMillis: Long,
    ) : MonitoringEvent

    data class FlappingDetected(
        override val target: MonitoringTarget,
        override val occurredAtEpochMillis: Long,
    ) : MonitoringEvent

    data class FlappingCleared(
        override val target: MonitoringTarget,
        override val occurredAtEpochMillis: Long,
    ) : MonitoringEvent
}

data class MonitoringStep(
    val state: MonitoringTargetState,
    val events: List<MonitoringEvent>,
)

/**
 * Pure state machine shared by Ping, service, camera-health and remote-health checks.
 * Persistence/notifications consume emitted events; they do not implement another outage state machine.
 */
class MonitoringIncidentEngine(
    private val incidentIdFactory: (MonitoringTarget, Long) -> IncidentId,
) {
    fun applyProbe(
        previous: MonitoringTargetState,
        result: ProbeResult,
        policy: MonitoringPolicy,
        maintenance: MaintenanceWindow? = null,
    ): MonitoringStep {
        require(previous.lastProbeAtEpochMillis == null || result.observedAtEpochMillis >= previous.lastProbeAtEpochMillis) {
            "Probe results must be applied chronologically"
        }

        val inMaintenance = maintenance?.contains(result.observedAtEpochMillis) == true
        return if (result.successful) {
            applySuccess(previous, result, policy)
        } else {
            applyFailure(previous, result, policy, inMaintenance)
        }
    }

    private fun applyFailure(
        previous: MonitoringTargetState,
        result: ProbeResult,
        policy: MonitoringPolicy,
        inMaintenance: Boolean,
    ): MonitoringStep {
        val failureAttempts = previous.consecutiveFailureAttempts + result.attempts
        if (inMaintenance) {
            val state = previous.copy(
                lastProbeAtEpochMillis = result.observedAtEpochMillis,
                recoverySinceEpochMillis = null,
                consecutiveFailureAttempts = failureAttempts,
            )
            return MonitoringStep(
                state,
                listOf(MonitoringEvent.MaintenanceFailureObserved(previous.target, result.observedAtEpochMillis)),
            )
        }

        if (previous.status == MonitoringStatus.DISCONNECTED) {
            return MonitoringStep(
                previous.copy(
                    lastProbeAtEpochMillis = result.observedAtEpochMillis,
                    recoverySinceEpochMillis = null,
                    consecutiveFailureAttempts = failureAttempts,
                ),
                emptyList(),
            )
        }

        val suspectedSince = previous.suspectedSinceEpochMillis ?: result.observedAtEpochMillis
        val confirmationElapsed = result.observedAtEpochMillis - suspectedSince >= policy.outageConfirmationMillis
        if (!confirmationElapsed) {
            val event = if (previous.status != MonitoringStatus.SUSPECTED) {
                listOf(MonitoringEvent.Suspected(previous.target, result.observedAtEpochMillis))
            } else {
                emptyList()
            }
            return MonitoringStep(
                previous.copy(
                    status = MonitoringStatus.SUSPECTED,
                    suspectedSinceEpochMillis = suspectedSince,
                    recoverySinceEpochMillis = null,
                    consecutiveFailureAttempts = failureAttempts,
                    lastProbeAtEpochMillis = result.observedAtEpochMillis,
                    lastStatusChangeEpochMillis = if (previous.status != MonitoringStatus.SUSPECTED) {
                        result.observedAtEpochMillis
                    } else previous.lastStatusChangeEpochMillis,
                ),
                event,
            )
        }

        val incident = previous.activeIncident ?: ActiveIncident(
            id = incidentIdFactory(previous.target, suspectedSince),
            target = previous.target,
            startedAtEpochMillis = suspectedSince,
            failureAttemptsAtOpen = failureAttempts,
        )
        val transitioned = previous.status != MonitoringStatus.DISCONNECTED
        var state = previous.copy(
            status = MonitoringStatus.DISCONNECTED,
            suspectedSinceEpochMillis = suspectedSince,
            recoverySinceEpochMillis = null,
            activeIncident = incident,
            consecutiveFailureAttempts = failureAttempts,
            lastProbeAtEpochMillis = result.observedAtEpochMillis,
            lastStatusChangeEpochMillis = if (transitioned) result.observedAtEpochMillis else previous.lastStatusChangeEpochMillis,
        )
        val events = mutableListOf<MonitoringEvent>()
        if (previous.activeIncident == null) {
            events += MonitoringEvent.IncidentOpened(previous.target, result.observedAtEpochMillis, incident)
        }
        if (transitioned) {
            val flap = updateFlapping(state, result.observedAtEpochMillis, policy)
            state = flap.state
            events += flap.events
        }
        return MonitoringStep(state, events)
    }

    private fun applySuccess(
        previous: MonitoringTargetState,
        result: ProbeResult,
        policy: MonitoringPolicy,
    ): MonitoringStep {
        if (previous.status == MonitoringStatus.DISCONNECTED && previous.activeIncident != null) {
            val recoverySince = previous.recoverySinceEpochMillis ?: result.observedAtEpochMillis
            if (result.observedAtEpochMillis - recoverySince < policy.recoveryConfirmationMillis) {
                return MonitoringStep(
                    previous.copy(
                        recoverySinceEpochMillis = recoverySince,
                        consecutiveFailureAttempts = 0,
                        lastProbeAtEpochMillis = result.observedAtEpochMillis,
                        lastSuccessAtEpochMillis = result.observedAtEpochMillis,
                    ),
                    emptyList(),
                )
            }

            val incident = previous.activeIncident
            var state = previous.copy(
                status = MonitoringStatus.CONNECTED,
                suspectedSinceEpochMillis = null,
                recoverySinceEpochMillis = null,
                activeIncident = null,
                consecutiveFailureAttempts = 0,
                lastProbeAtEpochMillis = result.observedAtEpochMillis,
                lastSuccessAtEpochMillis = result.observedAtEpochMillis,
                lastStatusChangeEpochMillis = result.observedAtEpochMillis,
            )
            val events = mutableListOf<MonitoringEvent>(
                MonitoringEvent.IncidentRecovered(
                    target = previous.target,
                    occurredAtEpochMillis = result.observedAtEpochMillis,
                    incidentId = incident.id,
                    durationMillis = result.observedAtEpochMillis - incident.startedAtEpochMillis,
                ),
            )
            val flap = updateFlapping(state, result.observedAtEpochMillis, policy)
            state = flap.state
            events += flap.events
            return MonitoringStep(state, events)
        }

        val transitioned = previous.status != MonitoringStatus.CONNECTED
        var state = previous.copy(
            status = MonitoringStatus.CONNECTED,
            suspectedSinceEpochMillis = null,
            recoverySinceEpochMillis = null,
            consecutiveFailureAttempts = 0,
            lastProbeAtEpochMillis = result.observedAtEpochMillis,
            lastSuccessAtEpochMillis = result.observedAtEpochMillis,
            lastStatusChangeEpochMillis = if (transitioned) result.observedAtEpochMillis else previous.lastStatusChangeEpochMillis,
        )
        val events = mutableListOf<MonitoringEvent>()
        if (transitioned && previous.status != MonitoringStatus.SUSPECTED && previous.status != MonitoringStatus.UNVERIFIED) {
            val flap = updateFlapping(state, result.observedAtEpochMillis, policy)
            state = flap.state
            events += flap.events
        } else {
            val cleared = clearExpiredFlapping(state, result.observedAtEpochMillis, policy)
            state = cleared.state
            events += cleared.events
        }
        return MonitoringStep(state, events)
    }

    private fun updateFlapping(
        state: MonitoringTargetState,
        transitionAt: Long,
        policy: MonitoringPolicy,
    ): MonitoringStep {
        val minimum = transitionAt - policy.flappingWindowMillis
        val transitions = (state.confirmedTransitionsEpochMillis + transitionAt).filter { it >= minimum }
        val nowFlapping = transitions.size >= policy.flappingTransitionThreshold
        val events = if (!state.flapping && nowFlapping) {
            listOf(MonitoringEvent.FlappingDetected(state.target, transitionAt))
        } else {
            emptyList()
        }
        return MonitoringStep(
            state.copy(confirmedTransitionsEpochMillis = transitions, flapping = nowFlapping),
            events,
        )
    }

    private fun clearExpiredFlapping(
        state: MonitoringTargetState,
        now: Long,
        policy: MonitoringPolicy,
    ): MonitoringStep {
        val minimum = now - policy.flappingWindowMillis
        val transitions = state.confirmedTransitionsEpochMillis.filter { it >= minimum }
        val stillFlapping = transitions.size >= policy.flappingTransitionThreshold
        val events = if (state.flapping && !stillFlapping) {
            listOf(MonitoringEvent.FlappingCleared(state.target, now))
        } else {
            emptyList()
        }
        return MonitoringStep(
            state.copy(confirmedTransitionsEpochMillis = transitions, flapping = stillFlapping),
            events,
        )
    }
}

interface MonitoringStateRepository {
    suspend fun load(target: MonitoringTarget): MonitoringTargetState?
    suspend fun save(state: MonitoringTargetState)
    suspend fun append(events: List<MonitoringEvent>)
}

class PersistentMonitoringProcessor(
    private val repository: MonitoringStateRepository,
    private val engine: MonitoringIncidentEngine,
) {
    suspend fun process(
        target: MonitoringTarget,
        result: ProbeResult,
        policy: MonitoringPolicy,
        maintenance: MaintenanceWindow? = null,
    ): MonitoringStep {
        val previous = repository.load(target) ?: MonitoringTargetState(target)
        val step = engine.applyProbe(previous, result, policy, maintenance)
        repository.save(step.state)
        if (step.events.isNotEmpty()) repository.append(step.events)
        return step
    }
}
