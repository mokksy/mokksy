package dev.mokksy

import com.fasterxml.jackson.databind.ObjectMapper
import dev.mokksy.MokksyJackson.create
import dev.mokksy.mokksy.DEFAULT_HOST
import dev.mokksy.mokksy.ExperimentalMokksyApi
import dev.mokksy.mokksy.MokksyServer
import dev.mokksy.mokksy.ServerConfiguration
import io.ktor.serialization.jackson.jackson
import java.util.function.Consumer

/**
 * Factory for creating [Mokksy] instances configured with Jackson as the JSON serializer.
 *
 * Use this instead of [Mokksy.create] when tests send or receive Jackson-serialized payloads,
 * or when typed body matchers (`bodyMatchesPredicate`) are used against Java POJOs.
 *
 * The API mirrors [Mokksy.create] exactly — the same `host`, `port`, and `verbose` parameters —
 * with an additional optional [configureMapper] callback for [ObjectMapper] customisation.
 *
 * Requires `ktor-serialization-jackson` on the runtime classpath. Add it to your test dependencies:
 * ```kotlin
 * testImplementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
 * ```
 *
 * **Basic usage (Java):**
 * ```java
 * Mokksy mokksy = MokksyJackson.create().start();
 *
 * mokksy.post(CreateItemRequest.class,
 *     spec -> spec.path("/items")
 *                 .bodyMatchesPredicate(req -> "widget".equals(req.getName())))
 *   .respondsWith(builder -> builder.body("{\"id\":\"1\"}").status(201));
 * ```
 *
 * **Custom [ObjectMapper]:**
 * ```java
 * Mokksy mokksy = MokksyJackson.create(ObjectMapper::findAndRegisterModules).start();
 * ```
 *
 * **Kotlin — opt-in required:**
 * ```kotlin
 * @OptIn(ExperimentalMokksyApi::class)
 * val mokksy = MokksyJackson.create().start()
 * ```
 */
@ExperimentalMokksyApi
public object MokksyJackson {
    /**
     * Creates a [Mokksy] instance with Jackson and a custom [ObjectMapper] configuration,
     * binding to `"127.0.0.1"` on a random port.
     *
     * This overload exists so Java callers can pass a mapper callback without specifying
     * `host`/`port`/`verbose`. It delegates to [create] with default host, port, and verbosity:
     * ```java
     * Mokksy mokksy = MokksyJackson.create(ObjectMapper::findAndRegisterModules).start();
     * ```
     *
     * @param configureMapper Callback to customise the [ObjectMapper] used by Ktor.
     * @return A new, not-yet-started [Mokksy] instance.
     * @throws IllegalStateException if `ktor-serialization-jackson` is not on the runtime classpath.
     */
    @JvmStatic
    public fun create(configureMapper: Consumer<ObjectMapper>): Mokksy =
        create(host = DEFAULT_HOST, port = 0, verbose = false, configureMapper = configureMapper)

    /**
     * Creates a [Mokksy] instance with Jackson configured for JSON content negotiation.
     *
     * The server is **not** started automatically. Chain with [Mokksy.start]:
     * ```java
     * Mokksy mokksy = MokksyJackson.create().start();
     * ```
     *
     * @param host Network interface to bind to. Defaults to `"127.0.0.1"`.
     * @param port Port to bind to. Defaults to `0` (OS-assigned ephemeral port).
     * @param verbose Enables `DEBUG`-level request/response logging. Defaults to `false`.
     * @param configureMapper Optional callback to customise the [ObjectMapper] used by Ktor,
     *   e.g. `mapper -> mapper.findAndRegisterModules()`. Defaults to a no-op.
     * @return A new, not-yet-started [Mokksy] instance.
     * @throws IllegalStateException if `ktor-serialization-jackson` is not on the runtime classpath.
     */
    @JvmStatic
    @JvmOverloads
    public fun create(
        host: String = DEFAULT_HOST,
        port: Int = 0,
        verbose: Boolean = false,
        configureMapper: Consumer<ObjectMapper> = Consumer { },
    ): Mokksy {
        ensureJacksonRuntimeAvailable()
        return Mokksy(
            MokksyServer(
                host = host,
                port = port,
                configuration =
                    ServerConfiguration(
                        verbose = verbose,
                        contentNegotiationConfigurer = { config ->
                            config.jackson { configureMapper.accept(this) }
                        },
                    ),
            ),
        )
    }

    private fun ensureJacksonRuntimeAvailable() {
        try {
            Class.forName("io.ktor.serialization.jackson.JacksonConverter")
        } catch (e: ClassNotFoundException) {
            throw IllegalStateException(
                "ktor-serialization-jackson is not on the runtime classpath. " +
                    "Add 'io.ktor:ktor-serialization-jackson' to your test dependencies.",
                e,
            )
        }
    }
}
