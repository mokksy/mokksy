# Mokksy

[![Maven Central](https://img.shields.io/maven-central/v/dev.mokksy/mokksy)](https://repo1.maven.org/maven2/dev/mokksy/mokksy/)
[![Build](https://github.com/mokksy/mokksy/actions/workflows/gradle.yml/badge.svg?branch=main)](https://github.com/mokksy/mokksy/actions/workflows/gradle.yml)

[![Codacy Badge](https://app.codacy.com/project/badge/Grade/545ae1f12da24e199c9a5432d5290d2e)](https://app.codacy.com/gh/mokksy/mokksy/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![Codacy Badge](https://app.codacy.com/project/badge/Coverage/545ae1f12da24e199c9a5432d5290d2e)](https://app.codacy.com/gh/mokksy/mokksy/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_coverage)
[![codecov](https://codecov.io/github/mokksy/mokksy/branch/main/graph/badge.svg?token=IAAMJNDRX4)](https://codecov.io/github/mokksy/mokksy)

![Kotlin API](https://img.shields.io/badge/Kotlin-2.2-%237F52FF.svg?logo=kotlin&logoColor=white)
![Java](https://img.shields.io/badge/JVM-17-%23ED8B00.svg)
[![Kotlin Multiplatform](https://img.shields.io/badge/Platforms-%20JVM%20%7C%20Wasm%2FJS%20%7C%20Native%20-%237F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/docs/multiplatform.html)
![GitHub License](https://img.shields.io/github/license/mokksy/mokksy)

[![API Reference](https://img.shields.io/badge/API-Reference-blue)](https://mokksy.github.io/mokksy/)
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/mokksy/mokksy)

![mokksy-mascot-256.png](docs/mokksy-mascot-256.png)

**_Mokksy_** - Mock HTTP Server, built with [Kotlin](https://kotlinlang.org/) and [Ktor](https://ktor.io/).

**Check out the [AI-Mocks][ai-mocks] project for advanced LLM and [A2A protocol][a2a] mocking capabilities.**

> [!NOTE]
> Mokksy server was a part of the [AI-Mocks][ai-mocks] project and has now moved to a separate repository. No artefact relocation is required.



[![Buy me a Coffee](https://cdn.buymeacoffee.com/buttons/default-orange.png)](https://buymeacoffee.com/mailsk)

**Table of Contents**

<!--- TOC -->

* [Why Mokksy?](#why-mokksy?)
* [Key Features](#key-features)
* [Quick start](#quick-start)
* [Responding with predefined responses](#responding-with-predefined-responses)
  * [GET request](#get-request)
  * [POST request](#post-request)
  * [Status-only responses](#status-only-responses)
* [Server-Side Events (SSE) response](#server-side-events-sse-response)
* [Request Specification Matchers](#request-specification-matchers)
  * [Stub Specificity](#stub-specificity)
  * [Typed body matching](#typed-body-matching)
  * [Priority Example](#priority-example)
* [Verifying Requests](#verifying-requests)
  * [Verify all stubs were triggered](#verify-all-stubs-were-triggered)
  * [Verify no unexpected requests arrived](#verify-no-unexpected-requests-arrived)
  * [Recommended AfterEach setup](#recommended-aftereach-setup)
  * [Inspecting unmatched items](#inspecting-unmatched-items)
* [Request Journal](#request-journal)
* [Embedding in an existing Ktor application](#embedding-in-an-existing-ktor-application)
  * [Application-level installation](#application-level-installation)
  * [Route-level installation](#route-level-installation)
* [Java API](#java-api)
  * [Streaming responses](#streaming-responses)
  * [Jackson support](#jackson-support)

<!--- END -->

## Why Mokksy?

Wiremock does not support true SSE and streaming responses.

Mokksy is here to address those limitations.
Particularly, it might be useful for integration testing LLM clients.

## Key Features

- **Streaming Support**: True support for streaming responses and [Server-Side Events (SSE)][sse]
- **Response Control**: Flexibility to control server responses directly via `ApplicationCall` object
- **Delay Simulation**: Support for simulating response delays and delays between chunks
- **Modern API**: Fluent Kotlin DSL API with [Kotest Assertions](https://kotest.io/docs/assertions/assertions.html)
- **Error Simulation**: Ability to mock negative scenarios and error responses
- **Specificity-Based Matching**: When multiple stubs match a request, Mokksy automatically selects the most specific one — no explicit priority configuration required for common cases
- **Ktor Integration**: Embed Mokksy into any existing Ktor application via `Application.mokksy()` and `Route.mokksy()` extension functions — including behind authentication middleware

## Quick start
      
1. Add dependencies:
 
   Gradle _build.gradle.kts:_
   
   ```kotlin
   dependencies {               
        // for multiplatform projects
       implementation("dev.mokksy:mokksy:$latestVersion")
        // for JVM projects
       implementation("dev.mokksy:mokksy-jvm:$latestVersion")
   }
   ``` 
  
   _pom.xml:_
   ```xml
    <dependency>
        <groupId>dev.mokksy</groupId>
        <artifactId>mokksy-jvm</artifactId>
        <version>[LATEST_VERSION]</version>
        <scope>test</scope>
    </dependency>
   ```


2. Create and start Mokksy server:

   **Kotlin — all platforms (coroutine-based):**

   ```kotlin
   import dev.mokksy.mokksy.Mokksy

   val mokksy = Mokksy()
   mokksy.startSuspend()
   mokksy.awaitStarted() // port() and baseUrl() are safe after this point
   ```

   **Kotlin — JVM blocking:**

   ```kotlin
   import dev.mokksy.mokksy.Mokksy

   val mokksy = Mokksy().start()
   ```

   **Java** — see [Java API](#java-api) below.

3. Configure http client using Mokksy server's as baseUrl in your application:

```kotlin
val client = HttpClient {
  install(DefaultRequest) {
    url(mokksy.baseUrl())
  }
}
```

## Responding with predefined responses

Mokksy supports all HTTP verbs. Here are some examples.

### GET request

GET request example:

<!--- CLEAR -->
<!--- INCLUDE
import dev.mokksy.Mokksy
import io.kotest.matchers.equals.beEqual
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.withCharsetIfNeeded
import io.ktor.sse.ServerSentEvent
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class ReadmeTest {
    val mokksy = Mokksy.create().start()
    val client = HttpClient {
        install(DefaultRequest) {
            url(mokksy.baseUrl())
        }
    }
-->
<!--- INCLUDE
  @Test
  suspend fun testGet() {
-->

```kotlin
// given
val expectedResponse =
  // language=json
  """
    {
        "response": "Pong"

    }
    """.trimIndent()

mokksy.get {
  path = beEqual("/ping")
  containsHeader("Foo", "bar")
} respondsWith {
  body = expectedResponse
}

// when
val result = client.get("/ping") {
  headers.append("Foo", "bar")
}

// then
result.status shouldBe HttpStatusCode.OK
result.bodyAsText() shouldBe expectedResponse
```

When the request does not match - Mokksy server returns `404 (Not Found)`:

```kotlin
val notFoundResult = client.get("/ping") {
  headers.append("Foo", "baz")
}

notFoundResult.status shouldBe HttpStatusCode.NotFound
```

<!--- INCLUDE
  }
-->

### POST request

POST request example:

<!--- INCLUDE
  @Test
  suspend fun testPost() {
-->

```kotlin
// given
val id = Random.nextInt()
val expectedResponse =
  // language=json
  """
    {
        "id": "$id",
        "name": "thing-$id"
    }
    """.trimIndent()

mokksy.post {
  path = beEqual("/things")
  bodyContains("\"$id\"")
} respondsWith {
  body = expectedResponse
  httpStatus = HttpStatusCode.Created
  headers {
    // type-safe builder style
    append(HttpHeaders.Location, "/things/$id")
  }
  headers += "Foo" to "bar" // list style
}

// when
val result =
  client.post("/things") {
    headers.append("Content-Type", "application/json")
    setBody(
      // language=json
      """
            {
                "id": "$id"
            }
            """.trimIndent(),
    )
  }

// then
result.status shouldBe HttpStatusCode.Created
result.bodyAsText() shouldBe expectedResponse
result.headers["Location"] shouldBe "/things/$id"
result.headers["Foo"] shouldBe "bar"
```

<!--- INCLUDE
  }
-->

### Status-only responses

Use `respondsWithStatus` when the test only needs to verify a status code — no body needed.
It's an infix function, so it reads naturally next to the stub definition:
`mokksy.get { path("/ping") } respondsWithStatus HttpStatusCode.NoContent`.

Java callers use the `int` overload on `JavaBuildingStep`:

```java
mokksy.get(spec -> spec.path("/ping")).respondsWithStatus(204);
mokksy.delete(spec -> spec.path("/item")).respondsWithStatus(410);
```

## Server-Side Events (SSE) response

[Server-Side Events (SSE)](https://html.spec.whatwg.org/multipage/server-sent-events.html) is a technology that allows a
server to push updates to the client over a single, long-lived HTTP connection. This enables real-time updates without
requiring the client to continuously poll the server for new data.

SSE streams events in a standardized format, making it easy for clients to consume the data and handle events as they
arrive. It's lightweight and efficient, particularly well-suited for applications requiring real-time updates like live
notifications or feed updates.

Server-Side Events (SSE) example:

<!--- INCLUDE 
  @Test
  suspend fun testSse() {
-->

```kotlin
mokksy.post {
  path = beEqual("/sse")
} respondsWithSseStream {
  flow =
    flow {
      delay(200.milliseconds)
      emit(
        ServerSentEvent(
          data = "One",
        ),
      )
      delay(50.milliseconds)
      emit(
        ServerSentEvent(
          data = "Two",
        ),
      )
    }
}

// when
val result = client.post("/sse")

// then
result.status shouldBe HttpStatusCode.OK
result.contentType() shouldBe ContentType.Text.EventStream.withCharsetIfNeeded(Charsets.UTF_8)
result.bodyAsText() shouldBe "data: One\r\ndata: Two\r\n"
```

<!--- INCLUDE
  }
-->

## Request Specification Matchers

Mokksy provides various matcher types to specify conditions for matching incoming HTTP requests:

- **Path matchers** — `path("/things")` or `path = beEqual("/things")`
- **Header matchers** — `containsHeader("X-Request-ID", "abc")` checks for a header with an exact value
- **Content matchers** — `bodyContains("value")` checks if the raw body string contains a substring;
  `bodyString += contain("value")` adds a Kotest matcher directly
- **Predicate matchers** — `bodyMatchesPredicate { it?.name == "foo" }` matches against the typed,
  deserialized request body
- **Call matchers** — `successCallMatcher` matches if a function called with the body does not throw
- **Priority** — `priority = 10` on `RequestSpecificationBuilder` sets the `RequestSpecification.priority`
  of the stub; lower values indicate higher priority. Default is `Int.MAX_VALUE`.
  Priority is a tiebreaker: it applies only when two stubs match with an equal number of conditions satisfied.
  For most cases, specificity-based matching (see below) selects the right stub automatically.

### Stub Specificity

When multiple stubs could match the same request, Mokksy scores each one by counting how many conditions
it satisfies, then selects the highest-scoring stub. A stub with two matching conditions beats a stub with one,
regardless of registration order.

<!--- INCLUDE 
  @Test
  suspend fun testSpecificity() {
-->

```kotlin
// Generic: matches any POST to /users
mokksy.post {
    path("/users")
} respondsWith {
    body = "any user"
}

// Specific: matches only requests whose body contains "admin" — two conditions
mokksy.post {
    path("/users")
    bodyContains("admin")
} respondsWith {
    body = "admin user"
}

// Admin request → specific stub wins (score 2 beats score 1)
val adminResult = client.post("/users") { setBody("admin") }
adminResult.bodyAsText() shouldBe "admin user"

// Other request → only the generic stub matches
val genericResult = client.post("/users") { setBody("regular") }
genericResult.bodyAsText() shouldBe "any user"
```
<!--- INCLUDE
  }
-->
<!--- INCLUDE
  @Test
  suspend fun testRespondsWithStatus() {
-->

```kotlin
mokksy.get { path("/ping") } respondsWithStatus HttpStatusCode.NoContent

val response = client.get("/ping")

response.status shouldBe HttpStatusCode.NoContent
```

<!--- INCLUDE
  }
-->
<!--- SUFFIX
}
-->
<!--- KNIT example-readme-01.kt -->

### Typed body matching

When the request body is a structured object, use `bodyMatchesPredicate` to match against the
deserialized payload instead of raw string operations. Pass the type as a reified parameter (Kotlin)
or a `KClass` / `Class` token (Kotlin/Java):

```kotlin
// Kotlin — reified inline extension (hides from Java callers)
mokksy.post<CreateItemRequest> {
    path("/items")
    bodyMatchesPredicate { it?.name == "widget" }
} respondsWith {
    body = """{"id":"1"}"""
    httpStatus = HttpStatusCode.Created
}

// Kotlin — explicit KClass (works with the Mokksy Java wrapper too)
mokksy.post(CreateItemRequest::class) {
    path("/items")
    bodyMatchesPredicate { it?.name == "widget" }
} respondsWith {
    body = """{"id":"1"}"""
    httpStatus = HttpStatusCode.Created
}
```

Deserialization uses Ktor's `ContentNegotiation` plugin. For projects that use Jackson instead of
kotlinx.serialization, create the server with `MokksyJackson.create()` — see [Jackson support](#jackson-support).

When no stub matches and verbose mode is enabled 
(`Mokksy(verbose = true)` / `Mokksy.create(Mokksy.create("127.0.0.1", 0, true))` for Java), 
Mokksy logs the closest partial match and its failed conditions to help you diagnose the mismatch.

### Priority Example

If multiple stubs match with the same specificity score, the one with the lower `priority` value wins:

<!--- INCLUDE
  @Test
  suspend fun testPriority() {
-->

```kotlin
// Catch-all stub with low priority (high value)
mokksy.get {
  path = contain("/things")
  priority = 99
} respondsWith {
  body = "Generic Thing"
}

// Specific stub with high priority (low value)
mokksy.get {
  path = beEqual("/things/special")
  priority = 1
} respondsWith {
  body = "Special Thing"
}

// when
val generic = client.get("/things/123")
val special = client.get("/things/special")

// then
generic.bodyAsText() shouldBe "Generic Thing"
special.bodyAsText() shouldBe "Special Thing"
```

<!--- INCLUDE
  }
-->

## Verifying Requests

Mokksy provides two complementary verification methods that check opposite sides of the stub/request contract.

### Verify all stubs were triggered

`verifyNoUnmatchedStubs()` fails if any registered stub was never matched by an incoming request.
Use this to catch stubs you set up but that were never actually called — a sign the code under test took
a different path than expected.

```kotlin
// Fails if any stub has never been matched
mokksy.verifyNoUnmatchedStubs()
```

> **Note:** Be careful when running tests in parallel against a single `MokksyServer` instance.
> Some stubs might be unmatched when one test completes. Avoid calling this in `@AfterEach`/`@AfterTest`
> unless each test owns its own server instance.

### Verify no unexpected requests arrived

`verifyNoUnexpectedRequests()` fails if any HTTP request arrived at the server but no stub matched it.
These requests are recorded in the `RequestJournal` and reported together.

```kotlin
// Fails if any request arrived with no matching stub
mokksy.verifyNoUnexpectedRequests()
```

### Recommended AfterEach setup

Always run `verifyNoUnexpectedRequests()` in `@AfterEach` to catch requests that arrived but
matched no stub. For `verifyNoUnmatchedStubs()`, the right placement depends on your fixture strategy:

- **Per-test instance** (`@TestInstance(Lifecycle.PER_METHOD)` or a fresh server per test): call
  both checks in `@AfterEach` — every stub registered during that test should have been matched
  before the server is torn down.
- **Shared instance** (`@TestInstance(Lifecycle.PER_CLASS)` or a companion-object server): call
  `verifyNoUnmatchedStubs()` in `@AfterAll`, immediately before `shutdown()`. Calling it after
  each individual test would falsely report stubs registered for _later_ tests as unmatched.

<!--- CLEAR -->
<!--- INCLUDE
import dev.mokksy.Mokksy
import io.kotest.matchers.equals.beEqual
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
-->
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MyTest {

    val mokksy = Mokksy.create()
    lateinit var client: HttpClient

    @BeforeAll
    suspend fun setup() {
        mokksy.startSuspend()
        mokksy.awaitStarted() // port() and baseUrl() are safe after this point
        client = HttpClient {
            install(DefaultRequest) {
                url(mokksy.baseUrl())
            }
        }
    }

    @Test
    suspend fun testSomething() {
        mokksy.get {
            path("/hi")
        } respondsWith {
            delay = 100.milliseconds // wait 100ms, then reply
            body = "Hello"
        }

        // when
        val response = client.get("/hi")

        // then
        response.status shouldBe HttpStatusCode.OK
        response.bodyAsText() shouldBe "Hello"
    }

    @AfterEach
    fun afterEach() {
        mokksy.verifyNoUnexpectedRequests()
    }

    @AfterAll
    suspend fun afterAll() {
        client.close()
        mokksy.verifyNoUnmatchedStubs() // shared instance: check once, after all tests ran
        mokksy.shutdownSuspend()
    }
}
```
<!--- KNIT example-readme-02.kt -->

### Inspecting unmatched items

Use the `find*` variants to retrieve the unmatched items directly for custom assertions:

```kotlin
// List<RecordedRequest> — HTTP requests with no matching stub
val unmatchedRequests: List<RecordedRequest> = mokksy.findAllUnexpectedRequests()

// List<RequestSpecification<*>> — stubs that were never triggered
val unmatchedStubs: List<RequestSpecification<*>> = mokksy.findAllUnmatchedStubs()
```

`RecordedRequest` is an immutable snapshot that captures `method`, `uri`, and `headers` of the incoming request.

## Request Journal

Mokksy records incoming requests in a `RequestJournal`. The recording mode is controlled by `JournalMode` in
`ServerConfiguration`:

| Mode                           | Behaviour                                                                                                   |
|--------------------------------|-------------------------------------------------------------------------------------------------------------|
| `JournalMode.LEAN` _(default)_ | Records only requests with no matching stub. Lower overhead; sufficient for `verifyNoUnexpectedRequests()`. |
| `JournalMode.FULL`             | Records all incoming requests — both matched and unmatched.                                                 |

```kotlin
val mokksy = MokksyServer(
    configuration = ServerConfiguration(
        journalMode = JournalMode.FULL,
    ),
)
```

Call `resetMatchState()` between scenarios to clear stub match state and the journal:

```kotlin
@AfterTest
fun afterEach() {
    mokksy.resetMatchState()
}
```

> **Note:** Stubs configured with `eventuallyRemove = true` are permanently removed from the registry
> on first match and cannot be re-armed by `resetMatchState()`. Re-register them before the next scenario.

## Embedding in an existing Ktor application

If you already own a Ktor `Application` — a test harness with authentication middleware, custom
plugins, or routes that must coexist with stubs — use the `mokksy` extension functions to mount
stub handling directly, without allocating a second embedded server.

### Application-level installation

`Application.mokksy(server)` installs [SSE][sse], `DoubleReceive`, and `ContentNegotiation`
automatically, then mounts a catch-all route that dispatches every incoming request through the
stub registry:

```kotlin
import dev.mokksy.mokksy.MokksyServer
import dev.mokksy.mokksy.mokksy
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

val server = MokksyServer()
server.get { path("/ping") } respondsWith { body = "pong" }

embeddedServer(Netty, port = 8080) {
    mokksy(server)
}.start(wait = true)
```

Use this overload when Mokksy owns the entire application and you want the simplest possible setup.

### Route-level installation

`Route.mokksy(server)` mounts the stub handler inside an existing route scope. Unlike the
application-level overload, it does **not** install plugins — you are responsible for installing
`SSE`, `DoubleReceive`, and `ContentNegotiation` on the surrounding application. This makes it
suitable when Mokksy stubs coexist with real routes:

```kotlin
routing {
    get("/health") { call.respondText("OK") }
    mokksy(server)
}
```

To place stubs behind an authentication check, install the required plugins and wrap `mokksy` in
an `authenticate` block:

```kotlin
install(SSE)
install(DoubleReceive)
install(ContentNegotiation) { json() }
install(Authentication) {
    basic("auth-basic") {
        validate { credentials ->
            if (credentials.name == "user" && credentials.password == "pass") {
                UserIdPrincipal(credentials.name)
            } else null
        }
    }
}

routing {
    authenticate("auth-basic") {
        mokksy(server)
    }
}
```

Both extension functions accept any `path` pattern as a second parameter (default: `"{...}"`,
which matches all routes). Narrow the scope by passing a prefix:

```kotlin
mokksy(server, path = "/api/{...}")
```

## Java API

Java callers use `dev.mokksy.Mokksy` — a JVM-only, `AutoCloseable` wrapper that exposes a
`Consumer`-based fluent API instead of Kotlin lambdas with receivers.

**Lifecycle:**

```java
import dev.mokksy.Mokksy;

Mokksy mokksy = Mokksy.create("127.0.0.1", 0, true).start();

mokksy.get(spec -> spec.path("/ping"))
      .respondsWith(builder -> builder.body("Pong"));

mokksy.shutdown();
```

`Mokksy` implements `AutoCloseable`, so try-with-resources works for test fixtures that need a short-lived server:

```java
try (Mokksy mokksy = Mokksy.create().start()) {
    mokksy.post(spec -> spec.path("/items"))
          .respondsWith(builder -> builder.body("{\"id\":\"42\"}").status(201).header("Location", "/items/42"));
}
```

**JUnit 5 setup:**

```java
import dev.mokksy.Mokksy;
import java.net.http.HttpClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MyTest {

    private final Mokksy mokksy = Mokksy.create();
    private HttpClient httpClient;

    @BeforeAll
    void setUp() {
        mokksy.start();
        httpClient = HttpClient.newHttpClient();
    }
    
    @Test
    void test() {
      // call server
    }

    @AfterAll
    void tearDown() {
        mokksy.shutdown();
    }
}
```

**Status-only responses** — use `respondsWithStatus(int)` when no body is needed:

```java
mokksy.get(spec -> spec.path("/health")).respondsWithStatus(200);
mokksy.delete(spec -> spec.path("/items/1")).respondsWithStatus(204);
```

**Request matchers** — the `spec` block mirrors the Kotlin DSL:

```java
mokksy.post(spec -> {
    spec.path("/secured");
    spec.containsHeader("X-Api-Key", "secret");
    spec.bodyContains("\"role\":\"admin\"");
}).respondsWith(builder -> builder.body("authorized").status(200));
```

**All HTTP verbs** are available as named methods (`get`, `post`, `put`, `delete`, `patch`,
`head`, `options`). Use `method(String, spec)` for dynamic method names in parameterised tests:

```java
mokksy.method("PATCH", spec -> spec.path("/resource"))
      .respondsWith(builder -> builder.body("patched"));
```

### Streaming responses

Testing an LLM client or any endpoint that streams data chunk-by-chunk? Use `respondsWithStream`
to stub a chunked HTTP response. The default `Content-Type` is `text/event-stream; charset=UTF-8`,
which matches what most streaming AI APIs and SSE endpoints produce.

**Chunks from a list** — the simplest case:

```java
mokksy.get(spec -> spec.path("/stream"))
      .respondsWithStream(builder -> builder
          .chunks(List.of("Hello", " ", "World")));
```

**Chunks from a `Stream<T>`** — the stream is consumed lazily when the first matching request
arrives, not when the stub is registered. This is useful for live generators or mutable sources
that should reflect their state at request time:

```java
mokksy.get(spec -> spec.path("/events"))
      .respondsWithStream(builder -> builder
          .chunks(Stream.of("data1", "data2")));
```

**Delays** — simulate network and processing latency at two granularities:

```java
mokksy.get(spec -> spec.path("/slow-stream"))
      .respondsWithStream(builder -> builder
          .chunks(List.of("A", "B", "C"))
          .delayMillis(200L)               // pause before the first chunk
          .delayBetweenChunksMillis(100L)); // pause between each subsequent chunk
```

**Custom `Content-Type`** — override the default when the stream carries a different format, such
as NDJSON:

```java
mokksy.get(spec -> spec.path("/ndjson"))
      .respondsWithStream(builder -> builder
          .chunks(List.of("{\"value\":1}", "{\"value\":2}"))
          .contentType("application/x-ndjson"));
```

For typed chunks, pass the class token as the first argument. Chunks are serialized to the
response body using each object's `toString()`:

```java
mokksy.get(spec -> spec.path("/typed"))
      .respondsWithStream(MyEvent.class, builder -> builder
          .chunk(new MyEvent("start"))
          .chunk(new MyEvent("end")));
```

### Jackson support

Use `MokksyJackson.create()` when your tests match typed Java objects deserialized from the
request body. The API mirrors `Mokksy.create()` exactly — same `host`, `port`, and `verbose`
parameters — with an optional `ObjectMapper` configuration callback.

Add the dependency alongside `mokksy`:

```kotlin
testImplementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
```

Then create the server:

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mokksy.Mokksy;
import dev.mokksy.MokksyJackson;

// Default Jackson configuration
Mokksy mokksy = MokksyJackson.create().start();

// Custom ObjectMapper — e.g. register Java time / records support
Mokksy mokksy = MokksyJackson.create(ObjectMapper::findAndRegisterModules).start();
```

Typed body matchers work the same way as in the standard API — pass the `Class` token to
the stub-registration method and use `bodyMatchesPredicate` to assert on the deserialized object:

```java
record CreateItemRequest(String name, int quantity) {}

mokksy.post(
    CreateItemRequest.class,
    spec -> spec.path("/items")
                .bodyMatchesPredicate(req -> "widget".equals(req.name()))
).respondsWith(builder -> builder.body("{\"id\":\"1\"}").status(201));
```

[sse]: https://html.spec.whatwg.org/multipage/server-sent-events.html "Server-Side Events Specification"
[ai-mocks]: https://github.com/mokksy/ai-mocks/ "AI-Mock: Mokksy extensions for AI integrations"
[a2a]: https://a2a-protocol.org/ "Agent2Agent (A2A) Protocol, an open standard designed to enable seamless communication and collaboration between AI agents."
