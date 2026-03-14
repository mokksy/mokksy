package dev.mokksy.mokksy

import dev.mokksy.Mokksy
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Test

internal class MokksyServerJavaLifecycleIT {
    @Test
    fun `start and shutdown work as blocking lifecycle methods`() {
        val mokksy = Mokksy.create().start()

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
        val mokksy = Mokksy.create().start()
        mokksy.port() shouldBeGreaterThan 0
        mokksy.close() // must not throw
    }

    @Test
    fun `shutdown with custom timings completes without error`() {
        val mokksy = Mokksy.create().start()

        mokksy.shutdown(gracePeriodMillis = 100, timeoutMillis = 200)
    }

    @Test
    fun `MokksyServer start extension blocks until port is bound and returns server`() {
        val server = MokksyServer()
        val returned = server.start()
        try {
            assertSoftly {
                returned shouldBe server
                server.port() shouldBeGreaterThan 0
            }
        } finally {
            server.shutdown()
        }
    }

    @Test
    suspend fun `server responds after start via createKtorClient`() {
        val mokksy = Mokksy.create().start()
        val client = createKtorClient(mokksy.port())

        try {
            mokksy.get {
                path("/java-lifecycle-test")
            } respondsWith {
                body = "alive"
            }

            val result = client.get("/java-lifecycle-test")
            val responseBody = result.bodyAsText()

            assertSoftly {
                result.status shouldBe HttpStatusCode.OK
                responseBody shouldBe "alive"
            }
        } finally {
            client.close()
            mokksy.shutdown()
        }
    }
}
