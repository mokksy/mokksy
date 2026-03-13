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
 * Thread-safe, multiplatform stub registry.
 *
 * The stub list is kept sorted by (priority, creationOrder) at insertion time so that
 * [getAll] returns stubs in a stable, human-readable order without an additional sort.
 *
 * [add] is lock-free via an [AtomicRef.update] CAS loop and can be called from any context.
 * Removal inside [findMatchingStub] is serialised by [mutex] (coroutine-only path).
 * The snapshot read in Phase 1 of [findMatchingStub] is lock-free; [AtomicRef] provides
 * the necessary visibility guarantees.
 *
 * @author Konstantin Pavlov
 */
internal class StubRegistry {
    // Written lock-free by add() and under mutex by findMatchingStub's removal step.
    // AtomicRef provides the visibility guarantees needed for the lock-free Phase 1 snapshot read.
    private val stubs: AtomicRef<PersistentList<Stub<*, *>>> = atomic(persistentListOf())

    // Used only for the brief claim-and-remove step inside findMatchingStub.
    private val mutex = Mutex()

    /**
     * Adds a stub to the registry in sorted order.
     *
     * Lock-free: uses a CAS retry loop. The duplicate check is performed on the snapshot
     * that wins the CAS, so it is always consistent with the list that was actually written.
     *
     * @throws IllegalArgumentException if the same stub instance is already registered.
     */
    fun add(stub: Stub<*, *>) {
        stubs.update { current ->
            val index = current.binarySearch(stub, StubComparator)
            require(index < 0) { "Duplicate stub detected: ${stub.toLogString()}" }
            current.add(-(index + 1), stub)
        }
    }

    /**
     * Finds the best matching stub for [request] using a two-phase approach:
     *
     * **Phase 1 (lock-free):** Snapshot the current stub list and evaluate all matchers,
     * including any suspend I/O (body reads). Concurrent requests run in parallel here.
     *
     * **Phase 2 (locked):** Atomically claim the winner via [Stub.claimMatch].
     * If another coroutine already claimed it (lost CAS on an [StubConfiguration.eventuallyRemove]
     * stub), retry from Phase 1 with a fresh snapshot.
     *
     * @return The matched stub, or `null` if no stub matched.
     */
    suspend fun findMatchingStub(
        request: RoutingRequest,
        verbose: Boolean,
        logger: Logger,
        formatter: HttpFormatter,
    ): Stub<*, *>? {
        val formattedRequest = formatRequest(request, verbose, formatter)

        // Retries only when an eventuallyRemove stub is claimed by a concurrent coroutine
        // between Phase 1 evaluation and the Phase 2 claim — an extremely rare edge case.
        while (true) {
            // Phase 1: snapshot + evaluate outside the lock (suspend I/O runs freely).
            val candidate =
                evaluate(stubs.value, request, verbose, logger, formattedRequest)
                    ?: return null

            // Phase 2: claim the winner (lock held only for this brief step).
            if (candidate.configuration.eventuallyRemove) {
                // CAS inside the lock: only one coroutine wins; losers retry Phase 1.
                val claimed =
                    mutex.withLock {
                        candidate.claimMatch().also { won ->
                            if (won) {
                                stubs.update { it.remove(candidate) }
                                if (verbose) {
                                    logger.debug(
                                        "Removed used stub: ${candidate.toLogString()}",
                                    )
                                }
                            }
                        }
                    }
                if (!claimed) continue
            } else {
                candidate.claimMatch()
            }

            return candidate
        }
    }

    /**
     * Returns a snapshot of all registered stubs.
     */
    fun getAll(): List<Stub<*, *>> = stubs.value

    // region Private helpers

    @Suppress("TooGenericExceptionCaught")
    private suspend fun formatRequest(
        request: RoutingRequest,
        verbose: Boolean,
        formatter: HttpFormatter,
    ): String {
        if (!verbose) return ""
        return try {
            formatter.formatRequest(request)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            "<Unable to format request: ${e.message}>"
        }
    }

    private suspend fun evaluate(
        snapshot: List<Stub<*, *>>,
        request: RoutingRequest,
        verbose: Boolean,
        logger: Logger,
        formattedRequest: String,
    ): Stub<*, *>? {
        val results =
            snapshot
                .filter { !it.configuration.eventuallyRemove || !it.hasBeenMatched() }
                .map { stub -> stub to stub.requestSpecification.matches(request) }

        if (verbose) {
            results.forEach { (stub, result) ->
                result.exceptionOrNull()?.let { ex ->
                    logger.warn(
                        "Failed to evaluate condition for stub: ${stub.toLogString()}. " +
                            "Request: $formattedRequest",
                        ex,
                    )
                }
            }
        }

        val evaluated =
            results.mapNotNull { (stub, result) -> result.getOrNull()?.let { stub to it } }
        val fullMatches = evaluated.filter { (_, mr) -> mr.matched }

        if (fullMatches.isNotEmpty()) {
            return fullMatches
                .sortedWith(
                    compareByDescending<Pair<Stub<*, *>, MatchResult>> { (_, r) -> r.score }
                        .thenBy { (s, _) -> s.requestSpecification.priority }
                        .thenBy { (s, _) -> s.creationOrder },
                ).first()
                .first
        }

        if (verbose) {
            evaluated.maxByOrNull { (_, r) -> r.score }?.let { (stub, mr) ->
                logger.warn(
                    "No stub matched request. Closest stub: ${stub.toLogString()}\n" +
                        "Failed matchers: ${mr.failedMatchers.joinToString()}",
                )
            }
        }
        return null
    }

    // endregion
}
