package dev.mokksy.mokksy

import assertk.assertThat
import assertk.assertions.isEqualTo
import dev.mokksy.mokksy.request.RequestSpecificationBuilder
import io.kotest.matchers.equals.beEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import kotlin.random.Random
import kotlin.test.BeforeTest
import kotlin.test.fail

@Suppress("UastIncorrectHttpHeaderInspection")
internal class TypesafeMethodsIT : AbstractIT() {
    private lateinit var name: String

    private lateinit var requestPayload: TestPerson

    @BeforeTest
    fun before() {
        name = UUID.randomUUID().toString()

        requestPayload = TestPerson.random()
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS",
        ],
    )
    suspend fun `Should respond to Method`(methodName: String) {
        val method = HttpMethod.parse(methodName)
        doTestCallMethod(method) {
            mokksy.method(name, method, TestPerson::class, it)
        }
    }

    private suspend fun <P : Any> doTestCallMethod(
        method: HttpMethod,
        block: (RequestSpecificationBuilder<P>.() -> Unit) -> BuildingStep<*>,
    ) {
        val configurer: RequestSpecificationBuilder<P>.() -> Unit = {
            path("/method-$method")
            this.containsHeader("X-Seed", "$seed")
        }

        val expectedResponseRef = AtomicReference<String>()
        val requestAsString = Json.encodeToString(requestPayload)
        val capturedError = AtomicReference<Throwable?>()

        block.invoke {
            configurer(this)
        } respondsWith {
            try {
                request.bodyAsString() shouldBe requestAsString
                request.body() shouldBe requestPayload
            } catch (e: AssertionError) {
                capturedError.set(e)
            }

            val responsePayload = TestOrder.random(person = requestPayload)
            body = Json.encodeToString(responsePayload)
            expectedResponseRef.set(body)
        }

        // when
        val result =
            client.request("/method-$method") {
                this.method = method
                headers.append("X-Seed", "$seed")
                contentType(ContentType.Application.Json)
                setBody(requestAsString)
            }

        // then
        capturedError.get()?.let { throw it }
        result.status shouldBe HttpStatusCode.OK

        if (method != HttpMethod.Head) {
            result.bodyAsText() shouldBe expectedResponseRef.get()
        } else {
            result.bodyAsText() shouldBe ""
        }
    }

    @Test
    suspend fun `Should respond 404 to unknown request`() {
        // when
        val result = client.get("/unknown")

        // then
        assertThat(result.status).isEqualTo(HttpStatusCode.NotFound)
    }

    @Test
    suspend fun `Should respond 404 to unmatched headers`() {
        val uri = "/unmatched-headers"
        mokksy
            .get {
                path = beEqual(uri)
                this.containsHeader("Foo", "bar")
            }.respondsWith(String::class) {
                fail("✋🛑 Should not be called")
            }
        // when
        val result = client.get(uri)

        // then
        result.status shouldBe HttpStatusCode.NotFound
    }

    @Test
    suspend fun `Should respond to POST`() {
        // given
        val id = Random.nextInt()
        val expectedResponse =
            // language=json
            """
            {
                "id": "$id",
                "name": "thing-$id"
            }
            """.trimIndent()

        mokksy
            .post(name = "post", Input::class) {
                path = beEqual("/things-$seed")
                bodyContains("$id")
            }.respondsWith(String::class) {
                contentType = ContentType.Application.Json
                body = expectedResponse
                httpStatus = HttpStatusCode.Created
                headers {
                    // type-safe builder style
                    append(HttpHeaders.Location, "/things/$id")
                }
                headers += "Foo" to "bar" // list style
            }

        // when
        val result =
            client.post("/things-$seed") {
                contentType(ContentType.Application.Json)
                setBody(
                    // language=json
                    """
                    {
                        "name": "the thing: $id"
                    }
                    """.trimIndent(),
                )
            }

        // then
        result.status shouldBe HttpStatusCode.Created
        result.bodyAsText() shouldBe expectedResponse
        result.headers["Location"] shouldBe "/things/$id"
        result.headers["Foo"] shouldBe "bar"
    }

    @Test
    suspend fun `Should respond to POST typesafe`() {
        // given
        val id = Random.nextBytes(1).toHexString()
        val name = "R2-$id"

        mokksy
            .post<Input>(name = "post example") {
                path("/things-$seed")
                bodyMatchesPredicate("body should match Input") {
                    it?.name == name
                }
            } respondsWith {
            body = Output("Hello, $name!")
            httpStatus = HttpStatusCode.OK
            headers += "Foo" to "bar" // list style
        }

        // when
        val response =
            client.post("/things-$seed") {
                contentType(ContentType.Application.Json)
                setBody(Input(name))
            }

        // then
        response shouldNotBeNull {
            status shouldBe HttpStatusCode.OK
            headers["Foo"] shouldBe "bar"
            body<Output>() shouldNotBeNull {
                result shouldBe "Hello, $name!"
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"])
    suspend fun `Should respond to reified method shortcut`(methodName: String) {
        val method = HttpMethod.parse(methodName)
        val path = "/reified-$method-$seed"
        val requestAsString = Json.encodeToString(requestPayload)
        val expectedResponseRef = AtomicReference<String>()
        val capturedError = AtomicReference<Throwable?>()

        val stub =
            when (method) {
                HttpMethod.Get -> {
                    mokksy.get<TestPerson>(name) {
                        path(path)
                        containsHeader(
                            "X-Seed",
                            "$seed",
                        )
                    }
                }

                HttpMethod.Post -> {
                    mokksy.post<TestPerson>(name) {
                        path(path)
                        containsHeader(
                            "X-Seed",
                            "$seed",
                        )
                    }
                }

                HttpMethod.Put -> {
                    mokksy.put<TestPerson>(name) {
                        path(path)
                        containsHeader(
                            "X-Seed",
                            "$seed",
                        )
                    }
                }

                HttpMethod.Patch -> {
                    mokksy.patch<TestPerson>(name) {
                        path(path)
                        containsHeader(
                            "X-Seed",
                            "$seed",
                        )
                    }
                }

                HttpMethod.Delete -> {
                    mokksy.delete<TestPerson>(name) {
                        path(path)
                        containsHeader(
                            "X-Seed",
                            "$seed",
                        )
                    }
                }

                HttpMethod.Head -> {
                    mokksy.head<TestPerson>(name) {
                        path(path)
                        containsHeader(
                            "X-Seed",
                            "$seed",
                        )
                    }
                }

                HttpMethod.Options -> {
                    mokksy.options<TestPerson>(name) {
                        path(path)
                        containsHeader(
                            "X-Seed",
                            "$seed",
                        )
                    }
                }

                else -> {
                    error("Unexpected method: $method")
                }
            }

        stub respondsWith {
            try {
                request.body() shouldBe requestPayload
            } catch (e: AssertionError) {
                capturedError.set(e)
            }
            val responsePayload = TestOrder.random(person = requestPayload)
            body = Json.encodeToString(responsePayload)
            expectedResponseRef.set(body)
        }

        val result =
            client.request(path) {
                this.method = method
                headers.append("X-Seed", "$seed")
                contentType(ContentType.Application.Json)
                setBody(requestAsString)
            }

        capturedError.get()?.let { throw it }
        result.status shouldBe HttpStatusCode.OK
        if (method != HttpMethod.Head) {
            result.bodyAsText() shouldBe expectedResponseRef.get()
        }
    }

    @Test
    suspend fun `Should respond to reified method extension`() {
        val path = "/reified-method-ext-$seed"

        mokksy.method<Input>(name, HttpMethod.Put) {
            path(path)
            bodyMatchesPredicate { it?.name == name }
        } respondsWith {
            body = Output("method-put: $name")
        }

        val result =
            client.request(path) {
                method = HttpMethod.Put
                contentType(ContentType.Application.Json)
                setBody(Input(name))
            }

        result.status shouldBe HttpStatusCode.OK
        result.body<Output>() shouldBe Output("method-put: $name")
    }

    @Test
    suspend fun `Should respond using reified post with StubConfiguration`() {
        val path = "/stub-config-$seed"

        mokksy.post<Input>(StubConfiguration(name = "stub-$seed")) {
            path(path)
            bodyMatchesPredicate { it?.name == name }
        } respondsWith {
            body = Output("configured: $name")
            httpStatus = HttpStatusCode.Created
        }

        val result =
            client.post(path) {
                contentType(ContentType.Application.Json)
                setBody(Input(name))
            }

        result.status shouldBe HttpStatusCode.Created
        result.body<Output>() shouldBe Output("configured: $name")
    }

    @Test
    suspend fun `Should return 404 when typed body predicate does not match`() {
        val path = "/predicate-no-match-$seed"

        mokksy.post<Input>(name = "predicate-no-match") {
            path(path)
            bodyMatchesPredicate("name must equal expected") { it?.name == name }
        } respondsWith {
            body = Output("should not be returned")
        }

        val result =
            client.post(path) {
                contentType(ContentType.Application.Json)
                setBody(Input("different-$name"))
            }

        result.status shouldBe HttpStatusCode.NotFound
    }

    @Test
    suspend fun `Should match only when all typed body predicates pass`() {
        val path = "/multi-predicate-$seed"
        val expectedName = name

        mokksy.post<Input>(name = "multi-predicate") {
            path(path)
            bodyMatchesPredicates(
                { it?.name == expectedName },
                { it?.name?.isNotBlank() == true },
            )
        } respondsWith {
            body = Output("all matched")
        }

        client
            .post(path) {
                contentType(ContentType.Application.Json)
                setBody(Input(expectedName))
            }.status shouldBe HttpStatusCode.OK

        client
            .post(path) {
                contentType(ContentType.Application.Json)
                setBody(Input("wrong-$expectedName"))
            }.status shouldBe HttpStatusCode.NotFound
    }

    @Test
    suspend fun `Should use typed request body to build dynamic response`() {
        val path = "/dynamic-response-$seed"

        mokksy.post<Input>(name = "dynamic-response") {
            path(path)
            bodyMatchesPredicate { it?.name?.isNotBlank() == true }
        } respondsWith {
            val req = request.body()
            body = Output("Hello, ${req.name}!")
        }

        val result =
            client.post(path) {
                contentType(ContentType.Application.Json)
                setBody(Input(name))
            }

        result.status shouldBe HttpStatusCode.OK
        result.body<Output>() shouldBe Output("Hello, $name!")
    }
}
