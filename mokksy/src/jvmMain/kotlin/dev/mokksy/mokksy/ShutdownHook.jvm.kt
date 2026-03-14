package dev.mokksy.mokksy

import kotlinx.coroutines.runBlocking

internal actual fun registerShutdownHook(server: MokksyServer) {
    val thread = Thread {
        runBlocking {
            try {
                server.shutdownSuspend()
            } catch (_: Exception) {
                // already stopped or error during shutdown — ignore
            }
        }
    }
    thread.isDaemon = true
    Runtime.getRuntime().addShutdownHook(thread)
}
