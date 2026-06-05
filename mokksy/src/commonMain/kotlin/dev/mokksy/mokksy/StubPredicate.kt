@file:OptIn(ExperimentalMokksyApi::class)

package dev.mokksy.mokksy

/**
 * SAM-convertible predicate over a [StubHandle].
 *
 * Used by [MokksyServer.findStub] and [MokksyServer.findStubs] to filter
 * registered stubs by arbitrary criteria beyond exact name or id lookup.
 *
 * Backed by `fun interface` so it is also SAM-convertible from a Kotlin lambda:
 * ```kotlin
 * mokksy.findStubs { it.matchCount() == 0 && it.name?.startsWith("test-") == true }
 * ```
 *
 * Java callers can use a lambda expression:
 * ```java
 * mokksy.findStubs(stub -> stub.matchCount() == 0);
 * ```
 */
@ExperimentalMokksyApi
public fun interface StubPredicate {
    /**
     * Returns `true` to include this [StubHandle] in the result set.
     */
    public fun test(stub: StubHandle): Boolean
}
