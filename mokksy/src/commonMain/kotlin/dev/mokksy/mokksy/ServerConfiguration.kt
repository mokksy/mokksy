package dev.mokksy.mokksy

import io.ktor.server.plugins.contentnegotiation.ContentNegotiationConfig
import kotlin.jvm.JvmOverloads

/**
 * Controls which requests are recorded in the [dev.mokksy.mokksy.request.RequestJournal].
 */
public enum class JournalMode {
    /**
     * Records only requests with no matching stub.
     * Lower overhead; sufficient for [MokksyServer.verifyNoUnexpectedRequests].
     */
    LEAN,

    /**
     * Records all incoming requests, differentiating matched from unmatched.
     * Enables inspection of both [MokksyServer.findAllUnexpectedRequests] and matched requests.
     */
    FULL,
}

/**
 * Configuration for a [MokksyServer] instance.
 *
 * @property verbose Enables `DEBUG`-level request logging when `true`. Defaults to `false`.
 * @property name Human-readable server name used in log output. Defaults to `"Mokksy"`.
 * @property journalMode Controls which requests are recorded in the
 *                       [dev.mokksy.mokksy.request.RequestJournal].
 *                       Defaults to [JournalMode.LEAN] (only unmatched requests).
 * @property contentNegotiationConfigurer Configures the Ktor [ContentNegotiationConfig] installed on the server.
 *                                        Defaults to JSON with [Json.ignoreUnknownKeys] enabled.
 */
public data class ServerConfiguration
    @JvmOverloads
    constructor(
        val verbose: Boolean = false,
        val name: String? = "Mokksy",
        val journalMode: JournalMode = JournalMode.LEAN,
        val contentNegotiationConfigurer: (
            ContentNegotiationConfig,
        ) -> Unit = ::configureContentNegotiation,
    )
