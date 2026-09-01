package com.mobinpdalab.networkcentermonitor.data.discovery

import com.mobinpdalab.networkcentermonitor.domain.discovery.DiscoveryProbeAdapter
import com.mobinpdalab.networkcentermonitor.domain.discovery.DiscoveryProbeMethod
import com.mobinpdalab.networkcentermonitor.domain.discovery.DiscoveryRequest
import com.mobinpdalab.networkcentermonitor.domain.discovery.ProbeObservation
import com.mobinpdalab.networkcentermonitor.domain.model.Ipv4Address
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

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

data class HttpProbeResponse(
    val statusCode: Int,
    val serverHeader: String? = null,
) {
    init { require(statusCode in 100..599) }
}

fun interface HttpProbeTransport {
    fun execute(
        scheme: String,
        host: String,
        port: Int,
        timeoutMillis: Int,
    ): HttpProbeResponse?
}

/** Real HTTP transport. Any valid HTTP response proves that an HTTP service answered. */
class UrlConnectionHttpProbeTransport : HttpProbeTransport {
    override fun execute(
        scheme: String,
        host: String,
        port: Int,
        timeoutMillis: Int,
    ): HttpProbeResponse? = runCatching {
        val connection = URL("$scheme://$host:$port/").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "HEAD"
            connection.connectTimeout = timeoutMillis
            connection.readTimeout = timeoutMillis
            connection.instanceFollowRedirects = false
            connection.useCaches = false
            connection.setRequestProperty("Connection", "close")
            val status = connection.responseCode
            if (status !in 100..599) null else HttpProbeResponse(
                statusCode = status,
                serverHeader = connection.getHeaderField("Server")?.takeIf { it.isNotBlank() },
            )
        } finally {
            connection.disconnect()
        }
    }.getOrNull()
}

/**
 * Shared HTTP/HTTPS discovery adapter. It performs low-bandwidth HEAD requests only, never follows
 * redirects, and treats authentication/error responses as positive service detection because the
 * remote HTTP stack responded. Explicit request ports are honored; otherwise 80/443 is used.
 */
class HttpDiscoveryProbeAdapter(
    override val method: DiscoveryProbeMethod,
    private val transport: HttpProbeTransport = UrlConnectionHttpProbeTransport(),
    private val clock: () -> Long = System::currentTimeMillis,
) : DiscoveryProbeAdapter {
    init {
        require(method == DiscoveryProbeMethod.HTTP || method == DiscoveryProbeMethod.HTTPS) {
            "HttpDiscoveryProbeAdapter only supports HTTP or HTTPS"
        }
    }

    override suspend fun probe(target: Ipv4Address, request: DiscoveryRequest): ProbeObservation =
        withContext(Dispatchers.IO) {
            val scheme = if (method == DiscoveryProbeMethod.HTTPS) "https" else "http"
            val defaultPort = if (method == DiscoveryProbeMethod.HTTPS) 443 else 80
            val ports = if (request.ports.isEmpty()) {
                listOf(defaultPort)
            } else {
                request.ports.map { it.value }.sorted()
            }

            val responses = ports.mapNotNull { port ->
                transport.execute(
                    scheme = scheme,
                    host = target.value,
                    port = port,
                    timeoutMillis = request.config.timeoutMillis.toSafeInt(),
                )?.let { port to it }
            }

            val attributes = buildMap {
                if (responses.isNotEmpty()) {
                    put("responsivePorts", responses.joinToString(",") { (port, _) -> port.toString() })
                    put("responses", responses.joinToString(",") { (port, response) -> "$port:${response.statusCode}" })
                    responses.firstNotNullOfOrNull { (_, response) -> response.serverHeader }
                        ?.let { put("server", it) }
                }
            }

            ProbeObservation(
                method = method,
                target = target,
                successful = responses.isNotEmpty(),
                observedAtEpochMillis = clock(),
                attributes = attributes,
            )
        }
}

private fun Long.toSafeInt(): Int = coerceIn(1L, Int.MAX_VALUE.toLong()).toInt()
