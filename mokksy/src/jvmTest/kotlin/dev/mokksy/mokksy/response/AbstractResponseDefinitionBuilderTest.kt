package dev.mokksy.mokksy.response

import dev.mokksy.mokksy.request.CapturedRequest
import dev.mokksy.mokksy.utils.logger.HttpFormatter
import io.kotest.matchers.shouldBe
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

class AbstractResponseDefinitionBuilderTest {
    private val formatter = HttpFormatter()

    // region httpStatus(Int)

    @Test
    fun `httpStatus(Int) sets status code by integer value`() =
        testApplication {
            routing {
                post("/test") {
                    val builder =
                        ResponseDefinitionBuilder<String, String>(
                            request = CapturedRequest(call.request, String::class),
                            formatter = formatter,
                        )
                    builder.httpStatus(201)

                    val result =
                        "${builder.httpStatusCode}|${builder.httpStatus.value}"
                    call.respondText(result)
                }
            }

            val response = client.post("/test") { setBody("") }
            response.bodyAsText() shouldBe "201|201"
        }

    @Test
    fun `httpStatus(Int) overrides previously set HttpStatusCode`() =
        testApplication {
            routing {
                post("/test") {
                    val builder =
                        ResponseDefinitionBuilder<String, String>(
                            request = CapturedRequest(call.request, String::class),
                            formatter = formatter,
                        )
                    builder.httpStatus = HttpStatusCode.Accepted
                    builder.httpStatus(404)

                    call.respondText("${builder.httpStatusCode}")
                }
            }

            val response = client.post("/test") { setBody("") }
            response.bodyAsText() shouldBe "404"
        }

    // endregion

    // region delayMillis

    @Test
    fun `delayMillis sets delay as milliseconds Duration`() =
        testApplication {
            routing {
                post("/test") {
                    val builder =
                        ResponseDefinitionBuilder<String, String>(
                            request = CapturedRequest(call.request, String::class),
                            formatter = formatter,
                        )
                    builder.delayMillis(250)

                    call.respondText("${builder.delay}")
                }
            }

            val response = client.post("/test") { setBody("") }
            response.bodyAsText() shouldBe "250ms"
        }

    @Test
    fun `delayMillis of zero sets zero delay`() =
        testApplication {
            routing {
                post("/test") {
                    val builder =
                        ResponseDefinitionBuilder<String, String>(
                            request = CapturedRequest(call.request, String::class),
                            formatter = formatter,
                        )
                    builder.delay = 5.seconds
                    builder.delayMillis(0)

                    call.respondText("${builder.delay}")
                }
            }

            val response = client.post("/test") { setBody("") }
            response.bodyAsText() shouldBe "0s"
        }

    // endregion

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
                    val headers = mutableListOf<String>()
                    definition.headers?.invoke(
                        object : io.ktor.server.response.ResponseHeaders() {
                            override fun engineAppendHeader(
                                name: String,
                                value: String,
                            ) {
                                headers.add("$name=$value")
                            }

                            override fun getEngineHeaderNames(): List<String> = emptyList()

                            override fun getEngineHeaderValues(name: String): List<String> =
                                emptyList()
                        },
                    )
                    call.respondText(headers.joinToString(","))
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
                    val headers = mutableListOf<String>()
                    definition.headers?.invoke(
                        object : io.ktor.server.response.ResponseHeaders() {
                            override fun engineAppendHeader(
                                name: String,
                                value: String,
                            ) {
                                headers.add("$name=$value")
                            }

                            override fun getEngineHeaderNames(): List<String> = emptyList()

                            override fun getEngineHeaderValues(name: String): List<String> =
                                emptyList()
                        },
                    )
                    call.respondText(headers.sorted().joinToString(","))
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
                    val headers = mutableListOf<String>()
                    definition.headers?.invoke(
                        object : io.ktor.server.response.ResponseHeaders() {
                            override fun engineAppendHeader(
                                name: String,
                                value: String,
                            ) {
                                headers.add("$name=$value")
                            }

                            override fun getEngineHeaderNames(): List<String> = emptyList()

                            override fun getEngineHeaderValues(name: String): List<String> =
                                emptyList()
                        },
                    )
                    call.respondText(headers.joinToString(","))
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
                    val headers = mutableListOf<String>()
                    definition.headers?.invoke(
                        object : io.ktor.server.response.ResponseHeaders() {
                            override fun engineAppendHeader(
                                name: String,
                                value: String,
                            ) {
                                headers.add("$name=$value")
                            }

                            override fun getEngineHeaderNames(): List<String> = emptyList()

                            override fun getEngineHeaderValues(name: String): List<String> =
                                emptyList()
                        },
                    )
                    call.respondText(headers.joinToString(","))
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
                    val headers = mutableListOf<String>()
                    definition.headers?.invoke(
                        object : io.ktor.server.response.ResponseHeaders() {
                            override fun engineAppendHeader(
                                name: String,
                                value: String,
                            ) {
                                headers.add("$name=$value")
                            }

                            override fun getEngineHeaderNames(): List<String> = emptyList()

                            override fun getEngineHeaderValues(name: String): List<String> =
                                emptyList()
                        },
                    )
                    call.respondText(headers.joinToString(","))
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
    fun `httpStatus setter updates both httpStatus and httpStatusCode`() =
        testApplication {
            routing {
                post("/test") {
                    val builder =
                        ResponseDefinitionBuilder<String, String>(
                            request = CapturedRequest(call.request, String::class),
                            formatter = formatter,
                        )
                    builder.httpStatus = HttpStatusCode.NotFound

                    call.respondText("${builder.httpStatus.value}|${builder.httpStatusCode}")
                }
            }

            val response = client.post("/test") { setBody("") }
            response.bodyAsText() shouldBe "404|404"
        }

    // endregion
}
