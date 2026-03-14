package dev.mokksy

import dev.mokksy.Mokksy.Companion.create
import dev.mokksy.mokksy.BuildingStep
import dev.mokksy.mokksy.DEFAULT_HOST
import dev.mokksy.mokksy.JavaBuildingStep
import dev.mokksy.mokksy.JavaRequestSpecificationBuilder
import dev.mokksy.mokksy.MokksyServer
import dev.mokksy.mokksy.StubConfiguration
import dev.mokksy.mokksy.request.RecordedRequest
import dev.mokksy.mokksy.request.RequestSpecification
import dev.mokksy.mokksy.request.RequestSpecificationBuilder
import io.ktor.http.HttpMethod
import io.ktor.http.HttpMethod.Companion.Delete
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpMethod.Companion.Head
import io.ktor.http.HttpMethod.Companion.Options
import io.ktor.http.HttpMethod.Companion.Patch
import io.ktor.http.HttpMethod.Companion.Post
import io.ktor.http.HttpMethod.Companion.Put
import kotlinx.coroutines.runBlocking
import java.util.function.Consumer
import kotlin.reflect.KClass

/**
 * The primary entry point for Mokksy — a lightweight HTTP stub server for JVM tests.
 *
 * **Kotlin** — construct directly:
 * ```kotlin
 * val mokksy = Mokksy()
 * mokksy.start()
 * mokksy.get { path("/ping") }.respondsWith { body("Pong") }
 * mokksy.shutdown()
 * ```
 *
 * **Java** — use [create] for a fluent setup:
 * ```java
 * Mokksy mokksy = Mokksy.create().start();
 *
 * mokksy.get(spec -> spec.path("/ping"))
 *       .respondsWith(builder -> builder.body("Pong"));
 *
 * mokksy.shutdown();
 * ```
 *
 * For automatic cleanup in Java, use try-with-resources:
 * ```java
 * try (Mokksy mokksy = Mokksy.create().start()) {
 *     mokksy.post(spec -> spec.path("/items"))
 *           .respondsWith(builder -> builder.body("ok").status(201));
 * }
 * ```
 *
 * @constructor Creates a [Mokksy] backed by a new [MokksyServer]. The server is **not** started
 *   automatically — call [start] or chain [create] with `.start()`.
 */
@Suppress("TooManyFunctions")
public class Mokksy(
    /**
     * The underlying [MokksyServer]. Available for Kotlin callers that need direct access to the
     * server. Intentionally hidden from Java — use the [Mokksy] methods directly.
     */
    @get:JvmSynthetic
    public val delegate: MokksyServer,
) : AutoCloseable {
    /**
     * @param host Network interface to bind to. Defaults to `"127.0.0.1"` (loopback only).
     * @param port Port to bind to. Defaults to `0` (OS-assigned ephemeral port).
     * @param verbose Enables `DEBUG`-level request/response logging. Defaults to `false`.
     */
    @JvmOverloads
    public constructor(
        host: String = DEFAULT_HOST,
        port: Int = 0,
        verbose: Boolean = false,
    ) : this(MokksyServer(port = port, host = host, verbose = verbose))

    // region Lifecycle

    /**
     * Starts the server and blocks until the port is bound and the server is ready.
     *
     * Returns `this` so that construction and startup can be chained:
     * ```kotlin
     * val mokksy = Mokksy(8080).start()
     * ```
     * ```java
     * Mokksy mokksy = Mokksy.create(8080).start();
     * ```
     *
     * @return This instance, for chaining.
     */
    public fun start(): Mokksy {
        runBlocking {
            delegate.startSuspend()
            delegate.awaitStarted()
        }
        return this
    }

    /**
     * Stops the server and blocks the calling thread until shutdown is complete.
     *
     * @param gracePeriodMillis Milliseconds to wait for active connections to finish. Defaults to `500`.
     * @param timeoutMillis Maximum milliseconds before shutdown is forced. Defaults to `1000`.
     */
    @JvmOverloads
    public fun shutdown(
        gracePeriodMillis: Long = 500,
        timeoutMillis: Long = 1000,
    ) {
        runBlocking {
            delegate.shutdownSuspend(gracePeriodMillis, timeoutMillis)
        }
    }

    /** Calls [shutdown] with default timeouts. Enables try-with-resources. */
    override fun close(): Unit = shutdown()

    /**
     * Returns the base URL of the server, e.g. `"http://127.0.0.1:8080"`.
     *
     * @throws IllegalStateException if [start] has not been called yet.
     */
    public fun baseUrl(): String = delegate.baseUrl()

    /**
     * Returns the port the server is bound to. Available after [start] has returned.
     */
    public fun port(): Int = delegate.port()

    // endregion

    // region Stub registration

    private fun <P : Any> method(
        configuration: StubConfiguration,
        httpMethod: HttpMethod,
        requestType: KClass<P>,
        spec: Consumer<JavaRequestSpecificationBuilder<P>>,
    ): JavaBuildingStep<P> =
        JavaBuildingStep(
            delegate.method(configuration, httpMethod, requestType) {
                spec.accept(JavaRequestSpecificationBuilder(this))
            },
        )

    private fun method(
        configuration: StubConfiguration,
        httpMethod: HttpMethod,
        spec: Consumer<JavaRequestSpecificationBuilder<String>>,
    ): JavaBuildingStep<String> = method(configuration, httpMethod, String::class, spec)

    // region GET

    /** Registers a GET stub with a [String] request body. */
    public fun get(
        spec: Consumer<JavaRequestSpecificationBuilder<String>>,
    ): JavaBuildingStep<String> = method(StubConfiguration(), Get, spec)

    /** Registers a GET stub with a [String] request body and [StubConfiguration]. */
    public fun get(
        configuration: StubConfiguration,
        spec: Consumer<JavaRequestSpecificationBuilder<String>>,
    ): JavaBuildingStep<String> = method(configuration, Get, spec)

    /** Registers a GET stub with a typed request body. */
    public fun <P : Any> get(
        requestType: Class<P>,
        spec: Consumer<JavaRequestSpecificationBuilder<P>>,
    ): JavaBuildingStep<P> = method(StubConfiguration(), Get, requestType.kotlin, spec)

    /** Registers a GET stub with a typed request body and [StubConfiguration]. */
    public fun <P : Any> get(
        configuration: StubConfiguration,
        requestType: Class<P>,
        spec: Consumer<JavaRequestSpecificationBuilder<P>>,
    ): JavaBuildingStep<P> = method(configuration, Get, requestType.kotlin, spec)

    // endregion

    // region POST

    /** Registers a POST stub with a [String] request body. */
    public fun post(
        spec: Consumer<JavaRequestSpecificationBuilder<String>>,
    ): JavaBuildingStep<String> = method(StubConfiguration(), Post, spec)

    /** Registers a POST stub with a [String] request body and [StubConfiguration]. */
    public fun post(
        configuration: StubConfiguration,
        spec: Consumer<JavaRequestSpecificationBuilder<String>>,
    ): JavaBuildingStep<String> = method(configuration, Post, spec)

    /** Registers a POST stub with a typed request body. */
    public fun <P : Any> post(
        requestType: Class<P>,
        spec: Consumer<JavaRequestSpecificationBuilder<P>>,
    ): JavaBuildingStep<P> = method(StubConfiguration(), Post, requestType.kotlin, spec)

    /** Registers a POST stub with a typed request body and [StubConfiguration]. */
    public fun <P : Any> post(
        configuration: StubConfiguration,
        requestType: Class<P>,
        spec: Consumer<JavaRequestSpecificationBuilder<P>>,
    ): JavaBuildingStep<P> = method(configuration, Post, requestType.kotlin, spec)

    // endregion

    // region PUT

    /** Registers a PUT stub with a [String] request body. */
    public fun put(
        spec: Consumer<JavaRequestSpecificationBuilder<String>>,
    ): JavaBuildingStep<String> = method(StubConfiguration(), Put, spec)

    /** Registers a PUT stub with a [String] request body and [StubConfiguration]. */
    public fun put(
        configuration: StubConfiguration,
        spec: Consumer<JavaRequestSpecificationBuilder<String>>,
    ): JavaBuildingStep<String> = method(configuration, Put, spec)

    /** Registers a PUT stub with a typed request body. */
    public fun <P : Any> put(
        requestType: Class<P>,
        spec: Consumer<JavaRequestSpecificationBuilder<P>>,
    ): JavaBuildingStep<P> = method(StubConfiguration(), Put, requestType.kotlin, spec)

    /** Registers a PUT stub with a typed request body and [StubConfiguration]. */
    public fun <P : Any> put(
        configuration: StubConfiguration,
        requestType: Class<P>,
        spec: Consumer<JavaRequestSpecificationBuilder<P>>,
    ): JavaBuildingStep<P> = method(configuration, Put, requestType.kotlin, spec)

    // endregion

    // region DELETE

    /** Registers a DELETE stub with a [String] request body. */
    public fun delete(
        spec: Consumer<JavaRequestSpecificationBuilder<String>>,
    ): JavaBuildingStep<String> = method(StubConfiguration(), Delete, spec)

    /** Registers a DELETE stub with a [String] request body and [StubConfiguration]. */
    public fun delete(
        configuration: StubConfiguration,
        spec: Consumer<JavaRequestSpecificationBuilder<String>>,
    ): JavaBuildingStep<String> = method(configuration, Delete, spec)

    /** Registers a DELETE stub with a typed request body. */
    public fun <P : Any> delete(
        requestType: Class<P>,
        spec: Consumer<JavaRequestSpecificationBuilder<P>>,
    ): JavaBuildingStep<P> = method(StubConfiguration(), Delete, requestType.kotlin, spec)

    /** Registers a DELETE stub with a typed request body and [StubConfiguration]. */
    public fun <P : Any> delete(
        configuration: StubConfiguration,
        requestType: Class<P>,
        spec: Consumer<JavaRequestSpecificationBuilder<P>>,
    ): JavaBuildingStep<P> = method(configuration, Delete, requestType.kotlin, spec)

    // endregion

    // region PATCH

    /** Registers a PATCH stub with a [String] request body. */
    public fun patch(
        spec: Consumer<JavaRequestSpecificationBuilder<String>>,
    ): JavaBuildingStep<String> = method(StubConfiguration(), Patch, spec)

    /** Registers a PATCH stub with a [String] request body and [StubConfiguration]. */
    public fun patch(
        configuration: StubConfiguration,
        spec: Consumer<JavaRequestSpecificationBuilder<String>>,
    ): JavaBuildingStep<String> = method(configuration, Patch, spec)

    /** Registers a PATCH stub with a typed request body. */
    public fun <P : Any> patch(
        requestType: Class<P>,
        spec: Consumer<JavaRequestSpecificationBuilder<P>>,
    ): JavaBuildingStep<P> = method(StubConfiguration(), Patch, requestType.kotlin, spec)

    /** Registers a PATCH stub with a typed request body and [StubConfiguration]. */
    public fun <P : Any> patch(
        configuration: StubConfiguration,
        requestType: Class<P>,
        spec: Consumer<JavaRequestSpecificationBuilder<P>>,
    ): JavaBuildingStep<P> = method(configuration, Patch, requestType.kotlin, spec)

    // endregion

    // region HEAD

    /** Registers a HEAD stub. */
    public fun head(
        spec: Consumer<JavaRequestSpecificationBuilder<String>>,
    ): JavaBuildingStep<String> = method(StubConfiguration(), Head, spec)

    /** Registers a HEAD stub with a typed request body. */
    public fun <P : Any> head(
        requestType: Class<P>,
        spec: Consumer<JavaRequestSpecificationBuilder<P>>,
    ): JavaBuildingStep<P> = method(StubConfiguration(), Head, requestType.kotlin, spec)

    /** Registers a HEAD stub with [StubConfiguration]. */
    public fun head(
        configuration: StubConfiguration,
        spec: Consumer<JavaRequestSpecificationBuilder<String>>,
    ): JavaBuildingStep<String> = method(configuration, Head, spec)

    /** Registers a HEAD stub with a typed request body and [StubConfiguration]. */
    public fun <P : Any> head(
        configuration: StubConfiguration,
        requestType: Class<P>,
        spec: Consumer<JavaRequestSpecificationBuilder<P>>,
    ): JavaBuildingStep<P> = method(configuration, Head, requestType.kotlin, spec)

    // endregion

    // region OPTIONS

    /** Registers an OPTIONS stub. */
    public fun options(
        spec: Consumer<JavaRequestSpecificationBuilder<String>>,
    ): JavaBuildingStep<String> = method(StubConfiguration(), Options, spec)

    /** Registers an OPTIONS stub with a typed request body. */
    public fun <P : Any> options(
        requestType: Class<P>,
        spec: Consumer<JavaRequestSpecificationBuilder<P>>,
    ): JavaBuildingStep<P> = method(StubConfiguration(), Options, requestType.kotlin, spec)

    /** Registers an OPTIONS stub with [StubConfiguration]. */
    public fun options(
        configuration: StubConfiguration,
        spec: Consumer<JavaRequestSpecificationBuilder<String>>,
    ): JavaBuildingStep<String> = method(configuration, Options, spec)

    /** Registers an OPTIONS stub with a typed request body and [StubConfiguration]. */
    public fun <P : Any> options(
        configuration: StubConfiguration,
        requestType: Class<P>,
        spec: Consumer<JavaRequestSpecificationBuilder<P>>,
    ): JavaBuildingStep<P> = method(configuration, Options, requestType.kotlin, spec)

    // endregion

    // region Arbitrary method

    /**
     * Registers a stub for an arbitrary HTTP method with a [String] request body.
     *
     * Useful in `@ParameterizedTest` scenarios where the method name is a variable:
     * ```java
     * @ParameterizedTest
     * @ValueSource(strings = {"PUT", "DELETE", "PATCH"})
     * void shouldRespond(String method) throws Exception {
     *     mokksy.method(method, spec -> spec.path("/" + method.toLowerCase()))
     *           .respondsWith(builder -> builder.body("ok"));
     * }
     * ```
     *
     * @param httpMethod The HTTP method string, e.g. `"GET"`, `"POST"`, `"PUT"`.
     * @param spec A [Consumer] to configure the request specification.
     * @return A [JavaBuildingStep] to configure the response.
     */
    public fun method(
        httpMethod: String,
        spec: Consumer<JavaRequestSpecificationBuilder<String>>,
    ): JavaBuildingStep<String> = method(StubConfiguration(), HttpMethod(httpMethod), spec)

    /**
     * Registers a stub for an arbitrary HTTP method with a [String] request body and [StubConfiguration].
     *
     * @param configuration Stub lifecycle and logging configuration.
     * @param httpMethod The HTTP method string, e.g. `"GET"`, `"POST"`, `"PUT"`.
     * @param spec A [Consumer] to configure the request specification.
     * @return A [JavaBuildingStep] to configure the response.
     */
    public fun method(
        configuration: StubConfiguration,
        httpMethod: String,
        spec: Consumer<JavaRequestSpecificationBuilder<String>>,
    ): JavaBuildingStep<String> = method(configuration, HttpMethod(httpMethod), spec)

    // endregion

    // endregion

    // region Verification

    @Deprecated(
        message = "Use resetMatchState() instead.",
        replaceWith = ReplaceWith("resetMatchState()"),
    )
    public fun resetMatchCounts(): Unit = resetMatchState()

    /** Resets stub match state and clears the request journal. */
    public fun resetMatchState(): Unit = delegate.resetMatchState()

    /** Returns all stubs that were never matched. */
    public fun findAllUnmatchedStubs(): List<RequestSpecification<*>> =
        delegate.findAllUnmatchedStubs()

    /** Returns all requests that were not matched by any stub. */
    public fun findAllUnexpectedRequests(): List<RecordedRequest> =
        delegate.findAllUnexpectedRequests()

    /**
     * Asserts that every registered stub was matched at least once.
     *
     * @throws AssertionError if any stub was never triggered.
     */
    public fun verifyNoUnmatchedStubs(): Unit = delegate.verifyNoUnmatchedStubs()

    /**
     * Asserts that every request received by the server was matched by a stub.
     *
     * @throws AssertionError if any request had no matching stub.
     */
    public fun verifyNoUnexpectedRequests(): Unit = delegate.verifyNoUnexpectedRequests()

    // endregion

    // region Kotlin DSL

    /**
     * Starts the server asynchronously. Suspends until the port is bound.
     *
     * Prefer this over [start] in coroutine-based test setups (e.g. `@BeforeAll suspend fun`).
     */
    @JvmSynthetic
    public suspend fun startSuspend(wait: Boolean = false): Unit = delegate.startSuspend(wait)

    /** Suspends until the server is fully started and ready to accept connections. */
    @JvmSynthetic
    public suspend fun awaitStarted(): Unit = delegate.awaitStarted()

    /**
     * Stops the server asynchronously.
     *
     * @param gracePeriodMillis Milliseconds to wait for active connections to finish.
     * @param timeoutMillis Maximum milliseconds before shutdown is forced.
     */
    @JvmSynthetic
    public suspend fun shutdownSuspend(
        gracePeriodMillis: Long = 500,
        timeoutMillis: Long = 1000,
    ): Unit = delegate.shutdownSuspend(gracePeriodMillis, timeoutMillis)

    /** Registers a GET stub using a Kotlin DSL block. */
    @JvmSynthetic
    public fun get(block: RequestSpecificationBuilder<String>.() -> Unit): BuildingStep<String> =
        delegate.method(StubConfiguration(), Get, String::class, block)

    /** Registers a GET stub with [StubConfiguration] using a Kotlin DSL block. */
    @JvmSynthetic
    public fun get(
        configuration: StubConfiguration,
        block: RequestSpecificationBuilder<String>.() -> Unit,
    ): BuildingStep<String> = delegate.method(configuration, Get, String::class, block)

    /** Registers a GET stub for a typed request body using a Kotlin DSL block. */
    @JvmSynthetic
    public fun <P : Any> get(
        requestType: KClass<P>,
        block: RequestSpecificationBuilder<P>.() -> Unit,
    ): BuildingStep<P> = delegate.method(StubConfiguration(), Get, requestType, block)

    /** Registers a POST stub using a Kotlin DSL block. */
    @JvmSynthetic
    public fun post(block: RequestSpecificationBuilder<String>.() -> Unit): BuildingStep<String> =
        delegate.method(StubConfiguration(), Post, String::class, block)

    /** Registers a POST stub with [StubConfiguration] using a Kotlin DSL block. */
    @JvmSynthetic
    public fun post(
        configuration: StubConfiguration,
        block: RequestSpecificationBuilder<String>.() -> Unit,
    ): BuildingStep<String> = delegate.method(configuration, Post, String::class, block)

    /** Registers a POST stub for a typed request body using a Kotlin DSL block. */
    @JvmSynthetic
    public fun <P : Any> post(
        requestType: KClass<P>,
        block: RequestSpecificationBuilder<P>.() -> Unit,
    ): BuildingStep<P> = delegate.method(StubConfiguration(), Post, requestType, block)

    /** Registers a PUT stub using a Kotlin DSL block. */
    @JvmSynthetic
    public fun put(block: RequestSpecificationBuilder<String>.() -> Unit): BuildingStep<String> =
        delegate.method(StubConfiguration(), Put, String::class, block)

    /** Registers a PUT stub with [StubConfiguration] using a Kotlin DSL block. */
    @JvmSynthetic
    public fun put(
        configuration: StubConfiguration,
        block: RequestSpecificationBuilder<String>.() -> Unit,
    ): BuildingStep<String> = delegate.method(configuration, Put, String::class, block)

    /** Registers a PUT stub for a typed request body using a Kotlin DSL block. */
    @JvmSynthetic
    public fun <P : Any> put(
        requestType: KClass<P>,
        block: RequestSpecificationBuilder<P>.() -> Unit,
    ): BuildingStep<P> = delegate.method(StubConfiguration(), Put, requestType, block)

    /** Registers a DELETE stub using a Kotlin DSL block. */
    @JvmSynthetic
    public fun delete(
        block: RequestSpecificationBuilder<String>.() -> Unit,
    ): BuildingStep<String> = delegate.method(StubConfiguration(), Delete, String::class, block)

    /** Registers a DELETE stub with [StubConfiguration] using a Kotlin DSL block. */
    @JvmSynthetic
    public fun delete(
        configuration: StubConfiguration,
        block: RequestSpecificationBuilder<String>.() -> Unit,
    ): BuildingStep<String> = delegate.method(configuration, Delete, String::class, block)

    /** Registers a DELETE stub for a typed request body using a Kotlin DSL block. */
    @JvmSynthetic
    public fun <P : Any> delete(
        requestType: KClass<P>,
        block: RequestSpecificationBuilder<P>.() -> Unit,
    ): BuildingStep<P> = delegate.method(StubConfiguration(), Delete, requestType, block)

    /** Registers a PATCH stub using a Kotlin DSL block. */
    @JvmSynthetic
    public fun patch(block: RequestSpecificationBuilder<String>.() -> Unit): BuildingStep<String> =
        delegate.method(StubConfiguration(), Patch, String::class, block)

    /** Registers a PATCH stub with [StubConfiguration] using a Kotlin DSL block. */
    @JvmSynthetic
    public fun patch(
        configuration: StubConfiguration,
        block: RequestSpecificationBuilder<String>.() -> Unit,
    ): BuildingStep<String> = delegate.method(configuration, Patch, String::class, block)

    /** Registers a PATCH stub for a typed request body using a Kotlin DSL block. */
    @JvmSynthetic
    public fun <P : Any> patch(
        requestType: KClass<P>,
        block: RequestSpecificationBuilder<P>.() -> Unit,
    ): BuildingStep<P> = delegate.method(StubConfiguration(), Patch, requestType, block)

    /** Registers a HEAD stub using a Kotlin DSL block. */
    @JvmSynthetic
    public fun head(block: RequestSpecificationBuilder<String>.() -> Unit): BuildingStep<String> =
        delegate.method(StubConfiguration(), Head, String::class, block)

    /** Registers a HEAD stub with [StubConfiguration] using a Kotlin DSL block. */
    @JvmSynthetic
    public fun head(
        configuration: StubConfiguration,
        block: RequestSpecificationBuilder<String>.() -> Unit,
    ): BuildingStep<String> = delegate.method(configuration, Head, String::class, block)

    /** Registers a HEAD stub for a typed request body using a Kotlin DSL block. */
    @JvmSynthetic
    public fun <P : Any> head(
        requestType: KClass<P>,
        block: RequestSpecificationBuilder<P>.() -> Unit,
    ): BuildingStep<P> = delegate.method(StubConfiguration(), Head, requestType, block)

    /** Registers an OPTIONS stub using a Kotlin DSL block. */
    @JvmSynthetic
    public fun options(
        block: RequestSpecificationBuilder<String>.() -> Unit,
    ): BuildingStep<String> = delegate.method(StubConfiguration(), Options, String::class, block)

    /** Registers an OPTIONS stub with [StubConfiguration] using a Kotlin DSL block. */
    @JvmSynthetic
    public fun options(
        configuration: StubConfiguration,
        block: RequestSpecificationBuilder<String>.() -> Unit,
    ): BuildingStep<String> = delegate.method(configuration, Options, String::class, block)

    /** Registers an OPTIONS stub for a typed request body using a Kotlin DSL block. */
    @JvmSynthetic
    public fun <P : Any> options(
        requestType: KClass<P>,
        block: RequestSpecificationBuilder<P>.() -> Unit,
    ): BuildingStep<P> = delegate.method(StubConfiguration(), Options, requestType, block)

    // endregion

    public companion object {
        /**
         * Creates a [Mokksy] instance backed by a new server.
         *
         * The server is **not** started automatically. Chain with [start] to start immediately:
         * ```java
         * Mokksy mokksy = Mokksy.create().start();
         * // or, for try-with-resources:
         * try (Mokksy mokksy = Mokksy.create().start()) { ... }
         * ```
         *
         * @param host Network interface to bind to. Defaults to `"127.0.0.1"`.
         * @param port Port to bind to. Defaults to `0` (OS-assigned ephemeral port).
         * @param verbose Enables `DEBUG`-level logging. Defaults to `false`.
         * @return A new, not-yet-started [Mokksy] instance.
         */
        @JvmStatic
        @JvmOverloads
        public fun create(
            host: String = DEFAULT_HOST,
            port: Int = 0,
            verbose: Boolean = false,
        ): Mokksy = Mokksy(host, port, verbose)
    }
}
