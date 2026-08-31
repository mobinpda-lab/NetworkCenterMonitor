package com.mobinpdalab.networkcentermonitor.domain.monitoring

import com.mobinpdalab.networkcentermonitor.domain.model.IncidentId
import com.mobinpdalab.networkcentermonitor.domain.model.IpEndpointId
import com.mobinpdalab.networkcentermonitor.domain.model.MonitoringStatus
import com.mobinpdalab.networkcentermonitor.domain.model.ServiceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MonitoringEngineTest {
    private val target = MonitoringTarget.Ping(IpEndpointId("ip-1"))
    private val engine = MonitoringIncidentEngine { _, started -> IncidentId("incident-$started") }

    @Test
    fun firstFailureIsSuspectedAndDoesNotOpenIncident() {
        val step = engine.applyProbe(
            previous = MonitoringTargetState(target),
            result = ProbeResult(successful = false, observedAtEpochMillis = 1_000),
            policy = MonitoringPolicy(outageConfirmationMillis = 60_000),
        )

        assertEquals(MonitoringStatus.SUSPECTED, step.state.status)
        assertNull(step.state.activeIncident)
        assertTrue(step.events.single() is MonitoringEvent.Suspected)
    }

    @Test
    fun confirmedFailureOpensExactlyOneIncident() {
        val policy = MonitoringPolicy(outageConfirmationMillis = 60_000)
        val first = engine.applyProbe(
            MonitoringTargetState(target),
            ProbeResult(false, 1_000),
            policy,
        )
        val confirmed = engine.applyProbe(
            first.state,
            ProbeResult(false, 61_000, attempts = 2),
            policy,
        )
        val repeated = engine.applyProbe(
            confirmed.state,
            ProbeResult(false, 62_000),
            policy,
        )

        assertEquals(MonitoringStatus.DISCONNECTED, confirmed.state.status)
        assertNotNull(confirmed.state.activeIncident)
        assertEquals(1, confirmed.events.count { it is MonitoringEvent.IncidentOpened })
        assertTrue(repeated.events.none { it is MonitoringEvent.IncidentOpened })
        assertEquals(confirmed.state.activeIncident?.id, repeated.state.activeIncident?.id)
    }

    @Test
    fun recoveryClosesSameIncidentOnlyAfterConfirmation() {
        val policy = MonitoringPolicy(
            outageConfirmationMillis = 0,
            recoveryConfirmationMillis = 5_000,
        )
        val down = engine.applyProbe(
            MonitoringTargetState(target),
            ProbeResult(false, 10_000),
            policy,
        )
        val firstSuccess = engine.applyProbe(
            down.state,
            ProbeResult(true, 20_000),
            policy,
        )
        val recovered = engine.applyProbe(
            firstSuccess.state,
            ProbeResult(true, 25_000),
            policy,
        )

        assertEquals(MonitoringStatus.DISCONNECTED, firstSuccess.state.status)
        assertNotNull(firstSuccess.state.activeIncident)
        assertEquals(MonitoringStatus.CONNECTED, recovered.state.status)
        assertNull(recovered.state.activeIncident)
        val event = recovered.events.filterIsInstance<MonitoringEvent.IncidentRecovered>().single()
        assertEquals(down.state.activeIncident?.id, event.incidentId)
        assertEquals(15_000, event.durationMillis)
    }

    @Test
    fun maintenanceFailureDoesNotOpenNormalIncident() {
        val maintenance = MaintenanceWindow(5_000, 20_000)
        val step = engine.applyProbe(
            MonitoringTargetState(target, status = MonitoringStatus.CONNECTED),
            ProbeResult(false, 10_000),
            MonitoringPolicy(outageConfirmationMillis = 0),
            maintenance,
        )

        assertEquals(MonitoringStatus.CONNECTED, step.state.status)
        assertNull(step.state.activeIncident)
        assertTrue(step.events.single() is MonitoringEvent.MaintenanceFailureObserved)
    }

    @Test
    fun pingAndServiceTargetsKeepIndependentState() {
        val serviceTarget = MonitoringTarget.Service(
            endpointId = IpEndpointId("ip-1"),
            serviceId = ServiceId("https"),
        )
        val policy = MonitoringPolicy(outageConfirmationMillis = 0)

        val ping = engine.applyProbe(
            MonitoringTargetState(target),
            ProbeResult(true, 1_000),
            policy,
        )
        val service = engine.applyProbe(
            MonitoringTargetState(serviceTarget),
            ProbeResult(false, 1_000),
            policy,
        )

        assertEquals(MonitoringStatus.CONNECTED, ping.state.status)
        assertEquals(MonitoringStatus.DISCONNECTED, service.state.status)
    }

    @Test
    fun policyOverridesApplyFromBroadToSpecific() {
        val resolved = resolveMonitoringPolicy(
            global = MonitoringPolicy(timeoutMillis = 2_000, retryCount = 1),
            overrides = listOf(
                MonitoringPolicyOverride(timeoutMillis = 3_000),
                MonitoringPolicyOverride(retryCount = 4),
                MonitoringPolicyOverride(timeoutMillis = 900),
            ),
        )

        assertEquals(900, resolved.timeoutMillis)
        assertEquals(4, resolved.retryCount)
    }

    @Test
    fun flappingIsDetectedAfterConfiguredConfirmedTransitions() {
        val policy = MonitoringPolicy(
            outageConfirmationMillis = 0,
            recoveryConfirmationMillis = 0,
            flappingWindowMillis = 60_000,
            flappingTransitionThreshold = 4,
        )
        var state = MonitoringTargetState(target, status = MonitoringStatus.CONNECTED)
        val emitted = mutableListOf<MonitoringEvent>()

        listOf(
            ProbeResult(false, 10_000),
            ProbeResult(true, 20_000),
            ProbeResult(false, 30_000),
            ProbeResult(true, 40_000),
        ).forEach { result ->
            val step = engine.applyProbe(state, result, policy)
            state = step.state
            emitted += step.events
        }

        assertTrue(state.flapping)
        assertEquals(1, emitted.count { it is MonitoringEvent.FlappingDetected })
    }

    @Test
    fun expiredFlappingClearsWithoutCreatingNewIncident() {
        val policy = MonitoringPolicy(
            outageConfirmationMillis = 0,
            recoveryConfirmationMillis = 0,
            flappingWindowMillis = 10_000,
            flappingTransitionThreshold = 2,
        )
        val flappingState = MonitoringTargetState(
            target = target,
            status = MonitoringStatus.CONNECTED,
            confirmedTransitionsEpochMillis = listOf(1_000, 2_000),
            flapping = true,
        )

        val step = engine.applyProbe(
            flappingState,
            ProbeResult(true, 20_000),
            policy,
        )

        assertFalse(step.state.flapping)
        assertTrue(step.events.single() is MonitoringEvent.FlappingCleared)
    }
}
