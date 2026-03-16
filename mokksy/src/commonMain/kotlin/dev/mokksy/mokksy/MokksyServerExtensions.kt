@file:Suppress("TooManyFunctions")

package dev.mokksy.mokksy

import dev.mokksy.mokksy.request.RequestSpecificationBuilder
import io.ktor.http.HttpMethod
import kotlin.jvm.JvmSynthetic

/**
 * Reified Kotlin-only overloads of [MokksyServer] stub-registration methods.
 *
 * Three-layer API design:
 *
 * 1. **String shortcuts** (member functions) — most common, body type is `String`:
 *    ```kotlin
 *    mokksy.post { path("/items") } respondsWith { body = "ok" }
 *    ```
 *
 * 2. **Reified extensions** (this file, `@JvmSynthetic`) — typed, type inferred at call site:
 *    ```kotlin
 *    mokksy.post<MyRequest> { path("/items") } respondsWith { ... }
 *    ```
 *
 * 3. **KClass members** (member functions) — typed, for dynamic types or Java callers:
 *    ```kotlin
 *    mokksy.post(name = "stub", requestType = MyRequest::class) { path("/items") }
 *    ```
 *
 * All extensions here are `@JvmSynthetic` — Java callers use layer 1 or 3 directly.
 */

// region method

/**
 * Reified shortcut for [MokksyServer.method] with a [StubConfiguration]. Infers [P] from the call site.
 *
 * @param P The expected type of the request body.
 * @param configuration The [StubConfiguration] controlling stub behaviour (name, priority, etc.).
 * @param httpMethod The HTTP method to match.
 * @param block Lambda to configure the [RequestSpecificationBuilder].
 * @return A [BuildingStep] for response configuration and stub registration.
 */
@JvmSynthetic
public inline fun <reified P : Any> MokksyServer.method(
    configuration: StubConfiguration,
    httpMethod: HttpMethod,
    noinline block: RequestSpecificationBuilder<P>.() -> Unit,
): BuildingStep<P> = method(configuration, httpMethod, P::class, block)

/**
 * Reified shortcut for [MokksyServer.method] with an optional stub name. Infers [P] from the call site.
 *
 * @param P The expected type of the request body.
 * @param name Optional identifier for this stub.
 * @param httpMethod The HTTP method to match.
 * @param block Lambda to configure the [RequestSpecificationBuilder].
 * @return A [BuildingStep] for response configuration and stub registration.
 */
@JvmSynthetic
public inline fun <reified P : Any> MokksyServer.method(
    name: String? = null,
    httpMethod: HttpMethod,
    noinline block: RequestSpecificationBuilder<P>.() -> Unit,
): BuildingStep<P> = method(name, httpMethod, P::class, block)

// endregion

// region GET

/**
 * Reified shortcut for [MokksyServer.get] with an optional stub name. Infers [P] from the call site.
 *
 * @param P The expected type of the request body.
 * @param name Optional identifier for this stub.
 * @param block Lambda to configure the [RequestSpecificationBuilder].
 * @return A [BuildingStep] for response configuration and stub registration.
 */
@JvmSynthetic
public inline fun <reified P : Any> MokksyServer.get(
    name: String? = null,
    noinline block: RequestSpecificationBuilder<P>.() -> Unit,
): BuildingStep<P> = get(name, P::class, block)

/**
 * Reified shortcut for [MokksyServer.get] with a [StubConfiguration]. Infers [P] from the call site.
 *
 * @param P The expected type of the request body.
 * @param configuration The [StubConfiguration] controlling stub behaviour (name, priority, etc.).
 * @param block Lambda to configure the [RequestSpecificationBuilder].
 * @return A [BuildingStep] for response configuration and stub registration.
 */
@JvmSynthetic
public inline fun <reified P : Any> MokksyServer.get(
    configuration: StubConfiguration,
    noinline block: RequestSpecificationBuilder<P>.() -> Unit,
): BuildingStep<P> = get(configuration, P::class, block)

// endregion

// region POST

/**
 * Reified shortcut for [MokksyServer.post] with an optional stub name. Infers [P] from the call site.
 *
 * @param P The expected type of the request body.
 * @param name Optional identifier for this stub.
 * @param block Lambda to configure the [RequestSpecificationBuilder].
 * @return A [BuildingStep] for response configuration and stub registration.
 */
@JvmSynthetic
public inline fun <reified P : Any> MokksyServer.post(
    name: String? = null,
    noinline block: RequestSpecificationBuilder<P>.() -> Unit,
): BuildingStep<P> = post(name, P::class, block)

/**
 * Reified shortcut for [MokksyServer.post] with a [StubConfiguration]. Infers [P] from the call site.
 *
 * @param P The expected type of the request body.
 * @param configuration The [StubConfiguration] controlling stub behaviour (name, priority, etc.).
 * @param block Lambda to configure the [RequestSpecificationBuilder].
 * @return A [BuildingStep] for response configuration and stub registration.
 */
@JvmSynthetic
public inline fun <reified P : Any> MokksyServer.post(
    configuration: StubConfiguration,
    noinline block: RequestSpecificationBuilder<P>.() -> Unit,
): BuildingStep<P> = post(configuration, P::class, block)

// endregion

// region PUT

/**
 * Reified shortcut for [MokksyServer.put] with an optional stub name. Infers [P] from the call site.
 *
 * @param P The expected type of the request body.
 * @param name Optional identifier for this stub.
 * @param block Lambda to configure the [RequestSpecificationBuilder].
 * @return A [BuildingStep] for response configuration and stub registration.
 */
@JvmSynthetic
public inline fun <reified P : Any> MokksyServer.put(
    name: String? = null,
    noinline block: RequestSpecificationBuilder<P>.() -> Unit,
): BuildingStep<P> = put(name, P::class, block)

/**
 * Reified shortcut for [MokksyServer.put] with a [StubConfiguration]. Infers [P] from the call site.
 *
 * @param P The expected type of the request body.
 * @param configuration The [StubConfiguration] controlling stub behaviour (name, priority, etc.).
 * @param block Lambda to configure the [RequestSpecificationBuilder].
 * @return A [BuildingStep] for response configuration and stub registration.
 */
@JvmSynthetic
public inline fun <reified P : Any> MokksyServer.put(
    configuration: StubConfiguration,
    noinline block: RequestSpecificationBuilder<P>.() -> Unit,
): BuildingStep<P> = put(configuration, P::class, block)

// endregion

// region DELETE

/**
 * Reified shortcut for [MokksyServer.delete] with an optional stub name. Infers [P] from the call site.
 *
 * @param P The expected type of the request body.
 * @param name Optional identifier for this stub.
 * @param block Lambda to configure the [RequestSpecificationBuilder].
 * @return A [BuildingStep] for response configuration and stub registration.
 */
@JvmSynthetic
public inline fun <reified P : Any> MokksyServer.delete(
    name: String? = null,
    noinline block: RequestSpecificationBuilder<P>.() -> Unit,
): BuildingStep<P> = delete(name, P::class, block)

/**
 * Reified shortcut for [MokksyServer.delete] with a [StubConfiguration]. Infers [P] from the call site.
 *
 * @param P The expected type of the request body.
 * @param configuration The [StubConfiguration] controlling stub behaviour (name, priority, etc.).
 * @param block Lambda to configure the [RequestSpecificationBuilder].
 * @return A [BuildingStep] for response configuration and stub registration.
 */
@JvmSynthetic
public inline fun <reified P : Any> MokksyServer.delete(
    configuration: StubConfiguration,
    noinline block: RequestSpecificationBuilder<P>.() -> Unit,
): BuildingStep<P> = delete(configuration, P::class, block)

// endregion

// region PATCH

/**
 * Reified shortcut for [MokksyServer.patch] with an optional stub name. Infers [P] from the call site.
 *
 * @param P The expected type of the request body.
 * @param name Optional identifier for this stub.
 * @param block Lambda to configure the [RequestSpecificationBuilder].
 * @return A [BuildingStep] for response configuration and stub registration.
 */
@JvmSynthetic
public inline fun <reified P : Any> MokksyServer.patch(
    name: String? = null,
    noinline block: RequestSpecificationBuilder<P>.() -> Unit,
): BuildingStep<P> = patch(name, P::class, block)

/**
 * Reified shortcut for [MokksyServer.patch] with a [StubConfiguration]. Infers [P] from the call site.
 *
 * @param P The expected type of the request body.
 * @param configuration The [StubConfiguration] controlling stub behaviour (name, priority, etc.).
 * @param block Lambda to configure the [RequestSpecificationBuilder].
 * @return A [BuildingStep] for response configuration and stub registration.
 */
@JvmSynthetic
public inline fun <reified P : Any> MokksyServer.patch(
    configuration: StubConfiguration,
    noinline block: RequestSpecificationBuilder<P>.() -> Unit,
): BuildingStep<P> = patch(configuration, P::class, block)

// endregion

// region HEAD

/**
 * Reified shortcut for [MokksyServer.head] with an optional stub name. Infers [P] from the call site.
 *
 * @param P The expected type of the request body.
 * @param name Optional identifier for this stub.
 * @param block Lambda to configure the [RequestSpecificationBuilder].
 * @return A [BuildingStep] for response configuration and stub registration.
 */
@JvmSynthetic
public inline fun <reified P : Any> MokksyServer.head(
    name: String? = null,
    noinline block: RequestSpecificationBuilder<P>.() -> Unit,
): BuildingStep<P> = head(name, P::class, block)

/**
 * Reified shortcut for [MokksyServer.head] with a [StubConfiguration]. Infers [P] from the call site.
 *
 * @param P The expected type of the request body.
 * @param configuration The [StubConfiguration] controlling stub behaviour (name, priority, etc.).
 * @param block Lambda to configure the [RequestSpecificationBuilder].
 * @return A [BuildingStep] for response configuration and stub registration.
 */
@JvmSynthetic
public inline fun <reified P : Any> MokksyServer.head(
    configuration: StubConfiguration,
    noinline block: RequestSpecificationBuilder<P>.() -> Unit,
): BuildingStep<P> = head(configuration, P::class, block)

// endregion

// region OPTIONS

/**
 * Reified shortcut for [MokksyServer.options] with an optional stub name. Infers [P] from the call site.
 *
 * @param P The expected type of the request body.
 * @param name Optional identifier for this stub.
 * @param block Lambda to configure the [RequestSpecificationBuilder].
 * @return A [BuildingStep] for response configuration and stub registration.
 */
@JvmSynthetic
public inline fun <reified P : Any> MokksyServer.options(
    name: String? = null,
    noinline block: RequestSpecificationBuilder<P>.() -> Unit,
): BuildingStep<P> = options(name, P::class, block)

/**
 * Reified shortcut for [MokksyServer.options] with a [StubConfiguration]. Infers [P] from the call site.
 *
 * @param P The expected type of the request body.
 * @param configuration The [StubConfiguration] controlling stub behaviour (name, priority, etc.).
 * @param block Lambda to configure the [RequestSpecificationBuilder].
 * @return A [BuildingStep] for response configuration and stub registration.
 */
@JvmSynthetic
public inline fun <reified P : Any> MokksyServer.options(
    configuration: StubConfiguration,
    noinline block: RequestSpecificationBuilder<P>.() -> Unit,
): BuildingStep<P> = options(configuration, P::class, block)

// endregion
