package dev.mokksy.mokksy

import io.kotest.matchers.shouldBe
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.BeforeTest

internal class MokksyServerMethodsIT : AbstractIT() {
    private lateinit var name: String

    private lateinit var requestPayload: TestPerson

    @BeforeTest
    fun before() {
        name = UUID.randomUUID().toString()
        requestPayload = TestPerson.random()
    }

    @ParameterizedTest
    @ValueSource(strings = ["GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"])
    suspend fun `Should respond with reified type to Method`(methodName: String) {
        val method = HttpMethod.parse(methodName)
        val requestAsString = Json.encodeToString(requestPayload)
        val capturedError = AtomicReference<Throwable?>()
        val expectedResponseRef = AtomicReference<String>()

        mokksy.method<TestPerson>(name, method) {
            path("/reified-method-$method-$seed")
            containsHeader("X-Seed", "$seed")
        } respondsWith {
            try {
                this.request.bodyAsString() shouldBe requestAsString
                this.request.body() shouldBe requestPayload
            } catch (e: AssertionError) {
                capturedError.set(e)
            }
            val responsePayload = TestOrder.random(person = requestPayload)
            body = Json.encodeToString(responsePayload)
            expectedResponseRef.set(body)
        }

        val result =
            client.request("/reified-method-$method-$seed") {
                this.method = method
                headers.append("X-Seed", "$seed")
                contentType(ContentType.Application.Json)
                setBody(requestAsString)
            }

        capturedError.get()?.let { throw it }
        result.status shouldBe HttpStatusCode.OK
        if (method != HttpMethod.Head) {
            result.bodyAsText() shouldBe expectedResponseRef.get()
        } else {
            result.bodyAsText() shouldBe ""
        }
    }

    @Test
    suspend fun `get with reified type infers request body type`() {
        val capturedPerson = AtomicReference<TestPerson?>()
        val capturedError = AtomicReference<Throwable?>()
        val requestAsString = Json.encodeToString(requestPayload)

        mokksy.get<TestPerson>(name = name) {
            path("/reified-get-$seed")
            containsHeader("X-Seed", "$seed")
        } respondsWith {
            try {
                capturedPerson.set(request.body())
            } catch (e: AssertionError) {
                capturedError.set(e)
            }
            body = "ok"
        }

        client.request("/reified-get-$seed") {
            method = HttpMethod.Get
            headers.append("X-Seed", "$seed")
            contentType(ContentType.Application.Json)
            setBody(requestAsString)
        }

        capturedError.get()?.let { throw it }
        capturedPerson.get() shouldBe requestPayload
    }

    @Test
    suspend fun `post with StubConfiguration removes stub after first match`() {
        val uri = "/reified-post-remove-$seed"
        val requestAsString = Json.encodeToString(requestPayload)

        mokksy.post<TestPerson>(StubConfiguration(removeAfterMatch = true)) {
            path(uri)
        } respondsWith {
            body = "matched"
        }

        client
            .request(uri) {
                method = HttpMethod.Post
                contentType(ContentType.Application.Json)
                setBody(requestAsString)
            }.status shouldBe HttpStatusCode.OK

        client
            .request(uri) {
                method = HttpMethod.Post
                contentType(ContentType.Application.Json)
                setBody(requestAsString)
            }.status shouldBe HttpStatusCode.NotFound
    }

    @ParameterizedTest
    @ValueSource(strings = ["GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"])
    suspend fun `method with StubConfiguration responds with reified type`(methodName: String) {
        val method = HttpMethod.parse(methodName)
        val requestAsString = Json.encodeToString(requestPayload)
        val capturedError = AtomicReference<Throwable?>()
        val expectedResponseRef = AtomicReference<String>()

        mokksy.method<TestPerson>(StubConfiguration(name = name), method) {
            path("/reified-config-method-$method-$seed")
            containsHeader("X-Seed", "$seed")
        } respondsWith {
            try {
                this.request.body() shouldBe requestPayload
            } catch (e: AssertionError) {
                capturedError.set(e)
            }
            val responsePayload = TestOrder.random(person = requestPayload)
            body = Json.encodeToString(responsePayload)
            expectedResponseRef.set(body)
        }

        val result =
            client.request("/reified-config-method-$method-$seed") {
                this.method = method
                headers.append("X-Seed", "$seed")
                contentType(ContentType.Application.Json)
                setBody(requestAsString)
            }

        capturedError.get()?.let { throw it }
        result.status shouldBe HttpStatusCode.OK
        if (method != HttpMethod.Head) {
            result.bodyAsText() shouldBe expectedResponseRef.get()
        } else {
            result.bodyAsText() shouldBe ""
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"])
    suspend fun `verb shortcut with name routes to correct HTTP method`(methodName: String) {
        val method = HttpMethod.parse(methodName)
        val path = "/reified-verb-$methodName-$seed"

        val step =
            when (method) {
                HttpMethod.Get -> mokksy.get<TestPerson>(name = name) { path(path) }
                HttpMethod.Post -> mokksy.post<TestPerson>(name = name) { path(path) }
                HttpMethod.Put -> mokksy.put<TestPerson>(name = name) { path(path) }
                HttpMethod.Delete -> mokksy.delete<TestPerson>(name = name) { path(path) }
                HttpMethod.Patch -> mokksy.patch<TestPerson>(name = name) { path(path) }
                HttpMethod.Head -> mokksy.head<TestPerson>(name = name) { path(path) }
                HttpMethod.Options -> mokksy.options<TestPerson>(name = name) { path(path) }
                else -> error("Unexpected method: $method")
            }
        step respondsWith { body = "ok" }

        val result = client.request(path) { this.method = method }

        result.status shouldBe HttpStatusCode.OK
        if (method != HttpMethod.Head) {
            result.bodyAsText() shouldBe "ok"
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"])
    suspend fun `verb shortcut with StubConfiguration removes stub after first match`(
        methodName: String,
    ) {
        val method = HttpMethod.parse(methodName)
        val path = "/reified-verb-config-$methodName-$seed"
        val config = StubConfiguration(name = name, removeAfterMatch = true)

        val step =
            when (method) {
                HttpMethod.Get -> mokksy.get<TestPerson>(config) { path(path) }
                HttpMethod.Post -> mokksy.post<TestPerson>(config) { path(path) }
                HttpMethod.Put -> mokksy.put<TestPerson>(config) { path(path) }
                HttpMethod.Delete -> mokksy.delete<TestPerson>(config) { path(path) }
                HttpMethod.Patch -> mokksy.patch<TestPerson>(config) { path(path) }
                HttpMethod.Head -> mokksy.head<TestPerson>(config) { path(path) }
                HttpMethod.Options -> mokksy.options<TestPerson>(config) { path(path) }
                else -> error("Unexpected method: $method")
            }
        step respondsWith { body = "matched" }

        client.request(path) { this.method = method }.status shouldBe HttpStatusCode.OK
        client.request(path) { this.method = method }.status shouldBe HttpStatusCode.NotFound
    }
}
