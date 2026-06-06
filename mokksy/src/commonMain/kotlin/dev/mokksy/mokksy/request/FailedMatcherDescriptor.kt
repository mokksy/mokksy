package dev.mokksy.mokksy.request

/**
 * Typed descriptor for a matcher that failed during request evaluation.
 *
 * Replaces raw string labels to eliminate string parsing in
 * [describeFailedMatcher].
 *
 * - [Simple] — single-entity matchers (method, path).
 * - [Indexed] — indexed matchers (headers[0], body[1], multipart[0], …).
 */
internal sealed interface FailedMatcherDescriptor {
    data class Simple(val category: MatcherCategory) : FailedMatcherDescriptor
    data class Indexed(val category: MatcherCategory, val index: Int) : FailedMatcherDescriptor {
        override fun toString(): String = "$category[$index]"
    }
}
