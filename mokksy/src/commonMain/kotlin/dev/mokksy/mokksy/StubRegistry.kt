@file:OptIn(InternalMokksyApi::class)

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
            val name = stub.configuration.name
            if (!name.isNullOrBlank()) {
                require(current.none { it.configuration.name == name }) {
                    "A stub with name '$name' is already registered: ${stub.toLogString()}"
                }
            }
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
     * @return [StubLookupResult.Matched] with the matched stub, or
     * [StubLookupResult.NotMatched] with per-stub evaluation data when no stub matched.
     */
    suspend fun findMatchingStub(
        request: RoutingRequest,
        verbose: Boolean,
        logger: Logger,
        formatter: HttpFormatter,
    ): StubLookupResult {
        val formattedRequest = formatRequest(request, verbose, formatter)

        // Retries only when an eventuallyRemove stub is claimed by a concurrent coroutine
        // between Phase 1 evaluation and the Phase 2 claim — an extremely rare edge case.
        // Each retry evaluates a fresh snapshot, and claimMatch succeeds at most once
        // per stub, so the loop always terminates with a different winner.
        while (true) {
            // Phase 1: snapshot + evaluate outside the lock (suspend I/O runs freely).
            val result = evaluate(stubs.value, request, verbose, logger, formattedRequest)

            when (result) {
                is StubLookupResult.NotMatched -> return result
                is StubLookupResult.Matched -> {
                    val stub = result.stub

                    // Phase 2: claim the winner (lock held only for this brief step).
                    if (stub.configuration.eventuallyRemove) {
                        // CAS inside the lock: only one coroutine wins; losers retry Phase 1.
                        val claimed =
                            mutex.withLock {
                                stub.claimMatch().also { won ->
                                    if (won) {
                                        stubs.update { it.remove(stub) }
                                        if (verbose) {
                                            logger.debug(
                                                "Removed used stub: ${stub.toLogString()}",
                                            )
                                        }
                                    }
                                }
                            }
                        if (!claimed) continue
                    } else {
                        stub.markMatched()
                    }

                    return result
                }
            }
        }
    }

    /**
     * Returns a snapshot of all registered stubs.
     */
    fun getAll(): List<Stub<*, *>> = stubs.value

    /**
     * Finds a stub by its human-readable name, or `null` if no stub has that name.
     */
    fun findByName(name: String): Stub<*, *>? =
        if (name.isBlank()) {
            null
        } else {
            stubs.value.firstOrNull { it.configuration.name == name }
        }

    /**
     * Finds a stub by its stable unique identifier, or `null` if no stub has that id.
     */
    fun findById(id: StubId): Stub<*, *>? = stubs.value.firstOrNull { it.id == id }

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
    ): StubLookupResult {
        val evaluated = buildList {
            for (stub in snapshot) {
                if (stub.configuration.eventuallyRemove && stub.hasBeenMatched()) continue
                val result = stub.requestSpecification.matches(request)
                result.exceptionOrNull()?.let { ex ->
                    if (verbose) {
                        logger.warn(
                            "Failed to evaluate condition for stub: ${stub.toLogString()}. " +
                                "Request: $formattedRequest",
                            ex,
                        )
                    }
                }
                result.getOrNull()?.let { add(stub to it) }
            }
        }

        val fullMatches = evaluated.filter { (_, mr) -> mr.matched }

        if (fullMatches.isNotEmpty()) {
            val winner = fullMatches
                .sortedWith(
                    compareByDescending<Pair<Stub<*, *>, MatchResult>> { (_, r) -> r.score }
                        .thenByDescending { (s, _) -> s.requestSpecification.priority }
                        .thenBy { (s, _) -> s.creationOrder },
                ).first()
                .first
            return StubLookupResult.Matched(winner)
        }

        // No full match — build evaluation list for diagnostics
        val evaluations = evaluated.map { (stub, mr) -> StubEvaluation(stub, mr) }
        return StubLookupResult.NotMatched(evaluations)
    }

    // endregion
}
