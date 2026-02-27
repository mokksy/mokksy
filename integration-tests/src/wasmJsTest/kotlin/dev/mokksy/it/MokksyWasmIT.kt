package dev.mokksy.it

import dev.mokksy.mokksy.Mokksy
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MokksyWasmIT {

    @Test
    fun `GET returns configured response`() = runTest {
        val mokksy = Mokksy()
        mokksy.startSuspend()
        mokksy.awaitStarted()
        try {
            mokksy.get { path("/ping") } respondsWith { body = "Pong" }

            val response = HttpClient().get(mokksy.baseUrl() + "/ping")

            assertEquals("Pong", response.bodyAsText())
        } finally {
            mokksy.shutdownSuspend()
        }
    }
}
