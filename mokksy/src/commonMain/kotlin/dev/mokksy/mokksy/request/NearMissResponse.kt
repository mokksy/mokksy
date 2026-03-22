package dev.mokksy.mokksy.request

import kotlinx.serialization.Serializable

/**
 * Diagnostic response returned as the 404 body when no stub matches an incoming request.
 *
 * Lists the request that arrived and, for each registered stub, which matchers
 * passed and which failed (with human-readable reasons).
 */
@Serializable
internal data class NearMissResponse(
    val message: String,
    val request: NearMissRequest,
    val nearMisses: List<NearMissStub>,
)

/**
 * Snapshot of the incoming HTTP request included in the diagnostic response.
 */
@Serializable
internal data class NearMissRequest(
    val method: String,
    val path: String,
    val headers: Map<String, List<String>>,
    val body: String? = null,
)

/**
 * Per-stub diagnostic showing which matchers passed and which failed.
 */
@Serializable
internal data class NearMissStub(
    val name: String?,
    val passed: List<String>,
    val failed: List<NearMissFailedMatcher>,
)

/**
 * A single failed matcher with its label and the human-readable reason for failure.
 */
@Serializable
internal data class NearMissFailedMatcher(
    val matcher: String,
    val reason: String?,
)
