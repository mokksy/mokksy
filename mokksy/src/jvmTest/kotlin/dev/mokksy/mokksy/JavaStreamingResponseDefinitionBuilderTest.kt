package dev.mokksy.mokksy

import dev.mokksy.mokksy.response.StreamingResponseDefinitionBuilder
import dev.mokksy.mokksy.utils.logger.HttpFormatter
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.ResponseHeaders
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.Stream
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds

class JavaStreamingResponseDefinitionBuilderTest {
    private val delegate =
        StreamingResponseDefinitionBuilder<String, String>(
            request = mockk(),
            formatter = HttpFormatter(),
        )
    private val sut = JavaStreamingResponseDefinitionBuilder(delegate)

    private fun collectHeaders(headersBlock: (ResponseHeaders.() -> Unit)?): List<String> {
        val result = mutableListOf<String>()
        headersBlock?.invoke(
            object : ResponseHeaders() {
                override fun engineAppendHeader(
                    name: String,
                    value: String,
                ) {
                    result.add("$name=$value")
                }

                override fun getEngineHeaderNames(): List<String> = emptyList()

                override fun getEngineHeaderValues(name: String): List<String> = emptyList()
            },
        )
        return result
    }

    // region chunks(List)

    @Test
    fun `chunks(List) sets delegate chunks`() {
        sut.chunks(listOf("a", "b"))

        delegate.chunks shouldBe mutableListOf("a", "b")
    }

    @Test
    fun `chunks(List) replaces chunks previously added via chunk()`() {
        sut.chunk("old")
        sut.chunks(listOf("new"))

        delegate.chunks shouldBe mutableListOf("new")
    }

    // endregion

    // region chunks(Stream)

    @Test
    fun `chunks(Stream) is not consumed at stub registration time`() {
        val consumed = AtomicBoolean(false)
        sut.chunks(Stream.of("x").peek { consumed.set(true) })

        consumed.get() shouldBe false
    }

    @Test
    fun `chunks(Stream) assigns a flow to the delegate`() {
        sut.chunks(Stream.of("x"))

        delegate.flow shouldNotBe null
    }

    @Test
    fun `chunks Stream flow emits all stream elements in order`(): Unit =
        runBlocking {
            sut.chunks(Stream.of("x", "y", "z"))

            delegate.flow!!.toList() shouldBe listOf("x", "y", "z")
        }

    // endregion

    // region chunk

    @Test
    fun `chunk() appends to delegate chunks`() {
        sut.chunk("a").chunk("b")

        delegate.chunks shouldBe mutableListOf("a", "b")
    }

    // endregion

    // region delay

    @Test
    fun `delayBetweenChunksMillis sets inter-chunk delay on delegate`() {
        sut.delayBetweenChunksMillis(150L)

        delegate.delayBetweenChunks shouldBe 150.milliseconds
    }

    @Test
    fun `delayMillis sets initial response delay on delegate`() {
        sut.delayMillis(200L)

        delegate.delay shouldBe 200.milliseconds
    }

    // endregion

    // region status / header / contentType

    @Test
    fun `status sets HTTP status code on delegate`() {
        sut.status(201)

        delegate.httpStatus shouldBe HttpStatusCode.Created
    }

    @Test
    fun `contentType(String) parses and sets content type on delegate`() {
        sut.contentType("application/x-ndjson")

        delegate.contentType shouldBe ContentType.parse("application/x-ndjson")
    }

    @Test
    fun `contentType(ContentType) sets content type directly on delegate`() {
        sut.contentType(ContentType.Application.Json)

        delegate.contentType shouldBe ContentType.Application.Json
    }

    @Test
    fun `header(name, value) sets header on delegate`() {
        sut.header("X-Test-Header", "test-value")

        val definition = delegate.build()
        val headers = collectHeaders(definition.headers)
        headers shouldBe listOf("X-Test-Header=test-value")
    }

    @Test
    fun `header() accumulates multiple headers`() {
        sut.header("X-First", "one").header("X-Second", "two")

        val headers = collectHeaders(delegate.build().headers)
        assertSoftly(headers) {
            shouldContain("X-First=one")
            shouldContain("X-Second=two")
        }
    }

    // endregion

    // region fluent API

    @Test
    fun `all mutating methods return the same builder instance`() {
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
