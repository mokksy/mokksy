@file:JvmName("KtorExtensions")

package dev.mokksy.mokksy

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.pluginOrNull
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.doublereceive.DoubleReceive
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE
import kotlin.jvm.JvmName

/**
 * Installs Mokksy request handling into this [Application].
 *
 * Registers the [SSE], [DoubleReceive], and [ContentNegotiation] plugins (using the server's
 * configured content negotiation) and mounts a catch-all route that dispatches every incoming
 * request through [server]'s stub registry.
 *
 * Use this overload when you own the [Application] and want Mokksy to handle all routes:
 * ```kotlin
 * embeddedServer(Netty, port = 8080) {
 *     mokksy(server)
 * }.start(wait = true)
 * ```
 *
 * @param server The [MokksyHandler] whose stubs and journal will handle requests.
 * @param path The route path pattern to mount. Defaults to `"{...}"` (catch-all).
 */
public fun Application.mokksy(
    server: MokksyHandler,
    path: String = "{...}",
) {
    if (pluginOrNull(SSE) == null) install(SSE)
    if (pluginOrNull(DoubleReceive) == null) install(DoubleReceive)
    if (pluginOrNull(ContentNegotiation) == null) {
        install(ContentNegotiation) {
            server.configuration.contentNegotiationConfigurer(this)
        }
    }
    routing {
        mokksy(server, path)
    }
}

/**
 * Mounts Mokksy request handling into this [Route] scope.
 *
 * Unlike [Application.mokksy], this extension does **not** install plugins — the caller is
 * responsible for installing [SSE], [DoubleReceive], and [ContentNegotiation] on the surrounding
 * [Application].  Pass the handler's content-negotiation setup via
 * [MokksyServer.configuration]'s `contentNegotiationConfigurer` so the serialization config
 * matches the handler's expectations.  This overload is suitable for use behind authentication
 * or other middleware:
 *
 * ```kotlin
 * embeddedServer(Netty, port = 8080) {
 *     install(SSE)
 *     install(DoubleReceive)
 *     install(ContentNegotiation) { server.configuration.contentNegotiationConfigurer(this) }
 *
 *     routing {
 *         authenticate(AUTH_REALM) {
 *             mokksy(server)
 *         }
 *     }
 * }.start(wait = true)
 * ```
 *
 * @param server The [MokksyHandler] whose stubs and journal will handle requests.
 * @param path The route path pattern to mount. Defaults to `"{...}"` (catch-all).
 */
public fun Route.mokksy(
    server: MokksyHandler,
    path: String = "{...}",
) {
    route(path) {
        handle {
            server.handle(this@handle)
        }
    }
}
