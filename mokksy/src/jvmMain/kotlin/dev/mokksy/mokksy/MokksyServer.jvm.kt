@file:JvmName("MokksyJava")
@file:JvmMultifileClass

package dev.mokksy.mokksy

import dev.mokksy.mokksy.response.ResponseDefinitionBuilder
import dev.mokksy.mokksy.response.StreamingResponseDefinitionBuilder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import java.util.function.Consumer

// region Lifecycle

/**
 * Starts the Mokksy server and blocks until the port is bound and ready to accept requests.
 *
 * Intended for Java callers and blocking JVM test setups. Kotlin callers should prefer
 * [startSuspend] combined with [awaitStarted].
 */
public fun Mokksy.start(): Unit = runBlocking {
    this@start.startSuspend()
    this@start.awaitStarted()
}

/**
 * Starts the Mokksy server on the given dispatcher, blocking until the port is bound.
 *
 * The [dispatcher] does not affect which thread is blocked — [runBlocking] always blocks
 * the calling thread. This overload exists for Kotlin callers that need coroutine context
 * control; Java callers should use [start] with no arguments.
 *
 * @param dispatcher The [CoroutineDispatcher] for the [runBlocking] coroutine context.
 */
@JvmSynthetic
public fun Mokksy.start(dispatcher: CoroutineDispatcher): Unit = runBlocking(dispatcher) {
    this@start.startSuspend()
    this@start.awaitStarted()
}

/**
 * Stops the Mokksy server, blocking until shutdown is complete.
 *
 * @param gracePeriodMillis Duration in milliseconds for graceful shutdown. Defaults to 500.
 * @param timeoutMillis Maximum duration in milliseconds to wait for shutdown. Defaults to 1000.
 */
@JvmOverloads
public fun Mokksy.shutdown(
    gracePeriodMillis: Long = 500,
    timeoutMillis: Long = 1000,
): Unit = runBlocking {
    this@shutdown.shutdownSuspend(gracePeriodMillis, timeoutMillis)
}

/**
 * Stops the Mokksy server on the given dispatcher, blocking until shutdown is complete.
 *
 * The [dispatcher] does not affect which thread is blocked. Java callers should use
 * [shutdown] without a dispatcher argument.
 *
 * @param gracePeriodMillis Duration in milliseconds for graceful shutdown. Defaults to 500.
 * @param timeoutMillis Maximum duration in milliseconds to wait for shutdown. Defaults to 1000.
 * @param dispatcher The [CoroutineDispatcher] for the [runBlocking] coroutine context.
 */
@JvmSynthetic
public fun Mokksy.shutdown(
    gracePeriodMillis: Long = 500,
    timeoutMillis: Long = 1000,
    dispatcher: CoroutineDispatcher,
): Unit = runBlocking(dispatcher) {
    this@shutdown.shutdownSuspend(gracePeriodMillis, timeoutMillis)
}

// endregion

// region Java-friendly respondsWith overloads

/**
 * Java-friendly overload for [BuildingStep.respondsWith].
 *
 * Accepts a [Class] and a [Consumer] in place of a Kotlin suspend lambda, removing
 * `Continuation`, `Unit.INSTANCE`, and [kotlin.jvm.JvmClassMappingKt.getKotlinClass]
 * from Java call sites.
 *
 * Example (Java):
 * ```java
 * BuildingStep<String> step = mokksy.get(spec -> { spec.path("/ping"); return Unit.INSTANCE; });
 * MokksyJava.respondsWith(step, String.class, builder -> builder.setBody("Pong"));
 * ```
 *
 * @param T The type of the response body.
 * @param responseType The Java [Class] of the response type.
 * @param configurer A [Consumer] applied to a [ResponseDefinitionBuilder] to configure the response.
 */
public fun <P : Any, T : Any> BuildingStep<P>.respondsWith(
    responseType: Class<T>,
    configurer: Consumer<ResponseDefinitionBuilder<P, T>>,
): Unit = respondsWith(responseType.kotlin) { configurer.accept(this) }

/**
 * Java-friendly overload for [BuildingStep.respondsWithStream].
 *
 * Accepts a [Class] and a [Consumer] in place of a Kotlin suspend lambda.
 *
 * @param T The type of elements in the streaming response.
 * @param responseType The Java [Class] of the streaming element type.
 * @param configurer A [Consumer] applied to a [StreamingResponseDefinitionBuilder] to configure the response.
 */
public fun <P : Any, T : Any> BuildingStep<P>.respondsWithStream(
    responseType: Class<T>,
    configurer: Consumer<StreamingResponseDefinitionBuilder<P, T>>,
): Unit = respondsWithStream(responseType.kotlin) { configurer.accept(this) }

// endregion
