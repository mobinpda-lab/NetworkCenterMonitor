package com.mobinpdalab.networkcentermonitor.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkDomainTest {
    @Test
    fun `valid IPv4 addresses are accepted`() {
        assertEquals("192.168.1.25", Ipv4Address("192.168.1.25").value)
        assertEquals("0.0.0.0", Ipv4Address("0.0.0.0").value)
        assertEquals("255.255.255.255", Ipv4Address("255.255.255.255").value)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `IPv4 octet above 255 is rejected`() {
        Ipv4Address("192.168.1.256")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `IPv4 with missing octet is rejected`() {
        Ipv4Address("192.168.1")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `IPv4 with ambiguous leading zero is rejected`() {
        Ipv4Address("192.168.001.1")
    }

    @Test
    fun `ICMP probe has no port while TCP and UDP require validated port`() {
        val icmp: ServiceProbe = ServiceProbe.Icmp
        val tcp = ServiceProbe.Tcp(NetworkPort(443))
        val udp = ServiceProbe.Udp(NetworkPort(53))

        assertTrue(icmp is ServiceProbe.Icmp)
        assertEquals(443, tcp.port.value)
        assertEquals(53, udp.port.value)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `zero port is rejected`() {
        NetworkPort(0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `port above 65535 is rejected`() {
        NetworkPort(65536)
    }

    @Test
    fun `incident lifecycle is deterministic`() {
        val open = OutageIncident(
            id = IncidentId("incident-1"),
            endpointId = IpEndpointId("ip-1"),
            startedAtEpochMillis = 1_000,
        )
        assertTrue(open.isOpen)
        assertNull(open.durationMillis)

        val recovered = open.copy(recoveredAtEpochMillis = 4_500)
        assertFalse(recovered.isOpen)
        assertEquals(3_500L, recovered.durationMillis)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `incident cannot recover before it started`() {
        OutageIncident(
            id = IncidentId("incident-2"),
            endpointId = IpEndpointId("ip-1"),
            startedAtEpochMillis = 5_000,
            recoveredAtEpochMillis = 4_999,
        )
    }
}
