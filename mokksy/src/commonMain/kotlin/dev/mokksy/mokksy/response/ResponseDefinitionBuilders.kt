package dev.mokksy.mokksy.response

import dev.mokksy.mokksy.request.CapturedRequest
import dev.mokksy.mokksy.utils.logger.HttpFormatter
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.ResponseHeaders
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

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
public abstract class AbstractResponseDefinitionBuilder<P, T>(
    public var delay: Duration = Duration.ZERO,
) {
    private var _httpStatusCode: Int = HttpStatusCode.OK.value
    public val httpStatusCode: Int get() = _httpStatusCode

    public var httpStatus: HttpStatusCode = HttpStatusCode.OK
        set(value) {
            field = value
            _httpStatusCode = value.value
        }

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
            headersLambda = block
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
     * Sets a delay for the response in milliseconds.
     *
     * @param millis The delay duration in milliseconds before the response is sent.
     */
    public fun delayMillis(millis: Long) {
        this.delay = millis.milliseconds
    }

    public fun httpStatus(status: Int) {
        this.httpStatus = HttpStatusCode.fromValue(status)
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
 * @property httpStatusCode The HTTP status code of the response as Int, defaulting to 200.
 * @property httpStatus The HTTP status code of the response, defaulting to HttpStatusCode.OK.
 * @param headers A mutable list of additional custom headers for the response.
 *
 * Inherits functionality from [AbstractResponseDefinitionBuilder] to allow additional header manipulations
 * and provides a concrete implementation of the response building process.
 */
@Suppress("LongParameterList")
public open class ResponseDefinitionBuilder<P : Any, T : Any>(
    public val request: CapturedRequest<P>,
    public var contentType: ContentType? = null,
    public var body: T? = null,
    httpStatusCode: Int = 200,
    httpStatus: HttpStatusCode = HttpStatusCode.fromValue(httpStatusCode),
    private val formatter: HttpFormatter,
) : AbstractResponseDefinitionBuilder<P, T>() {
    init {
        this.httpStatus = httpStatus
    }

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
            httpStatusCode = httpStatusCode,
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
@Suppress("LongParameterList")
public open class StreamingResponseDefinitionBuilder<P : Any, T>(
    public val request: CapturedRequest<P>,
    public var flow: Flow<T>? = null,
    public var chunks: MutableList<T> = mutableListOf(),
    public var delayBetweenChunks: Duration = Duration.ZERO,
    httpStatus: HttpStatusCode = HttpStatusCode.OK,
    public val chunkContentType: ContentType? = null,
    private val formatter: HttpFormatter,
) : AbstractResponseDefinitionBuilder<P, T>() {
    init {
        this.httpStatus = httpStatus
    }

    /**
     * Builds an instance of [StreamResponseDefinition].
     *
     * This method finalizes the construction of a [StreamResponseDefinition] by encapsulating
     * the data flow, chunked list, HTTP status code, and headers defined in the current instance
     * of the builder. The resulting [StreamResponseDefinition] can then be used to represent
     * a streaming response.
     *
     * @param P The type of the request body.
     * @param T The type of data being streamed.
     * @return A fully constructed [StreamResponseDefinition] instance containing the configured response details.
     */
    public override fun build(): StreamResponseDefinition<P, T> =
        StreamResponseDefinition(
            chunkFlow = flow,
            chunks = chunks.toList(),
            httpStatus = httpStatus,
            headers = buildCombinedHeaders(),
            delayBetweenChunks = delayBetweenChunks,
            delay = delay,
            formatter = formatter,
            chunkContentType = chunkContentType,
        )
}
