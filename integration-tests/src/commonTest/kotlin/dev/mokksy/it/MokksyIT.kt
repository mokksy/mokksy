package dev.mokksy.it

import dev.mokksy.mokksy.Mokksy
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * This test intentionally creates and starts Mokksy once and never shuts it down after each test.
 * This verifies a scenario when Mokksy is started once and used across multiple tests.
 */
class MokksyIT {
    val mokksy = Mokksy()
    val client = HttpClient()

    @BeforeTest
    fun setup() =
        runTest {
            mokksy.startSuspend()
        }

    @AfterTest
    fun teardown() =
        runTest {
            client.close()
            mokksy.shutdownSuspend()
        }

    @Test
    fun `GET returns configured response`() =
        runTest {
            mokksy.awaitStarted()

            mokksy.get { path("/ping") } respondsWith { body = "Pong" }

            val response = client.get(mokksy.baseUrl() + "/ping")
            val body = response.bodyAsText()

            assertSoftly(response) {
                status shouldBe HttpStatusCode.OK
                body shouldBe "Pong"
            }
            print("""✅Response: "$body".""")
        }
}
