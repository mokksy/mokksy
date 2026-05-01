package dev.mokksy.mokksy.config

import dev.mokksy.mokksy.ENV_MOKKSY_CONFIG
import dev.mokksy.mokksy.MokksyServer
import dev.mokksy.mokksy.PROP_MOKKSY_CONFIG
import dev.mokksy.mokksy.createKtorSSEClient
import dev.mokksy.mokksy.loadStubsFromEnv
import dev.mokksy.mokksy.loadStubsFromFile
import dev.mokksy.mokksy.shutdown
import dev.mokksy.mokksy.start
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.withCharset
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeText
import kotlin.text.Charsets.UTF_8

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class StubFileConfigIT {
    private val mokksy = MokksyServer(verbose = true)
    private lateinit var client: HttpClient

    @BeforeAll
    fun setup() {
        val file = javaClass.getResource("/test-stubs.yaml")!!.file
        mokksy.start()
        mokksy.loadStubsFromFile(file)
        client = createKtorSSEClient(mokksy.port())
    }

    @AfterAll
    fun teardown() {
        mokksy.shutdown()
        client.close()
    }

    // region plain responses

    @Test
    suspend fun `GET stub returns plain response from file config`() {
        val result = client.get("/ping")

        assertSoftly(result) {
            status shouldBe HttpStatusCode.OK
            bodyAsText() shouldBe """{"response":"Pong"}"""
        }
    }

    @Test
    suspend fun `POST stub with body match returns configured status and headers`() {
        val result =
            client.post("/things") {
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody("""{"id":"42"}""")
            }

        assertSoftly(result) {
            status shouldBe HttpStatusCode.Created
            bodyAsText() shouldBe """{"id":"42","name":"thing-42"}"""
            headers["Location"] shouldBe "/things/42"
            headers["Foo"] shouldBe "bar"
        }
    }

    @Test
    suspend fun `POST stub does not match when body does not contain expected string`() {
        val result =
            client.post("/things") {
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody("""{"id":"99"}""")
            }

        result.status shouldBe HttpStatusCode.NotFound
    }

    @Test
    suspend fun `GET delayed stub returns response after delay`() {
        val result = client.get("/delayed")

        assertSoftly(result) {
            status shouldBe HttpStatusCode.OK
            bodyAsText() shouldBe "ok"
        }
    }

    // endregion

    // region SSE stream

    @Test
    suspend fun `POST stub returns SSE stream from file config`() {
        val result = client.post("/sse")

        assertSoftly(result) {
            status shouldBe HttpStatusCode.OK
            contentType() shouldBe ContentType.Text.EventStream.withCharset(UTF_8)
            bodyAsText() shouldBe "data: One\r\n\r\ndata: Two\r\n\r\n"
        }
    }

    // endregion

    // region plain text stream

    @Test
    suspend fun `GET stub returns plain text stream from file config`() {
        val result = client.get("/stream")

        assertSoftly(result) {
            status shouldBe HttpStatusCode.OK
            contentType() shouldBe ContentType.Text.Plain.withCharset(UTF_8)
            bodyAsText() shouldBe "Hello World"
        }
    }

    // endregion

    // region error cases (use a fresh server, no need to start it for parse errors)

    @Test
    fun `loadStubsFromFile fails with clear message when file is missing`() {
        val server = MokksyServer()
        val missing = File("/nonexistent/path/stubs.yaml")

        val ex = shouldThrow<IllegalArgumentException> { server.loadStubsFromFile(missing) }
        ex.message shouldContain "not found"
        ex.message shouldContain missing.absolutePath
    }

    @Test
    fun `loadStubsFromFile fails with clear message for invalid YAML`() {
        val server = MokksyServer()
        val badYaml = createTempFile("bad", ".yaml")
        badYaml.writeText("stubs: [{ path: /x, response: { status: not-a-number } }]")

        try {
            val ex =
                shouldThrow<IllegalArgumentException> { server.loadStubsFromFile(badYaml.toFile()) }
            ex.message shouldContain "Invalid YAML"
        } finally {
            badYaml.deleteIfExists()
        }
    }

    @Test
    fun `loadStubsFromFile fails with clear message for unknown HTTP method`() {
        val server = MokksyServer()
        val yaml =
            """
            stubs:
              - name: bad-method
                method: BREW
                path: /coffee
                response:
                  body: ok
            """.trimIndent()
        val file = createTempFile("stubs", ".yaml")
        file.writeText(yaml)

        try {
            val ex =
                shouldThrow<IllegalArgumentException> { server.loadStubsFromFile(file.toFile()) }
            ex.message shouldContain "BREW"
        } finally {
            file.deleteIfExists()
        }
    }

    @Test
    fun `loadStubsFromFile fails with clear message for SSE with no chunks`() {
        val server = MokksyServer()
        val yaml =
            """
            stubs:
              - name: empty-sse
                path: /sse
                response:
                  type: sse
            """.trimIndent()
        val file = createTempFile("stubs", ".yaml")
        file.writeText(yaml)

        try {
            val ex =
                shouldThrow<IllegalArgumentException> { server.loadStubsFromFile(file.toFile()) }
            ex.message shouldContain "empty-sse"
            ex.message shouldContain "chunk"
        } finally {
            file.deleteIfExists()
        }
    }

    // endregion

    // region env / system property loading

    @Test
    fun `loadStubsFromEnv reads path from system property`() {
        assumeTrue(
            System.getenv(ENV_MOKKSY_CONFIG) == null,
            "MOKKSY_CONFIG env var is set — skipping",
        )
        val file = File(javaClass.getResource("/test-stubs.yaml")!!.toURI())
        System.setProperty(PROP_MOKKSY_CONFIG, file.absolutePath)
        try {
            val server = MokksyServer()
            server.loadStubsFromEnv() // should not throw
        } finally {
            System.clearProperty(PROP_MOKKSY_CONFIG)
        }
    }

    @Test
    fun `loadStubsFromEnv throws when neither env var nor property is set`() {
        Assumptions.assumeTrue(
            System.getenv(ENV_MOKKSY_CONFIG) == null,
            "MOKKSY_CONFIG env var is set — skipping",
        )
        System.clearProperty(PROP_MOKKSY_CONFIG)
        val server = MokksyServer()

        val ex = shouldThrow<IllegalStateException> { server.loadStubsFromEnv() }
        ex.message shouldContain ENV_MOKKSY_CONFIG
        ex.message shouldContain PROP_MOKKSY_CONFIG
    }

    // endregion
}
