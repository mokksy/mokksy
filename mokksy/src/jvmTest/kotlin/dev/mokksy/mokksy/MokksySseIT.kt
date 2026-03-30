package dev.mokksy.mokksy

import io.kotest.matchers.equals.beEqual
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.withCharset
import io.ktor.sse.ServerSentEvent
import io.ktor.sse.TypedServerSentEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import org.junit.jupiter.api.Test
import kotlin.test.AfterTest
import kotlin.text.Charsets.UTF_8
import kotlin.time.Duration.Companion.milliseconds

internal class MokksySseIT : AbstractIT({ createKtorSSEClient(it) }) {
    @Test
    suspend fun `Should respond to SSE (flow)`() {
        mokksy.get(name = "sse-get-flow", requestType = Any::class) {
            path("/sse-flow")
        } respondsWithSseStream {
            flow =
                flow {
                    delay(100.milliseconds)
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
        verifySseStream("/sse-flow")
    }

    @Test
    suspend fun `Should respond to SSE (chunks)`() {
        mokksy.get(name = "sse-get-chunks", requestType = Any::class) {
            path = beEqual("/sse-chunks")
        } respondsWithSseStream {
            chunks += ServerSentEvent(data = "One")
            chunks += ServerSentEvent(data = "Two")
        }

        // when
        verifySseStream("/sse-chunks")
    }

    private suspend fun verifySseStream(uri: String) {
        val result = client.get(uri)

        // then
        result.status shouldBe HttpStatusCode.OK
        result.contentType() shouldBe ContentType.Text.EventStream.withCharset(UTF_8)
        result.bodyAsText() shouldBe "data: One\r\n\r\ndata: Two\r\n\r\n"
    }

    // region SSE spec compliance (https://html.spec.whatwg.org/multipage/server-sent-events.html)

    @Test
    suspend fun `SSE spec - simple sequential messages separated by blank lines`() {
        mokksy.get(name = "sse-spec-simple", requestType = Any::class) {
            path("/sse-spec-simple")
        } respondsWithSseStream {
            chunks += ServerSentEvent(data = "This is the first message.")
            chunks += ServerSentEvent(data = "This is the second message, it\nhas two lines.")
            chunks += ServerSentEvent(data = "This is the third message.")
        }

        val result = client.get("/sse-spec-simple")

        result.status shouldBe HttpStatusCode.OK
        result.bodyAsText() shouldBe
            "data: This is the first message.\r\n" +
            "\r\n" +
            "data: This is the second message, it\r\n" +
            "data: has two lines.\r\n" +
            "\r\n" +
            "data: This is the third message.\r\n" +
            "\r\n"
    }

    @Test
    suspend fun `SSE spec - different event types`() {
        mokksy.get(name = "sse-spec-events", requestType = Any::class) {
            path("/sse-spec-events")
        } respondsWithSseStream {
            chunks += ServerSentEvent(data = "73857293", event = "add")
            chunks += ServerSentEvent(data = "2153", event = "remove")
            chunks += ServerSentEvent(data = "113411", event = "add")
        }

        val result = client.get("/sse-spec-events")

        result.status shouldBe HttpStatusCode.OK
        result.bodyAsText() shouldBe
            "event: add\r\ndata: 73857293\r\n" +
            "\r\n" +
            "event: remove\r\ndata: 2153\r\n" +
            "\r\n" +
            "event: add\r\ndata: 113411\r\n" +
            "\r\n"
    }

    // endregion

    @AfterTest
    @Suppress("DEPRECATION")
    fun afterEach() {
        mokksy.checkForUnmatchedRequests()
        mokksy.checkForUnmatchedStubs()
    }
}

suspend fun main() {
    val mokksy =
        MokksyServer(verbose = true, port = 8080) {
            println("Running server with $engine engine")
        }
    mokksy.get {
        path = beEqual("/sse")
    } respondsWithSseStream {
        this.httpStatus = HttpStatusCode.OK
        this.flow =
            flow {
                repeat(10) {
                    emit(
                        TypedServerSentEvent(
                            data = "Event $it",
                        ),
                    )
                }
            }
    }

    delay(1000_000L)
}
