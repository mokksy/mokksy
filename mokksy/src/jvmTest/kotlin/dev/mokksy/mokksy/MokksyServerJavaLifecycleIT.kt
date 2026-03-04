package dev.mokksy.mokksy

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

internal class MokksyServerJavaLifecycleIT {
    @Test
    fun `start and shutdown work as blocking lifecycle methods`() {
        val mokksy = MokksyServerJava()
        mokksy.start()

        try {
            assertSoftly {
                mokksy.port() shouldBeGreaterThan 0
                mokksy.baseUrl() shouldContain "http://"
                mokksy.baseUrl() shouldContain "${mokksy.port()}"
            }
        } finally {
            mokksy.shutdown()
        }
    }

    @Test
    fun `close shuts down the server`() {
        val mokksy = MokksyServerJava()
        mokksy.start()
        val port = mokksy.port()

        mokksy.close()

        port shouldBeGreaterThan 0
    }

    @Test
    fun `shutdown with custom timings completes without error`() {
        val mokksy = MokksyServerJava()
        mokksy.start()

        mokksy.shutdown(gracePeriodMillis = 100, timeoutMillis = 200)
    }

    @Test
    fun `server responds after start via createKtorClient`() {
        val mokksy = MokksyServerJava()
        mokksy.start()

        try {
            mokksy.get { spec ->
                spec.path("/java-lifecycle-test")
            }.respondsWith { builder ->
                builder.body = "alive"
            }

            val client = createKtorClient(mokksy.port())
            val result = runBlocking { client.get("/java-lifecycle-test") }
            val responseBody = runBlocking { result.bodyAsText() }

            assertSoftly {
                result.status shouldBe HttpStatusCode.OK
                responseBody shouldBe "alive"
            }

            client.close()
        } finally {
            mokksy.shutdown()
        }
    }
}
