package dev.mokksy.mokksy

import dev.mokksy.mokksy.request.MatchResult
import dev.mokksy.mokksy.request.matches
import dev.mokksy.mokksy.utils.logger.HttpFormatter
import io.ktor.server.routing.RoutingRequest
import io.ktor.util.logging.Logger
import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe, multiplatform stub registry with atomic operations.
 *
 * Uses kotlinx.atomicfu for lock-free operations where possible,
 * and kotlinx.coroutines Mutex for complex multistep operations that may suspend.
 * @author Konstantin Pavlov
 */
internal class StubRegistry {
    // Atomic reference to an immutable sorted list
    private val stubs: AtomicRef<PersistentList<Stub<*, *>>> = atomic(persistentListOf())

    // Lock for operations requiring atomicity across multiple steps
    private val mutex = Mutex()

    /**
     * Atomically adds a stub to the registry.
     *
     * Uses lock-free update operation that retries until successful.
     *
     * @throws IllegalArgumentException if stub is already registered
     */
    fun add(stub: Stub<*, *>) {
        stubs.update { currentList ->
            val index = currentList.binarySearch(stub, StubComparator)
            require(index < 0) { "Duplicate stub detected: ${stub.toLogString()}" }
            val insertionIndex = -(index + 1)
            currentList.add(insertionIndex, stub)
        }
    }

    /**
     * Atomically finds and optionally removes the best matching stub.
     *
     * This operation is atomic to prevent TOCTOU race conditions:
     * - Match and remove happen in a single critical section
     * - Match count is incremented atomically
     * - No other thread can interfere between match and remove
     *
     * @param request The incoming HTTP request to match
     * @return The matched stub, or null if no match found
     */
    @Suppress("LongMethod")
    suspend fun findMatchingStub(
        request: RoutingRequest,
        verbose: Boolean,
        logger: Logger,
        formatter: HttpFormatter,
    ): Stub<*, *>? {
        val formattedRequest =
            if (verbose) {
                @Suppress("TooGenericExceptionCaught")
                try {
                    formatter.formatRequest(request)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    "<Unable to format request: ${e.message}>"
                }
            } else {
                ""
            }

        return mutex.withLock {
            val currentSnapshot = stubs.value

            // Evaluate every stub — no short-circuit — so we can pick the most specific match.
            val results: List<Pair<Stub<*, *>, Result<MatchResult>>> =
                currentSnapshot.map { stub ->
                    stub to stub.requestSpecification.matches(request)
                }

            if (verbose) {
                results.forEach { (stub, result) ->
                    result.exceptionOrNull()?.let { exception ->
                        logger.warn(
                            "Failed to evaluate condition for stub: ${stub.toLogString()}. " +
                                "Request: $formattedRequest",
                            exception,
                        )
                    }
                }
            }

            val evaluated: List<Pair<Stub<*, *>, MatchResult>> =
                results.mapNotNull { (stub, result) -> result.getOrNull()?.let { stub to it } }

            // Phase 1: collect all full matches, pick most specific.
            val fullMatches = evaluated.filter { (_, mr) -> mr.matched }

            val match: Stub<*, *>? =
                if (fullMatches.isNotEmpty()) {
                    fullMatches
                        .sortedWith(
                            compareByDescending<Pair<Stub<*, *>, MatchResult>> { (_, r) -> r.score }
                                .thenBy { (s, _) -> s.requestSpecification.priority }
                                .thenBy { (s, _) -> s.creationOrder },
                        ).first()
                        .first
                } else {
                    // Phase 2: nothing matched — log closest stub to aid debugging.
                    val closest = evaluated.maxByOrNull { (_, r) -> r.score }
                    if (verbose && closest != null) {
                        logger.warn(
                            "No stub matched request. " +
                                "Closest stub: ${closest.first.toLogString()}\n" +
                                "Failed matchers: ${closest.second.failedMatchers.joinToString()}",
                        )
                    }
                    null
                }

            if (match == null) return@withLock null

            match.incrementMatchCount()

            if (match.configuration.removeAfterMatch) {
                stubs.update { it.remove(match) }
                if (verbose) {
                    logger.debug("Removed used stub: ${match.toLogString()}")
                }
            }

            match
        }
    }

    /**
     * Atomically removes a specific stub.
     *
     * @return true if stub was removed, false if it wasn't present
     */
    fun remove(stub: Stub<*, *>): Boolean {
        while (true) {
            val current = stubs.value
            val idx = current.binarySearch(stub, StubComparator)
            if (idx < 0) return false
            if (stubs.compareAndSet(current, current.removeAt(idx))) return true
        }
    }

    /**
     * Returns a snapshot of all registered stubs.
     *
     * This is a consistent snapshot at a point in time.
     */
    fun getAll(): List<Stub<*, *>> = stubs.value
}
