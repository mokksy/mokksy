package dev.mokksy.mokksy

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
 * Java-friendly wrapper around [MokksyServer] that provides a fully instance-method-based API.
 *
 * Eliminates Kotlin-specific boilerplate from Java test code:
 * - `start()` and `shutdown()` are instance methods
 * - HTTP stub methods (`get`, `post`, etc.) accept `Consumer<RequestSpecificationBuilder<P>>`
 *   instead of Kotlin lambdas with receivers (no `return Unit.INSTANCE`)
 * - Returns [JavaBuildingStep], which exposes `respondsWith` and `respondsWithStream`
 *   as chainable instance methods (no separate `MokksyJava.respondsWith(step, ...)` call)
 *
 * Example:
 * ```java
 * @TestInstance(TestInstance.Lifecycle.PER_CLASS)
 * class MyTest {
 *     private final MokksyServerJava mokksy = new MokksyServerJava();
 *
 *     @BeforeAll void setUp() { mokksy.start(); }
 *     @AfterAll void tearDown() { mokksy.shutdown(); }
 *
 *     @Test
 *     void myTest() throws Exception {
 *         mokksy.get(spec -> spec.path("/ping"))
 *               .respondsWith(builder -> builder.setBody("Pong"));
 *         // ... make HTTP call and assert
 *     }
 * }
 * ```
 *
 * @property delegate The underlying [MokksyServer]. Accessible for advanced configuration and
 *   for Kotlin callers that mix the two APIs.
 * @constructor Wraps an existing [MokksyServer] instance.
 */
@Suppress("TooManyFunctions")
public class MokksyServerJava(
    public val delegate: MokksyServer,
) {
    /**
     * Creates a [MokksyServerJava] backed by a new [MokksyServer].
     *
     * The server is **not** started automatically; call [start] before registering stubs or
     * making requests.
     *
     * Example:
     * ```java
     * // Random port (default) — suitable for parallel test runs:
     * MokksyServerJava mokksy = new MokksyServerJava();
     *
     * // Fixed port with verbose logging:
     * MokksyServerJava mokksy = new MokksyServerJava(8080, "127.0.0.1", true);
     * ```
     *
     * @param port Port to bind to. Defaults to `0` (OS-assigned ephemeral port). Use a fixed
     *   value only when the port must be predictable; prefer `0` to avoid conflicts in
     *   parallel test runs.
     * @param host Network interface to bind to. Defaults to `"127.0.0.1"` (loopback only).
     * @param verbose When `true`, enables `DEBUG`-level request/response logging via the
     *   configured [MokksyServer] logger. Defaults to `false`.
     */
    @JvmOverloads
    public constructor(
        port: Int = 0,
        host: String = DEFAULT_HOST,
        verbose: Boolean = false,
    ) : this(MokksyServer(port = port, host = host, verbose = verbose))

    // region Lifecycle

    /**
     * Starts the server and blocks the calling thread until the port is bound and the server
     * is ready to accept requests.
     *
     * Call this method once before registering stubs or issuing test requests, typically in
     * a `@BeforeAll` (JUnit 5) or `@Before` (JUnit 4) setup method.
     *
     * Example:
     * ```java
     * @BeforeAll
     * void setUp() {
     *     mokksy.start();
     * }
     * ```
     */
    public fun start(): Unit =
        runBlocking {
            delegate.startSuspend()
            delegate.awaitStarted()
        }

    /**
     * Stops the server and blocks the calling thread until shutdown is complete.
     *
     * Active connections are given [gracePeriodMillis] milliseconds to finish before the server
     * stops accepting new work. If connections do not close within [timeoutMillis] milliseconds,
     * shutdown is forced.
     *
     * Call this method in a `@AfterAll` (JUnit 5) or `@After` (JUnit 4) teardown method.
     *
     * Example:
     * ```java
     * @AfterAll
     * void tearDown() {
     *     mokksy.shutdown();          // gracePeriodMillis=500, timeoutMillis=1000
     *     mokksy.shutdown(200, 400);  // custom timings
     * }
     * ```
     *
     * @param gracePeriodMillis Milliseconds to wait for active connections to finish before
     *   the server stops accepting new work. Defaults to `500`.
     * @param timeoutMillis Maximum milliseconds to wait for all connections to close before
     *   forcing shutdown. Must be ≥ [gracePeriodMillis]. Defaults to `1000`.
     */
    @JvmOverloads
    public fun shutdown(
        gracePeriodMillis: Long = 500,
        timeoutMillis: Long = 1000,
    ): Unit =
        runBlocking {
            delegate.shutdownSuspend(gracePeriodMillis, timeoutMillis)
        }

    /**
     * Returns the base URL of the server in the form `http://<host>:<port>`.
     *
     * Use this URL as the root for all HTTP requests in tests:
     * ```java
     * HttpRequest request = HttpRequest.newBuilder()
     *     .uri(URI.create(mokksy.baseUrl() + "/ping"))
     *     .GET()
     *     .build();
     * ```
     *
     * @return The base URL string, e.g. `"http://127.0.0.1:8080"`.
     * @throws IllegalStateException if [start] has not been called yet.
     */
    public fun baseUrl(): String = delegate.baseUrl()

    /**
     * Returns the port the server is bound to.
     *
     * The value is available only after [start] has returned. When the server was created
     * with `port = 0` (the default), this returns the ephemeral port assigned by the OS.
     *
     * @return The bound port number, e.g. `8080`.
     */
    public fun port(): Int = delegate.port()

    // endregion

    // region Stub registration

    private fun <P : Any> method(
        configuration: StubConfiguration,
        httpMethod: HttpMethod,
        requestType: KClass<P>,
        spec: Consumer<RequestSpecificationBuilder<P>>,
    ): JavaBuildingStep<P> =
        JavaBuildingStep(
            delegate.method(configuration, httpMethod, requestType) { spec.accept(this) },
        )

    private fun method(
        configuration: StubConfiguration,
        httpMethod: HttpMethod,
        spec: Consumer<RequestSpecificationBuilder<String>>,
    ): JavaBuildingStep<String> = method(configuration, httpMethod, String::class, spec)

    // region GET

    /** Registers a GET stub with a [String] request body. */
    public fun get(spec: Consumer<RequestSpecificationBuilder<String>>): JavaBuildingStep<String> =
        method(StubConfiguration(), Get, spec)

    /** Registers a GET stub with a [String] request body and [StubConfiguration]. */
    public fun get(
        configuration: StubConfiguration,
        spec: Consumer<RequestSpecificationBuilder<String>>,
    ): JavaBuildingStep<String> = method(configuration, Get, spec)

    /** Registers a GET stub with a typed request body. */
    public fun <P : Any> get(
        requestType: Class<P>,
        spec: Consumer<RequestSpecificationBuilder<P>>,
    ): JavaBuildingStep<P> = method(StubConfiguration(), Get, requestType.kotlin, spec)

    /** Registers a GET stub with a typed request body and [StubConfiguration]. */
    public fun <P : Any> get(
        configuration: StubConfiguration,
        requestType: Class<P>,
        spec: Consumer<RequestSpecificationBuilder<P>>,
    ): JavaBuildingStep<P> = method(configuration, Get, requestType.kotlin, spec)

    // endregion

    // region POST

    /** Registers a POST stub with a [String] request body. */
    public fun post(
        spec: Consumer<RequestSpecificationBuilder<String>>,
    ): JavaBuildingStep<String> = method(StubConfiguration(), Post, spec)

    /** Registers a POST stub with a [String] request body and [StubConfiguration]. */
    public fun post(
        configuration: StubConfiguration,
        spec: Consumer<RequestSpecificationBuilder<String>>,
    ): JavaBuildingStep<String> = method(configuration, Post, spec)

    /** Registers a POST stub with a typed request body. */
    public fun <P : Any> post(
        requestType: Class<P>,
        spec: Consumer<RequestSpecificationBuilder<P>>,
    ): JavaBuildingStep<P> = method(StubConfiguration(), Post, requestType.kotlin, spec)

    /** Registers a POST stub with a typed request body and [StubConfiguration]. */
    public fun <P : Any> post(
        configuration: StubConfiguration,
        requestType: Class<P>,
        spec: Consumer<RequestSpecificationBuilder<P>>,
    ): JavaBuildingStep<P> = method(configuration, Post, requestType.kotlin, spec)

    // endregion

    // region PUT

    /** Registers a PUT stub with a [String] request body. */
    public fun put(spec: Consumer<RequestSpecificationBuilder<String>>): JavaBuildingStep<String> =
        method(StubConfiguration(), Put, spec)

    /** Registers a PUT stub with a [String] request body and [StubConfiguration]. */
    public fun put(
        configuration: StubConfiguration,
        spec: Consumer<RequestSpecificationBuilder<String>>,
    ): JavaBuildingStep<String> = method(configuration, Put, spec)

    /** Registers a PUT stub with a typed request body. */
    public fun <P : Any> put(
        requestType: Class<P>,
        spec: Consumer<RequestSpecificationBuilder<P>>,
    ): JavaBuildingStep<P> = method(StubConfiguration(), Put, requestType.kotlin, spec)

    /** Registers a PUT stub with a typed request body and [StubConfiguration]. */
    public fun <P : Any> put(
        configuration: StubConfiguration,
        requestType: Class<P>,
        spec: Consumer<RequestSpecificationBuilder<P>>,
    ): JavaBuildingStep<P> = method(configuration, Put, requestType.kotlin, spec)

    // endregion

    // region DELETE

    /** Registers a DELETE stub with a [String] request body. */
    public fun delete(
        spec: Consumer<RequestSpecificationBuilder<String>>,
    ): JavaBuildingStep<String> = method(StubConfiguration(), Delete, spec)

    /** Registers a DELETE stub with a [String] request body and [StubConfiguration]. */
    public fun delete(
        configuration: StubConfiguration,
        spec: Consumer<RequestSpecificationBuilder<String>>,
    ): JavaBuildingStep<String> = method(configuration, Delete, spec)

    /** Registers a DELETE stub with a typed request body. */
    public fun <P : Any> delete(
        requestType: Class<P>,
        spec: Consumer<RequestSpecificationBuilder<P>>,
    ): JavaBuildingStep<P> = method(StubConfiguration(), Delete, requestType.kotlin, spec)

    /** Registers a DELETE stub with a typed request body and [StubConfiguration]. */
    public fun <P : Any> delete(
        configuration: StubConfiguration,
        requestType: Class<P>,
        spec: Consumer<RequestSpecificationBuilder<P>>,
    ): JavaBuildingStep<P> = method(configuration, Delete, requestType.kotlin, spec)

    // endregion

    // region PATCH

    /** Registers a PATCH stub with a [String] request body. */
    public fun patch(
        spec: Consumer<RequestSpecificationBuilder<String>>,
    ): JavaBuildingStep<String> = method(StubConfiguration(), Patch, spec)

    /** Registers a PATCH stub with a [String] request body and [StubConfiguration]. */
    public fun patch(
        configuration: StubConfiguration,
        spec: Consumer<RequestSpecificationBuilder<String>>,
    ): JavaBuildingStep<String> = method(configuration, Patch, spec)

    /** Registers a PATCH stub with a typed request body. */
    public fun <P : Any> patch(
        requestType: Class<P>,
        spec: Consumer<RequestSpecificationBuilder<P>>,
    ): JavaBuildingStep<P> = method(StubConfiguration(), Patch, requestType.kotlin, spec)

    /** Registers a PATCH stub with a typed request body and [StubConfiguration]. */
    public fun <P : Any> patch(
        configuration: StubConfiguration,
        requestType: Class<P>,
        spec: Consumer<RequestSpecificationBuilder<P>>,
    ): JavaBuildingStep<P> = method(configuration, Patch, requestType.kotlin, spec)

    // endregion

    // region HEAD

    /** Registers a HEAD stub. */
    public fun head(
        spec: Consumer<RequestSpecificationBuilder<String>>,
    ): JavaBuildingStep<String> = method(StubConfiguration(), Head, spec)

    /** Registers a HEAD stub with a typed request body. */
    public fun <P : Any> head(
        requestType: Class<P>,
        spec: Consumer<RequestSpecificationBuilder<P>>,
    ): JavaBuildingStep<P> = method(StubConfiguration(), Head, requestType.kotlin, spec)

    /** Registers a HEAD stub with [StubConfiguration]. */
    public fun head(
        configuration: StubConfiguration,
        spec: Consumer<RequestSpecificationBuilder<String>>,
    ): JavaBuildingStep<String> = method(configuration, Head, spec)

    /** Registers a HEAD stub with a typed request body and [StubConfiguration]. */
    public fun <P : Any> head(
        configuration: StubConfiguration,
        requestType: Class<P>,
        spec: Consumer<RequestSpecificationBuilder<P>>,
    ): JavaBuildingStep<P> = method(configuration, Head, requestType.kotlin, spec)

    // endregion

    // region OPTIONS

    /** Registers an OPTIONS stub. */
    public fun options(
        spec: Consumer<RequestSpecificationBuilder<String>>,
    ): JavaBuildingStep<String> = method(StubConfiguration(), Options, spec)

    /** Registers an OPTIONS stub with a typed request body. */
    public fun <P : Any> options(
        requestType: Class<P>,
        spec: Consumer<RequestSpecificationBuilder<P>>,
    ): JavaBuildingStep<P> = method(StubConfiguration(), Options, requestType.kotlin, spec)

    /** Registers an OPTIONS stub with [StubConfiguration]. */
    public fun options(
        configuration: StubConfiguration,
        spec: Consumer<RequestSpecificationBuilder<String>>,
    ): JavaBuildingStep<String> = method(configuration, Options, spec)

    /** Registers an OPTIONS stub with a typed request body and [StubConfiguration]. */
    public fun <P : Any> options(
        configuration: StubConfiguration,
        requestType: Class<P>,
        spec: Consumer<RequestSpecificationBuilder<P>>,
    ): JavaBuildingStep<P> = method(configuration, Options, requestType.kotlin, spec)

    // endregion

    // endregion

    // region Verification

    /** Resets stub match counts and clears the request journal. */
    public fun resetMatchCounts(): Unit = delegate.resetMatchCounts()

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
}
