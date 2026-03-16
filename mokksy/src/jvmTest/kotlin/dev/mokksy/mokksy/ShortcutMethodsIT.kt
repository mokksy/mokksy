package dev.mokksy.mokksy

import dev.mokksy.mokksy.request.RequestSpecificationBuilder
import io.kotest.matchers.shouldBe
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.withCharset
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.BeforeTest

@Suppress("UastIncorrectHttpHeaderInspection")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ShortcutMethodsIT : AbstractIT() {
    private lateinit var requestPayload: TestPerson

    @BeforeTest
    fun before() {
        requestPayload = TestPerson.random()
    }

    @ParameterizedTest
    @ValueSource(strings = ["GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"])
    suspend fun `Should respond to shortcut method`(methodName: String) {
        val method = HttpMethod.parse(methodName)
        doTestCallMethod(method) { configurer ->
            when (method) {
                HttpMethod.Get -> mokksy.get(configurer)
                HttpMethod.Post -> mokksy.post(configurer)
                HttpMethod.Put -> mokksy.put(configurer)
                HttpMethod.Patch -> mokksy.patch(configurer)
                HttpMethod.Delete -> mokksy.delete(configurer)
                HttpMethod.Head -> mokksy.head(configurer)
                HttpMethod.Options -> mokksy.options(configurer)
                else -> error("Unexpected method: $method")
            }
        }
    }

    private suspend fun doTestCallMethod(
        method: HttpMethod,
        block: (RequestSpecificationBuilder<String>.() -> Unit) -> BuildingStep<String>,
    ) {
        val configurer: RequestSpecificationBuilder<String>.() -> Unit = {
            path("/shortcut-method-$method")
            this.containsHeader("X-Seed", "$seed")
        }

        val expectedResponseRef = AtomicReference<String>()
        val requestAsString = Json.encodeToString(requestPayload)
        val expectedContentType = ContentType.Application.ProblemJson.withCharset(Charsets.UTF_32)
        val capturedError = AtomicReference<Throwable?>()

        block.invoke {
            configurer(this)
        } respondsWith {
            try {
                this.request.bodyAsString() shouldBe requestAsString
                this.request.body() shouldBe requestAsString
            } catch (e: AssertionError) {
                capturedError.set(e)
            }

            val responsePayload = TestOrder.random(person = requestPayload)
            contentType = expectedContentType
            body = Json.encodeToString(responsePayload)
            expectedResponseRef.set(body)
        }

        // when
        val result =
            client.request("/shortcut-method-$method") {
                this.method = method
                headers.append("X-Seed", "$seed")
                contentType(ContentType.Application.Json)
                setBody(requestAsString)
            }

        // then
        capturedError.get()?.let { throw it }
        result.status shouldBe HttpStatusCode.OK
        result.contentType() shouldBe expectedContentType

        if (method != HttpMethod.Head) {
            result.bodyAsText() shouldBe expectedResponseRef.get()
        } else {
            result.bodyAsText() shouldBe ""
        }
    }

    @AfterEach
    fun afterEach() {
        mokksy.verifyNoUnexpectedRequests()
    }

    @AfterAll
    fun afterAll() {
        mokksy.verifyNoUnmatchedStubs()
    }
}
