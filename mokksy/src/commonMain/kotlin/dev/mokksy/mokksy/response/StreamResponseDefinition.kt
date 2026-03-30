@file:OptIn(InternalMokksyApi::class)

package dev.mokksy.mokksy.response

import dev.mokksy.mokksy.InternalMokksyApi
import dev.mokksy.mokksy.utils.logger.HttpFormatter
import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.withCharset
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.log
import io.ktor.server.request.httpVersion
import io.ktor.server.response.ResponseHeaders
import io.ktor.server.response.cacheControl
import io.ktor.server.response.respondBytesWriter
import io.ktor.util.logging.Logger
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.CancellationException
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.yield
import kotlin.time.Duration

internal const val SEND_BUFFER_CAPACITY = 256

/**
 * Represents a definition for streaming responses, supporting flow-based content streaming.
 * This class extends [AbstractResponseDefinition] to provide functionality specific
 * to streamed responses. It handles flow-based content delivery, manages chunk-wise delays,
 * and supports various output formats.
 *
 * @param T The type of the response data being streamed.
 * @property chunkFlow A [Flow] of chunks to be streamed as part of the response.
 * @property delayBetweenChunks Delay between the transmission of each chunk.
 * @property httpStatus The HTTP status code of the response, defaulting to HttpStatusCode.OK.
 *
 * @see AbstractResponseDefinition
 *
 * @author Konstantin Pavlov
 */
@Suppress("LongParameterList")
public open class StreamResponseDefinition<T>(
    public open val chunkFlow: Flow<T>,
    public val delayBetweenChunks: Duration = Duration.ZERO,
    contentType: ContentType = ContentType.Text.EventStream.withCharset(Charsets.UTF_8),
    private val chunkContentType: ContentType? = null,
    httpStatus: HttpStatusCode = HttpStatusCode.OK,
    headers: (ResponseHeaders.() -> Unit)? = null,
    delay: Duration,
    protected val formatter: HttpFormatter,
) : AbstractResponseDefinition<T>(
        contentType = contentType,
        httpStatus = httpStatus,
        headers = headers,
        delay = delay,
    ) {

    /**
     * Configures response headers before streaming begins.
     * Subclasses override to add transport-specific headers (e.g. SSE headers).
     */
    protected open fun configureHeaders(call: ApplicationCall) {
        call.response.cacheControl(CacheControl.NoCache(null))
    }

    /**
     * Serializes a chunk value to its wire representation as bytes.
     * Subclasses override to customize formatting (e.g. appending SSE blank-line terminators)
     * or to produce binary content directly.
     */
    protected open fun serialize(value: T): ByteArray = "$value".encodeToByteArray()

    override suspend fun writeResponse(
        call: ApplicationCall,
        verbose: Boolean,
    ) {
        headers?.invoke(call.response.headers)
        configureHeaders(call)
        call.respondBytesWriter(
            status = this.httpStatus,
            contentType = this.contentType,
        ) {
            if (verbose) {
                call.application.log.debug(
                    "Sending:\n---\n${
                        formatter.formatResponseHeader(
                            httpVersion = call.request.httpVersion,
                            headers = call.response.headers,
                            status = httpStatus,
                        )
                    }",
                )
            }
            writeChunksFromFlow(
                writer = this,
                verbose = verbose,
                logger = call.application.log,
            )
        }
    }

    private suspend fun writeChunksFromFlow(
        logger: Logger,
        writer: ByteWriteChannel,
        verbose: Boolean,
    ) {
        if (this.delay.isPositive()) {
            delay(delay)
        }
        chunkFlow
            .filterNotNull()
            .cancellable()
            .buffer(
                capacity = SEND_BUFFER_CAPACITY,
                onBufferOverflow = BufferOverflow.SUSPEND,
            ).catch { e ->
                if (e !is CancellationException) {
                    logger.warn("Error while sending chunks", e)
                }
                throw e
            }.collect {
                writeChunk(
                    writer = writer,
                    value = it,
                    verbose = verbose,
                    logger = logger,
                )
            }
    }

    private suspend fun writeChunk(
        writer: ByteWriteChannel,
        value: T,
        verbose: Boolean,
        logger: Logger,
    ) {
        val serializedValue = serialize(value)
        if (verbose) {
            val type =
                chunkContentType
                    ?: when (value) {
                        is CharSequence -> ContentType.Text.Plain
                        else -> ContentType.Application.Json
                    }
            logger.debug(
                "Writing chunk:\n ${
                    formatter.formatResponseChunk(
                        chunk = serializedValue.decodeToString(),
                        contentType = type,
                    )
                }",
            )
        }
        writer.writeFully(serializedValue)
        writer.flush()
        if (delayBetweenChunks.isPositive()) {
            delay(delayBetweenChunks)
        } else {
            yield()
        }
    }
}
