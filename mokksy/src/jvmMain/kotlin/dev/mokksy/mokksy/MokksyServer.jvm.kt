package dev.mokksy.mokksy

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
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

public fun Mokksy.start() {
    runBlocking(Dispatchers.Default) {
        this@start.startSuspend()
    }
}

@JvmOverloads
public fun Mokksy.shutdown(
    gracePeriodMillis: Long = 500,
    timeoutMillis: Long = 1000,
) {
    runBlocking(Dispatchers.Default) {
        this@shutdown.shutdownSuspend(gracePeriodMillis, timeoutMillis)
    }
}
