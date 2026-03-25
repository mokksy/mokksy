package dev.mokksy.mokksy

import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledForJreRange
import org.junit.jupiter.api.condition.JRE

class VirtualThreadsIT {
    @Test
    fun `server starts and serves requests with default IO config`() {
        val mokksy = MokksyServer(verbose = true).start()
        try {
            val client = createKtorClient(mokksy.port())
            mokksy.get { path("/vt-test") } respondsWith { body = "ok" }

            val response =
                runBlocking {
                    client.get("${mokksy.baseUrl()}/vt-test")
                }
            response.status shouldBe HttpStatusCode.OK

            client.close()
        } finally {
            mokksy.shutdown()
        }
    }

    @Test
    @EnabledForJreRange(min = JRE.JAVA_21)
    fun `server starts with auto mode when virtual threads available`() {
        val mokksy = MokksyServer(verbose = true).start()
        try {
            val client = createKtorClient(mokksy.port())
            mokksy.get { path("/vt-auto") } respondsWith { body = "virtual" }

            val response =
                runBlocking {
                    client.get("${mokksy.baseUrl()}/vt-auto")
                }
            response.status shouldBe HttpStatusCode.OK

            client.close()
        } finally {
            mokksy.shutdown()
        }
    }
}
