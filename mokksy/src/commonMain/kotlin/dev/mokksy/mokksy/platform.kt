package dev.mokksy.mokksy

import io.ktor.server.application.Application
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal const val DEFAULT_HOST: String = "127.0.0.1"
internal val DEFAULT_START_TIMEOUT: Duration = 5.seconds

/**
 * Platform-specific hook to subscribe to application start event.
 *
 * On JVM, this subscribes to [io.ktor.server.application.ApplicationStarted]
 * so that [MokksyServer.awaitStarted] returns only when the Ktor engine is
 * actually ready to accept connections. On non-JVM targets this is a no-op,
 * because the event may not fire reliably.
 *
 * @param application The Ktor application instance.
 * @param onStarted Callback to invoke when the application has started.
 */
internal expect fun subscribeToApplicationStarted(
    application: Application,
    onStarted: () -> Unit,
)

/**
 * Creates and returns an embedded Ktor server instance
 * with the specified host, port, server configuration, and application module.
 *
 * This function is platform-specific and must be implemented for each supported target.
 *
 * @param host The host name on which the server will run. Default value is [DEFAULT_HOST] (`127.0.0.1`).
 * @param port The port number on which the server will run. Default value is `0` - randomly assigned port.
 * @param configuration The [ServerConfiguration] settings.
 * @param module The application module to install in the server.
 * @return An embedded server instance configured with the provided parameters.
 */
internal expect fun createEmbeddedServer(
    host: String = DEFAULT_HOST,
    port: Int = 0,
    configuration: ServerConfiguration,
    module: Application.() -> Unit,
): EmbeddedServer<
    out ApplicationEngine,
    out ApplicationEngine.Configuration,
>
