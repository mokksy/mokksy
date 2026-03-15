package dev.mokksy.mokksy

import dev.mokksy.mokksy.response.StreamingResponseDefinitionBuilder
import io.ktor.http.ContentType
import java.util.stream.Stream
import kotlin.time.Duration.Companion.milliseconds

/**
 * Java-friendly wrapper around [StreamingResponseDefinitionBuilder] that returns `this` from every
 * mutating method, enabling fluent chaining without Kotlin-specific idioms.
 *
 * Java callers receive this type from [JavaBuildingStep.respondsWithStream] and use it to configure
 * the streaming HTTP response for a stub:
 * ```java
 * mokksy.get(spec -> spec.path("/stream"))
 *     .respondsWithStream(builder -> builder
 *         .chunks(List.of("Hello", " ", "World"))
 *         .delayBetweenChunksMillis(50));
 * ```
 *
 * Alternatively, supply chunks as a [Stream]:
 * ```java
 * mokksy.get(spec -> spec.path("/events"))
 *     .respondsWithStream(builder -> builder
 *         .chunks(Stream.of("data1", "data2")));
 * ```
 *
 * @param P The type of the request payload.
 * @param T The type of each streamed chunk.
 */
public class JavaStreamingResponseDefinitionBuilder<P : Any, T : Any> internal constructor(
    private val delegate: StreamingResponseDefinitionBuilder<P, T>,
) {
    /**
     * Sets the chunks to stream from a [List].
     *
     * **Replaces** any chunks previously added via [chunk] or an earlier [chunks] call.
     * If you need to combine bulk initialization with individual additions, call [chunks] first
     * and then [chunk].
     *
     * @param chunks The list of chunks to stream.
     * @return This builder instance.
     * @see chunk
     */
    public fun chunks(chunks: List<T>): JavaStreamingResponseDefinitionBuilder<P, T> =
        apply { delegate.chunks = chunks.toMutableList() }

    /**
     * Sets the chunks to stream from a [Stream].
     *
     * The stream is consumed **eagerly** on this call — it is fully drained before the method
     * returns and must not be reused afterwards.
     *
     * **Replaces** any chunks previously added via [chunk] or an earlier [chunks] call.
     * If you need to combine bulk initialization with individual additions, call [chunks] first
     * and then [chunk].
     *
     * @param chunks The stream of chunks to send. Must not have been previously consumed.
     * @return This builder instance.
     * @see chunk
     */
    public fun chunks(chunks: Stream<T>): JavaStreamingResponseDefinitionBuilder<P, T> =
        apply {
            val list = ArrayList<T>()
            chunks.forEach { list.add(it) }
            delegate.chunks = list
        }

    /**
         * Appends a single chunk to the streaming response, preserving insertion order.
         *
         * @param chunk The chunk to append.
         * @return This builder instance.
         */
    public fun chunk(chunk: T): JavaStreamingResponseDefinitionBuilder<P, T> =
        apply { delegate.chunks.add(chunk) }

    /**
         * Sets the delay between consecutive chunks.
         *
         * @param millis Delay between chunks in milliseconds.
         * @return This builder instance for fluent chaining.
         */
    public fun delayBetweenChunksMillis(
        millis: Long,
    ): JavaStreamingResponseDefinitionBuilder<P, T> =
        apply { delegate.delayBetweenChunks = millis.milliseconds }

    /**
     * Sets a delay before the response streaming begins.
     *
     * @param millis The delay in milliseconds.
     * @return This builder instance.
     */
    public fun delayMillis(millis: Long): JavaStreamingResponseDefinitionBuilder<P, T> =
        apply { delegate.delayMillis(millis) }

    /**
     * Sets the HTTP status code.
     *
     * @param code The status code as an integer, e.g. `200`, `206`.
     * @return This builder instance.
     */
    public fun status(code: Int): JavaStreamingResponseDefinitionBuilder<P, T> =
        apply { delegate.httpStatus(code) }

    /**
     * Adds a response header.
     *
     * @param name The header name.
     * @param value The header value.
     * @return This builder instance.
     */
    public fun header(
        name: String,
        value: String,
    ): JavaStreamingResponseDefinitionBuilder<P, T> = apply { delegate.addHeader(name, value) }

    /**
     * Sets the `Content-Type` of the streaming response.
     *
     * The default is `text/event-stream; charset=UTF-8`. Override when the stream carries a
     * different media type, e.g. `application/x-ndjson`:
     * ```java
     * .contentType("application/x-ndjson")
     * ```
     *
     * @param contentType A content-type string such as `"application/x-ndjson"`.
     * @return This builder instance.
     */
    public fun contentType(contentType: String): JavaStreamingResponseDefinitionBuilder<P, T> =
        apply { delegate.contentType = ContentType.parse(contentType) }

    /**
     * Sets the `Content-Type` of the streaming response.
     *
     * The default is `text/event-stream; charset=UTF-8`. Override when the stream carries a
     * different media type:
     * ```java
     * .contentType(ContentType.Application.Json)
     * ```
     *
     * @param contentType The content type, e.g. `ContentType.Application.Json`.
     * @return This builder instance.
     */
    public fun contentType(contentType: ContentType): JavaStreamingResponseDefinitionBuilder<P, T> =
        apply { delegate.contentType = contentType }
}
