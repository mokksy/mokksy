@file:OptIn(InternalMokksyApi::class)

package dev.mokksy.mokksy.response

import dev.mokksy.mokksy.InternalMokksyApi
import dev.mokksy.mokksy.request.CapturedRequest
import dev.mokksy.mokksy.utils.logger.HttpFormatter
import io.kotest.matchers.shouldBe
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.ResponseHeaders
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.testing.testApplication
import kotlin.test.Test

class AbstractResponseDefinitionBuilderTest {
    private val formatter = HttpFormatter()

    private fun collectHeaders(headersBlock: (ResponseHeaders.() -> Unit)?): List<String> {
        val result = mutableListOf<String>()
        headersBlock?.invoke(
            object : ResponseHeaders() {
                override fun engineAppendHeader(name: String, value: String) {
                    result.add("$name=$value")
                }

                override fun getEngineHeaderNames(): List<String> = emptyList()

                override fun getEngineHeaderValues(name: String): List<String> = emptyList()
            },
        )
        return result
    }

    // region addHeader

    @Test
    fun `addHeader causes built response to have non-null headers`() =
        testApplication {
            routing {
                post("/test") {
                    val builder =
                        ResponseDefinitionBuilder<String, String>(
                            request = CapturedRequest(call.request, String::class),
                            formatter = formatter,
                        )
                    builder.addHeader("X-Custom", "value-1")
                    val definition = builder.build()
                    call.respondText(collectHeaders(definition.headers).joinToString(","))
                }
            }

            val response = client.post("/test") { setBody("") }
            response.bodyAsText() shouldBe "X-Custom=value-1"
        }

    @Test
    fun `addHeader multiple times accumulates all headers`() =
        testApplication {
            routing {
                post("/test") {
                    val builder =
                        ResponseDefinitionBuilder<String, String>(
                            request = CapturedRequest(call.request, String::class),
                            formatter = formatter,
                        )
                    builder.addHeader("X-One", "1")
                    builder.addHeader("X-Two", "2")

                    val definition = builder.build()
                    call.respondText(collectHeaders(definition.headers).sorted().joinToString(","))
                }
            }

            val response = client.post("/test") { setBody("") }
            response.bodyAsText() shouldBe "X-One=1,X-Two=2"
        }

    // endregion

    // region headers += shorthand

    @Test
    fun `headers plusAssign adds header pair included in built response`() =
        testApplication {
            routing {
                post("/test") {
                    val builder =
                        ResponseDefinitionBuilder<String, String>(
                            request = CapturedRequest(call.request, String::class),
                            formatter = formatter,
                        )
                    builder.headers += "Foo" to "bar"

                    val definition = builder.build()
                    call.respondText(collectHeaders(definition.headers).joinToString(","))
                }
            }

            val response = client.post("/test") { setBody("") }
            response.bodyAsText() shouldBe "Foo=bar"
        }

    // endregion

    // region headers lambda

    @Test
    fun `headers lambda is included in built response`() =
        testApplication {
            routing {
                post("/test") {
                    val builder =
                        ResponseDefinitionBuilder<String, String>(
                            request = CapturedRequest(call.request, String::class),
                            formatter = formatter,
                        )
                    builder.headers {
                        append("X-Lambda", "yes")
                    }

                    val definition = builder.build()
                    call.respondText(collectHeaders(definition.headers).joinToString(","))
                }
            }

            val response = client.post("/test") { setBody("") }
            response.bodyAsText() shouldBe "X-Lambda=yes"
        }

    @Test
    fun `multiple headers lambda calls are accumulated in built response`() =
        testApplication {
            routing {
                post("/test") {
                    val builder =
                        ResponseDefinitionBuilder<String, String>(
                            request = CapturedRequest(call.request, String::class),
                            formatter = formatter,
                        )
                    builder.headers {
                        append("X-First", "1")
                    }
                    builder.headers {
                        append("X-Second", "2")
                    }

                    val definition = builder.build()
                    call.respondText(collectHeaders(definition.headers).joinToString(","))
                }
            }

            val response = client.post("/test") { setBody("") }
            response.bodyAsText() shouldBe "X-First=1,X-Second=2"
        }

    // endregion

    // region build with no headers

    @Test
    fun `build with no headers produces null headers in definition`() =
        testApplication {
            routing {
                post("/test") {
                    val builder =
                        ResponseDefinitionBuilder<String, String>(
                            request = CapturedRequest(call.request, String::class),
                            formatter = formatter,
                        )

                    val definition = builder.build()
                    call.respondText("${definition.headers == null}")
                }
            }

            val response = client.post("/test") { setBody("") }
            response.bodyAsText() shouldBe "true"
        }

    // endregion

    // region body() fluent method

    @Test
    fun `body() sets body and returns builder for chaining`() =
        testApplication {
            routing {
                post("/test") {
                    val builder =
                        ResponseDefinitionBuilder<String, String>(
                            request = CapturedRequest(call.request, String::class),
                            formatter = formatter,
                        )
                    val returned = builder.body("fluent-body")

                    call.respondText("${returned === builder}|${builder.body}")
                }
            }

            val response = client.post("/test") { setBody("") }
            response.bodyAsText() shouldBe "true|fluent-body"
        }

    @Test
    fun `status() sets http status code and returns builder for chaining`() =
        testApplication {
            routing {
                post("/test") {
                    val builder =
                        ResponseDefinitionBuilder<String, String>(
                            request = CapturedRequest(call.request, String::class),
                            formatter = formatter,
                        )
                    val returned = builder.status(404)

                    call.respondText("${returned === builder}|${builder.httpStatus.value}")
                }
            }

            val response = client.post("/test") { setBody("") }
            response.bodyAsText() shouldBe "true|404"
        }

    @Test
    fun `header() adds a header and returns builder for chaining`() =
        testApplication {
            routing {
                post("/test") {
                    val builder =
                        ResponseDefinitionBuilder<String, String>(
                            request = CapturedRequest(call.request, String::class),
                            formatter = formatter,
                        )
                    val returned = builder.header("X-Result", "ok")
                    val definition = builder.build()
                    call.respondText("${returned === builder}|${collectHeaders(definition.headers).joinToString()}")
                }
            }

            val response = client.post("/test") { setBody("") }
            response.bodyAsText() shouldBe "true|X-Result=ok"
        }

    // endregion

    // region contentType and body propagated through build

    @Test
    fun `build propagates body and contentType to ResponseDefinition`() =
        testApplication {
            routing {
                post("/test") {
                    val builder =
                        ResponseDefinitionBuilder<String, String>(
                            request = CapturedRequest(call.request, String::class),
                            formatter = formatter,
                        )
                    builder.body = "response body"
                    builder.contentType = ContentType.Text.Plain

                    val definition = builder.build()
                    call.respondText("${definition.body}|${definition.contentType}")
                }
            }

            val response = client.post("/test") { setBody("") }
            response.bodyAsText() shouldBe "response body|text/plain"
        }

    // endregion

    // region httpStatus setter

    @Test
    fun `httpStatus setter updates httpStatus`() =
        testApplication {
            routing {
                post("/test") {
                    val builder =
                        ResponseDefinitionBuilder<String, String>(
                            request = CapturedRequest(call.request, String::class),
                            formatter = formatter,
                        )
                    builder.httpStatus = HttpStatusCode.NotFound

                    call.respondText("${builder.httpStatus.value}")
                }
            }

            val response = client.post("/test") { setBody("") }
            response.bodyAsText() shouldBe "404"
        }

    // endregion

    // region withResponseBody

    @Test
    fun `withResponseBody sets body when responseBody is null`() =
        testApplication {
            routing {
                post("/test") {
                    val definition = ResponseDefinitionBuilder<String, String>(
                        request = CapturedRequest(call.request, String::class),
                        formatter = formatter,
                    ).build()
                    definition.withResponseBody { "initial" }
                    call.respondText(definition.responseBody ?: "null")
                }
            }

            val response = client.post("/test") { setBody("") }
            response.bodyAsText() shouldBe "initial"
        }

    @Test
    fun `withResponseBody transforms existing responseBody`() =
        testApplication {
            routing {
                post("/test") {
                    val definition = ResponseDefinitionBuilder<String, String>(
                        request = CapturedRequest(call.request, String::class),
                        formatter = formatter,
                    ).build()
                    definition.responseBody = "hello"
                    definition.withResponseBody { this?.uppercase() }
                    call.respondText(definition.responseBody ?: "null")
                }
            }

            val response = client.post("/test") { setBody("") }
            response.bodyAsText() shouldBe "HELLO"
        }

    @Test
    fun `withResponseBody can clear responseBody to null`() =
        testApplication {
            routing {
                post("/test") {
                    val definition = ResponseDefinitionBuilder<String, String>(
                        request = CapturedRequest(call.request, String::class),
                        formatter = formatter,
                    ).build()
                    definition.responseBody = "something"
                    definition.withResponseBody { null }
                    call.respondText(definition.responseBody ?: "null")
                }
            }

            val response = client.post("/test") { setBody("") }
            response.bodyAsText() shouldBe "null"
        }

    // endregion
}
