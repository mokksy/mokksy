@file:OptIn(InternalMokksyApi::class)

package dev.mokksy.mokksy.request

import dev.mokksy.mokksy.InternalMokksyApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Structured 404 response body returned when no stub matches and
 * [dev.mokksy.mokksy.ServerConfiguration.verbose] is `true`.
 *
 * Note: This format is a diagnostic aid, not a public API contract.
 * The JSON schema may change without notice.
 */
@InternalMokksyApi
@Serializable
internal data class DiagnosticResponse(
    val request: RequestInfo,
    @SerialName("closestStub")
    val stubEvaluations: List<StubMatchResult>,
)

@InternalMokksyApi
@Serializable
internal data class RequestInfo(
    val method: String,
    val path: String,
    val headers: Map<String, String>,
    val body: JsonElement? = null,
)

/**
 * Per-stub evaluation result.
 *
 * [configuredMatchers] lists the matchers configured on this stub
 * (e.g., "method: GET", "path: '/api/items'", "headers: Authorization = Bearer token").
 * [failedMatchers] lists matchers that did not pass with expected vs actual
 * (e.g., "method: expected GET but was POST", "path: expected '/wrong-path' but was '/actual-path'").
 */
@InternalMokksyApi
@Serializable
internal data class StubMatchResult(
    val name: String,
    val configuredMatchers: List<String>,
    val failedMatchers: List<String>,
)
