package dev.mokksy.mokksy.request

import io.kotest.matchers.Matcher
import io.kotest.matchers.equals.beEqual
import io.ktor.http.ContentType
import kotlin.jvm.JvmSynthetic

/**
 * Base builder for matching part content type and content.
 *
 * Used by [FilePartSpecBuilder] and [DataPartSpecBuilder].
 *
 * Example:
 * ```kotlin
 * FilePartSpecBuilder("avatar").apply {
 *     contentType("image/png")
 *     text("expected content")
 * }
 * ```
 */
@Suppress("TooManyFunctions", "AbstractClassCanBeConcreteClass")
public abstract class AbstractDataPartSpecBuilder<T : AbstractDataPartSpecBuilder<T>>
    internal constructor() {
        private var contentTypeMatcher: Matcher<ContentType?>? = null
        private val contentMatchers: MutableList<ContentMatcher> = mutableListOf()

        /**
         * Requires the part content type to equal [contentType].
         */
        @JvmSynthetic
        public fun contentType(contentType: String): T =
            self { contentType(ContentType.parse(contentType)) }

        /**
         * Requires the part content type to equal [contentType].
         */
        @JvmSynthetic
        public fun contentType(contentType: ContentType): T =
            self {
                contentTypeMatcher =
                    beEqual(contentType)
            }

        /**
         * Requires the part content type to satisfy [matcher].
         */
        @JvmSynthetic
        public fun contentType(matcher: Matcher<ContentType?>): T =
            self { contentTypeMatcher = matcher }

        /**
         * Requires the part content decoded as UTF-8 text to equal [value].
         */
        @JvmSynthetic
        public fun text(value: String): T = self { text(beEqual(value)) }

        /**
         * Requires the part content decoded as UTF-8 text to satisfy [predicate].
         */
        @JvmSynthetic
        public fun text(predicate: (String?) -> Boolean): T =
            self { text(predicateMatcher(predicate = predicate)) }

        /**
         * Requires the part content decoded as UTF-8 text to satisfy a Kotest [matcher].
         */
        @JvmSynthetic
        public fun text(matcher: Matcher<String?>): T =
            self { contentMatchers += StringContentMatcher(matcher) }

        /**
         * Requires the part content bytes to equal [value].
         */
        @JvmSynthetic
        public fun bytes(value: ByteArray): T = self { bytes(byteArrayEqual(value)) }

        /**
         * Requires the part content bytes to satisfy [predicate].
         */
        @JvmSynthetic
        public fun bytes(predicate: (ByteArray?) -> Boolean): T =
            self { bytes(predicateMatcher(predicate = predicate)) }

        /**
         * Requires the part content bytes to satisfy a Kotest [matcher].
         */
        @JvmSynthetic
        public fun bytes(matcher: Matcher<ByteArray?>): T =
            self { contentMatchers += ByteArrayContentMatcher(matcher) }

        @Suppress("UNCHECKED_CAST")
        private fun self(block: T.() -> Unit): T {
            val typed = this as T
            typed.block()
            return typed
        }

        internal fun builtContentTypeMatcher(): Matcher<ContentType?>? = contentTypeMatcher

        internal fun builtContentMatchers(): List<ContentMatcher> = contentMatchers.toList()
    }
