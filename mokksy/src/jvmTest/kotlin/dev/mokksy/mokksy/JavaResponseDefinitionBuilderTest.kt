package dev.mokksy.mokksy

import dev.mokksy.mokksy.response.ResponseDefinitionBuilder
import io.kotest.matchers.shouldBe
import io.ktor.http.ContentType
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test

class JavaResponseDefinitionBuilderTest {
    private val delegate = mockk<ResponseDefinitionBuilder<String, String>>(relaxed = true)
    private val sut = JavaResponseDefinitionBuilder(delegate)

    // region body

    @Test
    fun `body delegates to delegate and returns this`() {
        val result = sut.body("hello")
        verify { delegate.body("hello") }
        result shouldBe sut
    }

    // endregion

    // region status

    @Test
    fun `status delegates to delegate and returns this`() {
        val result = sut.status(201)
        verify { delegate.status(201) }
        result shouldBe sut
    }

    // endregion

    // region header

    @Test
    fun `header delegates name and value to delegate and returns this`() {
        val result = sut.header("X-Custom", "value")
        verify { delegate.header("X-Custom", "value") }
        result shouldBe sut
    }

    // endregion

    // region delayMillis

    @Test
    fun `delayMillis delegates to delegate and returns this`() {
        val result = sut.delayMillis(250)
        verify { delegate.delayMillis(250) }
        result shouldBe sut
    }

    // endregion

    // region contentType

    @Test
    fun `contentType(ContentType) sets contentType on delegate and returns this`() {
        val result = sut.contentType(ContentType.Application.Json)
        verify { delegate.contentType = ContentType.Application.Json }
        result shouldBe sut
    }

    @Test
    fun `contentType(String) parses and sets contentType on delegate and returns this`() {
        val result = sut.contentType("application/json")
        verify { delegate.contentType = ContentType.Application.Json }
        result shouldBe sut
    }

    // endregion

    // region chaining

    @Test
    fun `methods can be chained fluently`() {
        val result =
            sut
                .body("ok")
                .status(200)
                .header("X-H", "v")
                .delayMillis(0)
                .contentType(ContentType.Text.Plain)
        result shouldBe sut
    }

    // endregion
}
