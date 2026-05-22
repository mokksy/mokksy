package dev.mokksy.mokksy.request

import kotlin.jvm.JvmSynthetic
import io.kotest.matchers.Matcher as KotestMatcher

/**
 * A type-safe content matcher for multipart form-data parts.
 *
 * Wraps a Kotest [io.kotest.matchers.Matcher] and handles the appropriate content type conversion internally.
 * Use [StringContentMatcher] for text content and [ByteArrayContentMatcher] for raw byte content.
 *
 * @see FormSpecBuilder.field
 * @see FilePartSpecBuilder.text
 * @see FilePartSpecBuilder.bytes
 */
internal sealed class ContentMatcher {
    /**
     * Evaluates this matcher against the given [value].
     *
     * @param value The content value — either a [String] or [ByteArray] depending on the subclass.
     * @return `true` if the value matches the wrapped matcher's criteria.
     */
    abstract fun matches(value: Any?): Boolean

    @JvmSynthetic
    protected fun <T> KotestMatcher<T>.matchesValue(value: T): Boolean = test(value).passed()
}

/**
 * A [ContentMatcher] that matches string content.
 *
 * Accepts both [String] and [ByteArray] inputs — `ByteArray` is decoded as UTF-8.
 *
 * @param matcher A Kotest [io.kotest.matchers.Matcher] applied to the content as a string.
 */
internal class StringContentMatcher
    internal constructor(
        private val matcher: KotestMatcher<String?>,
    ) : ContentMatcher() {
        override fun matches(value: Any?): Boolean {
            val content = value.asStringOrNull() ?: return false
            return matcher.matchesValue(content)
        }

        private fun Any?.asStringOrNull(): String? =
            when (this) {
                is String -> this
                is ByteArray -> decodeToString()
                else -> null
            }
    }

/**
 * A [ContentMatcher] that matches raw byte content.
 *
 * Only accepts [ByteArray] input. Use this for binary file matching
 * when string decoding is not appropriate.
 *
 * @param matcher A Kotest [io.kotest.matchers.Matcher] applied to the raw byte content.
 */
internal class ByteArrayContentMatcher
    internal constructor(
        private val matcher: KotestMatcher<ByteArray?>,
    ) : ContentMatcher() {
        override fun matches(value: Any?): Boolean {
            val content = value as? ByteArray ?: return false
            return matcher.matchesValue(content)
        }
    }
