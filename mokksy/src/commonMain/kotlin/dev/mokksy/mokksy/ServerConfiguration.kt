package dev.mokksy.mokksy

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.contentnegotiation.ContentNegotiationConfig
import kotlinx.serialization.json.Json
import kotlin.jvm.JvmOverloads

/**
 * Controls which requests are recorded in the [dev.mokksy.mokksy.request.RequestJournal].
 */
public enum class JournalMode {
    /**
     * This mode disables request recording entirely.
     * Journal-dependent methods like [MokksyServer.verifyNoUnexpectedRequests] and
     * [MokksyServer.findAllUnexpectedRequests] throw [IllegalStateException] when active.
     */
    NONE,

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
 *                       Supported: [JournalMode.NONE], [JournalMode.LEAN], [JournalMode.FULL].
 * @property json The [Json] instance used for both content negotiation and response body logging.
 *               Defaults to `Json { ignoreUnknownKeys = true }`. Provide a custom instance to
 *               share serializers modules (e.g. for polymorphic types) between deserialization
 *               and the verbose debug formatter.
 * @property contentNegotiationConfigurer Configures the Ktor [ContentNegotiationConfig] installed on the server.
 *                                        Defaults to installing [json] as the JSON codec.
 */
public data class ServerConfiguration
    @JvmOverloads
    constructor(
        val verbose: Boolean = false,
        val name: String? = "Mokksy",
        val journalMode: JournalMode = JournalMode.LEAN,
        val json: Json = Json { ignoreUnknownKeys = true },
        val contentNegotiationConfigurer: (
            ContentNegotiationConfig,
        ) -> Unit = { it.json(json) },
    )
