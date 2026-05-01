package dev.mokksy.mokksy.config

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

internal class MokksyFileConfigTest {
    // region parsing

    @Test
    fun `parses minimal plain stub`() {
        // language=yaml
        val yaml =
            """
            stubs:
              - path: /ping
                response:
                  body: pong
            """.trimIndent()

        val config = parseYamlConfig(yaml)

        config.stubs shouldHaveSize 1
        val stub = config.stubs[0]
        assertSoftly(stub) {
            name shouldBe null
            method shouldBe "GET"
            path shouldBe "/ping"
            response.type shouldBe ResponseType.PLAIN
            response.body shouldBe "pong"
            response.status shouldBe 200
        }
    }

    @Test
    fun `parses all plain response fields`() {
        // language=yaml
        val yaml =
            """
            stubs:
              - name: create-thing
                method: POST
                path: /things
                match:
                  bodyContains:
                    - '"42"'
                  headers:
                    Content-Type: application/json
                response:
                  body: '{"id":"42"}'
                  status: 201
                  headers:
                    Location: /things/42
                  delayMs: 100
            """.trimIndent()

        val config = parseYamlConfig(yaml)

        config.stubs shouldHaveSize 1
        val stub = config.stubs[0]
        assertSoftly(stub) {
            name shouldBe "create-thing"
            method shouldBe "POST"
            path shouldBe "/things"
            match.bodyContains shouldBe listOf("\"42\"")
            match.headers shouldBe mapOf("Content-Type" to "application/json")
            response.type shouldBe ResponseType.PLAIN
            response.body shouldBe """{"id":"42"}"""
            response.status shouldBe 201
            response.headers shouldBe mapOf("Location" to "/things/42")
            response.delayMs shouldBe 100L
        }
    }

    @Test
    fun `parses SSE response`() {
        // language=yaml
        val yaml =
            """
            stubs:
              - path: /sse
                method: POST
                response:
                  type: sse
                  chunks:
                    - "One"
                    - "Two"
                  delayBetweenChunksMs: 50
            """.trimIndent()

        val config = parseYamlConfig(yaml)

        val response = config.stubs[0].response
        assertSoftly(response) {
            type shouldBe ResponseType.SSE
            chunks shouldBe listOf("One", "Two")
            delayBetweenChunksMs shouldBe 50L
        }
    }

    @Test
    fun `parses stream response`() {
        // language=yaml
        val yaml =
            """
            stubs:
              - path: /stream
                response:
                  type: stream
                  chunks:
                    - "Hello"
                    - " World"
                  contentType: text/plain; charset=UTF-8
            """.trimIndent()

        val config = parseYamlConfig(yaml)

        val response = config.stubs[0].response
        assertSoftly(response) {
            type shouldBe ResponseType.STREAM
            chunks shouldBe listOf("Hello", " World")
            contentType shouldBe "text/plain; charset=UTF-8"
        }
    }

    @Test
    fun `parses multiple stubs`() {
        // language=yaml
        val yaml =
            """
            stubs:
              - path: /a
                response:
                  body: a
              - path: /b
                method: POST
                response:
                  body: b
            """.trimIndent()

        val config = parseYamlConfig(yaml)

        config.stubs shouldHaveSize 2
        config.stubs[0].path shouldBe "/a"
        config.stubs[1].path shouldBe "/b"
    }

    @Test
    fun `parses empty stubs list`() {
        // language=yaml
        val yaml = "stubs: []"

        val config = parseYamlConfig(yaml)

        config.stubs shouldHaveSize 0
    }

    // endregion

    // region validation

    @Test
    fun `validation passes for valid config`() {
        val config =
            MokksyFileConfig(
                stubs =
                    listOf(
                        StubConfig(
                            path = "/ping",
                            method = "GET",
                            response = ResponseConfig(body = "ok"),
                        ),
                    ),
            )

        validateConfig(config) // should not throw
    }

    @Test
    fun `validation fails for blank path`() {
        val config =
            MokksyFileConfig(
                stubs =
                    listOf(
                        StubConfig(
                            name = "blank-path",
                            path = "  ",
                            response = ResponseConfig(),
                        ),
                    ),
            )

        val ex = shouldThrow<IllegalArgumentException> { validateConfig(config) }
        ex.message shouldContain "blank-path"
        ex.message shouldContain "path"
    }

    @Test
    fun `validation fails for unknown HTTP method`() {
        val config =
            MokksyFileConfig(
                stubs =
                    listOf(
                        StubConfig(
                            path = "/x",
                            method = "BREW",
                            response = ResponseConfig(),
                        ),
                    ),
            )

        val ex = shouldThrow<IllegalArgumentException> { validateConfig(config) }
        ex.message shouldContain "BREW"
    }

    @Test
    fun `validation fails for SSE response with no chunks`() {
        val config =
            MokksyFileConfig(
                stubs =
                    listOf(
                        StubConfig(
                            name = "empty-sse",
                            path = "/sse",
                            response = ResponseConfig(type = ResponseType.SSE),
                        ),
                    ),
            )

        val ex = shouldThrow<IllegalArgumentException> { validateConfig(config) }
        ex.message shouldContain "empty-sse"
        ex.message shouldContain "sse"
        ex.message shouldContain "chunk"
    }

    @Test
    fun `validation fails for stream response with no chunks`() {
        val config =
            MokksyFileConfig(
                stubs =
                    listOf(
                        StubConfig(
                            name = "empty-stream",
                            path = "/stream",
                            response = ResponseConfig(type = ResponseType.STREAM),
                        ),
                    ),
            )

        val ex = shouldThrow<IllegalArgumentException> { validateConfig(config) }
        ex.message shouldContain "empty-stream"
        ex.message shouldContain "stream"
        ex.message shouldContain "chunk"
    }

    @Test
    fun `validation error includes stub index when name is absent`() {
        val config =
            MokksyFileConfig(
                stubs =
                    listOf(
                        StubConfig(path = "/ok", response = ResponseConfig()),
                        StubConfig(path = "/ok2", method = "BREW", response = ResponseConfig()),
                    ),
            )

        val ex = shouldThrow<IllegalArgumentException> { validateConfig(config) }
        ex.message shouldContain "stubs[1]"
    }

    // endregion
}
