package dev.mokksy.mokksy

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.slf4j.event.Level

internal actual fun createEmbeddedServer(
    host: String,
    port: Int,
    configuration: ServerConfiguration,
    module: Application.() -> Unit,
): EmbeddedServer<out ApplicationEngine, out ApplicationEngine.Configuration> =
    embeddedServer(
        factory = Netty,
        host = host,
        port = port,
    ) {
        module()
        install(CallLogging) {
            level = if (configuration.verbose) Level.DEBUG else Level.INFO
        }
    }

/**
 * Starts the Mokksy server, allowing it to process and respond to requests.
 *
 * @param dispatcher The CoroutineDispatcher to be used for blocking operations. Defaults to Dispatchers.Default.
 */
@JvmOverloads
public fun Mokksy.start(dispatcher: CoroutineDispatcher = Dispatchers.Default) {
    runBlocking(dispatcher) {
        this@start.startSuspend()
    }
}

/**
 * Initiates the shutdown process of the Mokksy embedded server.
 *
 * @param gracePeriodMillis The duration in milliseconds for the server to attempt a graceful shutdown.
 *        By default, it is set to 500 milliseconds.
 * @param timeoutMillis The maximum duration in milliseconds to wait for the shutdown process to complete.
 *        The default value is 1000 milliseconds.
 * @param dispatcher The coroutine dispatcher on which the shutdown process will run. By default, it uses
 *        `Dispatchers.Default`.
 */
@JvmOverloads
public fun Mokksy.shutdown(
    gracePeriodMillis: Long = 500,
    timeoutMillis: Long = 1000,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    runBlocking(dispatcher) {
        this@shutdown.shutdownSuspend(gracePeriodMillis, timeoutMillis)
    }
}
