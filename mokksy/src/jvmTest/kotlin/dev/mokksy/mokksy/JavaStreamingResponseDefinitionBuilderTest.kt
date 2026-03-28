@file:OptIn(InternalMokksyApi::class)

package dev.mokksy.mokksy

import dev.mokksy.mokksy.request.CapturedRequest
import dev.mokksy.mokksy.response.StreamingResponseDefinitionBuilder
import dev.mokksy.mokksy.utils.logger.HttpFormatter
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.flow.toList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.Stream
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds

class JavaStreamingResponseDefinitionBuilderTest {
    private val formatter = HttpFormatter()

    private fun withBuilder(
        block: suspend (
            StreamingResponseDefinitionBuilder<String, String>,
            JavaStreamingResponseDefinitionBuilder<String, String>,
        ) -> Unit,
    ) {
        testApplication {
            routing {
                get("/test") {
                    val delegate =
                        StreamingResponseDefinitionBuilder<String, String>(
                            request = CapturedRequest(call.request, String::class),
                            formatter = formatter,
                        )
                    val sut = JavaStreamingResponseDefinitionBuilder(delegate)
                    block(delegate, sut)
                    call.respondText("ok")
                }
            }
            client.get("/test")
        }
    }

    // region chunks(List)

    @Test
    fun `chunks(List) sets delegate chunks`() =
        withBuilder { delegate, sut ->
            sut.chunks(listOf("a", "b"))
            delegate.chunks shouldBe mutableListOf("a", "b")
        }

    @Test
    fun `chunks(List) replaces chunks previously added via chunk()`() =
        withBuilder { delegate, sut ->
            sut.chunk("old")
            sut.chunks(listOf("new"))
            delegate.chunks shouldBe mutableListOf("new")
        }

    // endregion

    // region chunks(Stream)

    @Test
    fun `chunks(Stream) is not consumed at stub registration time`() =
        withBuilder { _, sut ->
            val consumed = AtomicBoolean(false)
            sut.chunks(Stream.of("x").peek { consumed.set(true) })
            consumed.get() shouldBe false
        }

    @Test
    fun `chunks(Stream) assigns a flow to the delegate`() =
        withBuilder { delegate, sut ->
            sut.chunks(Stream.of("x"))
            delegate.flow shouldNotBe null
        }

    @Test
    fun `chunks Stream flow emits all stream elements in order`() =
        withBuilder { delegate, sut ->
            sut.chunks(Stream.of("x", "y", "z"))
            delegate.flow!!.toList() shouldBe listOf("x", "y", "z")
        }

    // endregion

    // region chunk

    @Test
    fun `chunk() appends to delegate chunks`() =
        withBuilder { delegate, sut ->
            sut.chunk("a").chunk("b")
            delegate.chunks shouldBe mutableListOf("a", "b")
        }

    // endregion

    // region delay

    @Test
    fun `delayBetweenChunksMillis sets inter-chunk delay on delegate`() =
        withBuilder { delegate, sut ->
            sut.delayBetweenChunksMillis(150L)
            delegate.delayBetweenChunks shouldBe 150.milliseconds
        }

    @Test
    fun `delayMillis sets initial response delay on delegate`() =
        withBuilder { delegate, sut ->
            sut.delayMillis(200L)
            delegate.delay shouldBe 200.milliseconds
        }

    // endregion

    // region status / header / contentType

    @Test
    fun `status sets HTTP status code on delegate`() =
        withBuilder { delegate, sut ->
            sut.status(201)
            delegate.httpStatus shouldBe HttpStatusCode.Created
        }

    @Test
    fun `contentType(String) parses and sets content type on delegate`() =
        withBuilder { delegate, sut ->
            sut.contentType("application/x-ndjson")
            delegate.contentType shouldBe ContentType.parse("application/x-ndjson")
        }

    @Test
    fun `contentType(ContentType) sets content type directly on delegate`() =
        withBuilder { delegate, sut ->
            sut.contentType(ContentType.Application.Json)
            delegate.contentType shouldBe ContentType.Application.Json
        }

    // endregion

    // region fluent API

    @Test
    fun `all mutating methods return the same builder instance`() =
        withBuilder { _, sut ->
            val result =
                sut
                    .chunks(listOf("a"))
                    .chunk("b")
                    .chunks(Stream.of("c"))
                    .delayBetweenChunksMillis(50L)
                    .delayMillis(100L)
                    .status(200)
                    .header("X-Custom", "value")
                    .contentType("text/plain")
            result shouldBe sut
        }

    // endregion
}
