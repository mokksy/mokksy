package dev.mokksy.mokksy

import dev.mokksy.mokksy.request.RequestSpecificationBuilder
import io.kotest.matchers.Matcher
import java.util.function.Predicate

/**
 * Java-friendly wrapper around [RequestSpecificationBuilder] that replaces Kotlin functional
 * types with their `java.util.function` equivalents.
 *
 * Java callers receive this type from [dev.mokksy.Mokksy]'s stub-registration methods
 * (`get`, `post`, etc.) and use it to configure request-matching criteria.
 *
 * All methods return `this` for fluent chaining:
 * ```java
 * mokksy.post(spec -> spec
 *     .path("/items")
 *     .bodyMatchesPredicate(body -> body.contains("token")))
 *   .respondsWith(builder -> builder.body("ok"));
 * ```
 *
 * @param P The type of the request payload.
 */
public class JavaRequestSpecificationBuilder<P : Any> internal constructor(
    private val delegate: RequestSpecificationBuilder<P>,
) {
    /**
     * Matches the request path exactly against [path].
     *
     * @param path The expected request path, e.g. `"/api/items"`.
     * @return This builder instance.
     */
    public fun path(path: String): JavaRequestSpecificationBuilder<P> =
        apply { delegate.path(path) }

    /**
     * Matches the request path using a Kotest [Matcher].
     *
     * @param matcher A [Matcher] applied to the request path string.
     * @return This builder instance.
     */
    public fun path(matcher: Matcher<String>): JavaRequestSpecificationBuilder<P> =
        apply { delegate.path(matcher) }

    /**
     * Requires the request body string to contain all of [strings].
     *
     * @param strings One or more substrings that must appear in the request body.
     * @return This builder instance.
     */
    public fun bodyContains(vararg strings: String): JavaRequestSpecificationBuilder<P> =
        apply { delegate.bodyContains(*strings) }

    /**
     * Adds a predicate to match against the deserialized request body.
     *
     * Unlike the Kotlin overload, this method accepts a [Predicate] so Java callers
     * can pass a lambda directly without dealing with `Function1` SAM types.
     *
     * @param predicate A [Predicate] applied to the deserialized body, which is guaranteed non-null.
     * @return This builder instance.
     */
    public fun bodyMatchesPredicate(predicate: Predicate<P>): JavaRequestSpecificationBuilder<P> =
        apply { delegate.bodyMatchesPredicate { it != null && predicate.test(it) } }

    /**
     * Adds a predicate to match against the deserialized request body, with a description.
     *
     * @param description Human-readable label shown in mismatch reports.
     * @param predicate A [Predicate] applied to the deserialized body, which is guaranteed non-null.
     * @return This builder instance.
     */
    public fun bodyMatchesPredicate(
        description: String?,
        predicate: Predicate<P>,
    ): JavaRequestSpecificationBuilder<P> =
        apply { delegate.bodyMatchesPredicate(description) { it != null && predicate.test(it) } }

    /**
     * Requires the request to contain a header with [name] equal to [value].
     *
     * @param name The header name.
     * @param value The expected header value.
     * @return This builder instance.
     */
    public fun containsHeader(
        name: String,
        value: String,
    ): JavaRequestSpecificationBuilder<P> = apply { delegate.containsHeader(name, value) }

    /**
     * Sets the priority for this stub. Lower values win over higher values.
     *
     * @param value Priority value; lower means higher precedence.
     * @return This builder instance.
     */
    public fun priority(value: Int): JavaRequestSpecificationBuilder<P> =
        apply { delegate.priority(value) }
}
