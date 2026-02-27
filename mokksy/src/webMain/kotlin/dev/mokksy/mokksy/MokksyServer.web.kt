package dev.mokksy.mokksy

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer

private val logger: KLogger = KotlinLogging.logger("MokksyServer")

internal actual fun createEmbeddedServer(
    host: String,
    port: Int,
    configuration: ServerConfiguration,
    module: Application.() -> Unit,
): EmbeddedServer<out ApplicationEngine, out ApplicationEngine.Configuration> =
    embeddedServer(
        factory = CIO,
        host = host,
        port = port,
    ) {
        // CallLogging is JVM-only; use a simple interceptor for native
        if (configuration.verbose) {
            intercept(ApplicationCallPipeline.Monitoring) {
                val request = call.request
                logger.debug {
                    "${request.local.method.value} ${request.local.uri}"
                }
            }
        }
        module()
    }
