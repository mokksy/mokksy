package dev.mokksy.mokksy.utils

import io.kotest.matchers.shouldBe
import io.ktor.http.ContentType
import org.junit.jupiter.api.Test

class MimeTypesTest {

    @Test
    fun `asMimeType converts ContentType Application Json`() {
        ContentType.Application.Json.asMimeType() shouldBe "application/json"
    }

    @Test
    fun `asMimeType converts ContentType Text Plain`() {
        ContentType.Text.Plain.asMimeType() shouldBe "text/plain"
    }

    @Test
    fun `asMimeType converts ContentType Text EventStream`() {
        ContentType.Text.EventStream.asMimeType() shouldBe "text/event-stream"
    }
}
