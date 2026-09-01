package com.mobinpdalab.networkcentermonitor.data.discovery

import com.mobinpdalab.networkcentermonitor.domain.discovery.DiscoveryAddressScope
import com.mobinpdalab.networkcentermonitor.domain.discovery.DiscoveryProbeMethod
import com.mobinpdalab.networkcentermonitor.domain.discovery.DiscoveryRequest
import com.mobinpdalab.networkcentermonitor.domain.discovery.DiscoveryScanConfig
import com.mobinpdalab.networkcentermonitor.domain.model.CenterId
import com.mobinpdalab.networkcentermonitor.domain.model.Ipv4Address
import com.mobinpdalab.networkcentermonitor.domain.model.NetworkId
import com.mobinpdalab.networkcentermonitor.domain.model.NetworkPort
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkDiscoveryProbeAdaptersTest {
    private val target = Ipv4Address("10.10.10.10")

    @Test
    fun `http adapter uses default port and accepts auth response as detected service`() = runBlocking {
        val calls = mutableListOf<String>()
        val adapter = HttpDiscoveryProbeAdapter(
            method = DiscoveryProbeMethod.HTTP,
            transport = HttpProbeTransport { scheme, host, port, timeout ->
                calls += "$scheme|$host|$port|$timeout"
                HttpProbeResponse(401, "camera-http")
            },
            clock = { 123L },
        )

        val observation = adapter.probe(target, request(method = DiscoveryProbeMethod.HTTP))

        assertTrue(observation.successful)
        assertEquals(listOf("http|10.10.10.10|80|250"), calls)
        assertEquals("80", observation.attributes["responsivePorts"])
        assertEquals("80:401", observation.attributes["responses"])
        assertEquals("camera-http", observation.attributes["server"])
        assertEquals(123L, observation.observedAtEpochMillis)
    }

    @Test
    fun `https adapter probes explicit ports deterministically and reports only responders`() = runBlocking {
        val calls = mutableListOf<Int>()
        val adapter = HttpDiscoveryProbeAdapter(
            method = DiscoveryProbeMethod.HTTPS,
            transport = HttpProbeTransport { scheme, _, port, _ ->
                assertEquals("https", scheme)
                calls += port
                if (port == 8443) HttpProbeResponse(200) else null
            },
        )

        val observation = adapter.probe(
            target,
            request(
                method = DiscoveryProbeMethod.HTTPS,
                ports = setOf(NetworkPort(9443), NetworkPort(8443)),
            ),
        )

        assertEquals(listOf(8443, 9443), calls)
        assertTrue(observation.successful)
        assertEquals("8443", observation.attributes["responsivePorts"])
        assertEquals("8443:200", observation.attributes["responses"])
    }

    @Test
    fun `http adapter reports failure when no endpoint returns HTTP`() = runBlocking {
        val adapter = HttpDiscoveryProbeAdapter(
            method = DiscoveryProbeMethod.HTTP,
            transport = HttpProbeTransport { _, _, _, _ -> null },
        )

        val observation = adapter.probe(target, request(method = DiscoveryProbeMethod.HTTP))

        assertFalse(observation.successful)
        assertTrue(observation.attributes.isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `http adapter rejects unrelated discovery methods`() {
        HttpDiscoveryProbeAdapter(DiscoveryProbeMethod.RTSP)
    }

    private fun request(
        method: DiscoveryProbeMethod,
        ports: Set<NetworkPort> = emptySet(),
    ) = DiscoveryRequest(
        provinceId = null,
        centerId = CenterId("center-http"),
        networkId = NetworkId("network-http"),
        addressScope = DiscoveryAddressScope.Range(target, target),
        methods = setOf(method),
        ports = ports,
        config = DiscoveryScanConfig(
            timeoutMillis = 250,
            retryCount = 0,
            rateLimitPerSecond = 100,
            maxConcurrency = 4,
        ),
    )
}
