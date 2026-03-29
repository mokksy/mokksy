package dev.mokksy.mokksy

import dev.mokksy.mokksy.SseEvent.data
import io.ktor.sse.ServerSentEvent

/**
 * Java-friendly factory for creating [ServerSentEvent] instances without trailing nulls.
 *
 * The Ktor [ServerSentEvent] constructor requires all five parameters in Java because it lacks
 * `@JvmOverloads`. This utility provides concise alternatives:
 *
 * ```java
 * // Data-only (most common case)
 * SseEvent.data("Hello")
 *
 * // Multiple fields
 * SseEvent.builder().data("Hello").event("chat").id("1").build()
 * ```
 */
public object SseEvent {
    /**
     * Creates a [ServerSentEvent] with only the [data] field set.
     *
     * This is the most common SSE use case. All other fields (`event`, `id`, `retry`, `comments`)
     * default to `null`.
     *
     * ```java
     * mokksy.get(spec -> spec.path("/sse"))
     *     .respondsWithSseStream(builder -> builder
     *         .chunk(SseEvent.data("Hello"))
     *         .chunk(SseEvent.data("World")));
     * ```
     *
     * @param data The event data string.
     * @return A new [ServerSentEvent].
     */
    @JvmStatic
    public fun data(data: String): ServerSentEvent = ServerSentEvent(data = data)

    /**
     * Returns a new [SseEventBuilder] for constructing a [ServerSentEvent] with multiple fields.
     *
     * ```java
     * SseEvent.builder()
     *     .data("payload")
     *     .event("message")
     *     .id("42")
     *     .retry(5000L)
     *     .build()
     * ```
     *
     * @return A new, empty [SseEventBuilder].
     */
    @JvmStatic
    public fun builder(): SseEventBuilder = SseEventBuilder()
}

/**
 * Fluent builder for [ServerSentEvent].
 *
 * Obtain via [SseEvent.builder]. All fields are optional and default to `null`.
 */
public class SseEventBuilder internal constructor() {
    private var data: String? = null
    private var event: String? = null
    private var id: String? = null
    private var retry: Long? = null
    private var comments: String? = null

    /** Sets the data field. */
    public fun data(data: String): SseEventBuilder = apply { this.data = data }

    /** Sets the event type field. */
    public fun event(event: String): SseEventBuilder = apply { this.event = event }

    /** Sets the event ID field. */
    public fun id(id: String): SseEventBuilder = apply { this.id = id }

    /** Sets the retry interval in milliseconds. */
    public fun retry(retry: Long): SseEventBuilder = apply { this.retry = retry }

    /** Sets the comments field. */
    public fun comments(comments: String): SseEventBuilder = apply { this.comments = comments }

    /** Builds the [ServerSentEvent]. If no fields are set, produces an empty SSE keepalive event. */
    public fun build(): ServerSentEvent =
        ServerSentEvent(
            data = data,
            event = event,
            id = id,
            retry = retry,
            comments = comments,
        )
}
