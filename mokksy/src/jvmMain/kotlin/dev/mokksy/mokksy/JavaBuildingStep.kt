package dev.mokksy.mokksy

import io.ktor.http.HttpStatusCode
import io.ktor.sse.ServerSentEventMetadata
import java.util.function.Consumer

/**
 * A Java-friendly wrapper around [BuildingStep] that exposes `respondsWith`,
 * `respondsWithStream`, and `respondsWithSseStream` as instance methods accepting [Consumer]
 * instead of Kotlin suspend lambdas.
 *
 * Instances are returned by [dev.mokksy.Mokksy]'s HTTP method stubs (`get`, `post`, etc.).
 * Do not construct directly.
 *
 * Example (Java):
 * ```java
 * StubHandle ping = mokksy.get(spec -> spec.path("/ping"))
 *       .respondsWith(builder -> builder.body("Pong"));
 * assert ping.matchCount() == 0;
 * ```
 *
 * All response-definition methods return a [StubHandle] which exposes [StubHandle.matchCount]
 * and [StubHandle.name] for verifying stub invocation after tests.
 *
 * @param P The type of the request payload.
 */
public class JavaBuildingStep<P : Any> internal constructor(
    private val step: BuildingStep<P>,
) {
    /**
     * Configures a typed response for this stub.
     *
     * @param T The type of the response body.
     * @param responseType The Java [Class] of the response type.
     * @param configurer A [Consumer] that configures a [JavaResponseDefinitionBuilder].
     * @return A [StubHandle] for further inspection (e.g. [StubHandle.matchCount]).
     */
    public fun <T : Any> respondsWith(
        responseType: Class<T>,
        configurer: Consumer<JavaResponseDefinitionBuilder<P, T>>,
    ): StubHandle =
        step.respondsWith(responseType.kotlin) {
            configurer.accept(
                JavaResponseDefinitionBuilder(this),
            )
        }

    /**
     * Configures a [String] response for this stub.
     *
     * Shorthand for `respondsWith(String.class, configurer)`.
     *
     * @param configurer A [Consumer] that configures a [JavaResponseDefinitionBuilder].
     * @return A [StubHandle] for further inspection (e.g. [StubHandle.matchCount]).
     */
    public fun respondsWith(
        configurer: Consumer<JavaResponseDefinitionBuilder<P, String>>,
    ): StubHandle = respondsWith(String::class.java, configurer)

    /**
     * Configures a typed streaming response for this stub.
     *
     * @param T The type of elements in the streaming response.
     * @param responseType The Java [Class] of the streaming element type.
     * @param configurer A [Consumer] that configures a [JavaStreamingResponseDefinitionBuilder].
     * @return A [StubHandle] for further inspection (e.g. [StubHandle.matchCount]).
     */
    public fun <T : Any> respondsWithStream(
        responseType: Class<T>,
        configurer: Consumer<JavaStreamingResponseDefinitionBuilder<P, T>>,
    ): StubHandle =
        step.respondsWithStream(responseType.kotlin) {
            configurer.accept(JavaStreamingResponseDefinitionBuilder(this))
        }

    /**
     * Configures a [String] streaming response for this stub.
     *
     * Shorthand for `respondsWithStream(String.class, configurer)`.
     *
     * @param configurer A [Consumer] that configures a [JavaStreamingResponseDefinitionBuilder].
     * @return A [StubHandle] for further inspection (e.g. [StubHandle.matchCount]).
     */
    public fun respondsWithStream(
        configurer: Consumer<JavaStreamingResponseDefinitionBuilder<P, String>>,
    ): StubHandle = respondsWithStream(String::class.java, configurer)

    /**
     * Configures a typed SSE streaming response for this stub.
     *
     * The [dataType] parameter represents the type of the `data` field in each
     * [ServerSentEventMetadata] event. The consumer receives a
     * [JavaStreamingResponseDefinitionBuilder] parameterised over [ServerSentEventMetadata]`<T>`,
     * so chunks should be `ServerSentEvent` or `TypedServerSentEvent` instances.
     *
     * Example (Java):
     * ```java
     * mokksy.get(spec -> spec.path("/sse"))
     *       .respondsWithSseStream(String.class, builder -> builder
     *           .chunk(new ServerSentEvent("Hello", null, null, null, null))
     *           .chunk(new ServerSentEvent("World", null, null, null, null)));
     * ```
     *
     * @param T The type of the `data` field in each SSE event.
     * @param dataType The Java [Class] of the SSE event data type.
     * @param configurer A [Consumer] that configures a [JavaStreamingResponseDefinitionBuilder].
     * @return A [StubHandle] for further inspection (e.g. [StubHandle.matchCount]).
     */
    public fun <T : Any> respondsWithSseStream(
        dataType: Class<T>,
        configurer: Consumer<JavaStreamingResponseDefinitionBuilder<P, ServerSentEventMetadata<T>>>,
    ): StubHandle =
        step.respondsWithSseStream(dataType.kotlin) {
            configurer.accept(JavaStreamingResponseDefinitionBuilder(this))
        }

    /**
     * Configures an SSE streaming response for this stub with [ServerSentEventMetadata]`<String>` chunks.
     *
     * Shorthand for `respondsWithSseStream(String.class, configurer)`.
     *
     * @param configurer A [Consumer] that configures a [JavaStreamingResponseDefinitionBuilder].
     * @return A [StubHandle] for further inspection (e.g. [StubHandle.matchCount]).
     */
    public fun respondsWithSseStream(
        configurer:
            Consumer<JavaStreamingResponseDefinitionBuilder<P, ServerSentEventMetadata<String>>>,
    ): StubHandle =
        step.respondsWithSseStream(String::class) {
            configurer.accept(JavaStreamingResponseDefinitionBuilder(this))
        }

    /**
     * Configures a simple [String] response body for this stub with HTTP 200.
     *
     * Shorthand for `respondsWith(builder -> builder.body(body))`:
     * ```java
     * mokksy.get("/hello").respondsWith("Hello, World!");
     * ```
     *
     * @param body The response body string.
     * @return A [StubHandle] for further inspection (e.g. [StubHandle.matchCount]).
     */
    public fun respondsWith(body: String): StubHandle = respondsWith { it.body(body) }

    /**
     * Configures a simple [String] response body with a custom HTTP status code.
     *
     * Shorthand for `respondsWith(builder -> builder.body(body).status(statusCode))`:
     * ```java
     * mokksy.post("/items").respondsWith("{\"id\":42}", 201);
     * ```
     *
     * @param body The response body string.
     * @param statusCode The HTTP status code, e.g. `201`, `400`.
     * @return A [StubHandle] for further inspection (e.g. [StubHandle.matchCount]).
     */
    public fun respondsWith(
        body: String,
        statusCode: Int,
    ): StubHandle = respondsWith { it.body(body).status(statusCode) }

    /**
     * Associates this stub with a body-free response carrying only an HTTP status code.
     *
     * Example (Java):
     * ```java
     * mokksy.get(spec -> spec.path("/ping")).respondsWithStatus(204);
     * ```
     *
     * @param statusCode The HTTP status code to return (e.g. `204`, `404`).
     * @return A [StubHandle] for further inspection (e.g. [StubHandle.matchCount]).
     */
    public fun respondsWithStatus(statusCode: Int): StubHandle =
        step.respondsWithStatus(HttpStatusCode.fromValue(statusCode))
}
