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

/**
 * Registers a stub for any HTTP method with a typed request body, using an explicit
 * [StubConfiguration]. The type parameter [P] is inferred at the call site — no explicit
 * [kotlin.reflect.KClass] argument required.
 *
 * Example:
 * ```kotlin
 * mokksy.method<UpdateRequest>(StubConfiguration(name = "update-item"), HttpMethod.Put) {
 *     path("/items/1")
 * } respondsWith {
 *     body = """{"updated":true}"""
 * }
 * ```
 *
 * @param P The expected type of the deserialized request body.
 * @param configuration Stub configuration controlling the stub name, removal behaviour, and verbosity.
 * @param httpMethod The HTTP method to match.
 * @param block Lambda applied to [RequestSpecificationBuilder] to configure request matching.
 * @return A [BuildingStep] used to define the response via [BuildingStep.respondsWith]
 *   or [BuildingStep.respondsWithStream].
 * @see MokksyServer.method
 */
public inline fun <reified P : Any> MokksyServer.method(
    configuration: StubConfiguration,
    httpMethod: HttpMethod,
    noinline block: RequestSpecificationBuilder<P>.() -> Unit,
): BuildingStep<P> = method(configuration, httpMethod, P::class, block)

/**
 * Registers a stub for any HTTP method with a typed request body, optionally assigning a [name]
 * to the stub. The type parameter [P] is inferred at the call site — no explicit
 * [kotlin.reflect.KClass] argument required.
 *
 * Example:
 * ```kotlin
 * mokksy.method<UpdateRequest>(name = "update-item", HttpMethod.Put) {
 *     path("/items/1")
 * } respondsWith {
 *     body = """{"updated":true}"""
 * }
 * ```
 *
 * @param P The expected type of the deserialized request body.
 * @param name Optional identifier for the stub.
 * @param httpMethod The HTTP method to match.
 * @param block Lambda applied to [RequestSpecificationBuilder] to configure request matching.
 * @return A [BuildingStep] used to define the response via [BuildingStep.respondsWith]
 *   or [BuildingStep.respondsWithStream].
 * @see MokksyServer.method
 */
public inline fun <reified P : Any> MokksyServer.method(
    name: String? = null,
    httpMethod: HttpMethod,
    noinline block: RequestSpecificationBuilder<P>.() -> Unit,
): BuildingStep<P> = method(name, httpMethod, P::class, block)

// endregion

// region GET

/**
 * Registers a stub for HTTP GET requests with a typed request body, optionally assigning a [name]
 * to the stub. The type parameter [P] is inferred at the call site — no explicit
 * [kotlin.reflect.KClass] argument required.
 *
 * Example:
 * ```kotlin
 * mokksy.get<SearchRequest>(name = "search") {
 *     path("/search")
 *     containsHeader("Accept", "application/json")
 * } respondsWith {
 *     body = """{"results":[]}"""
 * }
 * ```
 *
 * @param P The expected type of the deserialized request body.
 * @param name Optional identifier for the stub.
 * @param block Lambda applied to [RequestSpecificationBuilder] to configure request matching.
 * @return A [BuildingStep] used to define the response via [BuildingStep.respondsWith]
 *   or [BuildingStep.respondsWithStream].
 * @see MokksyServer.get
 */
public inline fun <reified P : Any> MokksyServer.get(
    name: String? = null,
    noinline block: RequestSpecificationBuilder<P>.() -> Unit,
): BuildingStep<P> = get(name, P::class, block)

/**
 * Registers a stub for HTTP GET requests with a typed request body, using an explicit
 * [StubConfiguration]. The type parameter [P] is inferred at the call site — no explicit
 * [kotlin.reflect.KClass] argument required.
 *
 * Example:
 * ```kotlin
 * mokksy.get<SearchRequest>(StubConfiguration(name = "search", removeAfterMatch = true)) {
 *     path("/search")
 * } respondsWith {
 *     body = """{"results":[]}"""
 * }
 * ```
 *
 * @param P The expected type of the deserialized request body.
 * @param configuration Stub configuration controlling the stub name, removal behaviour, and verbosity.
 * @param block Lambda applied to [RequestSpecificationBuilder] to configure request matching.
 * @return A [BuildingStep] used to define the response via [BuildingStep.respondsWith]
 *   or [BuildingStep.respondsWithStream].
 * @see MokksyServer.get
 */
public inline fun <reified P : Any> MokksyServer.get(
    configuration: StubConfiguration,
    noinline block: RequestSpecificationBuilder<P>.() -> Unit,
): BuildingStep<P> = get(configuration, P::class, block)

// endregion

// region POST

/**
 * Registers a stub for HTTP POST requests with a typed request body, optionally assigning a [name]
 * to the stub. The type parameter [P] is inferred at the call site — no explicit
 * [kotlin.reflect.KClass] argument required.
 *
 * Example:
 * ```kotlin
 * mokksy.post<CreateItemRequest>(name = "create-item") {
 *     path("/items")
 *     bodyMatchesPredicate { it?.name?.isNotBlank() == true }
 * } respondsWith {
 *     body = """{"id":"1"}"""
 *     httpStatus = HttpStatusCode.Created
 * }
 * ```
 *
 * @param P The expected type of the deserialized request body.
 * @param name Optional identifier for the stub.
 * @param block Lambda applied to [RequestSpecificationBuilder] to configure request matching.
 * @return A [BuildingStep] used to define the response via [BuildingStep.respondsWith]
 *   or [BuildingStep.respondsWithStream].
 * @see MokksyServer.post
 */
public inline fun <reified P : Any> MokksyServer.post(
    name: String? = null,
    noinline block: RequestSpecificationBuilder<P>.() -> Unit,
): BuildingStep<P> = post(name, P::class, block)

/**
 * Registers a stub for HTTP POST requests with a typed request body, using an explicit
 * [StubConfiguration]. The type parameter [P] is inferred at the call site — no explicit
 * [kotlin.reflect.KClass] argument required.
 *
 * Example:
 * ```kotlin
 * mokksy.post<CreateItemRequest>(StubConfiguration(name = "create-item", removeAfterMatch = true)) {
 *     path("/items")
 * } respondsWith {
 *     body = """{"id":"1"}"""
 *     httpStatus = HttpStatusCode.Created
 * }
 * ```
 *
 * @param P The expected type of the deserialized request body.
 * @param configuration Stub configuration controlling the stub name, removal behaviour, and verbosity.
 * @param block Lambda applied to [RequestSpecificationBuilder] to configure request matching.
 * @return A [BuildingStep] used to define the response via [BuildingStep.respondsWith]
 *   or [BuildingStep.respondsWithStream].
 * @see MokksyServer.post
 */
public inline fun <reified P : Any> MokksyServer.post(
    configuration: StubConfiguration,
    noinline block: RequestSpecificationBuilder<P>.() -> Unit,
): BuildingStep<P> = post(configuration, P::class, block)

// endregion

// region PUT

/**
 * Registers a stub for HTTP PUT requests with a typed request body, optionally assigning a [name]
 * to the stub. The type parameter [P] is inferred at the call site — no explicit
 * [kotlin.reflect.KClass] argument required.
 *
 * Example:
 * ```kotlin
 * mokksy.put<ReplaceItemRequest>(name = "replace-item") {
 *     path("/items/1")
 * } respondsWith {
 *     body = """{"replaced":true}"""
 * }
 * ```
 *
 * @param P The expected type of the deserialized request body.
 * @param name Optional identifier for the stub.
 * @param block Lambda applied to [RequestSpecificationBuilder] to configure request matching.
 * @return A [BuildingStep] used to define the response via [BuildingStep.respondsWith]
 *   or [BuildingStep.respondsWithStream].
 * @see MokksyServer.put
 */
public inline fun <reified P : Any> MokksyServer.put(
    name: String? = null,
    noinline block: RequestSpecificationBuilder<P>.() -> Unit,
): BuildingStep<P> = put(name, P::class, block)

/**
 * Registers a stub for HTTP PUT requests with a typed request body, using an explicit
 * [StubConfiguration]. The type parameter [P] is inferred at the call site — no explicit
 * [kotlin.reflect.KClass] argument required.
 *
 * Example:
 * ```kotlin
 * mokksy.put<ReplaceItemRequest>(StubConfiguration(name = "replace-item", removeAfterMatch = true)) {
 *     path("/items/1")
 * } respondsWith {
 *     body = """{"replaced":true}"""
 * }
 * ```
 *
 * @param P The expected type of the deserialized request body.
 * @param configuration Stub configuration controlling the stub name, removal behaviour, and verbosity.
 * @param block Lambda applied to [RequestSpecificationBuilder] to configure request matching.
 * @return A [BuildingStep] used to define the response via [BuildingStep.respondsWith]
 *   or [BuildingStep.respondsWithStream].
 * @see MokksyServer.put
 */
public inline fun <reified P : Any> MokksyServer.put(
    configuration: StubConfiguration,
    noinline block: RequestSpecificationBuilder<P>.() -> Unit,
): BuildingStep<P> = put(configuration, P::class, block)

// endregion

// region DELETE

/**
 * Registers a stub for HTTP DELETE requests with a typed request body, optionally assigning a
 * [name] to the stub. The type parameter [P] is inferred at the call site — no explicit
 * [kotlin.reflect.KClass] argument required.
 *
 * Example:
 * ```kotlin
 * mokksy.delete<DeleteRequest>(name = "delete-item") {
 *     path("/items/1")
 * } respondsWith {
 *     httpStatus = HttpStatusCode.NoContent
 * }
 * ```
 *
 * @param P The expected type of the deserialized request body.
 * @param name Optional identifier for the stub,.
 * @param block Lambda applied to [RequestSpecificationBuilder] to configure request matching.
 * @return A [BuildingStep] used to define the response via [BuildingStep.respondsWith]
 *   or [BuildingStep.respondsWithStream].
 * @see MokksyServer.delete
 */
public inline fun <reified P : Any> MokksyServer.delete(
    name: String? = null,
    noinline block: RequestSpecificationBuilder<P>.() -> Unit,
): BuildingStep<P> = delete(name, P::class, block)

/**
 * Registers a stub for HTTP DELETE requests with a typed request body, using an explicit
 * [StubConfiguration]. The type parameter [P] is inferred at the call site — no explicit
 * [kotlin.reflect.KClass] argument required.
 *
 * Example:
 * ```kotlin
 * mokksy.delete<DeleteRequest>(StubConfiguration(name = "delete-item", removeAfterMatch = true)) {
 *     path("/items/1")
 * } respondsWith {
 *     httpStatus = HttpStatusCode.NoContent
 * }
 * ```
 *
 * @param P The expected type of the deserialized request body.
 * @param configuration Stub configuration controlling the stub name, removal behaviour, and verbosity.
 * @param block Lambda applied to [RequestSpecificationBuilder] to configure request matching.
 * @return A [BuildingStep] used to define the response via [BuildingStep.respondsWith]
 *   or [BuildingStep.respondsWithStream].
 * @see MokksyServer.delete
 */
public inline fun <reified P : Any> MokksyServer.delete(
    configuration: StubConfiguration,
    noinline block: RequestSpecificationBuilder<P>.() -> Unit,
): BuildingStep<P> = delete(configuration, P::class, block)

// endregion

// region PATCH

/**
 * Registers a stub for HTTP PATCH requests with a typed request body, optionally assigning a
 * [name] to the stub. The type parameter [P] is inferred at the call site — no explicit
 * [kotlin.reflect.KClass] argument required.
 *
 * Example:
 * ```kotlin
 * mokksy.patch<PatchRequest>(name = "patch-item") {
 *     path("/items/1")
 * } respondsWith {
 *     body = """{"patched":true}"""
 * }
 * ```
 *
 * @param P The expected type of the deserialized request body.
 * @param name Optional identifier for the stub.
 * @param block Lambda applied to [RequestSpecificationBuilder] to configure request matching.
 * @return A [BuildingStep] used to define the response via [BuildingStep.respondsWith]
 *   or [BuildingStep.respondsWithStream].
 * @see MokksyServer.patch
 */
public inline fun <reified P : Any> MokksyServer.patch(
    name: String? = null,
    noinline block: RequestSpecificationBuilder<P>.() -> Unit,
): BuildingStep<P> = patch(name, P::class, block)

/**
 * Registers a stub for HTTP PATCH requests with a typed request body, using an explicit
 * [StubConfiguration]. The type parameter [P] is inferred at the call site — no explicit
 * [kotlin.reflect.KClass] argument required.
 *
 * Example:
 * ```kotlin
 * mokksy.patch<PatchRequest>(StubConfiguration(name = "patch-item", removeAfterMatch = true)) {
 *     path("/items/1")
 * } respondsWith {
 *     body = """{"patched":true}"""
 * }
 * ```
 *
 * @param P The expected type of the deserialized request body.
 * @param configuration Stub configuration controlling the stub name, removal behaviour, and verbosity.
 * @param block Lambda applied to [RequestSpecificationBuilder] to configure request matching.
 * @return A [BuildingStep] used to define the response via [BuildingStep.respondsWith]
 *   or [BuildingStep.respondsWithStream].
 * @see MokksyServer.patch
 */
public inline fun <reified P : Any> MokksyServer.patch(
    configuration: StubConfiguration,
    noinline block: RequestSpecificationBuilder<P>.() -> Unit,
): BuildingStep<P> = patch(configuration, P::class, block)

// endregion

// region HEAD

/**
 * Registers a stub for HTTP HEAD requests with a typed request body, optionally assigning a [name]
 * to the stub. The type parameter [P] is inferred at the call site — no explicit
 * [kotlin.reflect.KClass] argument required.
 *
 * Note: HEAD responses must not include a body; set response headers only.
 *
 * Example:
 * ```kotlin
 * mokksy.head<Unit>(name = "check-item") {
 *     path("/items/1")
 * } respondsWith {
 *     httpStatus = HttpStatusCode.OK
 * }
 * ```
 *
 * @param P The expected type of the deserialized request body.
 * @param name Optional identifier for the stub.
 * @param block Lambda applied to [RequestSpecificationBuilder] to configure request matching.
 * @return A [BuildingStep] used to define the response via [BuildingStep.respondsWith]
 *   or [BuildingStep.respondsWithStream].
 * @see MokksyServer.head
 */
public inline fun <reified P : Any> MokksyServer.head(
    name: String? = null,
    noinline block: RequestSpecificationBuilder<P>.() -> Unit,
): BuildingStep<P> = head(name, P::class, block)

/**
 * Registers a stub for HTTP HEAD requests with a typed request body, using an explicit
 * [StubConfiguration]. The type parameter [P] is inferred at the call site — no explicit
 * [kotlin.reflect.KClass] argument required.
 *
 * Note: HEAD responses must not include a body; set response headers only.
 *
 * Example:
 * ```kotlin
 * mokksy.head<Unit>(StubConfiguration(name = "check-item", removeAfterMatch = true)) {
 *     path("/items/1")
 * } respondsWith {
 *     httpStatus = HttpStatusCode.OK
 * }
 * ```
 *
 * @param P The expected type of the deserialized request body.
 * @param configuration Stub configuration controlling the stub name, removal behaviour, and verbosity.
 * @param block Lambda applied to [RequestSpecificationBuilder] to configure request matching.
 * @return A [BuildingStep] used to define the response via [BuildingStep.respondsWith]
 *   or [BuildingStep.respondsWithStream].
 * @see MokksyServer.head
 */
public inline fun <reified P : Any> MokksyServer.head(
    configuration: StubConfiguration,
    noinline block: RequestSpecificationBuilder<P>.() -> Unit,
): BuildingStep<P> = head(configuration, P::class, block)

// endregion

// region OPTIONS

/**
 * Registers a stub for HTTP OPTIONS requests with a typed request body, optionally assigning a
 * [name] to the stub. The type parameter [P] is inferred at the call site — no explicit
 * [kotlin.reflect.KClass] argument required.
 *
 * Example:
 * ```kotlin
 * mokksy.options<Unit>(name = "cors-preflight") {
 *     path("/items")
 * } respondsWith {
 *     addHeader("Allow", "GET, POST, OPTIONS")
 * }
 * ```
 *
 * @param P The expected type of the deserialized request body.
 * @param name Optional identifier for the stub.
 * @param block Lambda applied to [RequestSpecificationBuilder] to configure request matching.
 * @return A [BuildingStep] used to define the response via [BuildingStep.respondsWith]
 *   or [BuildingStep.respondsWithStream].
 * @see MokksyServer.options
 */
public inline fun <reified P : Any> MokksyServer.options(
    name: String? = null,
    noinline block: RequestSpecificationBuilder<P>.() -> Unit,
): BuildingStep<P> = options(name, P::class, block)

/**
 * Registers a stub for HTTP OPTIONS requests with a typed request body, using an explicit
 * [StubConfiguration]. The type parameter [P] is inferred at the call site — no explicit
 * [kotlin.reflect.KClass] argument required.
 *
 * Example:
 * ```kotlin
 * mokksy.options<Unit>(StubConfiguration(name = "cors-preflight", removeAfterMatch = true)) {
 *     path("/items")
 * } respondsWith {
 *     addHeader("Allow", "GET, POST, OPTIONS")
 * }
 * ```
 *
 * @param P The expected type of the deserialized request body.
 * @param configuration Stub configuration controlling the stub name, removal behaviour, and verbosity.
 * @param block Lambda applied to [RequestSpecificationBuilder] to configure request matching.
 * @return A [BuildingStep] used to define the response via [BuildingStep.respondsWith]
 *   or [BuildingStep.respondsWithStream].
 * @see MokksyServer.options
 */
public inline fun <reified P : Any> MokksyServer.options(
    configuration: StubConfiguration,
    noinline block: RequestSpecificationBuilder<P>.() -> Unit,
): BuildingStep<P> = options(configuration, P::class, block)

// endregion
