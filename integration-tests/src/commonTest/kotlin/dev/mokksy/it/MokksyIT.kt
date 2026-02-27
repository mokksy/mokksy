package dev.mokksy.it

import dev.mokksy.mokksy.Mokksy
import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MokksyIT {
    val mokksy = Mokksy()
    lateinit var client: HttpClient

    @BeforeTest
    fun setup() =
        runTest {
            mokksy.startSuspend()
            mokksy.awaitStarted()
            client =
                HttpClient {
                    install(HttpTimeout)
                    install(DefaultRequest) {
                        url(mokksy.baseUrl())
                    }
                }
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
            mokksy.get { path("/ping") } respondsWith { body = "Pong" }

            val response = client.get("/ping")

            val content = response.bodyAsText()
            assertEquals("Pong", content)
            print("""✅Response: "$content".""")
        }
}
