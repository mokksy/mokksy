@file:OptIn(ExperimentalMokksyApi::class)

package dev.mokksy.mokksy

import dev.mokksy.mokksy.request.RecordedRequest
import dev.mokksy.mokksy.response.AbstractResponseDefinition
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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

        mokksy.method(config, method, String::class) { path(path) } respondsWith {
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

    // region getStub (deprecated, retained for backward-compat regression)

    @Suppress("DEPRECATION")
    @Test
    fun `getStub returns named stub handle`() {
        val expected =
            mokksy.get(name = "named-stub", requestType = String::class) {
                path("/named-stub-$seed")
            } respondsWith {
                body = "ok"
            }

        mokksy.getStub("named-stub").name shouldBe expected.name
    }

    @Suppress("DEPRECATION")
    @Test
    fun `getStub throws when stub not found`() {
        val error =
            shouldThrow<NoSuchElementException> {
                mokksy.getStub("missing-stub")
            }

        error.message shouldContain "No stub registered with name 'missing-stub'"
    }

    @Test
    fun `duplicate stub name throws at registration`() {
        mokksy.get(name = "duplicate-stub", requestType = String::class) {
            path("/duplicate-stub-a-$seed")
        } respondsWith {
            body = "a"
        }
        val error =
            shouldThrow<IllegalArgumentException> {
                mokksy.post(name = "duplicate-stub", requestType = String::class) {
                    path("/duplicate-stub-b-$seed")
                } respondsWith {
                    body = "b"
                }
            }

        error.message shouldContain "A stub with name 'duplicate-stub' is already registered"
    }

    @Test
    fun `findStubById returns handle for registered stub`() {
        val handle =
            mokksy.get { path("/find-by-id-$seed") } respondsWith {
                body = "ok"
            }
        mokksy.findStubById(handle.id) shouldNotBeNull {
            id shouldBe handle.id
            name shouldBe null
        }
    }

    @Test
    fun `findStubById returns null for unknown id`() {
        mokksy.findStubById("nonexistent") shouldBe null
    }

    @Test
    fun `findStubByName returns handle for named stub`() {
        mokksy.get(name = "findable", requestType = String::class) {
            path("/find-by-name-$seed")
        } respondsWith {
            body = "ok"
        }
        mokksy.findStubByName("findable") shouldNotBeNull {
            name shouldBe "findable"
        }
    }

    @Test
    fun `findStubByName returns null for unknown name`() {
        mokksy.findStubByName("nonexistent") shouldBe null
    }

    // endregion

    // region findStub / findStubs (predicate)

    @Test
    fun `findStub with predicate returns first match`() {
        mokksy.get { path("/predicate-first-$seed") } respondsWith { body = "ok" }
        mokksy.get { path("/predicate-second-$seed") } respondsWith { body = "ok" }

        mokksy.findStub { it.id.isNotEmpty() } shouldNotBe null
    }

    @Test
    fun `findStub with predicate returns null when no match`() {
        mokksy.get { path("/no-match-$seed") } respondsWith { body = "ok" }

        mokksy.findStub { it.name == "no-such-name" } shouldBe null
    }

    @Test
    fun `findStubs with predicate returns all matches`() {
        mokksy.get(name = "match-a-$seed", requestType = String::class) {
            path("/match-a-$seed")
        } respondsWith { body = "a" }
        mokksy.get(name = "match-b-$seed", requestType = String::class) {
            path("/match-b-$seed")
        } respondsWith { body = "b" }
        mokksy.get(name = "other-$seed", requestType = String::class) {
            path("/other-$seed")
        } respondsWith { body = "other" }

        mokksy.findStubs { it.name?.startsWith("match-") == true } shouldHaveSize 2
    }

    @Test
    fun `findStubs with predicate returns empty list when no match`() {
        mokksy.get { path("/nothing-$seed") } respondsWith { body = "ok" }

        mokksy.findStubs { it.name == "missing" } shouldBe emptyList()
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

    // region onResponseReady listener

    @Test
    suspend fun `onResponseReady listener fires with request and response when a stub matches`() {
        val path = "/listener-request-response-$seed"
        val captured = mutableListOf<Pair<RecordedRequest, AbstractResponseDefinition<*>>>()

        mokksy.onResponseReady { request, response ->
            captured.add(request to response)
        }

        mokksy.get {
            path(path)
        } respondsWith {
            body = "listener-ok"
        }

        client.get(path)

        captured shouldHaveSize 1
        captured[0].first.uri shouldBe path
        captured[0].second.httpStatus shouldBe HttpStatusCode.OK
        captured[0].second.getHttpStatusCode() shouldBe HttpStatusCode.OK.value
    }

    @Test
    suspend fun `addListener with RequestListener fires correctly from Kotlin`() {
        val path = "/add-listener-$seed"
        val captured = mutableListOf<Pair<RecordedRequest, AbstractResponseDefinition<*>>>()

        mokksy.addListener { request, response ->
            captured.add(request to response)
        }

        mokksy.get {
            path(path)
        } respondsWith {
            body = "listener-ok"
        }

        client.get(path)

        captured shouldHaveSize 1
        captured[0].first.uri shouldBe path
        captured[0].second.httpStatus shouldBe HttpStatusCode.OK
    }

    @Test
    suspend fun `onResponseReady listener is not invoked for unmatched requests`() {
        val captured = mutableListOf<RecordedRequest>()

        mokksy.onResponseReady { request, _ ->
            captured.add(request)
        }

        client.get("/unmatched-listener-$seed")

        captured.shouldBeEmpty()
    }

    @Test
    suspend fun `onResponseReady is replaceable`() {
        var firstCalled = false
        var secondCalled = false

        mokksy.onResponseReady { _, _ -> firstCalled = true }
        mokksy.onResponseReady { _, _ -> secondCalled = true }

        mokksy.get {
            path("/replace-listener-$seed")
        } respondsWith {
            body = "ok"
        }

        client.get("/replace-listener-$seed")

        firstCalled shouldBe false
        secondCalled shouldBe true
    }

    // endregion
}
