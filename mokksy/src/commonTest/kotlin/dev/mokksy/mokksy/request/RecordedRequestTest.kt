package dev.mokksy.mokksy.request

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.ktor.http.HttpMethod
import kotlin.test.Test

internal class RecordedRequestTest {
    // region toString

    @Test
    fun `toString with null body shows only method and URI`() {
        val request = RecordedRequest(HttpMethod.Get, "/test", emptyMap(), false)
        request.toString() shouldBe "GET /test"
    }

    @Test
    fun `toString with bodyAsText includes body preview`() {
        val request = RecordedRequest(
            HttpMethod.Post, "/api", emptyMap(), false,
            bodyAsText = """{"name":"test"}""",
        )
        request.toString() shouldContain "POST /api"
        request.toString() shouldContain """Body: {"name":"test"}"""
    }

    @Test
    fun `toString with typed body but no bodyAsText uses body toString`() {
        val typedBody = mapOf("key" to "value")
        val request = RecordedRequest(
            HttpMethod.Post, "/api", emptyMap(), true,
            body = typedBody,
        )
        request.toString() shouldContain "Body: {key=value}"
    }

    @Test
    fun `toString prefers bodyAsText over typed body toString`() {
        val request = RecordedRequest(
            HttpMethod.Post, "/api", emptyMap(), true,
            body = mapOf("key" to "value"),
            bodyAsText = """{"key":"value"}""",
        )
        request.toString() shouldContain """Body: {"key":"value"}"""
        request.toString() shouldNotContain "{key=value}"
    }

    @Test
    fun `toString with blank bodyAsText and null body omits body`() {
        val request = RecordedRequest(
            HttpMethod.Post, "/api", emptyMap(), false,
            bodyAsText = "   ",
        )
        request.toString() shouldNotContain "Body:"
    }

    // endregion

    // region equals and hashCode

    @Test
    fun `equals considers bodyAsText`() {
        val base = RecordedRequest(HttpMethod.Post, "/api", emptyMap(), false)
        val withBody = RecordedRequest(
            HttpMethod.Post, "/api", emptyMap(), false,
            bodyAsText = """{"name":"test"}""",
        )
        base shouldNotBe withBody
    }

    @Test
    fun `equals ignores typed body`() {
        val a = RecordedRequest(
            HttpMethod.Post, "/api", emptyMap(), false,
            body = "typed-a",
        )
        val b = RecordedRequest(
            HttpMethod.Post, "/api", emptyMap(), false,
            body = "typed-b",
        )
        a shouldBe b
    }

    @Test
    fun `hashCode considers bodyAsText`() {
        val base = RecordedRequest(HttpMethod.Post, "/api", emptyMap(), false)
        val withBody = RecordedRequest(
            HttpMethod.Post, "/api", emptyMap(), false,
            bodyAsText = """{"name":"test"}""",
        )
        base.hashCode() shouldNotBe withBody.hashCode()
    }

    // endregion

    // region defaults

    @Test
    fun `body defaults to null`() {
        val request = RecordedRequest(HttpMethod.Get, "/test", emptyMap(), true)
        request.body shouldBe null
        request.bodyAsText shouldBe null
    }

    // endregion
}
