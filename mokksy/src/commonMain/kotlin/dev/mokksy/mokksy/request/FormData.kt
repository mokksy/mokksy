package dev.mokksy.mokksy.request

import dev.mokksy.mokksy.ExperimentalMokksyApi
import dev.mokksy.mokksy.MokksyDsl
import io.kotest.matchers.Matcher
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.log
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.receiveMultipart
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.CancellationException
import kotlinx.io.Buffer
import kotlinx.io.readByteArray

private const val MAX_PART_SIZE: Long = 65535

internal data class FormDataPartSpec(
    val name: String,
    val filenameMatcher: Matcher<String?>? = null,
    val contentTypeMatcher: Matcher<ContentType?>? = null,
    val bodyMatchers: List<Matcher<String?>> = emptyList(),
)

/**
 * Builds form-data matching criteria within a `body` block.
 *
 * Use [field] to match text fields and [file] to match file uploads.
 * Each call adds a new part specification to the matching criteria.
 *
 * Obtained via [BodySpecBuilder.formData].
 */
@MokksyDsl
public class FormDataSpecBuilder internal constructor() {
    private val parts: MutableList<FormDataPartSpec> = mutableListOf()

    /**
     * Matches a form-data text field with the given [name] using the provided [bodyMatcher].
     *
     * @param name The form-data field name (the `name` attribute in Content-Disposition).
     * @param bodyMatcher A Kotest [Matcher] applied to the field's value string.
     */
    @ExperimentalMokksyApi
    public fun field(
        name: String,
        bodyMatcher: Matcher<String?>,
    ) {
        parts += FormDataPartSpec(name = name, bodyMatchers = listOf(bodyMatcher))
    }

    /**
     * Matches a form-data text field with the given [name] using a DSL builder block.
     *
     * @param name The form-data field name (the `name` attribute in Content-Disposition).
     * @param block A [FormDataFieldSpecBuilder] block for configuring body matchers.
     */
    @ExperimentalMokksyApi
    public fun field(
        name: String,
        block: FormDataFieldSpecBuilder.() -> Unit,
    ) {
        parts += FormDataFieldSpecBuilder(name).apply(block).build()
    }

    /**
     * Matches a form-data file part with the given [name] using a DSL builder block.
     *
     * @param name The form-data field name (the `name` attribute in Content-Disposition).
     * @param block A [FormDataFileSpecBuilder] block for configuring filename, content-type, and body matchers.
     */
    @ExperimentalMokksyApi
    public fun file(
        name: String,
        block: FormDataFileSpecBuilder.() -> Unit,
    ) {
        parts += FormDataFileSpecBuilder(name).apply(block).build()
    }

    internal fun build(): List<FormDataPartSpec> = parts.toList()
}

/**
 * Builder for matching a form-data text field's body content.
 *
 * Obtained via [FormDataSpecBuilder.field].
 */
@MokksyDsl
public class FormDataFieldSpecBuilder internal constructor(
    private val name: String,
) {
    private var bodyMatchers: List<Matcher<String?>> = emptyList()

    /**
     * Matches the field's value against the provided [matcher].
     *
     * @param matcher A Kotest [Matcher] applied to the field's value string.
     */
    @ExperimentalMokksyApi
    public fun body(matcher: Matcher<String?>) {
        bodyMatchers += matcher
    }

    internal fun build(): FormDataPartSpec =
        FormDataPartSpec(
            name = name,
            bodyMatchers = bodyMatchers,
        )
}

/**
 * Builder for matching a form-data file part.
 *
 * Configures expectations for filename, content-type, and body content.
 * File body content is read lazily — only when body matchers are configured.
 *
 * Obtained via [FormDataSpecBuilder.file].
 */
@MokksyDsl
public class FormDataFileSpecBuilder internal constructor(
    private val name: String,
) {
    private var filenameMatcher: Matcher<String?>? = null
    private var contentTypeMatcher: Matcher<ContentType?>? = null
    private var bodyMatchers: List<Matcher<String?>> = emptyList()

    /**
     * Matches the file part's original filename against the provided [matcher].
     *
     * @param matcher A Kotest [Matcher] applied to the file's original filename.
     */
    @ExperimentalMokksyApi
    public fun filename(matcher: Matcher<String?>) {
        filenameMatcher = matcher
    }

    /**
     * Matches the file part's content type against the provided [matcher].
     *
     * @param matcher A Kotest [Matcher] applied to the file's content type.
     */
    @ExperimentalMokksyApi
    public fun contentType(matcher: Matcher<ContentType?>) {
        contentTypeMatcher = matcher
    }

    /**
     * Matches the file part's body content against the provided [matcher].
     *
     * The file body is read lazily — only when this method is called.
     * Content is read as a UTF-8 string.
     *
     * @param matcher A Kotest [Matcher] applied to the file's body content string.
     */
    @ExperimentalMokksyApi
    public fun body(matcher: Matcher<String?>) {
        bodyMatchers += matcher
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
 * Builder for configuring body matching criteria within a `body` block.
 *
 * Groups all body-matching criteria — [formData], [predicate], and future matchers —
 * under a single block. Obtained via [RequestSpecificationBuilder.body].
 *
 * @param P The type of the deserialized request body.
 */
@MokksyDsl
public class BodySpecBuilder<P : Any> internal constructor() {
    private val formDataPartSpecs: MutableList<FormDataPartSpec> = mutableListOf()
    private val predicateMatchers: MutableList<Matcher<P?>> = mutableListOf()

    /**
     * Configures multipart/form-data matching.
     *
     * Implicitly checks that the request Content-Type is `multipart/form-data`.
     *
     * @param block A [FormDataSpecBuilder] block for configuring field and file matchers.
     */
    @ExperimentalMokksyApi
    public fun formData(block: FormDataSpecBuilder.() -> Unit) {
        formDataPartSpecs += FormDataSpecBuilder().apply(block).build()
    }

    /**
     * Adds a predicate to match against the deserialized request body.
     *
     * @param description Optional description of the predicate, used in failure messages.
     * @param predicate A function applied to the deserialized body (may be null).
     */
    @ExperimentalMokksyApi
    public fun predicate(
        description: String? = null,
        predicate: (P?) -> Boolean,
    ) {
        predicateMatchers += predicateMatcher(description = description, predicate = predicate)
    }

    internal fun build(): FormDataBuilderResult<P> =
        FormDataBuilderResult(
            formDataPartSpecs = formDataPartSpecs.toList(),
            predicateMatchers = predicateMatchers.toList(),
        )
}

internal data class FormDataBuilderResult<P : Any>(
    val formDataPartSpecs: List<FormDataPartSpec>,
    val predicateMatchers: List<Matcher<P?>>,
)

@Suppress("ReturnCount")
internal suspend fun scoreFormDataMatchers(
    request: ApplicationRequest,
    formDataPartSpecs: List<FormDataPartSpec>,
): Pair<Int, List<String>> {
    val contentTypeHeader = request.headers[HttpHeaders.ContentType]
    if (contentTypeHeader == null) {
        return 0 to formDataPartSpecs.indices.map { "formData[$it]" }
    }
    val parsedContentType = ContentType.parse(contentTypeHeader)
    if (!parsedContentType.match(ContentType.MultiPart.FormData)) {
        return 0 to formDataPartSpecs.indices.map { "formData[$it]" }
    }

    @Suppress("TooGenericExceptionCaught")
    val multipart =
        try {
            request.call.receiveMultipart()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            request.call.application.log
                .trace("Unable to receive multipart body for form-data matching: ${e.message}")
            return 0 to formDataPartSpecs.indices.map { "formData[$it]" }
        }

    val parts = mutableListOf<PartData>()
    @Suppress("TooGenericExceptionCaught")
    try {
        multipart.forEachPart { parts.add(it) }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        request.call.application.log
            .trace("Unable to read multipart parts for form-data matching: ${e.message}")
        return 0 to formDataPartSpecs.indices.map { "formData[$it]" }
    }

    var score = 0
    val failed = mutableListOf<String>()

    try {
        formDataPartSpecs.forEachIndexed { i, spec ->
            val matchingParts = parts.filter { it.name == spec.name }
            if (matchingParts.isEmpty()) {
                failed += "formData[$i]"
            } else {
                val passed = matchingParts.any { part -> matchFormDataPart(part, spec) }
                if (passed) score++ else failed += "formData[$i]"
            }
        }
    } finally {
        parts.forEach { it.dispose() }
    }

    return score to failed
}

private suspend fun matchFormDataPart(
    part: PartData,
    spec: FormDataPartSpec,
): Boolean =
    when (part) {
        is PartData.FormItem -> matchFormItem(part, spec)
        is PartData.FileItem -> matchFileItem(part, spec)
        is PartData.BinaryItem -> matchBinaryPart(part, spec)
        is PartData.BinaryChannelItem -> matchBinaryPart(part, spec)
    }

@Suppress("ReturnCount")
private fun matchFormItem(
    item: PartData.FormItem,
    spec: FormDataPartSpec,
): Boolean {
    if (spec.filenameMatcher != null && !spec.filenameMatcher.test(null).passed()) {
        return false
    }
    if (spec.contentTypeMatcher != null &&
        !spec.contentTypeMatcher
            .test(item.contentType)
            .passed()
    ) {
        return false
    }
    if (spec.bodyMatchers.isNotEmpty() && !spec.bodyMatchers.all { it.test(item.value).passed() }) {
        return false
    }
    return true
}

@Suppress("ReturnCount")
private suspend fun matchFileItem(
    item: PartData.FileItem,
    spec: FormDataPartSpec,
): Boolean {
    if (spec.filenameMatcher != null &&
        !spec.filenameMatcher
            .test(item.originalFileName)
            .passed()
    ) {
        return false
    }
    if (spec.contentTypeMatcher != null &&
        !spec.contentTypeMatcher
            .test(item.contentType)
            .passed()
    ) {
        return false
    }
    if (spec.bodyMatchers.isNotEmpty()) {
        val content =
            item
                .provider()
                .readRemaining()
                .readByteArray()
                .decodeToString()
        if (!spec.bodyMatchers.all { it.test(content).passed() }) {
            return false
        }
    }
    return true
}

@Suppress("ReturnCount")
private suspend fun matchBinaryPart(
    part: PartData,
    spec: FormDataPartSpec,
): Boolean {
    if (spec.filenameMatcher != null) return false
    if (spec.contentTypeMatcher != null &&
        !spec.contentTypeMatcher.test(part.contentType).passed()
    ) {
        return false
    }
    if (spec.bodyMatchers.isNotEmpty()) {
        val content =
            when (part) {
                is PartData.BinaryItem -> {
                    val buffer = Buffer()
                    part.provider().readAtMostTo(buffer, MAX_PART_SIZE)
                    buffer.readByteArray().decodeToString()
                }

                is PartData.BinaryChannelItem -> {
                    part
                        .provider()
                        .readRemaining()
                        .readByteArray()
                        .decodeToString()
                }

                else -> {
                    return false
                }
            }
        if (!spec.bodyMatchers.all { it.test(content).passed() }) {
            return false
        }
    }
    return true
}
