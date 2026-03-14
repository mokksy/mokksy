package dev.mokksy.mokksy

import io.ktor.server.application.Application
import io.ktor.server.routing.RoutingContext

/**
 * Minimal contract for embedding Mokksy stub handling into a Ktor application.
 *
 * [Application.mokksy][dev.mokksy.mokksy.mokksy] and
 * [Route.mokksy][dev.mokksy.mokksy.mokksy] accept any [MokksyPlugin], so stub routing
 * can be mounted into an existing Ktor application without constructing a full [MokksyServer]
 * (which also allocates an embedded server).
 *
 * [MokksyServer] is the standard implementation. Implement this interface directly only when
 * you need a custom stub-registry configuration without an embedded server.
 */
public interface MokksyPlugin {
    /** Server-level configuration, used to set up content negotiation when installed. */
    public val configuration: ServerConfiguration

    /**
     * Dispatches an incoming request to the appropriate stub and writes the response.
     *
     * Called by [Application.mokksy][dev.mokksy.mokksy.mokksy] and
     * [Route.mokksy][dev.mokksy.mokksy.mokksy] for every incoming request.
     */
    public suspend fun handle(context: RoutingContext, application: Application)
}
