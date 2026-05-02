package dev.mokksy

public fun main() {
    Mokksy(host = "0.0.0.0", port = 8080, verbose = true).start()
    Thread.currentThread().join()
}
