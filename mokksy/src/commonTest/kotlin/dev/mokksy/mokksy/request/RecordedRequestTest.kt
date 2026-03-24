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
        val request =
            RecordedRequest(
                HttpMethod.Post,
                "/api",
                emptyMap(),
                false,
                bodyAsText = """{"name":"test"}""",
            )
        request.toString() shouldContain "POST /api"
        request.toString() shouldContain """Body: {"name":"test"}"""
    }

    @Test
    fun `toString with blank bodyAsText and null body omits body`() {
        val request =
            RecordedRequest(
                HttpMethod.Post,
                "/api",
                emptyMap(),
                false,
                bodyAsText = "   ",
            )
        request.toString() shouldNotContain "Body:"
    }

    // endregion

    // region equals and hashCode

    @Test
    fun `equals considers bodyAsText`() {
        val base = RecordedRequest(HttpMethod.Post, "/api", emptyMap(), false)
        val withBody =
            RecordedRequest(
                HttpMethod.Post,
                "/api",
                emptyMap(),
                false,
                bodyAsText = """{"name":"test"}""",
            )
        base shouldNotBe withBody
    }

    @Test
    fun `hashCode considers bodyAsText`() {
        val base = RecordedRequest(HttpMethod.Post, "/api", emptyMap(), false)
        val withBody =
            RecordedRequest(
                HttpMethod.Post,
                "/api",
                emptyMap(),
                false,
                bodyAsText = """{"name":"test"}""",
            )
        // Different objects should have different hashCodes (not guaranteed but expected)
        // Also verify consistent hashing
        val copy = RecordedRequest(HttpMethod.Post, "/api", emptyMap(), false)
        base.hashCode() shouldBe copy.hashCode()
        base.hashCode() shouldNotBe withBody.hashCode()
    }

    // endregion

    // region defaults

    @Test
    fun `bodyAsText defaults to null`() {
        val request = RecordedRequest(HttpMethod.Get, "/test", emptyMap(), true)
        request.bodyAsText shouldBe null
    }

    // endregion
}
