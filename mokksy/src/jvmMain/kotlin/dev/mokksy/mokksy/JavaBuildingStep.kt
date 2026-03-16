package dev.mokksy.mokksy

import io.ktor.http.HttpStatusCode
import java.util.function.Consumer

/**
 * A Java-friendly wrapper around [BuildingStep] that exposes `respondsWith` and
 * `respondsWithStream` as instance methods accepting [Consumer] instead of
 * Kotlin suspend lambdas.
 *
 * Instances are returned by [dev.mokksy.Mokksy]'s HTTP method stubs (`get`, `post`, etc.).
 * Do not construct directly.
 *
 * Example (Java):
 * ```java
 * mokksy.get(spec -> spec.path("/ping"))
 *       .respondsWith(builder -> builder.body("Pong"));
 *
 * mokksy.post(MyRequest.class, spec -> spec.path("/items"))
 *       .respondsWith(MyResponse.class, builder -> builder
 *           .body(new MyResponse("created"))
 *           .status(201)
 *           .header("Location", "/items/1"));
 * ```
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
     */
    public fun <T : Any> respondsWith(
        responseType: Class<T>,
        configurer: Consumer<JavaResponseDefinitionBuilder<P, T>>,
    ): Unit =
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
     */
    public fun respondsWith(configurer: Consumer<JavaResponseDefinitionBuilder<P, String>>): Unit =
        respondsWith(String::class.java, configurer)

    /**
     * Configures a typed streaming response for this stub.
     *
     * @param T The type of elements in the streaming response.
     * @param responseType The Java [Class] of the streaming element type.
     * @param configurer A [Consumer] that configures a [JavaStreamingResponseDefinitionBuilder].
     */
    public fun <T : Any> respondsWithStream(
        responseType: Class<T>,
        configurer: Consumer<JavaStreamingResponseDefinitionBuilder<P, T>>,
    ): Unit =
        step.respondsWithStream(responseType.kotlin) {
            configurer.accept(JavaStreamingResponseDefinitionBuilder(this))
        }

    /**
     * Configures a [String] streaming response for this stub.
     *
     * Shorthand for `respondsWithStream(String.class, configurer)`.
     *
     * @param configurer A [Consumer] that configures a [JavaStreamingResponseDefinitionBuilder].
     */
    public fun respondsWithStream(
        configurer: Consumer<JavaStreamingResponseDefinitionBuilder<P, String>>,
    ): Unit = respondsWithStream(String::class.java, configurer)

    /**
     * Associates this stub with a body-free response carrying only an HTTP status code.
     *
     * Example (Java):
     * ```java
     * mokksy.get(spec -> spec.path("/ping")).respondsWithStatus(204);
     * ```
     *
     * @param statusCode The HTTP status code to return (e.g. `204`, `404`).
     */
    public fun respondsWithStatus(statusCode: Int): Unit =
        step.respondsWithStatus(HttpStatusCode.fromValue(statusCode))
}
