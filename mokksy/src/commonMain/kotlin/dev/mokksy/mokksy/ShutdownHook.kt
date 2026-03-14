package dev.mokksy.mokksy

/** Registers a platform-specific cleanup hook so the server shuts down on JVM exit. */
internal expect fun registerShutdownHook(server: MokksyServer)
