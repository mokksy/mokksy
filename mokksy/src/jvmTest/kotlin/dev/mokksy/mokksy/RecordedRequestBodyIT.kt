package dev.mokksy.mokksy

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

internal class RecordedRequestBodyIT : AbstractIT() {
    @Test
    suspend fun `unmatched POST captures bodyAsText`() {
        val path = "/unmatched-body-$seed"
        val json = """{"name":"test-$seed"}"""

        client.post(path) {
            contentType(ContentType.Application.Json)
            setBody(json)
        }

        val unexpected = mokksy.findAllUnexpectedRequests().filter { it.uri == path }
        unexpected shouldHaveSize 1
        unexpected[0].bodyAsText shouldBe json
    }

    @Test
    suspend fun `unmatched POST with typed stub captures bodyAsText`() {
        val path = "/typed-body-$seed"
        val input = Input("typed-$seed")

        // Register a stub that won't match (wrong path)
        mokksy.post(requestType = Input::class) {
            path("/other-path-$seed")
            bodyMatchesPredicate { it?.name == input.name }
        } respondsWith {
            body = "ok"
        }

        client.post(path) {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(input))
        }

        val unexpected = mokksy.findAllUnexpectedRequests().filter { it.uri == path }
        unexpected shouldHaveSize 1
        unexpected[0].bodyAsText shouldBe Json.encodeToString(input)
    }

    @Test
    suspend fun `GET with no body has null bodyAsText`() {
        val path = "/get-no-body-$seed"

        client.get(path)

        val unexpected = mokksy.findAllUnexpectedRequests().filter { it.uri == path }
        unexpected shouldHaveSize 1
        unexpected[0].bodyAsText.shouldBeNull()
    }

    @Test
    suspend fun `large body is captured in full`() {
        val path = "/large-body-$seed"
        val largeBody = "x".repeat(100_000)

        client.post(path) {
            contentType(ContentType.Text.Plain)
            setBody(largeBody)
        }

        val unexpected = mokksy.findAllUnexpectedRequests().filter { it.uri == path }
        unexpected shouldHaveSize 1
        unexpected[0].bodyAsText shouldBe largeBody
    }

    @Test
    suspend fun `verifyNoUnexpectedRequests error message includes body`() {
        val path = "/verify-body-$seed"
        val json = """{"key":"value-$seed"}"""

        client.post(path) {
            contentType(ContentType.Application.Json)
            setBody(json)
        }

        val error = assertFailsWith<AssertionError> {
            mokksy.verifyNoUnexpectedRequests()
        }
        error.message shouldContain json
    }

    @Test
    suspend fun `unmatched POST with body string matcher captures bodyAsText from attribute`() {
        val path = "/bodystring-attr-$seed"
        val bodyContent = """{"field":"value-$seed"}"""

        // Register a stub with bodyContains that won't match
        mokksy.post {
            path(path)
            bodyContains("nonexistent-$seed")
        } respondsWith {
            body = "ok"
        }

        client.post(path) {
            contentType(ContentType.Application.Json)
            setBody(bodyContent)
        }

        val unexpected = mokksy.findAllUnexpectedRequests().filter { it.uri == path }
        unexpected shouldHaveSize 1
        unexpected[0].bodyAsText shouldBe bodyContent
    }

    @Test
    suspend fun `matched POST is not recorded in LEAN mode`() {
        val path = "/matched-body-$seed"
        val json = """{"name":"matched-$seed"}"""

        mokksy.post {
            path(path)
        } respondsWith {
            body = "ok"
        }

        val result = client.post(path) {
            contentType(ContentType.Application.Json)
            setBody(json)
        }

        result.status shouldBe HttpStatusCode.OK
        // In default LEAN mode, matched requests are not recorded as unexpected
        mokksy.findAllUnexpectedRequests().filter { it.uri == path } shouldHaveSize 0
    }

    @Test
    suspend fun `matched POST body is recorded in FULL mode`() {
        val fullModeServer = MokksyServer(
            configuration = ServerConfiguration(journalMode = JournalMode.FULL, verbose = true),
        )
        fullModeServer.startSuspend()
        val fullClient = createKtorClient(fullModeServer.port())
        try {
            val path = "/full-matched-$seed"
            val json = """{"name":"full-$seed"}"""

            fullModeServer.post {
                path(path)
            } respondsWith {
                body = "ok"
            }

            val result = fullClient.post(path) {
                contentType(ContentType.Application.Json)
                setBody(json)
            }

            result.status shouldBe HttpStatusCode.OK
            val matched = fullModeServer.findAllMatchedRequests().filter { it.uri == path }
            matched shouldHaveSize 1
            matched[0].bodyAsText shouldBe json
        } finally {
            fullClient.close()
            fullModeServer.shutdownSuspend()
        }
    }
}
