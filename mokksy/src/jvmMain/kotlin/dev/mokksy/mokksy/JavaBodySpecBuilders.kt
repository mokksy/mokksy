@file:OptIn(ExperimentalMokksyApi::class)

package dev.mokksy.mokksy

import dev.mokksy.mokksy.request.BodyPartKind
import dev.mokksy.mokksy.request.BodyPartSpec
import dev.mokksy.mokksy.request.ByteArrayContentMatcher
import dev.mokksy.mokksy.request.ByteBodySpec
import dev.mokksy.mokksy.request.ContentMatcher
import dev.mokksy.mokksy.request.FormBodySpec
import dev.mokksy.mokksy.request.FormEncoding
import dev.mokksy.mokksy.request.MultipartBodySpec
import dev.mokksy.mokksy.request.RequestSpecificationBuilder
import dev.mokksy.mokksy.request.StringContentMatcher
import dev.mokksy.mokksy.request.byteArrayEqual
import dev.mokksy.mokksy.request.predicateMatcher
import io.kotest.matchers.Matcher
import io.kotest.matchers.equals.beEqual
import io.ktor.http.ContentType
import java.util.function.Consumer
import java.util.function.Predicate

// region Body spec builders

/**
 * Java-friendly `body` block equivalent.
 *
 * Example:
 * ```java
 * mokksy.post(spec -> spec.body(body -> body
 *     .form(form -> form.field("user", "alice"))
 *     .bytesMatches(bytes -> bytes != null && bytes.length > 0)));
 * ```
 */
public class JavaBodySpecBuilder<P : Any> internal constructor(
    private val delegate: RequestSpecificationBuilder<P>,
) {
    private val formSpecs: MutableList<FormBodySpec> = mutableListOf()
    private val multipartSpecs: MutableList<MultipartBodySpec> = mutableListOf()
    private val byteContentMatchers: MutableList<ContentMatcher> = mutableListOf()
    private var contentTypeMatcher: Matcher<ContentType?>? = null

    /**
     * Matches either URL-encoded form data or multipart form data.
     *
     * @param configurer Configures required fields and files.
     * @return This builder instance.
     */
    public fun form(configurer: Consumer<JavaFormSpecBuilder>): JavaBodySpecBuilder<P> =
        form(FormEncoding.AUTO, configurer)

    /**
     * Matches form data with an explicit accepted encoding.
     *
     * @param encoding Accepted form encoding.
     * @param configurer Configures required fields and files.
     * @return This builder instance.
     */
    public fun form(
        encoding: FormEncoding,
        configurer: Consumer<JavaFormSpecBuilder>,
    ): JavaBodySpecBuilder<P> {
        val builder = JavaFormSpecBuilder()
        configurer.accept(builder)
        formSpecs += FormBodySpec(encoding = encoding, parts = builder.build())
        return this
    }

    /**
     * Matches a multipart body with the expected content type, such as `multipart/mixed`.
     *
     * @param contentType Expected multipart content type.
     * @param configurer Configures required multipart parts.
     * @return This builder instance.
     */
    public fun multipart(
        contentType: String,
        configurer: Consumer<JavaMultipartSpecBuilder>,
    ): JavaBodySpecBuilder<P> {
        val builder = JavaMultipartSpecBuilder(ContentType.parse(contentType))
        configurer.accept(builder)
        multipartSpecs += builder.build()
        return this
    }

    /**
     * Matches the raw request body text exactly.
     *
     * @param value Expected body text.
     * @return This builder instance.
     */
    public fun text(value: String): JavaBodySpecBuilder<P> {
        byteContentMatchers += StringContentMatcher(beEqual(value))
        return this
    }

    /**
     * Matches the raw request body text using a predicate.
     *
     * @param predicate Predicate applied to request body text.
     * @return This builder instance.
     */
    public fun textMatches(predicate: Predicate<String?>): JavaBodySpecBuilder<P> {
        byteContentMatchers +=
            StringContentMatcher(predicateMatcher(predicate = { predicate.test(it) }))
        return this
    }

    /**
     * Matches the raw request body bytes exactly.
     *
     * @param value Expected request body bytes.
     * @return This builder instance.
     */
    public fun bytes(value: ByteArray): JavaBodySpecBuilder<P> {
        byteContentMatchers += ByteArrayContentMatcher(byteArrayEqual(value))
        return this
    }

    /**
     * Matches the raw request body bytes using a predicate.
     *
     * @param predicate Predicate applied to request body bytes.
     * @return This builder instance.
     */
    public fun bytesMatches(predicate: Predicate<ByteArray?>): JavaBodySpecBuilder<P> {
        byteContentMatchers +=
            ByteArrayContentMatcher(predicateMatcher(predicate = { predicate.test(it) }))
        return this
    }

    /**
     * Requires the request body content type to equal [contentType].
     *
     * @param contentType Expected content type, such as `application/octet-stream`.
     * @return This builder instance.
     */
    public fun contentType(contentType: String): JavaBodySpecBuilder<P> {
        contentTypeMatcher = beEqual(ContentType.parse(contentType))
        return this
    }

    /**
     * Adds a predicate for the deserialized request body.
     *
     * @param predicate Predicate applied to the deserialized body.
     * @return This builder instance.
     */
    public fun predicate(predicate: Predicate<P>): JavaBodySpecBuilder<P> {
        delegate.addBodyMatcher(
            predicateMatcher(description = null, predicate = { it != null && predicate.test(it) }),
        )
        return this
    }

    internal fun applyToDelegate() {
        delegate.addFormSpecs(formSpecs)
        delegate.addMultipartSpecs(multipartSpecs)
        if (byteContentMatchers.isNotEmpty() || contentTypeMatcher != null) {
            delegate.addByteBodySpec(
                ByteBodySpec(
                    contentTypeMatcher = contentTypeMatcher,
                    contentMatchers = byteContentMatchers.toList(),
                ),
            )
        }
    }
}

// endregion

// region Form spec builder

/**
 * Java-friendly form matcher builder.
 */
public class JavaFormSpecBuilder internal constructor() {
    private val parts: MutableList<BodyPartSpec> = mutableListOf()

    /**
     * Requires field [name] to have exactly [expectedValue].
     *
     * @param name Field name.
     * @param expectedValue Expected field value.
     * @return This builder instance.
     */
    public fun field(
        name: String,
        expectedValue: String,
    ): JavaFormSpecBuilder {
        parts +=
            BodyPartSpec(
                name = name,
                kind = BodyPartKind.FIELD,
                contentMatchers = listOf(StringContentMatcher(beEqual(expectedValue))),
            )
        return this
    }

    /**
     * Requires field [name] to satisfy [predicate].
     *
     * @param name Field name.
     * @param predicate Predicate applied to the field value.
     * @return This builder instance.
     */
    public fun fieldMatches(
        name: String,
        predicate: Predicate<String?>,
    ): JavaFormSpecBuilder {
        parts +=
            BodyPartSpec(
                name = name,
                kind = BodyPartKind.FIELD,
                contentMatchers =
                    listOf(
                        StringContentMatcher(
                            predicateMatcher(predicate = { predicate.test(it) }),
                        ),
                    ),
            )
        return this
    }

    /**
     * Requires a file part named [name].
     *
     * @param name File part name.
     * @param configurer Configures optional file metadata and content matchers.
     * @return This builder instance.
     */
    public fun file(
        name: String,
        configurer: Consumer<JavaFilePartSpecBuilder>,
    ): JavaFormSpecBuilder {
        val builder = JavaFilePartSpecBuilder(name)
        configurer.accept(builder)
        parts += builder.build()
        return this
    }

    internal fun build(): List<BodyPartSpec> = parts.toList()
}

// endregion

// region Multipart spec builder

/**
 * Java-friendly multipart matcher builder.
 */
public class JavaMultipartSpecBuilder internal constructor(
    private val contentType: ContentType,
) {
    private val parts: MutableList<BodyPartSpec> = mutableListOf()
    private var boundaryMatcher: Matcher<String?>? = null

    /**
     * Requires the multipart boundary to equal [value].
     *
     * @param value Expected boundary value.
     * @return This builder instance.
     */
    public fun boundary(value: String): JavaMultipartSpecBuilder {
        boundaryMatcher = beEqual(value)
        return this
    }

    /**
     * Requires the multipart boundary to satisfy [predicate].
     *
     * @param predicate Predicate applied to the boundary value.
     * @return This builder instance.
     */
    public fun boundaryMatches(predicate: Predicate<String?>): JavaMultipartSpecBuilder {
        boundaryMatcher = predicateMatcher(predicate = { predicate.test(it) })
        return this
    }

    /**
     * Requires a multipart part named [name].
     *
     * @param name Part name.
     * @param configurer Configures optional part metadata and content matchers.
     * @return This builder instance.
     */
    public fun part(
        name: String,
        configurer: Consumer<JavaDataPartSpecBuilder>,
    ): JavaMultipartSpecBuilder {
        val builder = JavaDataPartSpecBuilder(name)
        configurer.accept(builder)
        parts += builder.build()
        return this
    }

    internal fun build(): MultipartBodySpec =
        MultipartBodySpec(
            contentType = contentType,
            boundaryMatcher = boundaryMatcher,
            parts = parts.toList(),
        )
}

// endregion

// region Part spec builders

/**
 * Java-friendly file-part matcher builder.
 */
public class JavaFilePartSpecBuilder internal constructor(
    private val name: String,
) : AbstractJavaDataPartSpecBuilder<JavaFilePartSpecBuilder>() {
    private var filenameMatcher: Matcher<String?>? = null

    /**
     * Requires the uploaded filename to equal [expectedFilename].
     *
     * @param expectedFilename Expected uploaded filename.
     * @return This builder instance.
     */
    public fun filename(expectedFilename: String): JavaFilePartSpecBuilder {
        filenameMatcher = beEqual(expectedFilename)
        return this
    }

    /**
     * Requires the uploaded filename to satisfy [predicate].
     *
     * @param predicate Predicate applied to the uploaded filename.
     * @return This builder instance.
     */
    public fun filenameMatches(predicate: Predicate<String?>): JavaFilePartSpecBuilder {
        filenameMatcher = predicateMatcher(predicate = { predicate.test(it) })
        return this
    }

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
 * Java-friendly multipart data-part matcher builder.
 */
public class JavaDataPartSpecBuilder internal constructor(
    private val name: String,
) : AbstractJavaDataPartSpecBuilder<JavaDataPartSpecBuilder>() {
    internal fun build(): BodyPartSpec =
        BodyPartSpec(
            name = name,
            kind = BodyPartKind.PART,
            contentTypeMatcher = builtContentTypeMatcher(),
            contentMatchers = builtContentMatchers(),
        )
}

// endregion

// region Abstract data-part builder

/**
 * Base Java-friendly builder for matching multipart part content type and content.
 *
 * Used by [JavaFilePartSpecBuilder] and [JavaDataPartSpecBuilder].
 */
@Suppress("AbstractClassCanBeConcreteClass")
public abstract class AbstractJavaDataPartSpecBuilder<T : AbstractJavaDataPartSpecBuilder<T>>
    internal constructor() {
        private var contentTypeMatcher: Matcher<ContentType?>? = null
        private val contentMatchers: MutableList<ContentMatcher> = mutableListOf()

        /**
         * Requires the part content type to equal [contentType].
         *
         * @param contentType Expected content type, such as `image/png`.
         * @return This builder instance.
         */
        public fun contentType(contentType: String): T =
            self {
                contentTypeMatcher = beEqual(ContentType.parse(contentType))
            }

        /**
         * Requires the part content decoded as UTF-8 text to equal [value].
         *
         * @param value Expected text content.
         * @return This builder instance.
         */
        public fun text(value: String): T =
            self {
                contentMatchers += StringContentMatcher(beEqual(value))
            }

        /**
         * Requires the part content decoded as UTF-8 text to satisfy [predicate].
         *
         * @param predicate Predicate applied to text content.
         * @return This builder instance.
         */
        public fun textMatches(predicate: Predicate<String?>): T =
            self {
                contentMatchers +=
                    StringContentMatcher(predicateMatcher(predicate = { predicate.test(it) }))
            }

        /**
         * Requires the part content bytes to equal [value].
         *
         * @param value Expected bytes.
         * @return This builder instance.
         */
        public fun bytes(value: ByteArray): T =
            self {
                contentMatchers += ByteArrayContentMatcher(byteArrayEqual(value))
            }

        /**
         * Requires the part content bytes to satisfy [predicate].
         *
         * @param predicate Predicate applied to bytes.
         * @return This builder instance.
         */
        public fun bytesMatches(predicate: Predicate<ByteArray?>): T =
            self {
                contentMatchers +=
                    ByteArrayContentMatcher(predicateMatcher(predicate = { predicate.test(it) }))
            }

        @Suppress("UNCHECKED_CAST")
        private fun self(block: T.() -> Unit): T {
            val typed = this as T
            typed.block()
            return typed
        }

        internal fun builtContentTypeMatcher(): Matcher<ContentType?>? = contentTypeMatcher

        internal fun builtContentMatchers(): List<ContentMatcher> = contentMatchers.toList()
    }

// endregion

internal fun <P : Any> RequestSpecificationBuilder<P>.addFormSpecs(specs: List<FormBodySpec>) {
    formSpecs += specs
}

internal fun <P : Any> RequestSpecificationBuilder<P>.addMultipartSpecs(
    specs: List<MultipartBodySpec>,
) {
    multipartSpecs += specs
}

internal fun <P : Any> RequestSpecificationBuilder<P>.addByteBodySpec(spec: ByteBodySpec) {
    byteBodySpecs += spec
}

internal fun <P : Any> RequestSpecificationBuilder<P>.addBodyMatcher(matcher: Matcher<P?>) {
    body += matcher
}
