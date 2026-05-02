package dev.mokksy

public fun main() {
    val mokksy = Mokksy(host = "0.0.0.0", port = 8080, verbose = true)
    try {
        mokksy.loadStubsFromEnv()
    } catch (_: IllegalStateException) {
        // no MOKKSY_CONFIG set — start with empty stub registry
    }
    mokksy.start()
    Thread.currentThread().join()
}
