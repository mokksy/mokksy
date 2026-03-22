@file:OptIn(InternalMokksyApi::class)

package dev.mokksy.mokksy

import dev.mokksy.mokksy.request.RequestSpecification
import dev.mokksy.mokksy.response.ResponseDefinitionSupplier
import io.ktor.server.application.ApplicationCall
import kotlinx.atomicfu.atomic

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
internal data class Stub<P : Any, T : Any>(
    val configuration: StubConfiguration,
    val requestSpecification: RequestSpecification<P>,
    val responseDefinitionSupplier: ResponseDefinitionSupplier<T>,
) : Comparable<Stub<*, *>> {
    private companion object {
        // Multiplatform atomic counter for creation order
        private val COUNTER = atomic(0L)
    }

    /**
     * Represents the order of creation for an instance of the containing class.
     * This property is initialized with an incrementing value to ensure each instance
     * can be distinctly ordered based on the sequence of their creation.
     *
     * Used by [StubComparator].
     */
    internal val creationOrder = COUNTER.incrementAndGet()

    // region Match tracking

    /**
     * Whether this stub has been matched at least once.
     *
     * This flag is the single source of truth for match state. It is used by:
     * - [MokksyServer.findAllUnmatchedStubs] to find stubs that have never been hit.
     * - [MokksyServer.resetMatchCounts] to re-arm stubs for a new test scenario.
     * - [claimMatch] to enforce the [StubConfiguration.eventuallyRemove] invariant
     *   without holding the registry mutex during request evaluation.
     */
    private val matched = atomic(false)

    /**
     * Returns `true` if this stub has been matched at least once.
     */
    fun hasBeenMatched(): Boolean = matched.value

    /**
     * Atomically claims this stub for a match.
     *
     * Returns `true` exactly once — the first caller wins and is responsible for
     * scheduling removal when [StubConfiguration.eventuallyRemove] is set.
     * Every subsequent call returns `false`.
     */
    fun claimMatch(): Boolean = matched.compareAndSet(expect = false, update = true)

    /**
     * Resets match state to unmatched, re-arming the stub for a new test scenario.
     *
     * @see MokksyServer.resetMatchState
     */
    fun reset() {
        matched.value = false
    }

    // endregion

    /**
     * Compares this [Stub] instance to another [Stub] instance for order.
     *
     * The comparison is based primarily on the priority of the [requestSpecification].
     * If the priorities are equal, the [creationOrder] of the stubs is used as a tiebreaker.
     *
     * @param other The [Stub] instance to compare with this one.
     * @return A negative integer, zero, or a positive integer if this [Stub] is less than,
     * equal to, or greater than the specified [Stub], respectively.
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
 * This comparator is used to compare [Stub] instances based on the priority
 * defined in their [RequestSpecification].
 * Higher priority values are considered greater.
 *
 * If priorities are equal, then [Stub]s are compared by [Stub.creationOrder].
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
            a.requestSpecification.priority.compareTo(
                b.requestSpecification.priority,
            )
        return if (result != 0) {
            result
        } else {
            compareValues(a.creationOrder, b.creationOrder)
        }
    }
}
