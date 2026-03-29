package dev.mokksy.mokksy

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SseEventTest {
    @Test
    fun `should build full event`() {
        val sse =
            SseEventBuilder()
                .event("my-event")
                .id("my-id")
                .data("my-data")
                .retry(42)
                .comments("my-comments")
                .build()

        sse shouldNotBeNull {
            event shouldBe "my-event"
            id shouldBe "my-id"
            data shouldBe "my-data"
            retry shouldBe 42
            comments shouldBe "my-comments"
        }
    }

    @Test
    fun `should build minimal event`() {
        val sse =
            SseEventBuilder()
                .data("my-data")
                .build()

        sse shouldNotBeNull {
            event shouldBe null
            id shouldBe null
            data shouldBe "my-data"
            retry shouldBe null
            comments shouldBe null
        }
    }
}
