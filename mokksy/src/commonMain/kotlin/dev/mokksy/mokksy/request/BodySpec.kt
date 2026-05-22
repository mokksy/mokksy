@file:OptIn(ExperimentalMokksyApi::class)

package dev.mokksy.mokksy.request

import dev.mokksy.mokksy.ExperimentalMokksyApi
import dev.mokksy.mokksy.MokksyDsl
import io.kotest.matchers.Matcher
import io.kotest.matchers.equals.beEqual
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.log
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.contentType
import io.ktor.server.request.receiveChannel
import io.ktor.server.request.receiveMultipart
import io.ktor.server.request.receiveParameters
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.CancellationException
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlin.jvm.JvmSynthetic

private const val MAX_PART_SIZE: Long = 65535

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

internal suspend fun scoreFormMatchers(
    request: ApplicationRequest,
    formSpecs: List<FormBodySpec>,
): Pair<Int, List<String>> {
    var score = 0
    val failed = mutableListOf<String>()

    formSpecs.forEachIndexed { index, spec ->
        val (partScore, partFailed) = scoreFormMatcher(request, spec, "form[$index]")
        score += partScore
        failed += partFailed
    }

    return score to failed
}

private suspend fun scoreFormMatcher(
    request: ApplicationRequest,
    spec: FormBodySpec,
    label: String,
): Pair<Int, List<String>> {
    val contentType = request.contentType()
    return when {
        spec.encoding != FormEncoding.URL_ENCODED &&
            contentType.match(ContentType.MultiPart.FormData) -> {
            scoreMultipartParts(request, spec.parts, label)
        }

        spec.encoding != FormEncoding.MULTIPART &&
            contentType.match(ContentType.Application.FormUrlEncoded) -> {
            scoreUrlEncodedParts(request, spec.parts, label)
        }

        else -> {
            0 to spec.parts.indices.map { "$label[$it]" }
        }
    }
}

internal suspend fun scoreMultipartMatchers(
    request: ApplicationRequest,
    multipartSpecs: List<MultipartBodySpec>,
): Pair<Int, List<String>> {
    var score = 0
    val failed = mutableListOf<String>()

    multipartSpecs.forEachIndexed { index, spec ->
        val label = "multipart[$index]"
        val contentType = request.contentType()
        val boundaryPassed =
            spec.boundaryMatcher
                ?.test(contentType.parameter("boundary"))
                ?.passed() != false
        if (!contentType.match(spec.contentType) || !boundaryPassed) {
            failed += spec.parts.indices.map { "$label[$it]" }
        } else {
            val (partScore, partFailed) = scoreMultipartParts(request, spec.parts, label)
            score += partScore
            failed += partFailed
        }
    }

    return score to failed
}

internal suspend fun scoreByteBodyMatchers(
    request: ApplicationRequest,
    byteBodySpecs: List<ByteBodySpec>,
): Pair<Int, List<String>> {
    if (byteBodySpecs.isEmpty()) return 0 to emptyList()

    val body =
        @Suppress("TooGenericExceptionCaught")
        try {
            request.call
                .receiveChannel()
                .readRemaining()
                .readByteArray()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            request.call.application.log
                .trace("Unable to read raw body for matching: ${e.message}")
            null
        }

    var score = 0
    val failed = mutableListOf<String>()
    val contentType = request.headers[HttpHeaders.ContentType]?.let(ContentType::parse)

    byteBodySpecs.forEachIndexed { index, spec ->
        val label = "bytes[$index]"
        if (body == null) {
            failed += label
            return@forEachIndexed
        }
        if (spec.contentTypeMatcher != null &&
            !spec.contentTypeMatcher.test(contentType).passed()
        ) {
            failed += label
            return@forEachIndexed
        }
        if (spec.contentMatchers.all { it.matches(body) }) {
            score++
        } else {
            failed += label
        }
    }

    return score to failed
}

@Suppress("TooGenericExceptionCaught")
private suspend fun scoreUrlEncodedParts(
    request: ApplicationRequest,
    specs: List<BodyPartSpec>,
    label: String,
): Pair<Int, List<String>> {
    val parameters =
        try {
            request.call.receiveParameters()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            request.call.application.log
                .trace("Unable to receive form parameters: ${e.message}")
            return 0 to specs.indices.map { "$label[$it]" }
        }

    var score = 0
    val failed = mutableListOf<String>()
    specs.forEachIndexed { index, spec ->
        if (matchesUrlEncodedPart(parameters, spec)) {
            score++
        } else {
            failed += "$label[$index]"
        }
    }

    return score to failed
}

@Suppress("TooGenericExceptionCaught", "ReturnCount")
private suspend fun scoreMultipartParts(
    request: ApplicationRequest,
    specs: List<BodyPartSpec>,
    label: String,
): Pair<Int, List<String>> {
    val multipart =
        try {
            request.call.receiveMultipart()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            request.call.application.log
                .trace("Unable to receive multipart body for matching: ${e.message}")
            return 0 to specs.indices.map { "$label[$it]" }
        }

    val parts = mutableListOf<PartData>()
    try {
        multipart.forEachPart { parts.add(it) }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        request.call.application.log
            .trace("Unable to read multipart parts: ${e.message}")
        return 0 to specs.indices.map { "$label[$it]" }
    }

    var score = 0
    val failed = mutableListOf<String>()
    try {
        specs.forEachIndexed { index, spec ->
            val matchingParts =
                parts.filter { part ->
                    val partName = part.name ?: part.contentDisposition?.parameter("name")
                    partName == spec.name
                }
            if (matchingParts.any { part -> matchMultipartPart(part, spec) }) {
                score++
            } else {
                failed += "$label[$index]"
            }
        }
    } finally {
        parts.forEach { it.dispose() }
    }

    return score to failed
}

@Suppress("ReturnCount")
private suspend fun matchMultipartPart(
    part: PartData,
    spec: BodyPartSpec,
): Boolean {
    if (!matchesPartKind(part, spec.kind)) return false
    if (spec.filenameMatcher != null) {
        val filename =
            when (part) {
                is PartData.FileItem -> part.originalFileName
                else -> part.contentDisposition?.parameter("filename")
            }
        if (!spec.filenameMatcher.test(filename).passed()) return false
    }
    if (spec.contentTypeMatcher != null &&
        !spec.contentTypeMatcher.test(part.contentType).passed()
    ) {
        return false
    }
    if (spec.contentMatchers.isNotEmpty()) {
        val content = readPartBytes(part) ?: return false
        if (!spec.contentMatchers.all { it.matches(content) }) return false
    }
    return true
}

private fun matchesPartKind(
    part: PartData,
    kind: BodyPartKind,
): Boolean =
    when (kind) {
        BodyPartKind.FIELD -> part is PartData.FormItem
        BodyPartKind.FILE -> part !is PartData.FormItem
        BodyPartKind.PART -> true
    }

private suspend fun readPartBytes(part: PartData): ByteArray? =
    when (part) {
        is PartData.FormItem -> {
            part.value.encodeToByteArray()
        }

        is PartData.FileItem -> {
            part
                .provider()
                .readRemaining()
                .readByteArray()
        }

        is PartData.BinaryItem -> {
            val buffer = Buffer()
            part.provider().readAtMostTo(buffer, MAX_PART_SIZE)
            buffer.readByteArray()
        }

        is PartData.BinaryChannelItem -> {
            part
                .provider()
                .readRemaining()
                .readByteArray()
        }
    }
