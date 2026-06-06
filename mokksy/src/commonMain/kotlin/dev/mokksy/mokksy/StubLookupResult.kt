@file:OptIn(InternalMokksyApi::class)

package dev.mokksy.mokksy

import dev.mokksy.mokksy.request.MatchResult

/**
 * Outcome of matching a request against registered stubs.
 *
 * - [Matched] — a [Stub] matched the request and should handle it.
 * - [NotMatched] — no stub matched; [evaluations] provides diagnostics for
 *   the closest candidates, pairing each [Stub] with its [MatchResult].
 */
@InternalMokksyApi
internal sealed interface StubLookupResult {
    data class Matched(val stub: Stub<*, *>) : StubLookupResult
    data class NotMatched(val evaluations: List<StubEvaluation>) : StubLookupResult
}

/**
 * Associates a [Stub] with the [MatchResult] from evaluating its request specification.
 *
 * Used by [StubLookupResult.NotMatched] to report why each candidate stub
 * did (or did not) match for diagnostic purposes.
 */
@InternalMokksyApi
internal data class StubEvaluation(
    val stub: Stub<*, *>,
    val matchResult: MatchResult,
)
