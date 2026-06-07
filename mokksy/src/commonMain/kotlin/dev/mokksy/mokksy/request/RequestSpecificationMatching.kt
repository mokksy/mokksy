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

@Suppress("LongMethod")
private suspend fun <P : Any> RequestSpecification<P>.matchesTyped(
    request: ApplicationRequest,
): Result<MatchResult> {
    var score = 0
    val failed = mutableListOf<FailedMatcherDescriptor>()

    if (method != null) {
        val (s, f) =
            scoreSingleMatcher(
                method,
                request.httpMethod,
                MatcherCategory.METHOD,
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
                MatcherCategory.PATH,
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
                MatcherCategory.HEADERS,
                request.call,
            )
        score += headersScore
        failed += headersFailed
    }
    val (cookiesScore, cookiesFailed) = scoreCookieMatchers(request)
    score += cookiesScore
    failed += cookiesFailed

    val (queryParamsScore, queryParamsFailed) = scoreQueryParameterMatchers(request)
    score += queryParamsScore
    failed += queryParamsFailed

    val (bodyScore, bodyFailed) = scoreBodyMatchers(request)
    score += bodyScore
    failed += bodyFailed

    val (bodyStringScore, bodyStringFailed) = scoreBodyStringMatchers(request)
    score += bodyStringScore
    failed += bodyStringFailed

    val (formScore, formFailed) = scoreFormMatchers(request, formSpecs)
    score += formScore
    failed += formFailed

    val (multipartScore, multipartFailed) = scoreMultipartMatchers(request, multipartSpecs)
    score += multipartScore
    failed += multipartFailed

    val (byteBodyScore, byteBodyFailed) = scoreByteBodyMatchers(request, byteBodySpecs)
    score += byteBodyScore
    failed += byteBodyFailed

    return Result.success(MatchResult(matched = failed.isEmpty(), score = score, failedMatchers = failed))
}

private fun RequestSpecification<*>.scoreCookieMatchers(
    request: ApplicationRequest,
): Pair<Int, List<FailedMatcherDescriptor>> =
    if (cookies.isEmpty()) {
        0 to emptyList()
    } else {
        scoreMatchersSafely(
            cookies,
            request.cookies,
            MatcherCategory.COOKIES,
            request.call,
        )
    }

private fun RequestSpecification<*>.scoreQueryParameterMatchers(
    request: ApplicationRequest,
): Pair<Int, List<FailedMatcherDescriptor>> =
    if (queryParameters.isEmpty()) {
        0 to emptyList()
    } else {
        scoreMatchersSafely(
            queryParameters,
            request.queryParameters,
            MatcherCategory.QUERY_PARAMS,
            request.call,
        )
    }

private suspend fun <P : Any> RequestSpecification<P>.scoreBodyMatchers(
    request: ApplicationRequest,
): Pair<Int, List<FailedMatcherDescriptor>> =
    if (body.isEmpty()) {
        0 to emptyList()
    } else {
        receiveBodyOrNull(request)
            ?.let { scoreMatchersSafely(body, it, MatcherCategory.BODY, request.call) }
            ?: (0 to body.indices.map { FailedMatcherDescriptor.Indexed(MatcherCategory.BODY, it) })
    }

private suspend fun RequestSpecification<*>.scoreBodyStringMatchers(
    request: ApplicationRequest,
): Pair<Int, List<FailedMatcherDescriptor>> =
    if (bodyString.isEmpty()) {
        0 to emptyList()
    } else {
        receiveBodyStringOrNull(request)
            ?.let {
                scoreMatchersSafely(
                    bodyString,
                    it,
                    MatcherCategory.BODY_STRING,
                    request.call,
                )
            }
            ?: (0 to bodyString.indices.map {
                FailedMatcherDescriptor.Indexed(MatcherCategory.BODY_STRING, it)
            })
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
    } catch (e: ContentTransformationException) {
        request.call.application.log
            .trace("Unable to read body as string for matching: ${e.message}")
        null
    } catch (e: BadRequestException) {
        request.call.application.log
            .trace("Bad request body during string scoring: ${e.message}")
        null
    }

/**
 * Guards a single matcher invocation. Returns `1 to emptyList()` on pass,
 * `0 to listOf(Simple(category))` on mismatch or throw.
 */
@Suppress("TooGenericExceptionCaught")
private fun <T> scoreSingleMatcher(
    matcher: Matcher<T>,
    value: T,
    category: MatcherCategory,
    call: ApplicationCall,
): Pair<Int, List<FailedMatcherDescriptor>> =
    try {
        if (matcher.test(value).passed()) {
            1 to emptyList()
        } else {
            0 to listOf(FailedMatcherDescriptor.Simple(category))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        call.application.log.debug("Matcher $category threw during scoring: ${e.message}", e)
        0 to listOf(FailedMatcherDescriptor.Simple(category))
    }

/**
 * Runs each matcher individually, preserving scores already earned when a later matcher throws.
 * A throwing matcher is logged at debug level and counted as a failure for that slot only.
 */
@Suppress("TooGenericExceptionCaught")
private fun <T> scoreMatchersSafely(
    matchers: List<Matcher<T>>,
    value: T,
    category: MatcherCategory,
    call: ApplicationCall,
): Pair<Int, List<FailedMatcherDescriptor>> {
    var score = 0
    val failed = mutableListOf<FailedMatcherDescriptor>()
    matchers.forEachIndexed { i, matcher ->
        try {
            if (matcher.test(value).passed()) {
                score++
            } else {
                failed += FailedMatcherDescriptor.Indexed(category, i)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            call.application.log.debug("Matcher $category[$i] threw during scoring: ${e.message}", e)
            failed += FailedMatcherDescriptor.Indexed(category, i)
        }
    }
    return score to failed
}
