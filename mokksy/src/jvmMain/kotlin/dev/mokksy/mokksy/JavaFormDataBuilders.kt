@file:OptIn(ExperimentalMokksyApi::class)

package dev.mokksy.mokksy

import dev.mokksy.mokksy.request.FormDataPartSpec
import dev.mokksy.mokksy.request.RequestSpecificationBuilder
import dev.mokksy.mokksy.request.predicateMatcher
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.ktor.http.ContentType
import java.util.function.Consumer
import java.util.function.Predicate

/**
 * Java-friendly `body` block equivalent.
 *
 * Wraps [RequestSpecificationBuilder] and lets Java callers configure body matching
 * without exposing Kotlin function types or Ktor/Kotest types.
 *
 * Obtained via [JavaRequestSpecificationBuilder.body].
 */
public class JavaBodySpecBuilder<P : Any> internal constructor(
    private val delegate: RequestSpecificationBuilder<P>,
) {
    /**
     * Configures multipart/form-data matching.
     *
     * @param configurer A [Consumer] that configures form-data fields and files.
     * @return This builder for chaining.
     */
    public fun formData(configurer: Consumer<JavaFormDataSpecBuilder>): JavaBodySpecBuilder<P> {
        val builder = JavaFormDataSpecBuilder()
        configurer.accept(builder)
        delegate.addFormDataPartSpecs(builder.build())
        return this
    }

    /**
     * Adds a predicate to match against the deserialized request body.
     *
     * Unlike the Kotlin overload, this method accepts a [Predicate] so Java callers
     * can pass a lambda directly without dealing with `Function1` SAM types.
     *
     * @param predicate A [Predicate] applied to the deserialized body, which is guaranteed non-null.
     * @return This builder for chaining.
     */
    public fun predicate(predicate: Predicate<P>): JavaBodySpecBuilder<P> {
        delegate.addBodyMatcher(
            predicateMatcher(description = null, predicate = { it != null && predicate.test(it) }),
        )
        return this
    }
}

/**
 * Java-friendly [dev.mokksy.mokksy.request.FormDataSpecBuilder] equivalent.
 *
 * Builds form-data matching criteria for Java callers.
 */
public class JavaFormDataSpecBuilder internal constructor() {
    private val parts: MutableList<FormDataPartSpec> = mutableListOf()

    /**
     * Matches a form-data field with the given [name] against the exact [expectedValue].
     *
     * @param name The form-data field name.
     * @param expectedValue The exact expected value.
     * @return This builder for chaining.
     */
    public fun field(
        name: String,
        expectedValue: String,
    ): JavaFormDataSpecBuilder {
        parts +=
            FormDataPartSpec(
                name = name,
                bodyMatchers = listOf(exactStringMatcher(expectedValue)),
            )
        return this
    }

    /**
     * Matches a form-data field's value against a [Predicate].
     *
     * @param name The form-data field name.
     * @param predicate A predicate applied to the field's value string.
     * @return This builder for chaining.
     */
    public fun fieldMatches(
        name: String,
        predicate: Predicate<String?>,
    ): JavaFormDataSpecBuilder {
        parts +=
            FormDataPartSpec(
                name = name,
                bodyMatchers =
                    listOf(
                        predicateMatcher(description = null, predicate = { predicate.test(it) }),
                    ),
            )
        return this
    }

    /**
     * Configures file-part matching for the given [name].
     *
     * @param name The form-data field name.
     * @param configurer A [Consumer] that sets filename/content-type expectations.
     * @return This builder for chaining.
     */
    public fun file(
        name: String,
        configurer: Consumer<JavaFormDataFileSpecBuilder>,
    ): JavaFormDataSpecBuilder {
        val builder = JavaFormDataFileSpecBuilder(name)
        configurer.accept(builder)
        parts += builder.build()
        return this
    }

    internal fun build(): List<FormDataPartSpec> = parts.toList()
}

/**
 * Java-friendly [dev.mokksy.mokksy.request.FormDataFileSpecBuilder] equivalent.
 *
 * Sets filename and content-type expectations for a file part.
 */
public class JavaFormDataFileSpecBuilder internal constructor(
    private val name: String,
) {
    private var filenameMatcher: Matcher<String?>? = null
    private var contentTypeMatcher: Matcher<ContentType?>? = null
    private var bodyMatchers: List<Matcher<String?>> = emptyList()

    /**
     * Expects the file part to have the given [expectedFilename].
     *
     * @param expectedFilename The expected filename.
     * @return This builder for chaining.
     */
    public fun filename(expectedFilename: String): JavaFormDataFileSpecBuilder {
        filenameMatcher = exactStringMatcher(expectedFilename)
        return this
    }

    /**
     * Expects the file part to have the given content type.
     *
     * @param contentType The expected content type, e.g. `"image/jpeg"`.
     * @return This builder for chaining.
     */
    public fun contentType(contentType: String): JavaFormDataFileSpecBuilder {
        contentTypeMatcher =
            object : Matcher<ContentType?> {
                override fun test(value: ContentType?): MatcherResult {
                    val expected = ContentType.parse(contentType)
                    return MatcherResult(
                        value == expected,
                        { "expected content type \"$expected\" but got \"$value\"" },
                        { "expected content type not \"$expected\"" },
                    )
                }
            }
        return this
    }

    /**
     * Expects the file part body content to equal [expectedContent].
     *
     * @param expectedContent The exact expected body content as a string.
     * @return This builder for chaining.
     */
    public fun body(expectedContent: String): JavaFormDataFileSpecBuilder {
        bodyMatchers = listOf(exactStringMatcher(expectedContent))
        return this
    }

    /**
     * Matches the file part body content against a [Predicate].
     *
     * @param predicate A predicate applied to the file body content string.
     * @return This builder for chaining.
     */
    public fun bodyMatches(predicate: Predicate<String?>): JavaFormDataFileSpecBuilder {
        bodyMatchers =
            listOf(
                predicateMatcher(description = null, predicate = { predicate.test(it) }),
            )
        return this
    }

    internal fun build(): FormDataPartSpec =
        FormDataPartSpec(
            name = name,
            filenameMatcher = filenameMatcher,
            contentTypeMatcher = contentTypeMatcher,
            bodyMatchers = bodyMatchers,
        )
}

/**
 * Adds form-data part specs to the underlying request specification.
 * Exposed as an internal helper so [JavaBodySpecBuilder] can access it.
 */
internal fun <P : Any> RequestSpecificationBuilder<P>.addFormDataPartSpecs(
    specs: List<FormDataPartSpec>,
) {
    formDataPartSpecs += specs
}

/**
 * Adds a body matcher to the underlying request specification.
 * Exposed as an internal helper so [JavaBodySpecBuilder] can access it.
 */
internal fun <P : Any> RequestSpecificationBuilder<P>.addBodyMatcher(matcher: Matcher<P?>) {
    body += matcher
}

private fun exactStringMatcher(expected: String): Matcher<String?> =
    object : Matcher<String?> {
        override fun test(value: String?): MatcherResult =
            MatcherResult(
                value == expected,
                { "expected \"$expected\" but got \"$value\"" },
                { "expected not \"$expected\"" },
            )

        override fun toString(): String = "equalTo(\"$expected\")"
    }
