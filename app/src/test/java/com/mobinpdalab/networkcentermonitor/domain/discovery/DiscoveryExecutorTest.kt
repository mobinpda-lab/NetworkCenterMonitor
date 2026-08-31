package com.mobinpdalab.networkcentermonitor.domain.discovery

import com.mobinpdalab.networkcentermonitor.domain.model.CenterId
import com.mobinpdalab.networkcentermonitor.domain.model.Ipv4Address
import com.mobinpdalab.networkcentermonitor.domain.model.NetworkId
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class DiscoveryExecutorTest {
    private val centerId = CenterId("center-1")
    private val networkId = NetworkId("network-1")

    @Test
    fun `CIDR enumeration normalizes to network address`() {
        val values = enumerateAddresses(
            DiscoveryAddressScope.Cidr(Ipv4Address("192.168.1.3"), 30),
        ).map { it.value }.toList()

        assertEquals(
            listOf("192.168.1.0", "192.168.1.1", "192.168.1.2", "192.168.1.3"),
            values,
        )
    }

    @Test
    fun `executor keeps active probe work within configured concurrency`() = runBlocking {
        val active = AtomicInteger(0)
        val maximum = AtomicInteger(0)
        val adapter = object : DiscoveryProbeAdapter {
            override val method = DiscoveryProbeMethod.ICMP

            override suspend fun probe(target: Ipv4Address, request: DiscoveryRequest): ProbeObservation {
                val nowActive = active.incrementAndGet()
                maximum.updateAndGet { previous -> maxOf(previous, nowActive) }
                delay(8)
                active.decrementAndGet()
                return ProbeObservation(method, target, true, 1L)
            }
        }
        val request = request(
            from = "10.0.0.1",
            to = "10.0.0.12",
            retryCount = 0,
            maxConcurrency = 3,
        )

        val result = DiscoveryExecutor(listOf(adapter)).execute(
            DiscoverySession(DiscoverySessionId("session-1"), request),
        )

        assertEquals(DiscoverySessionStatus.COMPLETED, result.session.status)
        assertEquals(12L, result.session.progress.scannedAddresses)
        assertEquals(12, result.candidates.size)
        assertTrue("observed concurrency must be bounded", maximum.get() <= 3)
        assertTrue("test should exercise parallel work", maximum.get() >= 2)
    }

    @Test
    fun `executor retries a failed probe before accepting success`() = runBlocking {
        val attempts = AtomicInteger(0)
        val adapter = object : DiscoveryProbeAdapter {
            override val method = DiscoveryProbeMethod.ICMP

            override suspend fun probe(target: Ipv4Address, request: DiscoveryRequest): ProbeObservation {
                val attempt = attempts.incrementAndGet()
                return ProbeObservation(
                    method = method,
                    target = target,
                    successful = attempt >= 3,
                    observedAtEpochMillis = attempt.toLong(),
                )
            }
        }
        val request = request(
            from = "10.0.0.5",
            to = "10.0.0.5",
            retryCount = 2,
            maxConcurrency = 1,
        )

        val result = DiscoveryExecutor(listOf(adapter)).execute(
            DiscoverySession(DiscoverySessionId("session-retry"), request),
        )

        assertEquals(3, attempts.get())
        assertEquals(1, result.candidates.size)
        assertEquals(DiscoverySessionStatus.COMPLETED, result.session.status)
    }

    @Test
    fun `controller stops scan without converting it into failure`() = runBlocking {
        val controller = DiscoveryExecutionController()
        val adapter = object : DiscoveryProbeAdapter {
            override val method = DiscoveryProbeMethod.ICMP
            override suspend fun probe(target: Ipv4Address, request: DiscoveryRequest) =
                ProbeObservation(method, target, true, 1L)
        }
        val request = request(
            from = "10.0.1.1",
            to = "10.0.1.10",
            retryCount = 0,
            maxConcurrency = 1,
        )

        val result = DiscoveryExecutor(listOf(adapter)).execute(
            session = DiscoverySession(DiscoverySessionId("session-stop"), request),
            controller = controller,
            onProgress = { progress ->
                if (progress.scannedAddresses == 3L) controller.stop()
            },
        )

        assertEquals(DiscoverySessionStatus.STOPPED, result.session.status)
        assertEquals(3L, result.session.progress.scannedAddresses)
        assertEquals(3, result.candidates.size)
    }

    private fun request(
        from: String,
        to: String,
        retryCount: Int,
        maxConcurrency: Int,
    ) = DiscoveryRequest(
        provinceId = null,
        centerId = centerId,
        networkId = networkId,
        addressScope = DiscoveryAddressScope.Range(Ipv4Address(from), Ipv4Address(to)),
        methods = setOf(DiscoveryProbeMethod.ICMP),
        config = DiscoveryScanConfig(
            timeoutMillis = 50,
            retryCount = retryCount,
            rateLimitPerSecond = 1_000,
            maxConcurrency = maxConcurrency,
        ),
    )
}
