# Module mokksy

Mokksy is an embedded mock HTTP server for testing, built on [Ktor](https://ktor.io/).
It provides a fluent Kotlin DSL for registering request stubs, matching incoming requests,
and returning plain, streaming, or SSE responses.

## Getting started

```kotlin
val mokksy = Mokksy().apply { start() }

mokksy.get {
    path("/ping")
} respondsWith {
    body = """{"response":"Pong"}"""
}

// After the test:
mokksy.verifyNoUnmatchedStubs()
mokksy.shutdown()
```

## Key concepts

| Concept | Class | Description |
|---|---|---|
| Server | [MokksyServer] / `Mokksy` | Embedded HTTP server. Starts on a random port by default. |
| Server config | [ServerConfiguration] | Verbose logging, journal mode, content negotiation. |
| Stub | `BuildingStep` → `respondsWith` | Pair of a request matcher and a response definition. |
| Stub config | [StubConfiguration] | Per-stub name, `removeAfterMatch`, verbose flag. |
| Request matching | `RequestSpecificationBuilder` | Path, method, headers, query params, body predicates. |
| Response | `ResponseDefinitionBuilder` | Status, headers, body, delay. |
| Streaming | `StreamingResponseDefinitionBuilder` | Chunked body or SSE stream with per-chunk delay. |
| Request journal | [JournalMode] | `NONE` (disabled), `LEAN` (unmatched only) or `FULL` (all requests). |

## Package contents

| Package | Contents |
|---|---|
| `dev.mokksy.mokksy` | [MokksyServer], [BuildingStep], [StubConfiguration], [ServerConfiguration] |
| `dev.mokksy.mokksy.request` | Request matching, `CapturedRequest`, `RequestJournal` |
| `dev.mokksy.mokksy.response` | Response and streaming response builders |
| `dev.mokksy.mokksy.kotest` | Kotest matchers for stub and request assertions |

# Package dev.mokksy.mokksy

Core server, stub registration, and configuration.

# Package dev.mokksy.mokksy.request

Request matching and recording. Use `RequestSpecificationBuilder` inside `get { }` / `post { }` blocks
to match by path, headers, query parameters, and body.

# Package dev.mokksy.mokksy.response

Response builders for plain (`respondsWith`), chunked stream (`respondsWithStream`),
and SSE stream (`respondsWithSseStream`) responses.

# Package dev.mokksy.mokksy.kotest

Kotest assertion extensions for verifying stubs and recorded requests.
