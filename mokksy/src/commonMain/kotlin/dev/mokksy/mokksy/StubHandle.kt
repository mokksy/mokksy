package dev.mokksy.mokksy

/**
 * A lightweight public handle to a registered stub.
 *
 * Provides read-only access to the stub's name and invocation count without
 * exposing the internal [Stub] type.
 *
 * Returned by [BuildingStep.respondsWith] and related methods, and by
 * [MokksyServer.findStub], [MokksyServer.findAllStubs], and
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

    override fun toString(): String = stub.toLogString()
}
