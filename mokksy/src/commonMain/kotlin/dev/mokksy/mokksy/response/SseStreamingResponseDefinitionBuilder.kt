package dev.mokksy.mokksy.response

import dev.mokksy.mokksy.InternalMokksyApi
import dev.mokksy.mokksy.MokksyDsl
import dev.mokksy.mokksy.request.CapturedRequest
import dev.mokksy.mokksy.utils.logger.HttpFormatter
import io.ktor.sse.ServerSentEventMetadata
import kotlinx.coroutines.flow.Flow

/**
 * A [StreamingResponseDefinitionBuilder] specialization for SSE streams.
 *
 * Overrides [createDefinition] to produce an [SseStreamResponseDefinition] that
 * adds SSE-specific headers (`Cache-Control`, `Connection`, `X-Accel-Buffering`)
 * and blank-line event terminators per the
 * [SSE specification](https://html.spec.whatwg.org/multipage/server-sent-events.html).
 */
@MokksyDsl
@OptIn(InternalMokksyApi::class)
internal class SseStreamingResponseDefinitionBuilder<P : Any, T : Any>(
    request: CapturedRequest<P>,
    formatter: HttpFormatter,
) : StreamingResponseDefinitionBuilder<P, ServerSentEventMetadata<T>>(
    request = request,
    formatter = formatter,
) {
    override fun createDefinition(
        resolvedFlow: Flow<ServerSentEventMetadata<T>>,
    ): StreamResponseDefinition<ServerSentEventMetadata<T>> =
        SseStreamResponseDefinition(
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
