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

private suspend fun <P : Any> RequestSpecification<P>.matchesTyped(
    request: ApplicationRequest,
): Result<MatchResult> =
    runCatching {
        var score = 0
        val failed = mutableListOf<String>()

        if (method != null) {
            val (s, f) =
                scoreSingleMatcher(
                    method,
                    request.httpMethod,
                    "method",
                    request.call,
                )
            score += s
            failed += f
        }
        if (path != null) {
            val (s, f) =
                scoreSingleMatcher(
                    path,
                    request.path(),
                    "path",
                    request.call,
                )
            score += s
            failed += f
        }
        if (headers.isNotEmpty()) {
            val (headersScore, headersFailed) =
                scoreMatchersSafely(
                    headers,
                    request.headers,
                    "headers",
                    request.call,
                )
            score += headersScore
            failed += headersFailed
        }

        val (bodyScore, bodyFailed) = scoreBodyMatchers(request)
        score += bodyScore
        failed += bodyFailed

        val (bodyStringScore, bodyStringFailed) = scoreBodyStringMatchers(request)
        score += bodyStringScore
        failed += bodyStringFailed

        MatchResult(matched = failed.isEmpty(), score = score, failedMatchers = failed)
    }.onFailure {
        if (it is CancellationException || it is Error) throw it
    }

private suspend fun <P : Any> RequestSpecification<P>.scoreBodyMatchers(
    request: ApplicationRequest,
): Pair<Int, List<String>> =
    if (body.isEmpty()) {
        0 to emptyList()
    } else {
        receiveBodyOrNull(request)
            ?.let { scoreMatchersSafely(body, it, "body", request.call) }
            ?: (0 to body.indices.map { "body[$it]" })
    }

private suspend fun RequestSpecification<*>.scoreBodyStringMatchers(
    request: ApplicationRequest,
): Pair<Int, List<String>> =
    if (bodyString.isEmpty()) {
        0 to emptyList()
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
            ?: (0 to bodyString.indices.map { "bodyString[$it]" })
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
            .debug("Unable to read typed body for scoring: ${e.message}", e)
        null
    } catch (e: BadRequestException) {
        request.call.application.log
            .debug("Bad request body during scoring: ${e.message}", e)
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
 * Guards a single matcher invocation. Returns `1 to emptyList()` on pass,
 * `0 to listOf(label)` on mismatch or throw.
 */
@Suppress("TooGenericExceptionCaught")
private fun <T> scoreSingleMatcher(
    matcher: Matcher<T>,
    value: T,
    label: String,
    call: ApplicationCall,
): Pair<Int, List<String>> =
    try {
        if (matcher.test(value).passed()) 1 to emptyList() else 0 to listOf(label)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        call.application.log.debug("Matcher $label threw during scoring: ${e.message}", e)
        0 to listOf(label)
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
): Pair<Int, List<String>> {
    var score = 0
    val failed = mutableListOf<String>()
    matchers.forEachIndexed { i, matcher ->
        try {
            if (matcher.test(value).passed()) score++ else failed += "$label[$i]"
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            call.application.log.debug("Matcher $label[$i] threw during scoring: ${e.message}", e)
            failed += "$label[$i]"
        }
    }
    return score to failed
}
