package dev.mokksy.mokksy

/** Registers a platform-specific cleanup hook so the server shuts down on JVM exit. */
internal expect fun registerShutdownHook(server: MokksyServer)

/** Unregisters the cleanup hook previously registered by [registerShutdownHook]. */
internal expect fun unregisterShutdownHook(server: MokksyServer)
