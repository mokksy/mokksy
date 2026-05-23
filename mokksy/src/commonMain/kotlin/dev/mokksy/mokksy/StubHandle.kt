@file:OptIn(ExperimentalMokksyApi::class)

package dev.mokksy.mokksy

/**
 * A lightweight public handle to a registered stub.
 *
 * Provides read-only access to the stub's name and invocation count without
 * exposing the internal [Stub] type.
 *
 * Returned by [BuildingStep.respondsWith] and related methods, and by
 * [MokksyServer.findStub], [MokksyServer.allStubs], and
 * [MokksyServer.findAllUnmatchedStubs].
 */
public class StubHandle internal constructor(
    internal val stub: Stub<*, *>,
) {
    /**
     * The optional name assigned at stub registration, or `null` if unnamed.
     */
    public val name: String? get() = stub.configuration.name

    /**
     * How many times this stub has been matched so far.
     *
     * For reusable stubs this is the total invocation count.
     * For once-only ([StubConfiguration.eventuallyRemove]) stubs this is `1` after
     * the first match and does not increase further.
     */
    public fun matchCount(): Long = stub.matchCount()

    /**
     * Returns a [VerificationBuilder] for specifying the expected invocation count.
     *
     * No assertion runs until a terminal method ([VerificationBuilder.atLeast],
     * [VerificationBuilder.atMost], [VerificationBuilder.exactly], or
     * [VerificationBuilder.never]) is called. For the common case of "called at least once":
     */
    @ExperimentalMokksyApi
    public fun verifyCalled(): VerificationBuilder = VerificationBuilder(this)

    /**
     * Convenience shortcut — asserts this stub was called exactly [times] times.
     *
     * Equivalent to `verifyCalled().exactly(times)`.
     */
    @ExperimentalMokksyApi
    public fun verifyCalled(times: Int): StubHandle = verifyCalled().exactly(times)

    override fun toString(): String = stub.toLogString()
}

/**
 * Fluent builder for stub-verification assertions.
 *
 * Returned by [StubHandle.verifyCalled]. Every method is terminal — it asserts
 * immediately and returns the [StubHandle] for chaining.
 *
 * @see StubHandle.verifyCalled
 */
@ExperimentalMokksyApi
public class VerificationBuilder internal constructor(
    private val handle: StubHandle,
) {
    /**
     * Asserts the stub was called at least [times] times.
     *
     * @return The [StubHandle] for further assertions.
     */
    public fun atLeast(times: Int): StubHandle {
        require(times >= 0) { "atLeast must be >= 0, was $times" }
        val count = handle.matchCount()
        if (count < times) {
            throw AssertionError(
                "Stub '${handle.name}' was called $count time(s), expected at least $times",
            )
        }
        return handle
    }

    /**
     * Asserts the stub was called at most [times] times.
     *
     * @return The [StubHandle] for further assertions.
     */
    public fun atMost(times: Int): StubHandle {
        require(times >= 0) { "atMost must be >= 0, was $times" }
        val count = handle.matchCount()
        if (count > times) {
            throw AssertionError(
                "Stub '${handle.name}' was called $count time(s), expected at most $times",
            )
        }
        return handle
    }

    /**
     * Asserts the stub was called exactly [times] times.
     *
     * @return The [StubHandle] for further assertions.
     */
    public fun exactly(times: Int): StubHandle {
        require(times >= 0) { "exactly must be >= 0, was $times" }
        val count = handle.matchCount()
        if (count != times.toLong()) {
            throw AssertionError(
                "Stub '${handle.name}' was called $count time(s), expected exactly $times",
            )
        }
        return handle
    }

    /**
     * Asserts the stub was never called (exactly 0 times).
     *
     * Equivalent to `exactly(0)`.
     *
     * @return The [StubHandle] for further assertions.
     */
    public fun never(): StubHandle = exactly(0)
}
