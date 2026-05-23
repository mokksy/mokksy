@file:OptIn(ExperimentalMokksyApi::class)

package dev.mokksy.mokksy.request

import dev.mokksy.mokksy.ExperimentalMokksyApi
import dev.mokksy.mokksy.MokksyDsl
import io.kotest.matchers.Matcher
import io.kotest.matchers.equals.beEqual
import io.ktor.http.ContentType
import kotlin.jvm.JvmSynthetic

/**
 * Configures request body matching inside [RequestSpecificationBuilder.body].
 *
 * Example:
 * ```kotlin
 * body {
 *     form {
 *         field("user", "alice")
 *         file("avatar") { filename { it?.endsWith(".png") == true } }
 *     }
 *     bytes { it?.isNotEmpty() == true }
 * }
 * ```
 */
@MokksyDsl
@Suppress("TooManyFunctions")
public class BodySpecBuilder<P : Any> internal constructor() {
    private val formSpecs: MutableList<FormBodySpec> = mutableListOf()
    private val multipartSpecs: MutableList<MultipartBodySpec> = mutableListOf()
    private val byteContentMatchers: MutableList<ContentMatcher> = mutableListOf()
    private val predicateMatchers: MutableList<Matcher<P?>> = mutableListOf()
    private var contentTypeMatcher: Matcher<ContentType?>? = null

    /**
     * Matches an HTML form body.
     *
     * [FormEncoding.AUTO] accepts both URL-encoded forms and `multipart/form-data`.
     */
    @JvmSynthetic
    public fun form(
        encoding: FormEncoding = FormEncoding.AUTO,
        block: FormSpecBuilder.() -> Unit,
    ) {
        formSpecs += FormBodySpec(encoding, FormSpecBuilder().apply(block).build())
    }

    /**
     * Matches a multipart request by content type string, such as `"multipart/mixed"`.
     */
    @JvmSynthetic
    public fun multipart(
        contentType: String,
        block: MultipartSpecBuilder.() -> Unit,
    ) {
        multipart(ContentType.parse(contentType), block)
    }

    /**
     * Matches a multipart request by [ContentType].
     */
    @JvmSynthetic
    public fun multipart(
        contentType: ContentType,
        block: MultipartSpecBuilder.() -> Unit,
    ) {
        val builder = MultipartSpecBuilder(contentType).apply(block)
        multipartSpecs += builder.build()
    }

    /**
     * Matches the raw request body text exactly.
     */
    @JvmSynthetic
    public fun text(value: String) {
        text(beEqual(value))
    }

    /**
     * Matches the raw request body text using [predicate].
     */
    @JvmSynthetic
    public fun text(predicate: (String?) -> Boolean) {
        text(predicateMatcher(predicate = predicate))
    }

    /**
     * Matches the raw request body text using a Kotest [Matcher].
     */
    @JvmSynthetic
    public fun text(matcher: Matcher<String?>) {
        byteContentMatchers += StringContentMatcher(matcher)
    }

    /**
     * Matches the raw request body bytes exactly.
     */
    @JvmSynthetic
    public fun bytes(value: ByteArray) {
        bytes(byteArrayEqual(value))
    }

    /**
     * Matches the raw request body bytes using [predicate].
     */
    @JvmSynthetic
    public fun bytes(predicate: (ByteArray?) -> Boolean) {
        bytes(predicateMatcher(predicate = predicate))
    }

    /**
     * Matches the raw request body bytes using a Kotest [Matcher].
     */
    @JvmSynthetic
    public fun bytes(matcher: Matcher<ByteArray?>) {
        byteContentMatchers += ByteArrayContentMatcher(matcher)
    }

    /**
     * Requires the request body content type to equal [contentType].
     */
    @JvmSynthetic
    public fun contentType(contentType: String) {
        contentType(ContentType.parse(contentType))
    }

    /**
     * Requires the request body content type to equal [contentType].
     */
    @JvmSynthetic
    public fun contentType(contentType: ContentType) {
        contentTypeMatcher = beEqual(contentType)
    }

    /**
     * Requires the request body content type to satisfy [matcher].
     */
    @JvmSynthetic
    public fun contentType(matcher: Matcher<ContentType?>) {
        contentTypeMatcher = matcher
    }

    /**
     * Adds a typed request-body predicate in the same body block.
     *
     * [description] is used in mismatch diagnostics when provided.
     */
    @JvmSynthetic
    public fun predicate(
        description: String? = null,
        predicate: (P?) -> Boolean,
    ) {
        predicateMatchers += predicateMatcher(description = description, predicate = predicate)
    }

    internal fun build(): BodyBuilderResult<P> =
        BodyBuilderResult(
            formSpecs = formSpecs.toList(),
            multipartSpecs = multipartSpecs.toList(),
            byteBodySpecs =
                if (byteContentMatchers.isEmpty() && contentTypeMatcher == null) {
                    emptyList()
                } else {
                    listOf(
                        ByteBodySpec(
                            contentTypeMatcher = contentTypeMatcher,
                            contentMatchers = byteContentMatchers.toList(),
                        ),
                    )
                },
            predicateMatchers = predicateMatchers.toList(),
        )
}

/**
 * Configures form field and file-part matching for [BodySpecBuilder.form].
 */
@MokksyDsl
public class FormSpecBuilder internal constructor() {
    private val parts: MutableList<BodyPartSpec> = mutableListOf()

    /**
     * Requires field [name] to have exactly [value].
     */
    @JvmSynthetic
    public fun field(
        name: String,
        value: String,
    ): FormSpecBuilder = apply { field(name, beEqual(value)) }

    /**
     * Requires field [name] to satisfy [predicate].
     */
    @JvmSynthetic
    public fun field(
        name: String,
        predicate: (String?) -> Boolean,
    ): FormSpecBuilder = apply { field(name, predicateMatcher(predicate = predicate)) }

    /**
     * Requires field [name] to satisfy a Kotest [matcher].
     */
    @JvmSynthetic
    public fun field(
        name: String,
        matcher: Matcher<String?>,
    ): FormSpecBuilder =
        apply {
            parts +=
                BodyPartSpec(
                    name = name,
                    kind = BodyPartKind.FIELD,
                    contentMatchers = listOf(StringContentMatcher(matcher)),
                )
        }

    /**
     * Requires a file part named [name] and optionally configures file metadata/content matchers.
     */
    @JvmSynthetic
    public fun file(
        name: String,
        block: FilePartSpecBuilder.() -> Unit,
    ): FormSpecBuilder =
        apply {
            parts += FilePartSpecBuilder(name).apply(block).build()
        }

    internal fun build(): List<BodyPartSpec> = parts.toList()
}

/**
 * Configures matching for non-form multipart bodies.
 */
@MokksyDsl
public class MultipartSpecBuilder internal constructor(
    private val contentType: ContentType,
) {
    private val parts: MutableList<BodyPartSpec> = mutableListOf()
    private var boundaryMatcher: Matcher<String?>? = null

    /**
     * Requires the multipart boundary to equal [value].
     */
    @JvmSynthetic
    public fun boundary(value: String): MultipartSpecBuilder =
        apply {
            boundaryMatcher =
                beEqual(value)
        }

    /**
     * Requires the multipart boundary to satisfy [predicate].
     */
    @JvmSynthetic
    public fun boundary(predicate: (String?) -> Boolean): MultipartSpecBuilder =
        apply { boundaryMatcher = predicateMatcher(predicate = predicate) }

    /**
     * Requires a multipart part named [name] and configures its metadata/content matchers.
     */
    @JvmSynthetic
    public fun part(
        name: String,
        block: DataPartSpecBuilder.() -> Unit,
    ): MultipartSpecBuilder =
        apply {
            parts += DataPartSpecBuilder(name).apply(block).build()
        }

    internal fun build(): MultipartBodySpec =
        MultipartBodySpec(
            contentType = contentType,
            boundaryMatcher = boundaryMatcher,
            parts = parts.toList(),
        )
}

/**
 * Configures matching for a file part inside [FormSpecBuilder.file].
 */
@MokksyDsl
public class FilePartSpecBuilder internal constructor(
    private val name: String,
) : AbstractDataPartSpecBuilder<FilePartSpecBuilder>() {
    private var filenameMatcher: Matcher<String?>? = null

    /**
     * Requires the uploaded filename to equal [value].
     */
    @JvmSynthetic
    public fun filename(value: String): FilePartSpecBuilder =
        apply {
            filenameMatcher =
                beEqual(value)
        }

    /**
     * Requires the uploaded filename to satisfy [predicate].
     */
    @JvmSynthetic
    public fun filename(predicate: (String?) -> Boolean): FilePartSpecBuilder =
        apply { filenameMatcher = predicateMatcher(predicate = predicate) }

    /**
     * Requires the uploaded filename to satisfy a Kotest [matcher].
     */
    @JvmSynthetic
    public fun filename(matcher: Matcher<String?>): FilePartSpecBuilder =
        apply { filenameMatcher = matcher }

    internal fun build(): BodyPartSpec =
        BodyPartSpec(
            name = name,
            kind = BodyPartKind.FILE,
            filenameMatcher = filenameMatcher,
            contentTypeMatcher = builtContentTypeMatcher(),
            contentMatchers = builtContentMatchers(),
        )
}

/**
 * Configures matching for a multipart data part inside [MultipartSpecBuilder.part].
 */
@MokksyDsl
public class DataPartSpecBuilder internal constructor(
    private val name: String,
) : AbstractDataPartSpecBuilder<DataPartSpecBuilder>() {
    internal fun build(): BodyPartSpec =
        BodyPartSpec(
            name = name,
            kind = BodyPartKind.PART,
            contentTypeMatcher = builtContentTypeMatcher(),
            contentMatchers = builtContentMatchers(),
        )
}

internal data class BodyBuilderResult<P : Any>(
    val formSpecs: List<FormBodySpec>,
    val multipartSpecs: List<MultipartBodySpec>,
    val byteBodySpecs: List<ByteBodySpec>,
    val predicateMatchers: List<Matcher<P?>>,
)
