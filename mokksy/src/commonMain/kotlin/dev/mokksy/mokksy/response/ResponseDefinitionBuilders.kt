@file:OptIn(InternalMokksyApi::class)

package dev.mokksy.mokksy.response

import dev.mokksy.mokksy.InternalMokksyApi
import dev.mokksy.mokksy.MokksyDsl
import dev.mokksy.mokksy.request.CapturedRequest
import dev.mokksy.mokksy.utils.logger.HttpFormatter
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.withCharset
import io.ktor.server.response.ResponseHeaders
import io.ktor.utils.io.charsets.Charsets
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration
import kotlinx.coroutines.flow.flow as buildFlow

/**
 * Represents a base abstraction for defining the attributes of an HTTP response in the context of
 * request-to-response mappings. This class allows customization of the HTTP status code and headers
 * and provides a mechanism for building concrete response definitions.
 *
 * @param P The type of the request body. This determines the input type for which the response is being defined.
 * @param T The type of the response data, which is returned to the client.
 * @property httpStatus The HTTP status code to be associated with the response.
 * @author Konstantin Pavlov
 */
@MokksyDsl
public abstract class AbstractResponseDefinitionBuilder<P, T>(
    public var delay: Duration = Duration.ZERO,
) {
    public var httpStatus: HttpStatusCode = HttpStatusCode.OK

    private val headerPairs: MutableList<Pair<String, String>> = mutableListOf()
    private var headersLambda: (ResponseHeaders.() -> Unit)? = null

    /**
     * Adds a single response header.
     *
     * @param name The header name.
     * @param value The header value.
     */
    public fun addHeader(
        name: String,
        value: String,
    ) {
        headerPairs.add(name to value)
    }

    /**
     * Entry point for header configuration. Supports two syntaxes:
     *
     * **Lambda block** — for full [ResponseHeaders] access:
     * ```kotlin
     * headers {
     *     append(HttpHeaders.Location, "/things/$id")
     * }
     * ```
     *
     * **`+=` shorthand** — for simple name/value pairs:
     * ```kotlin
     * headers += "Foo" to "bar"
     * ```
     */
    public val headers: HeadersConfigurer = HeadersConfigurer()

    /**
     * Provides the `headers { }` lambda and `headers +=` pair-shorthand syntax
     * for configuring response headers within a [AbstractResponseDefinitionBuilder].
     */
    public inner class HeadersConfigurer internal constructor() {
        /**
         * Configures headers via a [ResponseHeaders] lambda block.
         *
         * @param block A lambda applied to [ResponseHeaders] to configure headers.
         */
        public operator fun invoke(block: ResponseHeaders.() -> Unit) {
            val previous = headersLambda
            headersLambda = {
                previous?.invoke(this)
                block.invoke(this)
            }
        }

        /**
         * Adds a single header via `+=` syntax.
         *
         * @param header A [Pair] of header name to header value.
         */
        public operator fun plusAssign(header: Pair<String, String>) {
            headerPairs.add(header)
        }
    }

    /**
     * Merges [addHeader] pairs and the [headers] lambda into a single [ResponseHeaders] configurator.
     * Returns `null` when no headers have been configured.
     */
    protected fun buildCombinedHeaders(): (ResponseHeaders.() -> Unit)? {
        val pairs = headerPairs.toList()
        val lambda = headersLambda
        return if (pairs.isEmpty() && lambda == null) {
            null
        } else {
            {
                pairs.forEach { (name, value) -> append(name, value) }
                lambda?.invoke(this)
            }
        }
    }

    /**
     * Abstract method to build a concrete response definition.
     *
     * @return An instance of [AbstractResponseDefinition].
     */
    protected abstract fun build(): AbstractResponseDefinition<T>
}

/**
 * Builder for constructing a definition of an HTTP response with configurable attributes.
 *
 * @param P The type of the request body.
 * @param T The type of the response body.
 * @property request The [CapturedRequest] being processed.
 * @property contentType Optional MIME type of the response. Defaults to `null` if not specified.
 * @property body The body of the response. Can be null.
 * @property httpStatus The HTTP status code of the response, defaulting to HttpStatusCode.OK.
 * @param headers A mutable list of additional custom headers for the response.
 *
 * Inherits functionality from [AbstractResponseDefinitionBuilder] to allow additional header manipulations
 * and provides a concrete implementation of the response building process.
 */
@MokksyDsl
@Suppress("LongParameterList")
public open class ResponseDefinitionBuilder<P : Any, T : Any> internal constructor(
    public val request: CapturedRequest<P>,
    public var contentType: ContentType? = null,
    public var body: T? = null,
    httpStatus: HttpStatusCode = HttpStatusCode.OK,
    private val formatter: HttpFormatter,
) : AbstractResponseDefinitionBuilder<P, T>() {
    init {
        this.httpStatus = httpStatus
    }

    /**
     * Sets the response body and returns this builder for chaining.
     *
     * Java-friendly fluent alternative to the `body` property setter.
     *
     * Example:
     * ```java
     *  builder.body(myObject).status(200).header("X-Custom", "value");
     *  ```
     *
     * @param value The response body value.
     * @return This builder instance.
     */
    public fun body(value: T): ResponseDefinitionBuilder<P, T> = apply { this.body = value }

    /**
     * Sets the HTTP status code and returns this builder for chaining.
     *
     * Java-friendly fluent alternative to [httpStatus].
     *
     * Example:
     * ```java
     * builder.body(myObject).status(201).header("Location", "/items/1");
     * ```
     *
     * @param code The HTTP status code as an integer, e.g. `201`, `404`.
     * @return This builder instance.
     */
    public fun status(code: Int): ResponseDefinitionBuilder<P, T> =
        apply { httpStatus = HttpStatusCode.fromValue(code) }

    /**
     * Adds a response header and returns this builder for chaining.
     *
     * Java-friendly fluent alternative to [addHeader].
     *
     * Example:
     * ```java
     * builder.body(myObject).status(200).header("X-Custom", "value");
     * ```
     *
     * @param name The header name.
     * @param value The header value.
     * @return This builder instance.
     */
    public fun header(
        name: String,
        value: String,
    ): ResponseDefinitionBuilder<P, T> = apply { addHeader(name, value) }

    /**
     * Constructs a new instance of [ResponseDefinition] with the configured attributes of the builder.
     *
     * This method uses the current state of the builder to create a concrete definition
     * of an HTTP response, including the body, content type, status code, headers, delay,
     * and formatter for enhanced response handling.
     *
     * @return A new instance of [ResponseDefinition] containing the response attributes defined in the builder.
     */
    public override fun build(): ResponseDefinition<P, T> =
        ResponseDefinition(
            body = body,
            contentType = contentType ?: ContentType.Application.Json,
            httpStatusCode = httpStatus.value,
            httpStatus = httpStatus,
            headers = buildCombinedHeaders(),
            delay = delay,
            formatter = formatter,
        )
}

/**
 * A builder for constructing streaming response definitions.
 *
 * This class is responsible for building instances of [StreamResponseDefinition],
 * which define responses capable of streaming data either as chunks or via a flow.
 *
 * @param P The type of the request body.
 * @param T The type of data being streamed.
 * @property request The [CapturedRequest] being processed.
 * @property flow A Flow representing streaming data content.
 * @property chunks A list of data chunks to be sent as part of the stream,
 *          if [flow] is not provided.
 */
@MokksyDsl
@Suppress("LongParameterList")
public open class StreamingResponseDefinitionBuilder<P : Any, T> internal constructor(
    public val request: CapturedRequest<P>,
    public var flow: Flow<T>? = null,
    public val chunks: MutableList<T> = mutableListOf(),
    public var delayBetweenChunks: Duration = Duration.ZERO,
    httpStatus: HttpStatusCode = HttpStatusCode.OK,
    public val chunkContentType: ContentType? = null,
    private val formatter: HttpFormatter,
) : AbstractResponseDefinitionBuilder<P, T>() {
    /**
     * The `Content-Type` of the HTTP response. Defaults to `text/event-stream; charset=UTF-8`.
     *
     * Override when the stream carries a different media type, e.g. `application/x-ndjson`.
     */
    public var contentType: ContentType = ContentType.Text.EventStream.withCharset(Charsets.UTF_8)

    init {
        this.httpStatus = httpStatus
    }

    /**
         * Constructs a StreamResponseDefinition populated from the builder's configured values.
         *
         * @return A [StreamResponseDefinition] with a single resolved [Flow] source.
         * If chunks were added via [plusAssign], they are converted to a cold [Flow] at build time.
         */
    public override fun build(): StreamResponseDefinition<P, T> {
        check(flow == null || chunks.isEmpty()) {
            "Cannot configure both flow and chunks on the same streaming response"
        }
        val resolvedFlow: Flow<T> = flow ?: buildFlow { chunks.forEach { emit(it) } }
        return StreamResponseDefinition(
            chunkFlow = resolvedFlow,
            httpStatus = httpStatus,
            headers = buildCombinedHeaders(),
            delayBetweenChunks = delayBetweenChunks,
            delay = delay,
            formatter = formatter,
            chunkContentType = chunkContentType,
            contentType = contentType,
        )
    }
}
