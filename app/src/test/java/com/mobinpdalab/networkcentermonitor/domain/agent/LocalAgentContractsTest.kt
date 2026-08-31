package com.mobinpdalab.networkcentermonitor.domain.agent

import com.mobinpdalab.networkcentermonitor.domain.model.CenterId
import com.mobinpdalab.networkcentermonitor.domain.model.DeviceId
import com.mobinpdalab.networkcentermonitor.domain.model.NetworkId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAgentContractsTest {
    private val agentId = LocalAgentId("agent-1")
    private val centerId = CenterId("center-1")
    private val networkId = NetworkId("network-1")

    @Test(expected = IllegalArgumentException::class)
    fun `agent scope must contain a canonical network`() {
        AgentScope(centerId = centerId, networkIds = emptySet())
    }

    @Test
    fun `agent capabilities are metadata and health only`() {
        val capabilityNames = AgentCapability.entries.map { it.name }
        assertFalse(capabilityNames.any { it.contains("STREAM") })
        assertTrue(AgentCapability.ONVIF_DISCOVERY in AgentCapability.entries)
        assertTrue(AgentCapability.PC_INVENTORY in AgentCapability.entries)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `online agent requires secure enrollment`() {
        LocalScannerAgent(
            id = agentId,
            scope = AgentScope(centerId, setOf(networkId)),
            capabilities = setOf(AgentCapability.LAN_DISCOVERY),
            status = AgentStatus.ONLINE,
            enrollment = null,
        )
    }

    @Test
    fun `payload can reference canonical device`() {
        val payload = AgentDeviceStatusPayload(
            agentId = agentId,
            centerId = centerId,
            networkId = networkId,
            deviceId = DeviceId("device-1"),
            candidateKey = null,
            online = true,
            observedAtEpochMillis = 100,
        )

        assertEquals("device-1", payload.deviceId?.value)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `payload without device or candidate key is rejected`() {
        AgentDeviceStatusPayload(
            agentId = agentId,
            centerId = centerId,
            networkId = networkId,
            deviceId = null,
            candidateKey = null,
            online = true,
            observedAtEpochMillis = 100,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `offline buffer is bounded`() {
        AgentRetryPolicy(maxBufferedBatches = 10_001)
    }
}
