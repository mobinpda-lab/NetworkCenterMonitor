package com.mobinpdalab.networkcentermonitor.data.discovery

import com.mobinpdalab.networkcentermonitor.domain.discovery.DiscoveryProbeAdapter
import com.mobinpdalab.networkcentermonitor.domain.discovery.DiscoveryProbeMethod
import com.mobinpdalab.networkcentermonitor.domain.discovery.DiscoveryRequest
import com.mobinpdalab.networkcentermonitor.domain.discovery.ProbeObservation
import com.mobinpdalab.networkcentermonitor.domain.model.Ipv4Address
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Low-cost reachability adapter used by the shared Discovery Engine.
 * `InetAddress.isReachable` is OS-dependent and may use ICMP echo or another reachability mechanism.
 * Monitoring-specific Ping can later provide a platform adapter without changing the Discovery domain contract.
 */
class IcmpReachabilityProbeAdapter(
    private val clock: () -> Long = System::currentTimeMillis,
) : DiscoveryProbeAdapter {
    override val method: DiscoveryProbeMethod = DiscoveryProbeMethod.ICMP

    override suspend fun probe(target: Ipv4Address, request: DiscoveryRequest): ProbeObservation =
        withContext(Dispatchers.IO) {
            val reachable = runCatching {
                InetAddress.getByName(target.value).isReachable(request.config.timeoutMillis.toSafeInt())
            }.getOrDefault(false)

            ProbeObservation(
                method = method,
                target = target,
                successful = reachable,
                observedAtEpochMillis = clock(),
                attributes = mapOf("probe" to "inet-reachability"),
            )
        }
}

/**
 * Scans only the explicitly requested TCP ports. Auto-port discovery is intentionally delegated to
 * higher-level fingerprint adapters so this adapter stays predictable and bounded.
 */
class TcpDiscoveryProbeAdapter(
    private val socketFactory: () -> Socket = ::Socket,
    private val clock: () -> Long = System::currentTimeMillis,
) : DiscoveryProbeAdapter {
    override val method: DiscoveryProbeMethod = DiscoveryProbeMethod.TCP

    override suspend fun probe(target: Ipv4Address, request: DiscoveryRequest): ProbeObservation =
        withContext(Dispatchers.IO) {
            if (request.ports.isEmpty()) {
                return@withContext ProbeObservation(
                    method = method,
                    target = target,
                    successful = false,
                    observedAtEpochMillis = clock(),
                    attributes = mapOf("reason" to "no-explicit-tcp-port"),
                )
            }

            val openPorts = request.ports
                .sortedBy { it.value }
                .filter { port ->
                    runCatching {
                        socketFactory().use { socket ->
                            socket.connect(
                                InetSocketAddress(target.value, port.value),
                                request.config.timeoutMillis.toSafeInt(),
                            )
                        }
                        true
                    }.getOrDefault(false)
                }

            ProbeObservation(
                method = method,
                target = target,
                successful = openPorts.isNotEmpty(),
                observedAtEpochMillis = clock(),
                attributes = if (openPorts.isEmpty()) {
                    emptyMap()
                } else {
                    mapOf("openPorts" to openPorts.joinToString(",") { it.value.toString() })
                },
            )
        }
}

private fun Long.toSafeInt(): Int = coerceIn(1L, Int.MAX_VALUE.toLong()).toInt()
