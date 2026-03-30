package dev.mokksy.mokksy.response

import dev.mokksy.mokksy.InternalMokksyApi
import dev.mokksy.mokksy.utils.logger.HttpFormatter
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.withCharset
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.ResponseHeaders
import io.ktor.server.response.header
import io.ktor.sse.ServerSentEventMetadata
import io.ktor.utils.io.charsets.Charsets
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.time.Duration

/**
 * A [StreamResponseDefinition] specialization for server-sent events.
 *
 * Overrides [configureHeaders] to add SSE-specific headers and [serialize] to append
 * a blank-line event terminator (`\r\n`) after each event per the
 * [SSE specification](https://html.spec.whatwg.org/multipage/server-sent-events.html).
 *
 * @param chunkFlow A Flow of ServerSentEvent representing the stream of SSE events. Defaults to [emptyFlow].
 * @param chunkContentType The ContentType for the chunks in the SSE stream. Defaults to `null`.
 * @param delay An optional delay between chunks, specified as a Duration. Defaults to `Duration.ZERO`.
 * @param formatter An [HttpFormatter] responsible for formatting the HTTP response or payloads.
 * @author Konstantin Pavlov
 */
@Suppress("LongParameterList")
public class SseStreamResponseDefinition<T> @OptIn(InternalMokksyApi::class) internal constructor(
    override val chunkFlow: Flow<ServerSentEventMetadata<T>> = emptyFlow(),
    chunkContentType: ContentType? = null,
    delayBetweenChunks: Duration = Duration.ZERO,
    delay: Duration = Duration.ZERO,
    httpStatus: HttpStatusCode = HttpStatusCode.OK,
    headers: (ResponseHeaders.() -> Unit)? = null,
    contentType: ContentType = ContentType.Text.EventStream.withCharset(Charsets.UTF_8),
    formatter: HttpFormatter,
) : StreamResponseDefinition<ServerSentEventMetadata<T>>(
    chunkFlow = chunkFlow,
    chunkContentType = chunkContentType ?: contentType,
    delayBetweenChunks = delayBetweenChunks,
    delay = delay,
    httpStatus = httpStatus,
    headers = headers,
    contentType = contentType,
    formatter = formatter,
) {
    override fun configureHeaders(call: ApplicationCall) {
        call.response.header(HttpHeaders.CacheControl, "no-store")
        call.response.header(HttpHeaders.Connection, "keep-alive")
        call.response.header("X-Accel-Buffering", "no")
    }

    override fun serialize(value: ServerSentEventMetadata<T>): ByteArray =
        (value.toString() + "\r\n").encodeToByteArray()
}
