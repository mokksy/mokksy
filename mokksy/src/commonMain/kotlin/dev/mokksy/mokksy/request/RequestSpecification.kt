package dev.mokksy.mokksy.request

import dev.mokksy.mokksy.MokksyDsl
import io.kotest.matchers.Matcher
import io.kotest.matchers.string.contain
import io.ktor.http.Headers
import io.ktor.http.HttpMethod
import kotlin.jvm.JvmOverloads
import kotlin.reflect.KClass

/**
 * The default priority value assigned to a stub when no explicit priority is specified.
 *
 * This constant is used in the context of mapping and comparing inbound [RequestSpecification]s
 * (such as stubs or routes) to determine their evaluation order. Higher numerical values indicate
 * higher priority.
 *
 * The default value is `0`. Stubs with positive priorities take precedence over the default,
 * while negative values (e.g. `priority(-1)`) can be used for catch-all / fallback stubs.
 */
public const val DEFAULT_STUB_PRIORITY: Int = 0

/**
 * Result of evaluating a [RequestSpecification] against an incoming request.
 *
 * @property matched `true` when every defined matcher passed.
 * @property score Number of matchers that passed; higher means more specific.
 * @property failedMatchers Human-readable labels of matchers that did not pass.
 */
internal data class MatchResult(
    val matched: Boolean,
    val score: Int,
    val failedMatchers: List<String>,
)

/**
 * Represents a specification for matching incoming HTTP requests based on defined criteria,
 * such as HTTP method, request path, and request body.
 *
 * This class is used to define the criteria against which incoming requests are tested.
 * It provides functionality for checking whether a given request satisfies the specified
 * conditions and supports prioritization for defining matching order.
 *
 * @param P type of the request payload
 * @property method Matcher for the HTTP method of the request. If null, the method is not validated.
 * @property path Matcher for the request path. If null, the path is not validated.
 * @property headers List of matchers for Ktor Headers object. All matchers must pass for a match to succeed.
 * @property body List of matchers for the request body as a [P]. All matchers must pass for a match to succeed.
 * @property bodyString List of matchers for the request body as a String.
 *                      All matchers must pass for a match to succeed.
 *                      **Note:** when both [body] and [bodyString] matchers are active, the request body is read
 *                      twice. The Ktor `DoubleReceive` plugin must be installed on the server for this to work
 *                      correctly; without it the second `receive()` call will fail silently and all matchers in
 *                      the later group will be treated as not matched.
 * @property priority The priority value used for comparing different specifications.
 * Higher values indicate higher priority. Default value is [DEFAULT_STUB_PRIORITY] (`0`).
 * Negative values can be used for catch-all / fallback stubs.
 *
 * Matching order: (1) highest score wins, (2) highest priority breaks ties,
 * (3) earliest registration order breaks remaining ties.
 */
@Suppress("LongParameterList")
public open class RequestSpecification<P : Any>(
    public val method: Matcher<HttpMethod>? = null,
    public val path: Matcher<String>? = null,
    public val headers: List<Matcher<Headers>> = listOf(),
    public val body: List<Matcher<P?>> = listOf(),
    public val bodyString: List<Matcher<String?>> = listOf(),
    public val priority: Int = DEFAULT_STUB_PRIORITY,
    internal val requestType: KClass<P>,
) {
    internal fun toLogString(): String =
        buildString {
            if (method != null) {
                appendLine("method: $method")
            }
            if (path != null) appendLine("path: $path")
            if (headers.isNotEmpty()) appendLine("headers: $headers")
            if (body.isNotEmpty()) appendLine("body: $body")
            if (bodyString.isNotEmpty()) appendLine("bodyString: $bodyString")
        }
}

@MokksyDsl
public open class RequestSpecificationBuilder<P : Any>(
    protected val requestType: KClass<P>,
) {
    protected var method: Matcher<HttpMethod>? = null
    public var path: Matcher<String>? = null
    public val headers: MutableList<Matcher<Headers>> = mutableListOf()
    public val body: MutableList<Matcher<P?>> = mutableListOf()
    public val bodyString: MutableList<Matcher<String?>> = mutableListOf()
    public var priority: Int = DEFAULT_STUB_PRIORITY

    public fun method(matcher: Matcher<HttpMethod>): RequestSpecificationBuilder<P> =
        apply {
            this.method = matcher
        }

    public fun path(matcher: Matcher<String>): RequestSpecificationBuilder<P> =
        apply {
            this.path = matcher
        }

    public fun path(pathString: String): RequestSpecificationBuilder<P> =
        apply {
            this.path = pathEqual(pathString)
        }

    public fun bodyContains(vararg strings: String): RequestSpecificationBuilder<P> =
        apply {
            strings.forEach { this.bodyString += contain(it) }
        }

    /**
     * Adds a predicate to match against the request body.
     *
     * The specified predicate will be used to evaluate whether the request body satisfies the defined condition.
     *
     * @param description The predicate's description. Returned as `toString()` value
     * @param predicate The predicate used to evaluate the request body. It defines the condition
     *                  that the request body must satisfy.
     * @return The same instance of [RequestSpecificationBuilder] with the predicate applied
     *         for further customization.
     */
    @JvmOverloads
    public fun bodyMatchesPredicate(
        description: String? = null,
        predicate: (P?) -> Boolean,
    ): RequestSpecificationBuilder<P> =
        apply {
            this.body +=
                predicateMatcher(
                    description = description,
                    predicate = predicate,
                )
        }

    /**
     * Adds multiple predicates to match against the request body.
     * Each predicate will be applied to evaluate whether the request body satisfies the specified conditions.
     *
     * @param predicate A variable number of predicates to evaluate the request body.
     *                  All predicates are added as matchers for the request body.
     * @return The same instance of [RequestSpecificationBuilder] with the predicates applied
     *         for further customization.
     */
    public fun bodyMatchesPredicates(
        vararg predicate: (P?) -> Boolean,
    ): RequestSpecificationBuilder<P> =
        apply {
            predicate.forEach { bodyMatchesPredicate(predicate = it) }
        }

    public fun containsHeader(
        headerName: String,
        headerValue: String,
    ): RequestSpecificationBuilder<P> =
        apply {
            headers +=
                dev.mokksy.mokksy.request
                    .containsHeader(headerName, headerValue)
        }

    public fun bodyString(matcher: Matcher<String?>): RequestSpecificationBuilder<P> =
        apply {
            this.bodyString += matcher
        }

    /**
     * Sets the priority for this stub. Higher values indicate higher priority.
     *
     * Priority is a tiebreaker: it only applies when two stubs match with equal specificity scores.
     *
     * Example:
     * ```kotlin
     * // Higher-priority stub — wins over default-priority stubs when scores tie
     * mokksy.post { path("/api"); priority(1) }
     *
     * // Lower-priority fallback — selected only when no higher-priority stub with the same score matches
     * mokksy.post { path("/api"); priority(-1) }
     * ```
     *
     * @see [RequestSpecification.priority]
     */
    public fun priority(value: Int): RequestSpecificationBuilder<P> =
        apply {
            this.priority = value
        }

    internal fun build(): RequestSpecification<P> =
        RequestSpecification(
            method = method,
            path = path,
            headers = headers,
            requestType = requestType,
            body = body,
            bodyString = bodyString,
            priority = priority,
        )
}
