@file:JvmName("MokksyServerMethods")
@file:Suppress("TooManyFunctions")

package dev.mokksy.mokksy

import dev.mokksy.mokksy.request.RequestSpecificationBuilder
import io.ktor.http.HttpMethod
import kotlin.jvm.JvmName

/*
 * Reified overloads of [MokksyServer] stub-registration methods.
 *
 * These extensions infer the request-body type [P] from the call site, eliminating the need to
 * pass an explicit [kotlin.reflect.KClass] argument:
 *
 * ```kotlin
 * // before
 * mokksy.post(requestType = MyRequest::class) { path("/items") } respondsWith { ... }
 *
 * // after
 * mokksy.post<MyRequest> { path("/items") } respondsWith { ... }
 * ```
 *
 * The underlying [MokksyServer] member functions (which accept a [kotlin.reflect.KClass]) remain
 * available for cases where the type is not known at compile time.
 */

// region method

/** Reified shortcut for [MokksyServer.method] with a [StubConfiguration]. */
public inline fun <reified P : Any> MokksyServer.method(
    configuration: StubConfiguration,
    httpMethod: HttpMethod,
    noinline block: RequestSpecificationBuilder<P>.() -> Unit,
): BuildingStep<P> = method(configuration, httpMethod, P::class, block)

/** Reified shortcut for [MokksyServer.method] with an optional stub name. */
public inline fun <reified P : Any> MokksyServer.method(
    name: String? = null,
    httpMethod: HttpMethod,
    noinline block: RequestSpecificationBuilder<P>.() -> Unit,
): BuildingStep<P> = method(name, httpMethod, P::class, block)

// endregion

// region GET

/** Reified shortcut for [MokksyServer.get] with an optional stub name. */
public inline fun <reified P : Any> MokksyServer.get(
    name: String? = null,
    noinline block: RequestSpecificationBuilder<P>.() -> Unit,
): BuildingStep<P> = get(name, P::class, block)

/** Reified shortcut for [MokksyServer.get] with a [StubConfiguration]. */
public inline fun <reified P : Any> MokksyServer.get(
    configuration: StubConfiguration,
    noinline block: RequestSpecificationBuilder<P>.() -> Unit,
): BuildingStep<P> = get(configuration, P::class, block)

// endregion

// region POST

/** Reified shortcut for [MokksyServer.post] with an optional stub name. */
public inline fun <reified P : Any> MokksyServer.post(
    name: String? = null,
    noinline block: RequestSpecificationBuilder<P>.() -> Unit,
): BuildingStep<P> = post(name, P::class, block)

/** Reified shortcut for [MokksyServer.post] with a [StubConfiguration]. */
public inline fun <reified P : Any> MokksyServer.post(
    configuration: StubConfiguration,
    noinline block: RequestSpecificationBuilder<P>.() -> Unit,
): BuildingStep<P> = post(configuration, P::class, block)

// endregion

// region PUT

/** Reified shortcut for [MokksyServer.put] with an optional stub name. */
public inline fun <reified P : Any> MokksyServer.put(
    name: String? = null,
    noinline block: RequestSpecificationBuilder<P>.() -> Unit,
): BuildingStep<P> = put(name, P::class, block)

/** Reified shortcut for [MokksyServer.put] with a [StubConfiguration]. */
public inline fun <reified P : Any> MokksyServer.put(
    configuration: StubConfiguration,
    noinline block: RequestSpecificationBuilder<P>.() -> Unit,
): BuildingStep<P> = put(configuration, P::class, block)

// endregion

// region DELETE

/** Reified shortcut for [MokksyServer.delete] with an optional stub name. */
public inline fun <reified P : Any> MokksyServer.delete(
    name: String? = null,
    noinline block: RequestSpecificationBuilder<P>.() -> Unit,
): BuildingStep<P> = delete(name, P::class, block)

/** Reified shortcut for [MokksyServer.delete] with a [StubConfiguration]. */
public inline fun <reified P : Any> MokksyServer.delete(
    configuration: StubConfiguration,
    noinline block: RequestSpecificationBuilder<P>.() -> Unit,
): BuildingStep<P> = delete(configuration, P::class, block)

// endregion

// region PATCH

/** Reified shortcut for [MokksyServer.patch] with an optional stub name. */
public inline fun <reified P : Any> MokksyServer.patch(
    name: String? = null,
    noinline block: RequestSpecificationBuilder<P>.() -> Unit,
): BuildingStep<P> = patch(name, P::class, block)

/** Reified shortcut for [MokksyServer.patch] with a [StubConfiguration]. */
public inline fun <reified P : Any> MokksyServer.patch(
    configuration: StubConfiguration,
    noinline block: RequestSpecificationBuilder<P>.() -> Unit,
): BuildingStep<P> = patch(configuration, P::class, block)

// endregion

// region HEAD

/** Reified shortcut for [MokksyServer.head] with an optional stub name. */
public inline fun <reified P : Any> MokksyServer.head(
    name: String? = null,
    noinline block: RequestSpecificationBuilder<P>.() -> Unit,
): BuildingStep<P> = head(name, P::class, block)

/** Reified shortcut for [MokksyServer.head] with a [StubConfiguration]. */
public inline fun <reified P : Any> MokksyServer.head(
    configuration: StubConfiguration,
    noinline block: RequestSpecificationBuilder<P>.() -> Unit,
): BuildingStep<P> = head(configuration, P::class, block)

// endregion

// region OPTIONS

/** Reified shortcut for [MokksyServer.options] with an optional stub name. */
public inline fun <reified P : Any> MokksyServer.options(
    name: String? = null,
    noinline block: RequestSpecificationBuilder<P>.() -> Unit,
): BuildingStep<P> = options(name, P::class, block)

/** Reified shortcut for [MokksyServer.options] with a [StubConfiguration]. */
public inline fun <reified P : Any> MokksyServer.options(
    configuration: StubConfiguration,
    noinline block: RequestSpecificationBuilder<P>.() -> Unit,
): BuildingStep<P> = options(configuration, P::class, block)

// endregion
