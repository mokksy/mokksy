package dev.mokksy.it

import dev.mokksy.mokksy.Mokksy
import dev.mokksy.mokksy.StubConfiguration
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.header
import io.ktor.client.request.options
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.measureTimedValue

/**
 * This test intentionally creates and starts Mokksy once and never shuts it down after each test.
 * This verifies a scenario when Mokksy is started once and used across multiple tests.
 */
class MokksyIT {
    val mokksy =
        Mokksy().apply {
            runTest {
                startSuspend()
                awaitStarted()
            }
        }
    val client = HttpClient()

    // region GET

    @Test
    fun `GET returns 200 with body`() =
        runTest {
            mokksy.get { path("/hello") } respondsWith { body = "Hello, World!" }

            val response = client.get(mokksy.baseUrl() + "/hello")

            assertSoftly(response) {
                status shouldBe HttpStatusCode.OK
                bodyAsText() shouldBe "Hello, World!"
            }
        }

    @Test
    fun `GET returns 404 when no stub matches`() =
        runTest {
            val response = client.get(mokksy.baseUrl() + "/no-stub")

            response.status shouldBe HttpStatusCode.NotFound
        }

    @Test
    fun `GET matches by header value`() =
        runTest {
            mokksy.get {
                path("/secured")
                containsHeader("X-Api-Key", "secret")
            } respondsWith { body = "authorized" }

            val authorized =
                client.get(mokksy.baseUrl() + "/secured") {
                    header("X-Api-Key", "secret")
                }
            val unauthorized = client.get(mokksy.baseUrl() + "/secured")

            authorized.status shouldBe HttpStatusCode.OK
            unauthorized.status shouldBe HttpStatusCode.NotFound
        }

    // endregion

    // region PUT / DELETE / PATCH / HEAD / OPTIONS

    @Test
    fun `PUT returns 200 with body`() =
        runTest {
            mokksy.put { path("/put-test") } respondsWith { body = "put-ok" }

            val response = client.put(mokksy.baseUrl() + "/put-test")

            assertSoftly(response) {
                status shouldBe HttpStatusCode.OK
                bodyAsText() shouldBe "put-ok"
            }
        }

    @Test
    fun `DELETE returns 200 with body`() =
        runTest {
            mokksy.delete { path("/delete-test") } respondsWith { body = "deleted" }

            val response = client.delete(mokksy.baseUrl() + "/delete-test")

            assertSoftly(response) {
                status shouldBe HttpStatusCode.OK
                bodyAsText() shouldBe "deleted"
            }
        }

    @Test
    fun `PATCH returns 200 with body`() =
        runTest {
            mokksy.patch { path("/patch-test") } respondsWith { body = "patched" }

            val response = client.patch(mokksy.baseUrl() + "/patch-test")

            assertSoftly(response) {
                status shouldBe HttpStatusCode.OK
                bodyAsText() shouldBe "patched"
            }
        }

    @Test
    fun `HEAD returns 200 with empty body`() =
        runTest {
            mokksy.head { path("/head-test") } respondsWith { body = "ignored-by-protocol" }

            val response = client.head(mokksy.baseUrl() + "/head-test")

            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText() shouldBe ""
        }

    @Test
    fun `OPTIONS returns 200 with body`() =
        runTest {
            mokksy.options { path("/options-test") } respondsWith { body = "OK" }

            val response = client.options(mokksy.baseUrl() + "/options-test")

            assertSoftly(response) {
                status shouldBe HttpStatusCode.OK
                bodyAsText() shouldBe "OK"
            }
        }

    // endregion

    // region POST

    @Test
    fun `POST returns 201 with Location header`() =
        runTest {
            mokksy.post { path("/items") } respondsWith {
                body = """{"id":"42"}"""
                httpStatus = HttpStatusCode.Created
                addHeader("Location", "/items/42")
            }

            val response =
                client.post(mokksy.baseUrl() + "/items") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"name":"widget"}""")
                }

            assertSoftly(response) {
                status shouldBe HttpStatusCode.Created
                bodyAsText() shouldBe """{"id":"42"}"""
                headers["Location"] shouldBe "/items/42"
            }
        }

    @Test
    fun `POST matches by bodyContains and returns 404 when body does not match`() =
        runTest {
            mokksy.post {
                path("/body-contains")
                bodyContains("expected-token")
            } respondsWith { body = "matched" }

            val matched =
                client.post(mokksy.baseUrl() + "/body-contains") {
                    setBody("""{"token": "expected-token"}""")
                }
            val notMatched =
                client.post(mokksy.baseUrl() + "/body-contains") {
                    setBody("""{"token": "other"}""")
                }

            matched.status shouldBe HttpStatusCode.OK
            notMatched.status shouldBe HttpStatusCode.NotFound
        }

    @Test
    fun `POST matches by body predicate`() =
        runTest {
            mokksy.post {
                path("/body-predicate")
                bodyMatchesPredicate { it?.contains("match-me") == true }
            } respondsWith { body = "predicate-matched" }

            val matched =
                client.post(mokksy.baseUrl() + "/body-predicate") {
                    setBody("""{"value": "match-me"}""")
                }
            val notMatched =
                client.post(mokksy.baseUrl() + "/body-predicate") {
                    setBody("""{"value": "no"}""")
                }

            matched.status shouldBe HttpStatusCode.OK
            notMatched.status shouldBe HttpStatusCode.NotFound
        }

    // endregion

    // region respondsWithStatus

    @Test
    fun `respondsWithStatus returns status code and empty body`() =
        runTest {
            mokksy.get { path("/status-only") } respondsWithStatus HttpStatusCode.NoContent

            val response = client.get(mokksy.baseUrl() + "/status-only")

            response.status shouldBe HttpStatusCode.NoContent
            response.bodyAsText() shouldBe ""
        }

    // endregion

    // region StubConfiguration

    @Test
    fun `removeAfterMatch returns 404 on second request`() =
        runTest {
            mokksy.get(StubConfiguration("once-only", eventuallyRemove = true)) {
                path("/once")
            } respondsWith { body = "First!" }

            val first = client.get(mokksy.baseUrl() + "/once")
            val second = client.get(mokksy.baseUrl() + "/once")

            first.status shouldBe HttpStatusCode.OK
            second.status shouldBe HttpStatusCode.NotFound
        }

    // endregion

    // region Stub priority

    @Test
    fun `higher priority value should win`() =
        runTest {
            mokksy.get {
                path("/priority")
                priority(1)
            } respondsWith { body = "low-priority" }

            mokksy.get {
                path("/priority")
                priority(10)
            } respondsWith { body = "high-priority" }

            val response = client.get(mokksy.baseUrl() + "/priority")

            assertSoftly(response) {
                status shouldBe HttpStatusCode.OK
                bodyAsText() shouldBe "high-priority"
            }
        }

    @Test
    fun `default priority vs positive - positive should win`() =
        runTest {
            mokksy.get { path("/priority-default-vs-positive") } respondsWith {
                body = "default-priority"
            }

            mokksy.get {
                path("/priority-default-vs-positive")
                priority(1)
            } respondsWith { body = "positive-priority" }

            val response = client.get(mokksy.baseUrl() + "/priority-default-vs-positive")

            response.bodyAsText() shouldBe "positive-priority"
        }

    @Test
    fun `default priority vs negative - default should win`() =
        runTest {
            mokksy.get {
                path("/priority-default-vs-negative")
                priority(-1)
            } respondsWith { body = "negative-priority" }

            mokksy.get { path("/priority-default-vs-negative") } respondsWith {
                body = "default-priority"
            }

            val response = client.get(mokksy.baseUrl() + "/priority-default-vs-negative")

            response.bodyAsText() shouldBe "default-priority"
        }

    // endregion

    // region Catch-all fallback pattern

    @Test
    fun `catch-all fallback pattern with priority`() =
        runTest {
            // Catch-all stub: matches any POST to /v1/chat/completions, returns 400
            mokksy.post {
                path("/v1/chat/completions")
                priority(-1)
            } respondsWith {
                body = """{"error":"unsupported request"}"""
                httpStatus = HttpStatusCode.BadRequest
            }

            // Specific stub: matches only when body contains "gpt-4", returns 200
            mokksy.post {
                path("/v1/chat/completions")
                bodyContains("gpt-4")
                priority(1)
            } respondsWith {
                body = """{"model":"gpt-4"}"""
            }

            // Specific request -> specific stub wins (higher priority + higher score)
            val specificResponse =
                client.post(mokksy.baseUrl() + "/v1/chat/completions") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"model":"gpt-4"}""")
                }
            assertSoftly(specificResponse) {
                status shouldBe HttpStatusCode.OK
                bodyAsText() shouldBe """{"model":"gpt-4"}"""
            }

            // Unmatched request -> catch-all fallback kicks in
            val fallbackResponse =
                client.post(mokksy.baseUrl() + "/v1/chat/completions") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"model":"other"}""")
                }
            assertSoftly(fallbackResponse) {
                status shouldBe HttpStatusCode.BadRequest
                bodyAsText() shouldBe """{"error":"unsupported request"}"""
            }
        }

    // endregion

    // region delay

    @Test
    fun `GET with delay should delay response`() =
        runTest {
            mokksy.get { path("/delayed") } respondsWith {
                body = "ok"
                delay = 200.milliseconds
            }

            val (response, elapsed) =
                measureTimedValue {
                    client.get(mokksy.baseUrl() + "/delayed")
                }

            response.status shouldBe HttpStatusCode.OK
            elapsed shouldBeGreaterThanOrEqualTo 200.milliseconds
        }

    // endregion

    // region Verification

    @Test
    fun `findAllUnmatchedStubs returns unmatched stub`() =
        runTest {
            mokksy.get { path("/unmatched-stub") } respondsWith { body = "never-called" }

            mokksy.findAllUnmatchedStubs() shouldHaveSize 1

            client.get(mokksy.baseUrl() + "/unmatched-stub")

            mokksy.findAllUnmatchedStubs().shouldBeEmpty()
        }

    @Test
    fun `findAllUnexpectedRequests returns unexpected request`() =
        runTest {
            mokksy.findAllUnexpectedRequests().shouldBeEmpty()

            client.get(mokksy.baseUrl() + "/no-stub-here")

            mokksy.findAllUnexpectedRequests() shouldHaveSize 1
        }

    @Test
    fun `verifyNoUnmatchedStubs throws when stub never called`() =
        runTest {
            mokksy.get { path("/never-called") } respondsWith { body = "unreachable" }

            shouldThrow<AssertionError> {
                mokksy.verifyNoUnmatchedStubs()
            }
        }

    @Test
    fun `verifyNoUnexpectedRequests throws when request had no stub`() =
        runTest {
            client.get(mokksy.baseUrl() + "/unexpected")

            shouldThrow<AssertionError> {
                mokksy.verifyNoUnexpectedRequests()
            }
        }

    @Test
    fun `resetMatchState makes matched stubs unmatched again`() =
        runTest {
            mokksy.get { path("/reset-test") } respondsWith { body = "ok" }

            client.get(mokksy.baseUrl() + "/reset-test")

            mokksy.verifyNoUnmatchedStubs()
            mokksy.verifyNoUnexpectedRequests()

            mokksy.resetMatchState()

            shouldThrow<AssertionError> {
                mokksy.verifyNoUnmatchedStubs()
            }
        }

    // endregion
}
