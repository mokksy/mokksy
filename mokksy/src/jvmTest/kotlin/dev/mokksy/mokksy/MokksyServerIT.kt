package dev.mokksy.mokksy

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.request
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class MokksyServerIT : AbstractIT() {
    // region Mokksy factory function

    @Test
    suspend fun `Mokksy factory function creates a startable MokksyServer`() {
        val server = Mokksy()
        server.startSuspend()
        try {
            server.port() shouldBeGreaterThan 0
        } finally {
            server.shutdownSuspend()
        }
    }

    // endregion

    // region StubConfiguration overloads

    @ParameterizedTest
    @ValueSource(strings = ["GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "PATCH"])
    suspend fun `StubConfiguration overload registers stub and responds`(methodName: String) {
        val method = HttpMethod.parse(methodName)
        val nameLower = methodName.lowercase()
        val path = "/$nameLower-config-$seed"
        val config = StubConfiguration(name = "$nameLower-config-stub")

        when (method) {
            HttpMethod.Get -> mokksy.get(config, String::class) { path(path) }
            HttpMethod.Post -> mokksy.post(config, String::class) { path(path) }
            HttpMethod.Put -> mokksy.put(config, String::class) { path(path) }
            HttpMethod.Delete -> mokksy.delete(config, String::class) { path(path) }
            HttpMethod.Head -> mokksy.head(config, String::class) { path(path) }
            HttpMethod.Options -> mokksy.options(config, String::class) { path(path) }
            HttpMethod.Patch -> mokksy.patch(config, String::class) { path(path) }
            else -> error("Unexpected method: $method")
        } respondsWith {
            body = "$nameLower-ok"
        }

        val result = client.request(path) { this.method = method }

        if (method == HttpMethod.Head) {
            result.status shouldBe HttpStatusCode.OK
        } else {
            result.bodyAsText() shouldBe "$nameLower-ok"
        }
    }

    // endregion

    // region method() with StubConfiguration extension overload

    @Test
    suspend fun `method extension with name string registers stub for any HTTP method`() {
        val path = "/method-ext-$seed"

        mokksy.method(
            name = "method-ext-stub",
            httpMethod = HttpMethod.Put,
            requestType = String::class,
        ) {
            path(path)
        } respondsWith {
            body = "method-ext-ok"
        }

        val result = client.put(path)
        result.bodyAsText() shouldBe "method-ext-ok"
    }

    // endregion

    // region baseUrl()

    @Test
    fun `baseUrl returns http host and port after server has started`() {
        val url = mokksy.baseUrl()

        assertSoftly {
            url shouldContain "http://"
            url shouldContain "${mokksy.port()}"
        }
    }

    @Test
    fun `baseUrl throws IllegalStateException when server is not started`() {
        val notStarted = MokksyServer()

        val error =
            shouldThrow<IllegalStateException> {
                notStarted.baseUrl()
            }

        error.message shouldContain "Server is not started"
    }

    // endregion

    // region resetMatchCounts

    @Test
    suspend fun `resetMatchCounts resets all stub match counts to zero`() {
        val freshMokksy = MokksyServer()
        freshMokksy.startSuspend()
        val freshClient = createKtorClient(freshMokksy.port())
        try {
            val path = "/reset-match-$seed"
            freshMokksy.get {
                path(path)
            } respondsWith {
                body = "ok"
            }

            freshClient.get(path)
            freshMokksy.findAllUnmatchedStubs().shouldBeEmpty()

            freshMokksy.resetMatchState()

            freshMokksy.findAllUnmatchedStubs() shouldHaveSize 1
        } finally {
            freshClient.close()
            freshMokksy.shutdownSuspend()
        }
    }

    // endregion

    // region findAllUnmatchedRequests (deprecated)

    @Suppress("DEPRECATION")
    @Test
    suspend fun `findAllUnmatchedRequests returns unmatched requests (deprecated alias)`() {
        val path = "/unmatched-deprecated-$seed"

        client.get(path)

        mokksy.findAllUnmatchedRequests() shouldHaveSize 1
    }

    // endregion
}
