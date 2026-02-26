package dev.mokksy.mokksy.request

import dev.mokksy.mokksy.Input
import io.kotest.matchers.equals.beEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.contain
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.doublereceive.DoubleReceive
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlin.test.Test

internal class RequestSpecificationTest {

    private fun specTestApplication(block: suspend ApplicationTestBuilder.() -> Unit) =
        testApplication {
            install(DoubleReceive)
            install(ContentNegotiation) { json() }
            block()
        }

    private suspend fun RoutingContext.respondMatchResult(spec: RequestSpecification<*>) =
        call.respondText(spec.matches(call.request).getOrThrow().toString())

    @Test
    fun `should match when only headers are specified`() =
        specTestApplication {
            routing {
                post("/test") {
                    respondMatchResult(
                        RequestSpecification(
                            headers = listOf(containsHeader("X-Request-ID", "RequestID")),
                            requestType = Input::class,
                        ),
                    )
                }
            }

            val response =
                client.post("/test") {
                    header("X-Request-ID", "RequestID")
                }

            response.bodyAsText() shouldBe "true"
        }

    @Test
    fun mismatchedMethod() =
        specTestApplication {
            routing {
                get("/test") {
                    respondMatchResult(
                        RequestSpecification(
                            method = beEqual(HttpMethod.Post),
                            path = contain("test"),
                            requestType = Input::class,
                        ),
                    )
                }
            }

            val response = client.get("/test")

            response.bodyAsText() shouldBe "false"
        }

    @Test
    fun mismatchedPath() =
        specTestApplication {
            routing {
                get("/other") {
                    respondMatchResult(
                        RequestSpecification(
                            method = beEqual(HttpMethod.Get),
                            path = contain("test"),
                            requestType = Input::class,
                        ),
                    )
                }
            }

            val response = client.get("/other")

            response.bodyAsText() shouldBe "false"
        }

    @Test
    fun mismatchedHeaders() =
        specTestApplication {
            routing {
                post("/test") {
                    respondMatchResult(
                        RequestSpecification(
                            headers = listOf(containsHeader("X-Request-ID", "RequestID")),
                            requestType = Input::class,
                        ),
                    )
                }
            }

            val response =
                client.post("/test") {
                    header("X-Request-ID", "WrongID")
                }

            response.bodyAsText() shouldBe "false"
        }

    @Test
    fun `should not match when bodyString differs`() =
        specTestApplication {
            routing {
                post("/test") {
                    respondMatchResult(
                        RequestSpecification(
                            bodyString = listOf(contain("expectedBody")),
                            requestType = String::class,
                        ),
                    )
                }
            }

            val response =
                client.post("/test") {
                    contentType(ContentType.Text.Plain)
                    setBody("Another body")
                }

            response.bodyAsText() shouldBe "false"
        }

    @Test
    fun `should not match when body differs`() =
        specTestApplication {
            routing {
                post("/test") {
                    respondMatchResult(
                        RequestSpecification(
                            body = listOf(beEqual(Input("Bob"))),
                            requestType = Input::class,
                        ),
                    )
                }
            }

            val response =
                client.post("/test") {
                    contentType(ContentType.Application.Json)
                    setBody(Json.encodeToString(Input("Alice")))
                }

            response.bodyAsText() shouldBe "false"
        }

    @Test
    fun `should match when all conditions are satisfied`() =
        specTestApplication {
            routing {
                post("/test") {
                    respondMatchResult(
                        RequestSpecification(
                            method = beEqual(HttpMethod.Post),
                            path = contain("test"),
                            headers = listOf(containsHeader("X-Request-ID", "RequestID")),
                            body = listOf(beEqual(Input("Alice"))),
                            bodyString = listOf(contain("Alice")),
                            requestType = Input::class,
                        ),
                    )
                }
            }

            val response =
                client.post("/test") {
                    header("X-Request-ID", "RequestID")
                    contentType(ContentType.Application.Json)
                    setBody(Json.encodeToString(Input("Alice")))
                }

            response.bodyAsText() shouldBe "true"
        }
}
