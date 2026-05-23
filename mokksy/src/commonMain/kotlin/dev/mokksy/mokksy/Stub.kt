@file:OptIn(InternalMokksyApi::class)

package dev.mokksy.mokksy

import dev.mokksy.mokksy.request.RequestSpecification
import dev.mokksy.mokksy.response.ResponseDefinitionSupplier
import io.ktor.server.application.ApplicationCall
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch

/**
 * Represents a mapping between an inbound [RequestSpecification] and an outbound response definition.
 *
 * This class encapsulates the logic needed to handle HTTP requests and responses, including
 * matching request specifications, sending responses, and handling response data of various types.
 * Resembles the [WireMock Stub](https://wiremock.org/docs/stubbing/) abstraction.
 *
 * @param P The type of the request payload.
 * @param T The type of the response data.
 * @param configuration Defines the behavior and attributes of the stub, including its name
 *          and logging verbosity settings.
 * @property requestSpecification Defines the criteria used to match incoming requests.
 * @property responseDefinitionSupplier Supplies the [ResponseDefinitionSupplier] for a matched request.
 *          This includes headers, body, and HTTP status code, which are applied to the HTTP response.
 * @author Konstantin Pavlov
 */
@OptIn(ExperimentalAtomicApi::class)
internal data class Stub<P : Any, T : Any>(
    val configuration: StubConfiguration,
    val requestSpecification: RequestSpecification<P>,
    val responseDefinitionSupplier: ResponseDefinitionSupplier<T>,
) : Comparable<Stub<*, *>> {
    private companion object {
        // Multiplatform atomic counter for creation order
        private val COUNTER = AtomicLong(0L)
    }

    /**
     * Represents the order of creation for an instance of the containing class.
     * This property is initialized with an incrementing value to ensure each instance
     * can be distinctly ordered based on the sequence of their creation.
     *
     * Used by [StubComparator].
     */
    internal val creationOrder = COUNTER.incrementAndFetch()

    // region Match tracking

    /**
     * How many times this stub has been matched.
     *
     * This is the single source of truth for match state. It is used by:
     * - [findAllUnmatchedStubs] to find stubs that have never been hit.
     * - [resetMatchState] to re-arm stubs for a new test scenario.
     * - [claimMatch] to enforce the [StubConfiguration.eventuallyRemove] invariant
     *   without holding the registry mutex during request evaluation.
     *
     * Exposed publicly via [matchCount].
     */
    private val matchCount = AtomicLong(0)

    /**
     * Returns `true` if this stub has been matched at least once.
     */
    fun hasBeenMatched(): Boolean = matchCount.load() > 0

    /**
     * Marks this stub as matched. Used for reusable stubs where every match succeeds.
     */
    fun markMatched() {
        matchCount.incrementAndFetch()
    }

    /**
     * Atomically claims this stub for a match.
     *
     * Returns `true` exactly once — the first caller wins and is responsible for
     * scheduling removal when [StubConfiguration.eventuallyRemove] is set.
     * Every subsequent call returns `false`.
     */
    fun claimMatch(): Boolean = matchCount.compareAndSet(0L, 1L)

    /**
     * Returns how many times this stub has been matched so far.
     *
     * For reusable stubs this is the total invocation count.
     * For once-only ([StubConfiguration.eventuallyRemove]) stubs this is `1` after the
     * first match and does not increase further because the stub is removed from the
     * registry immediately after its single claim.
     */
    fun matchCount(): Long = matchCount.load()

    /**
     * Resets match state to unmatched, re-arming the stub for a new test scenario.
     *
     * @see MokksyServer.resetMatchState
     */
    fun reset() {
        matchCount.store(0L)
    }

    // endregion

    /**
     * Compares this [Stub] instance to another [Stub] instance for order.
     *
     * Stubs are ordered by priority descending (higher priority first), then by
     * [creationOrder] ascending (earlier registration wins on ties).
     *
     * @param other The [Stub] instance to compare with this one.
     * @return A negative integer, zero, or a positive integer as defined by [Comparable].
     */
    override fun compareTo(other: Stub<*, *>): Int = StubComparator.compare(this, other)

    suspend fun respond(
        call: ApplicationCall,
        verbose: Boolean,
    ) {
        val responseDefinition = responseDefinitionSupplier.invoke(call)
        responseDefinition.headers?.invoke(call.response.headers)
        call.response.status(responseDefinition.httpStatus)

        responseDefinition.writeResponse(call, verbose)
    }

    fun toLogString(): String =
        if (configuration.name?.isNotBlank() == true) {
            "Stub('${configuration.name}')[config=$configuration requestSpec=${requestSpecification.toLogString()}]"
        } else {
            "Stub[config=$configuration requestSpec=${requestSpecification.toLogString()}]"
        }
}

/**
 * Comparator implementation for [Stub] objects.
 *
 * Orders stubs by priority descending (higher numeric values first), then by
 * [Stub.creationOrder] ascending (earlier registration wins on ties).
 *
 * Used internally for sorting or ordering [Stub] objects when multiple mappings need
 * to be evaluated or prioritized.
 */
internal object StubComparator : Comparator<Stub<*, *>> {
    override fun compare(
        a: Stub<*, *>,
        b: Stub<*, *>,
    ): Int {
        val result =
            b.requestSpecification.priority.compareTo(
                a.requestSpecification.priority,
            )
        return if (result != 0) {
            result
        } else {
            compareValues(a.creationOrder, b.creationOrder)
        }
    }
}
