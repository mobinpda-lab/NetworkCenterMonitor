package com.mobinpdalab.networkcentermonitor.domain.discovery

import com.mobinpdalab.networkcentermonitor.domain.model.CenterId
import com.mobinpdalab.networkcentermonitor.domain.model.DeviceType
import com.mobinpdalab.networkcentermonitor.domain.model.Ipv4Address
import com.mobinpdalab.networkcentermonitor.domain.model.MacAddress
import com.mobinpdalab.networkcentermonitor.domain.model.NetworkId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoveryModelTest {
    private val centerId = CenterId("center-1")
    private val networkId = NetworkId("network-1")

    @Test
    fun `cidr preview calculates address count`() {
        val request = DiscoveryRequest(
            provinceId = null,
            centerId = centerId,
            networkId = networkId,
            addressScope = DiscoveryAddressScope.Cidr(Ipv4Address("192.168.1.0"), 24),
            methods = setOf(DiscoveryProbeMethod.ICMP, DiscoveryProbeMethod.TCP),
        )

        val preview = request.preview()

        assertEquals(256L, preview.addressCount)
        assertEquals(512L, preview.estimatedProbeCount)
        assertFalse(preview.requiresBoundedExecution)
    }

    @Test
    fun `large ranges require bounded execution`() {
        val request = DiscoveryRequest(
            provinceId = null,
            centerId = centerId,
            networkId = networkId,
            addressScope = DiscoveryAddressScope.Cidr(Ipv4Address("10.0.0.0"), 16),
            methods = setOf(DiscoveryProbeMethod.ICMP),
        )

        assertTrue(request.preview().requiresBoundedExecution)
    }

    @Test
    fun `session supports pause resume and stop`() {
        val request = DiscoveryRequest(
            provinceId = null,
            centerId = centerId,
            networkId = networkId,
            addressScope = DiscoveryAddressScope.Range(
                Ipv4Address("10.0.0.1"),
                Ipv4Address("10.0.0.10"),
            ),
            methods = setOf(DiscoveryProbeMethod.ICMP),
        )
        val created = DiscoverySession(DiscoverySessionId("scan-1"), request)
        val running = created.apply(DiscoveryCommand.START)
        val paused = running.apply(DiscoveryCommand.PAUSE)
        val resumed = paused.apply(DiscoveryCommand.RESUME)
        val stopped = resumed.apply(DiscoveryCommand.STOP)

        assertEquals(DiscoverySessionStatus.STOPPED, stopped.status)
    }

    @Test
    fun `candidates with same MAC are deduplicated`() {
        val mac = MacAddress("AA:BB:CC:DD:EE:FF")
        val first = DiscoveryCandidate(
            id = DiscoveryCandidateId("a"),
            networkId = networkId,
            ipAddresses = listOf(Ipv4Address("10.0.0.2")),
            macAddress = mac,
            detectedType = DeviceType.CAMERA,
            confidence = 0.8,
        )
        val second = DiscoveryCandidate(
            id = DiscoveryCandidateId("b"),
            networkId = networkId,
            ipAddresses = listOf(Ipv4Address("10.0.0.3")),
            macAddress = MacAddress("AA-BB-CC-DD-EE-FF"),
            detectedType = DeviceType.UNKNOWN,
            confidence = 0.4,
        )

        assertEquals(1, deduplicateCandidates(listOf(first, second)).size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid reverse range is rejected`() {
        DiscoveryAddressScope.Range(
            from = Ipv4Address("10.0.0.10"),
            to = Ipv4Address("10.0.0.1"),
        )
    }
}
