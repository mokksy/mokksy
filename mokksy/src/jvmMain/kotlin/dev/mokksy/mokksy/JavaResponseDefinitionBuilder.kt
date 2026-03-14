package dev.mokksy.mokksy

import dev.mokksy.mokksy.response.ResponseDefinitionBuilder
import io.ktor.http.ContentType

/**
 * Java-friendly wrapper around [ResponseDefinitionBuilder] that returns `this` from every
 * mutating method, enabling fluent chaining without Kotlin-specific idioms.
 *
 * Java callers receive this type from [JavaBuildingStep.respondsWith] and use it to configure
 * the HTTP response for a stub:
 * ```java
 * mokksy.post(spec -> spec.path("/items"))
 *     .respondsWith(builder -> builder
 *         .body("{\"id\":\"42\"}")
 *         .status(201)
 *         .header("Location", "/items/42")
 *         .delayMillis(50));
 * ```
 *
 * For typed response bodies, supply the response class to `respondsWith`:
 * ```java
 * mokksy.post(MyRequest.class, spec -> spec.path("/items"))
 *     .respondsWith(MyResponse.class, builder -> builder
 *         .body(new MyResponse("created"))
 *         .status(201));
 * ```
 *
 * @param P The type of the request payload.
 * @param T The type of the response body.
 */
public class JavaResponseDefinitionBuilder<P : Any, T : Any> internal constructor(
    private val delegate: ResponseDefinitionBuilder<P, T>,
) {
    /**
     * Sets the response body.
     *
     * @param value The body value. Type is enforced at the call site via generic inference.
     * @return This builder instance.
     */
    public fun body(value: T): JavaResponseDefinitionBuilder<P, T> = apply { delegate.body(value) }

    /**
     * Sets the HTTP status code.
     *
     * @param code The status code as an integer, e.g. `201`, `404`.
     * @return This builder instance.
     */
    public fun status(code: Int): JavaResponseDefinitionBuilder<P, T> =
        apply { delegate.status(code) }

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
    ): JavaResponseDefinitionBuilder<P, T> = apply { delegate.header(name, value) }

    /**
     * Sets a delay before the response is sent.
     *
     * @param millis The delay in milliseconds.
     * @return This builder instance.
     */
    public fun delayMillis(millis: Long): JavaResponseDefinitionBuilder<P, T> =
        apply { delegate.delayMillis(millis) }

    /**
     * Sets the `Content-Type` of the response.
     *
     * @param contentType The content type, e.g. `ContentType.Application.Json`.
     * @return This builder instance.
     */
    public fun contentType(contentType: ContentType): JavaResponseDefinitionBuilder<P, T> =
        apply { delegate.contentType = contentType }

    /**
     * Sets the `Content-Type` of the response from a MIME-type string.
     *
     * @param contentType A content-type string such as `"application/json"`.
     * @return This builder instance.
     */
    public fun contentType(contentType: String): JavaResponseDefinitionBuilder<P, T> =
        apply { delegate.contentType = ContentType.parse(contentType) }
}
