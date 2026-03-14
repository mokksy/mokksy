package dev.mokksy.mokksy

import io.ktor.server.application.Application

public actual fun Mokksy(
    host: String,
    port: Int,
    verbose: Boolean,
    configurer: (Application.() -> Unit),
): MokksyServer = MokksyServer(host = host, port = port, verbose = verbose, configurer = configurer)
