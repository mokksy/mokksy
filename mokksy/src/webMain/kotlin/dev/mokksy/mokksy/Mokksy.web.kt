package dev.mokksy.mokksy

import io.ktor.server.application.Application

/**
 * Creates a new [MokksyServer] instance configured for the web target.
 *
 * @param host the hostname to bind the server to
 * @param port the port number for the server
 * @param verbose whether to enable verbose logging
 * @param configurer additional Ktor [Application] configuration block
 * @return a configured [MokksyServer] instance
 */
public actual fun Mokksy(
    host: String,
    port: Int,
    verbose: Boolean,
    configurer: (Application.() -> Unit),
): MokksyServer = MokksyServer(host = host, port = port, verbose = verbose, configurer = configurer)
