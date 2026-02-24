# Mokksy

[![Maven Central](https://img.shields.io/maven-central/v/dev.mokksy/mokksy)](https://repo1.maven.org/maven2/dev/mokksy/mokksy/)

_Mokksy_ - Mock HTTP Server, built with [Kotlin](https://kotlinlang.org/) and [Ktor](https://ktor.io/).

![mokksy-mascot-256.png](docs/mokksy-mascot-256.png)

**Table of Contents**

<!--- TOC -->

  * [Why Mokksy?](#why-mokksy?)
* [Key Features](#key-features)
* [Quick start](#quick-start)
* [Responding with predefined responses](#responding-with-predefined-responses)
  * [GET Request](#get-request)
  * [POST Request](#post-request)
* [Server-Side Events (SSE) Response](#server-side-events-sse-response)

<!--- END -->

### Why Mokksy?

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
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Test
import kotlin.random.Random
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.withCharsetIfNeeded
import io.ktor.sse.ServerSentEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration.Companion.milliseconds

class ReadmeTest {
-->

1. Create Mokksy server:

```kotlin
val mokksy = Mokksy()
```

2. Create an http client using MokksyServer's as baseUrl:

```kotlin
val client = HttpClient {
  install(DefaultRequest) {
    url(mokksy.baseUrl())
  }
}
```

## Responding with predefined responses

### GET Request

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

When the request does not match - MokksyServer returns `404 (Not Found)`:

```kotlin
val notFoundResult = client.get("/ping") {
  headers.append("Foo", "baz")
}

notFoundResult.status shouldBe HttpStatusCode.NotFound
```

<!--- INCLUDE
  }
-->

### POST Request

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

## Server-Side Events (SSE) Response

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

[sse]: https://html.spec.whatwg.org/multipage/server-sent-events.html "Server-Side Events Specification (HTML Living Standard)"

