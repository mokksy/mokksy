@file:JvmName("RequestSpecificationMatching")

package dev.mokksy.mokksy.request

import io.kotest.matchers.Matcher
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.log
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.ContentTransformationException
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.receive
import kotlinx.coroutines.CancellationException
import kotlin.jvm.JvmName

/**
 * Evaluates every matcher independently (no short-circuit) and returns a scored [MatchResult].
 *
 * Used by the stub registry to pick the *most specific* full match and, when nothing matches,
 * to surface the closest candidate in the log.
 */
internal suspend fun RequestSpecification<*>.matches(
    request: ApplicationRequest,
): Result<MatchResult> = this.matchesTyped(request)

/**
 * Intermediate scoring accumulator used by the matcher evaluation functions.
 */
private data class ScoringResult(
    val score: Int,
    val failedLabels: List<String>,
    val diagnostics: List<MatcherDiagnostic>,
) {
    companion object {
        val EMPTY = ScoringResult(0, emptyList(), emptyList())
    }
}

private suspend fun <P : Any> RequestSpecification<P>.matchesTyped(
    request: ApplicationRequest,
): Result<MatchResult> =
    runCatching {
        var score = 0
        val failed = mutableListOf<String>()
        val diagnostics = mutableListOf<MatcherDiagnostic>()

        if (method != null) {
            val r =
                scoreSingleMatcher(
                    method,
                    request.httpMethod,
                    "method",
                    request.call,
                )
            score += r.score
            failed += r.failedLabels
            diagnostics += r.diagnostics
        }
        if (path != null) {
            val r =
                scoreSingleMatcher(
                    path,
                    request.path(),
                    "path",
                    request.call,
                )
            score += r.score
            failed += r.failedLabels
            diagnostics += r.diagnostics
        }
        if (headers.isNotEmpty()) {
            val r =
                scoreMatchersSafely(
                    headers,
                    request.headers,
                    "headers",
                    request.call,
                )
            score += r.score
            failed += r.failedLabels
            diagnostics += r.diagnostics
        }

        val bodyResult = scoreBodyMatchers(request)
        score += bodyResult.score
        failed += bodyResult.failedLabels
        diagnostics += bodyResult.diagnostics

        val bodyStringResult = scoreBodyStringMatchers(request)
        score += bodyStringResult.score
        failed += bodyStringResult.failedLabels
        diagnostics += bodyStringResult.diagnostics

        MatchResult(
            matched = failed.isEmpty(),
            score = score,
            failedMatchers = failed,
            diagnostics = diagnostics,
        )
    }.onFailure {
        if (it is CancellationException || it is Error) throw it
    }

private suspend fun <P : Any> RequestSpecification<P>.scoreBodyMatchers(
    request: ApplicationRequest,
): ScoringResult =
    if (body.isEmpty()) {
        ScoringResult.EMPTY
    } else {
        receiveBodyOrNull(request)
            ?.let { scoreMatchersSafely(body, it, "body", request.call) }
            ?: ScoringResult(
                score = 0,
                failedLabels = body.indices.map { "body[$it]" },
                diagnostics = body.indices.map {
                    MatcherDiagnostic(
                        "body[$it]",
                        matched = false,
                        reason = "Unable to deserialize request body",
                    )
                },
            )
    }

private suspend fun RequestSpecification<*>.scoreBodyStringMatchers(
    request: ApplicationRequest,
): ScoringResult =
    if (bodyString.isEmpty()) {
        ScoringResult.EMPTY
    } else {
        receiveBodyStringOrNull(request)
            ?.let {
                scoreMatchersSafely(
                    bodyString,
                    it,
                    "bodyString",
                    request.call,
                )
            }
            ?: ScoringResult(
                score = 0,
                failedLabels = bodyString.indices.map { "bodyString[$it]" },
                diagnostics = bodyString.indices.map {
                    MatcherDiagnostic(
                        "bodyString[$it]",
                        matched = false,
                        reason = "Unable to read request body as string",
                    )
                },
            )
    }

private suspend fun <P : Any> RequestSpecification<P>.receiveBodyOrNull(
    request: ApplicationRequest,
): P? =
    try {
        request.call.receive(requestType)
    } catch (e: CancellationException) {
        throw e
    } catch (e: ContentTransformationException) {
        request.call.application.log
            .trace("Unable to read typed body for scoring: ${e.message}")
        null
    } catch (e: BadRequestException) {
        request.call.application.log
            .trace("Bad request body during scoring: ${e.message}")
        null
    }

@Suppress("TooGenericExceptionCaught")
private suspend fun receiveBodyStringOrNull(request: ApplicationRequest): String? =
    try {
        request.call.receive(type = String::class)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        request.call.application.log
            .debug(
                "Unable to read body as string for matching. " +
                    "Make sure the Ktor `DoubleReceive` plugin is installed. ${e.message}",
                e,
            )
        null
    }

/**
 * Guards a single matcher invocation and captures diagnostic detail.
 */
@Suppress("TooGenericExceptionCaught")
private fun <T> scoreSingleMatcher(
    matcher: Matcher<T>,
    value: T,
    label: String,
    call: ApplicationCall,
): ScoringResult =
    try {
        val result = matcher.test(value)
        if (result.passed()) {
            ScoringResult(1, emptyList(), listOf(MatcherDiagnostic(label, matched = true)))
        } else {
            val reason = result.failureMessage()
            ScoringResult(0, listOf(label), listOf(MatcherDiagnostic(label, matched = false, reason = reason)))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        call.application.log.debug("Matcher $label threw during scoring: ${e.message}", e)
        ScoringResult(0, listOf(label), listOf(MatcherDiagnostic(label, matched = false, reason = e.message)))
    }

/**
 * Runs each matcher individually, preserving scores already earned when a later matcher throws.
 * A throwing matcher is logged at debug level and counted as a failure for that slot only.
 */
@Suppress("TooGenericExceptionCaught")
private fun <T> scoreMatchersSafely(
    matchers: List<Matcher<T>>,
    value: T,
    label: String,
    call: ApplicationCall,
): ScoringResult {
    var score = 0
    val failedLabels = mutableListOf<String>()
    val diagnostics = mutableListOf<MatcherDiagnostic>()
    matchers.forEachIndexed { i, matcher ->
        val slotLabel = "$label[$i]"
        try {
            val result = matcher.test(value)
            if (result.passed()) {
                score++
                diagnostics += MatcherDiagnostic(slotLabel, matched = true)
            } else {
                failedLabels += slotLabel
                diagnostics += MatcherDiagnostic(slotLabel, matched = false, reason = result.failureMessage())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            call.application.log.debug("Matcher $slotLabel threw during scoring: ${e.message}", e)
            failedLabels += slotLabel
            diagnostics += MatcherDiagnostic(slotLabel, matched = false, reason = e.message)
        }
    }
    return ScoringResult(score, failedLabels, diagnostics)
}
