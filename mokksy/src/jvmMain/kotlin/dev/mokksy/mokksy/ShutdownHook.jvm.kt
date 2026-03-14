package dev.mokksy.mokksy

import java.util.concurrent.ConcurrentHashMap

private val registeredHooks = ConcurrentHashMap<MokksyServer, Thread>()

internal actual fun registerShutdownHook(server: MokksyServer) {
    registeredHooks.computeIfAbsent(server) {
        val thread =
            Thread {
                try {
                    server.shutdown()
                } catch (_: Exception) {
                    // already stopped or error during shutdown — ignore
                }
            }
        thread.isDaemon = true
        Runtime.getRuntime().addShutdownHook(thread)
        thread
    }
}

internal actual fun unregisterShutdownHook(server: MokksyServer) {
    registeredHooks.remove(server)?.let { thread ->
        try {
            Runtime.getRuntime().removeShutdownHook(thread)
        } catch (_: IllegalStateException) {
            // JVM is already shutting down — ignore
        }
    }
}
