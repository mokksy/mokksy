package dev.mokksy.mokksy

public actual fun Mokksy(
    host: String,
    port: Int,
    verbose: Boolean,
): MokksyServer = MokksyServer(host = host, port = port, verbose = verbose)
