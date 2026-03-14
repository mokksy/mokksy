package dev.mokksy.mokksy

import io.ktor.server.application.Application

/**
 * Creates a [MokksyServer] — idiomatic Kotlin entry point for all platforms.
 *
 * Example:
 * ```kotlin
 * val mokksy = Mokksy()
 * mokksy.startSuspend()
 * mokksy.get { path("/ping") } respondsWith { body = "Pong" }
 * mokksy.shutdownSuspend()
 * ```
 *
 * On JVM, [start][dev.mokksy.mokksy.start] and [shutdown][dev.mokksy.mokksy.shutdown] blocking extensions are also available:
 * ```kotlin
 * val mokksy = Mokksy().start()
 * ```
 *
 * Java callers should use [dev.mokksy.Mokksy.create] instead.
 *
 * @param host The host to bind to. Defaults to `127.0.0.1`.
 * @param port The port to bind to. Defaults to `0` (randomly assigned).
 * @param verbose Enables `DEBUG`-level request logging when `true`. Defaults to `false`.
 * @param configurer Additional Ktor [Application] configuration applied after the default routing setup.
 */
@Suppress("FunctionNaming")
public expect fun Mokksy(
    host: String = DEFAULT_HOST,
    port: Int = 0,
    verbose: Boolean = false,
    configurer: (Application.() -> Unit) = {},
): MokksyServer
