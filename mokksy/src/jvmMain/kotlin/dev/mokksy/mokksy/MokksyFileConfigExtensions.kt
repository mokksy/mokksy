package dev.mokksy.mokksy

import dev.mokksy.mokksy.config.applyConfig
import dev.mokksy.mokksy.config.parseYamlConfig
import dev.mokksy.mokksy.config.validateConfig
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/** Environment variable that provides the path to a Mokksy YAML config file. */
public const val ENV_MOKKSY_CONFIG: String = "MOKKSY_CONFIG"

/** JVM system property that provides the path to a Mokksy YAML config file. */
public const val PROP_MOKKSY_CONFIG: String = "mokksy.config"

/**
 * Loads stub definitions from a YAML file at [path] and registers them with this server.
 *
 * See [loadStubsFromFile] for the supported YAML format.
 *
 * @param path Absolute or relative path to the YAML config file.
 * @return This server instance for chaining.
 * @throws IllegalArgumentException if the file is missing, cannot be read, or contains invalid YAML.
 */
public fun MokksyServer.loadStubsFromFile(path: String): MokksyServer =
    loadStubsFromFile(File(path))

/**
 * Loads stub definitions from a YAML [file] and registers them with this server.
 *
 * Example file:
 * ```yaml
 * stubs:
 *   - name: ping
 *     method: GET
 *     path: /ping
 *     response:
 *       body: '{"response":"Pong"}'
 *       status: 200
 *
 *   - name: events
 *     method: POST
 *     path: /sse
 *     response:
 *       type: sse
 *       chunks:
 *         - "One"
 *         - "Two"
 * ```
 *
 * Supported response types:
 * - `plain` (default) — static body with optional status, headers, and delay
 * - `sse` — server-sent events stream; `chunks` are required
 * - `stream` — plain chunked stream; `chunks` are required
 *
 * @param file YAML config file.
 * @return This server instance for chaining.
 * @throws IllegalArgumentException if the file is missing, cannot be read, contains invalid YAML,
 *   or fails semantic validation (unknown method, empty chunks, etc.).
 */
@Suppress("ThrowsCount")
public fun MokksyServer.loadStubsFromFile(file: File): MokksyServer {
    val text =
        try {
            file.readText()
        } catch (e: FileNotFoundException) {
            throw IllegalArgumentException("Mokksy config file not found: ${file.absolutePath}", e)
        } catch (e: IOException) {
            throw IllegalArgumentException(
                "Cannot read Mokksy config file '${file.absolutePath}': ${e.message}",
                e,
            )
        }
    val config =
        @Suppress("TooGenericExceptionCaught")
        try {
            parseYamlConfig(text)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw IllegalArgumentException(
                "Invalid YAML in Mokksy config file '${file.absolutePath}': ${e.message}",
                e,
            )
        }
    validateConfig(config)
    applyConfig(config)
    return this
}

/**
 * Loads stub definitions from the YAML file specified by the [ENV_MOKKSY_CONFIG] environment
 * variable or the [PROP_MOKKSY_CONFIG] system property, in that order.
 *
 * Example:
 * ```
 * MOKKSY_CONFIG=/app/stubs.yaml java -jar app.jar
 * # or
 * java -Dmokksy.config=/app/stubs.yaml -jar app.jar
 * ```
 *
 * @return This server instance for chaining.
 * @throws IllegalStateException if neither the environment variable nor the system property is set.
 * @throws IllegalArgumentException if the resolved file is missing or invalid.
 */
public fun MokksyServer.loadStubsFromEnv(): MokksyServer {
    val path =
        System.getenv(ENV_MOKKSY_CONFIG)?.trim()?.takeIf { it.isNotEmpty() }
            ?: System.getProperty(PROP_MOKKSY_CONFIG)?.trim()?.takeIf { it.isNotEmpty() }
            ?: error(
                "No config path found. Set the '$ENV_MOKKSY_CONFIG' environment variable " +
                    "or the '-D$PROP_MOKKSY_CONFIG' system property.",
            )
    return loadStubsFromFile(path)
}
