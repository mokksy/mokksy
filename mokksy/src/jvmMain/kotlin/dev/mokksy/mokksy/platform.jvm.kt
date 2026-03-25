package dev.mokksy.mokksy

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

private val platformLog = LoggerFactory.getLogger("dev.mokksy.mokksy.platform")

internal actual fun createEmbeddedServer(
    host: String,
    port: Int,
    configuration: ServerConfiguration,
    module: Application.() -> Unit,
): EmbeddedServer<out ApplicationEngine, out ApplicationEngine.Configuration> {
    val props = MokksyProperties.load()
    val result = IoDispatcherFactory.create(props.ioThreadMode, props.ioParallelism)
    val ioDispatcher = result.dispatcher

    platformLog.info("Mokksy IO: {}", result.description)

    @Suppress("TooGenericExceptionCaught")
    try {
        return embeddedServer(
            factory = Netty,
            host = host,
            port = port,
        ) {
            // Install IO dispatcher interceptor for Mokksy's internal work.
            // Netty event loops are NOT affected — they keep running on platform threads.
            if (ioDispatcher != null) {
                intercept(ApplicationCallPipeline.Plugins) {
                    withContext(ioDispatcher + CoroutineName("mokksy-io")) {
                        proceed()
                    }
                }

                // Clean up the dispatcher when the server stops
                this@embeddedServer.monitor.subscribe(ApplicationStopped) {
                    (ioDispatcher as? ExecutorCoroutineDispatcher)?.close()
                }
            }

            module()
            install(CallLogging) {
                level = if (configuration.verbose) Level.DEBUG else Level.INFO
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        (ioDispatcher as? ExecutorCoroutineDispatcher)?.close()
        throw e
    }
}
