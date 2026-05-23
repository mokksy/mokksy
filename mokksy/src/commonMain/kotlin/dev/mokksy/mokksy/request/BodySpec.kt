@file:OptIn(ExperimentalMokksyApi::class)

package dev.mokksy.mokksy.request

import dev.mokksy.mokksy.ExperimentalMokksyApi
import io.kotest.matchers.Matcher
import io.ktor.http.ContentType

/**
 * Selects which HTML form encoding a [BodySpecBuilder.form] matcher accepts.
 *
 * Use [AUTO] for regular tests; select [URL_ENCODED] or [MULTIPART] when a stub must reject the other form encoding.
 */
public enum class FormEncoding {
    /**
     * Accepts either `application/x-www-form-urlencoded` or `multipart/form-data`.
     */
    AUTO,

    /**
     * Accepts only `application/x-www-form-urlencoded`.
     */
    URL_ENCODED,

    /**
     * Accepts only `multipart/form-data`.
     */
    MULTIPART,
}

internal enum class BodyPartKind {
    FIELD,
    FILE,
    PART,
}

internal data class BodyPartSpec(
    val name: String,
    val kind: BodyPartKind,
    val filenameMatcher: Matcher<String?>? = null,
    val contentTypeMatcher: Matcher<ContentType?>? = null,
    val contentMatchers: List<ContentMatcher> = emptyList(),
)

internal data class FormBodySpec(
    val encoding: FormEncoding,
    val parts: List<BodyPartSpec>,
)

internal data class MultipartBodySpec(
    val contentType: ContentType,
    val boundaryMatcher: Matcher<String?>? = null,
    val parts: List<BodyPartSpec>,
)

internal data class ByteBodySpec(
    val contentTypeMatcher: Matcher<ContentType?>? = null,
    val contentMatchers: List<ContentMatcher>,
)
