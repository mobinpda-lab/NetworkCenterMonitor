package com.mobinpdalab.networkcentermonitor.domain.discovery

import com.mobinpdalab.networkcentermonitor.domain.model.DeviceType
import com.mobinpdalab.networkcentermonitor.domain.model.Ipv4Address
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Runtime controller for one discovery execution.
 * It intentionally owns no persistence; durable session state belongs to the canonical repository layer.
 */
class DiscoveryExecutionController {
    private val state = MutableStateFlow(DiscoveryExecutionControl.RUNNING)

    fun pause() {
        if (state.value == DiscoveryExecutionControl.RUNNING) {
            state.value = DiscoveryExecutionControl.PAUSED
        }
    }

    fun resume() {
        if (state.value == DiscoveryExecutionControl.PAUSED) {
            state.value = DiscoveryExecutionControl.RUNNING
        }
    }

    fun stop() {
        state.value = DiscoveryExecutionControl.STOPPED
    }

    val isStopped: Boolean get() = state.value == DiscoveryExecutionControl.STOPPED

    suspend fun awaitRunnable(): Boolean {
        if (state.value == DiscoveryExecutionControl.PAUSED) {
            state.filter { it != DiscoveryExecutionControl.PAUSED }.first()
        }
        return state.value != DiscoveryExecutionControl.STOPPED
    }
}

enum class DiscoveryExecutionControl {
    RUNNING,
    PAUSED,
    STOPPED,
}

data class DiscoveryExecutionResult(
    val session: DiscoverySession,
    val candidates: List<DiscoveryCandidate>,
    val observations: List<ProbeObservation>,
    val errorMessage: String? = null,
)

/**
 * Executes the one shared Discovery Engine with bounded workers.
 * Camera, PC and vendor-specific discovery participate only through [DiscoveryProbeAdapter].
 */
class DiscoveryExecutor(
    adapters: Collection<DiscoveryProbeAdapter>,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val adaptersByMethod = adapters.associateBy { it.method }

    init {
        require(adaptersByMethod.size == adapters.size) {
            "Only one adapter may be registered for each discovery method"
        }
    }

    suspend fun execute(
        session: DiscoverySession,
        controller: DiscoveryExecutionController = DiscoveryExecutionController(),
        onProgress: suspend (DiscoveryProgress) -> Unit = {},
    ): DiscoveryExecutionResult {
        require(session.status == DiscoverySessionStatus.CREATED || session.status == DiscoverySessionStatus.RUNNING) {
            "Discovery execution can start only from CREATED or RUNNING"
        }

        val runningSession = if (session.status == DiscoverySessionStatus.CREATED) {
            session.apply(DiscoveryCommand.START)
        } else {
            session
        }

        val request = runningSession.request
        val selectedAdapters = request.methods.mapNotNull(adaptersByMethod::get)
        require(selectedAdapters.isNotEmpty()) {
            "No registered discovery adapter matches the requested methods"
        }

        val total = request.addressScope.estimatedAddressCount
        val scanned = AtomicLong(0)
        val candidateCount = AtomicInteger(0)
        val observationLock = Mutex()
        val observations = mutableListOf<ProbeObservation>()
        val candidates = mutableListOf<DiscoveryCandidate>()
        val candidateLock = Mutex()
        val limiter = ProbeRateLimiter(request.config.rateLimitPerSecond)

        return try {
            coroutineScope {
                val queue = Channel<Ipv4Address>(capacity = request.config.maxConcurrency * 2)
                val producer = launch(dispatcher) {
                    for (address in enumerateAddresses(request.addressScope)) {
                        if (!controller.awaitRunnable()) break
                        queue.send(address)
                    }
                    queue.close()
                }

                val workers = List(request.config.maxConcurrency) {
                    launch(dispatcher) {
                        for (target in queue) {
                            if (!controller.awaitRunnable()) break

                            val targetObservations = mutableListOf<ProbeObservation>()
                            for (adapter in selectedAdapters) {
                                if (!controller.awaitRunnable()) break
                                limiter.acquire()
                                targetObservations += runWithRetry(adapter, target, request)
                            }

                            observationLock.withLock {
                                observations += targetObservations
                            }

                            if (targetObservations.any { it.successful }) {
                                val candidate = DiscoveryCandidate(
                                    id = DiscoveryCandidateId("${request.networkId.value}:${target.value}"),
                                    networkId = request.networkId,
                                    ipAddresses = listOf(target),
                                    detectedType = DeviceType.UNKNOWN,
                                    confidence = 0.0,
                                )
                                candidateLock.withLock {
                                    candidates += candidate
                                }
                                candidateCount.incrementAndGet()
                            }

                            val progress = DiscoveryProgress(
                                totalAddresses = total,
                                scannedAddresses = scanned.incrementAndGet().coerceAtMost(total),
                                candidatesFound = candidateCount.get(),
                            )
                            onProgress(progress)
                        }
                    }
                }

                producer.join()
                workers.joinAll()
            }

            val finalProgress = DiscoveryProgress(
                totalAddresses = total,
                scannedAddresses = scanned.get().coerceAtMost(total),
                candidatesFound = candidateCount.get(),
            )
            val finalStatus = if (controller.isStopped) {
                DiscoverySessionStatus.STOPPED
            } else {
                DiscoverySessionStatus.COMPLETED
            }
            DiscoveryExecutionResult(
                session = runningSession.copy(status = finalStatus, progress = finalProgress),
                candidates = deduplicateCandidates(candidates),
                observations = observations.toList(),
            )
        } catch (error: Throwable) {
            DiscoveryExecutionResult(
                session = runningSession.copy(
                    status = DiscoverySessionStatus.FAILED,
                    progress = DiscoveryProgress(
                        totalAddresses = total,
                        scannedAddresses = scanned.get().coerceAtMost(total),
                        candidatesFound = candidateCount.get(),
                    ),
                ),
                candidates = deduplicateCandidates(candidates),
                observations = observations.toList(),
                errorMessage = error.message ?: error::class.java.simpleName,
            )
        }
    }

    private suspend fun runWithRetry(
        adapter: DiscoveryProbeAdapter,
        target: Ipv4Address,
        request: DiscoveryRequest,
    ): ProbeObservation {
        var lastFailure: ProbeObservation? = null
        repeat(request.config.retryCount + 1) {
            val observation = try {
                adapter.probe(target, request)
            } catch (error: Throwable) {
                ProbeObservation(
                    method = adapter.method,
                    target = target,
                    successful = false,
                    observedAtEpochMillis = clock(),
                    attributes = mapOf("error" to (error.message ?: error::class.java.simpleName)),
                )
            }
            if (observation.successful) return observation
            lastFailure = observation
        }
        return requireNotNull(lastFailure)
    }
}

private class ProbeRateLimiter(rateLimitPerSecond: Int) {
    private val minimumSpacingMillis = (1_000L / rateLimitPerSecond).coerceAtLeast(1L)
    private val lock = Mutex()
    private var lastPermitAtMillis = 0L

    suspend fun acquire() {
        lock.withLock {
            val now = System.currentTimeMillis()
            val waitMillis = (lastPermitAtMillis + minimumSpacingMillis - now).coerceAtLeast(0L)
            if (waitMillis > 0) delay(waitMillis)
            lastPermitAtMillis = System.currentTimeMillis()
        }
    }
}

internal fun enumerateAddresses(scope: DiscoveryAddressScope): Sequence<Ipv4Address> = sequence {
    val start: Long
    val count: Long
    when (scope) {
        is DiscoveryAddressScope.Cidr -> {
            val raw = ipv4ToLong(scope.networkAddress)
            val hostBits = 32 - scope.prefixLength
            val mask = if (scope.prefixLength == 0) 0L else (0xFFFF_FFFFL shl hostBits) and 0xFFFF_FFFFL
            start = raw and mask
            count = scope.estimatedAddressCount
        }
        is DiscoveryAddressScope.Range -> {
            start = ipv4ToLong(scope.from)
            count = scope.estimatedAddressCount
        }
    }

    var offset = 0L
    while (offset < count) {
        yield(longToIpv4(start + offset))
        offset++
    }
}

private fun ipv4ToLong(address: Ipv4Address): Long = address.value
    .split('.')
    .fold(0L) { acc, part -> (acc shl 8) or part.toLong() }

private fun longToIpv4(value: Long): Ipv4Address {
    val normalized = value and 0xFFFF_FFFFL
    return Ipv4Address(
        listOf(
            (normalized shr 24) and 0xFF,
            (normalized shr 16) and 0xFF,
            (normalized shr 8) and 0xFF,
            normalized and 0xFF,
        ).joinToString("."),
    )
}
