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

**_Mokksy_** - Mock HTTP Server, built with [Kotlin](https://kotlinlang.org/) and [Ktor](https://ktor.io/).

**Check out the [AI-Mocks][ai-mocks] project for advanced LLM and [A2A protocol][a2a] mocking capabilities.**

> [!NOTE]
> Mokksy server was a part of the [AI-Mocks][ai-mocks] project and has now moved to a separate repository. No artefact relocation is required.

![mokksy-mascot-256.png](docs/mokksy-mascot-256.png)

[![Buy me a Coffee](https://cdn.buymeacoffee.com/buttons/default-orange.png)](https://buymeacoffee.com/mailsk)

**Table of Contents**

<!--- TOC -->

* [Why Mokksy?](#why-mokksy?)
* [Key Features](#key-features)
* [Quick start](#quick-start)
* [Responding with predefined responses](#responding-with-predefined-responses)
  * [GET request](#get-request)
  * [POST request](#post-request)
* [Server-Side Events (SSE) response](#server-side-events-sse-response)
* [Request Specification Matchers](#request-specification-matchers)
* [Verifying Requests](#verifying-requests)
  * [Verify all stubs were triggered](#verify-all-stubs-were-triggered)
  * [Verify no unexpected requests arrived](#verify-no-unexpected-requests-arrived)
  * [Recommended AfterEach setup](#recommended-aftereach-setup)
  * [Inspecting unmatched items](#inspecting-unmatched-items)
* [Request Journal](#request-journal)

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


<!--- CLEAR -->
<!--- INCLUDE 
import dev.mokksy.mokksy.Mokksy
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
-->

2. Create and start Mokksy server:

   **JVM (blocking):**
   ```kotlin
   val mokksy = Mokksy().apply {
       runBlocking { 
           startSuspend() 
       }
   }
   ```
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
<!--- SUFFIX
}
-->
<!--- KNIT example-readme-01.kt -->

## Request Specification Matchers

Mokksy provides various matcher types to specify conditions for matching incoming HTTP requests:

- **Path matchers** — `path("/things")` or `path = beEqual("/things")`
- **Header matchers** — `containsHeader("X-Request-ID", "abc")` checks for a header with an exact value
- **Content matchers** — `bodyContains("value")` checks if the raw body string contains a substring;
  `bodyString += contain("value")` adds a Kotest matcher directly
- **Predicate matchers** — `bodyMatchesPredicate { it?.name == "foo" }` matches against the typed,
  deserialized request body
- **Call matchers** — `successCallMatcher` matches if a function called with the body does not throw

## Verifying Requests

Mokksy provides two complementary verification methods that check opposite sides of the stub/request contract.

### Verify all stubs were triggered

`verifyNoUnmatchedStubs()` fails if any registered stub was never matched by an incoming request.
Use this to catch stubs you set up but that were never actually called — a sign the code under test took
a different path than expected.

```kotlin
// Fails if any stub has matchCount == 0
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

Run both checks after every test to catch a mismatch in either direction:

<!--- CLEAR -->
<!--- INCLUDE
import dev.mokksy.mokksy.Mokksy
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

    val mokksy = Mokksy()
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
            path = beEqual("/hi")
        } respondsWith {
            body = "Hello"
            delay(100.milliseconds)
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

| Mode                           | Behaviour                                                                                                  |
|--------------------------------|------------------------------------------------------------------------------------------------------------|
| `JournalMode.LEAN` *(default)* | Records only requests with no matching stub. Lower overhead; sufficient for `verifyNoUnexpectedRequests()`. |
| `JournalMode.FULL`             | Records all incoming requests — both matched and unmatched.                                                |

```kotlin
val mokksy = Mokksy(
    configuration = ServerConfiguration(
        journalMode = JournalMode.FULL,
    ),
)
```

Call `resetMatchCounts()` between scenarios to clear both stub match counts and the journal:

```kotlin
@AfterTest
fun afterEach() {
    mokksy.resetMatchCounts()
}
```

[sse]: https://html.spec.whatwg.org/multipage/server-sent-events.html "Server-Side Events Specification (HTML Living Standard)"
[ai-mocks]: https://github.com/mokksy/ai-mocks/ "AI-Mock: Mokksy extensions for AI"
[a2a]: https://a2a-protocol.org/ "Agent2Agent (A2A) Protocol, an open standard designed to enable seamless communication and collaboration between AI agents."
