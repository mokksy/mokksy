package dev.mokksy.mokksy

import dev.mokksy.mokksy.response.ResponseDefinitionBuilder
import dev.mokksy.mokksy.response.StreamingResponseDefinitionBuilder
import java.util.function.Consumer

/**
 * A Java-friendly wrapper around [BuildingStep] that exposes `respondsWith` and
 * `respondsWithStream` as instance methods accepting [Consumer] instead of
 * Kotlin suspend lambdas.
 *
 * Instances are returned by [MokksyServerJava]'s HTTP method stubs (`get`, `post`, etc.).
 * Do not construct directly.
 *
 * Example (Java):
 * ```java
 * mokksy.get(spec -> spec.path("/ping"))
 *       .respondsWith(builder -> builder.setBody("Pong"));
 *
 * mokksy.post(MyRequest.class, spec -> spec.path("/items"))
 *       .respondsWith(MyResponse.class, builder -> {
 *           builder.setBody(new MyResponse("created"));
 *           builder.httpStatus(201);
 *           builder.addHeader("Location", "/items/1");
 *       });
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
     * @param configurer A [Consumer] that configures the [ResponseDefinitionBuilder].
     */
    public fun <T : Any> respondsWith(
        responseType: Class<T>,
        configurer: Consumer<ResponseDefinitionBuilder<P, T>>,
    ): Unit = step.respondsWith(responseType.kotlin) { configurer.accept(this) }

    /**
     * Configures a [String] response for this stub.
     *
     * Shorthand for `respondsWith(String.class, configurer)`.
     *
     * @param configurer A [Consumer] that configures the [ResponseDefinitionBuilder].
     */
    public fun respondsWith(
        configurer: Consumer<ResponseDefinitionBuilder<P, String>>,
    ): Unit = respondsWith(String::class.java, configurer)

    /**
     * Configures a typed streaming response for this stub.
     *
     * @param T The type of elements in the streaming response.
     * @param responseType The Java [Class] of the streaming element type.
     * @param configurer A [Consumer] that configures the [StreamingResponseDefinitionBuilder].
     */
    public fun <T : Any> respondsWithStream(
        responseType: Class<T>,
        configurer: Consumer<StreamingResponseDefinitionBuilder<P, T>>,
    ): Unit = step.respondsWithStream(responseType.kotlin) { configurer.accept(this) }

    /**
     * Configures a [String] streaming response for this stub.
     *
     * Shorthand for `respondsWithStream(String.class, configurer)`.
     *
     * @param configurer A [Consumer] that configures the [StreamingResponseDefinitionBuilder].
     */
    public fun respondsWithStream(
        configurer: Consumer<StreamingResponseDefinitionBuilder<P, String>>,
    ): Unit = respondsWithStream(String::class.java, configurer)
}
