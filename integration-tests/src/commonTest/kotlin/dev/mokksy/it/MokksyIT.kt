package dev.mokksy.it

import dev.mokksy.mokksy.Mokksy
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

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

            val content = response.bodyAsText()
            content shouldBe "Pong"
            print("""✅Response: "$content".""")
        }
}
