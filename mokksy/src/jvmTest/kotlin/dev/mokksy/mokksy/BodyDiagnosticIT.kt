package dev.mokksy.mokksy

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.parameters
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test

internal class BodyDiagnosticIT : AbstractIT() {
    @Test
    suspend fun `diagnostic response for url-encoded form mismatch`() {
        val path = "/form-url-mismatch-$seed"

        mokksy.post(StubConfiguration(name = "form-stub")) {
            path(path)
            body {
                form {
                    field("username", "expected")
                }
            }
        } respondsWith {
            body = "ok"
        }

        val result =
            client.submitForm(
                url = path,
                formParameters = parameters { append("username", "actual") },
            )

        result.status shouldBe HttpStatusCode.NotFound
        val json = Json.parseToJsonElement(result.bodyAsText()).jsonObject
        json["request"]!!.jsonObject["method"]!!.jsonPrimitive.content shouldBe "POST"
        json["closestStub"]!!.jsonArray.filter {
            it.jsonObject["name"]!!.jsonPrimitive.content == "form-stub"
        } shouldHaveSize 1
    }

    @Test
    suspend fun `diagnostic response prefers url-encoded form stub with partial part matches`() {
        val path = "/form-url-partial-score-$seed"

        mokksy.post(StubConfiguration(name = "partial-form-stub")) {
            path(path)
            body {
                form {
                    field("first", "one")
                    field("second", "two")
                    field("third", "expected")
                }
            }
        } respondsWith {
            body = "partial"
        }

        mokksy.post(StubConfiguration(name = "less-relevant-url-form-stub")) {
            path(path)
            queryParam("hint", "less")
            body {
                form {
                    field("missing", "value")
                }
            }
        } respondsWith {
            body = "less"
        }

        val result =
            client.submitForm(
                url = "$path?hint=less",
                formParameters =
                    parameters {
                        append("first", "one")
                        append("second", "two")
                        append("third", "actual")
                    },
            )

        result.status shouldBe HttpStatusCode.NotFound
        val json = Json.parseToJsonElement(result.bodyAsText()).jsonObject
        closestStubNames(json) shouldContainExactly listOf("partial-form-stub")
    }

    @Test
    suspend fun `diagnostic response for multipart form mismatch`() {
        val path = "/form-multipart-mismatch-$seed"

        mokksy.post(StubConfiguration(name = "multipart-form-stub")) {
            path(path)
            body {
                form {
                    field("locale", "expected")
                }
            }
        } respondsWith {
            body = "ok"
        }

        val result =
            client.post(path) {
                setBody(
                    MultiPartFormDataContent(
                        formData { append("locale", "actual") },
                    ),
                )
            }

        result.status shouldBe HttpStatusCode.NotFound
        val json = Json.parseToJsonElement(result.bodyAsText()).jsonObject
        json["request"]!!.jsonObject["method"]!!.jsonPrimitive.content shouldBe "POST"
        json["closestStub"]!!.jsonArray.filter {
            it.jsonObject["name"]!!.jsonPrimitive.content == "multipart-form-stub"
        } shouldHaveSize 1
    }

    @Test
    suspend fun `diagnostic response prefers multipart form stub with partial part matches`() {
        val path = "/form-multipart-partial-score-$seed"

        mokksy.post(StubConfiguration(name = "partial-multipart-form-stub")) {
            path(path)
            body {
                form {
                    field("first", "one")
                    field("second", "two")
                    field("third", "expected")
                }
            }
        } respondsWith {
            body = "partial"
        }

        mokksy.post(StubConfiguration(name = "less-relevant-multipart-form-stub")) {
            path(path)
            queryParam("hint", "less")
            body {
                form {
                    field("missing", "value")
                }
            }
        } respondsWith {
            body = "less"
        }

        val result =
            client.post("$path?hint=less") {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("first", "one")
                            append("second", "two")
                            append("third", "actual")
                        },
                    ),
                )
            }

        result.status shouldBe HttpStatusCode.NotFound
        val json = Json.parseToJsonElement(result.bodyAsText()).jsonObject
        closestStubNames(json) shouldContainExactly listOf("partial-multipart-form-stub")
    }

    @Test
    suspend fun `diagnostic response for file mismatch`() {
        val path = "/file-mismatch-$seed"

        mokksy.post(StubConfiguration(name = "file-stub")) {
            path(path)
            body {
                form {
                    file("avatar") {
                        filename("expected.jpg")
                        contentType(ContentType.Image.JPEG)
                    }
                }
            }
        } respondsWith {
            body = "ok"
        }

        val result =
            client.post(path) {
                setBody(
                    MultiPartFormDataContent(
                        formData { append("avatar", "actual.png") },
                    ),
                )
            }

        result.status shouldBe HttpStatusCode.NotFound
        val json = Json.parseToJsonElement(result.bodyAsText()).jsonObject
        json["request"]!!.jsonObject["method"]!!.jsonPrimitive.content shouldBe "POST"
        json["closestStub"]!!.jsonArray.filter {
            it.jsonObject["name"]!!.jsonPrimitive.content == "file-stub"
        } shouldHaveSize 1
    }

    @Test
    suspend fun `diagnostic response for body byte mismatch`() {
        val path = "/bytes-mismatch-$seed"
        val payload = "expected-bytes".encodeToByteArray()

        mokksy.post(StubConfiguration(name = "bytes-stub")) {
            path(path)
            body {
                bytes(payload)
            }
        } respondsWith {
            body = "ok"
        }

        val result =
            client.post(path) {
                contentType(ContentType.Application.OctetStream)
                setBody("actual-bytes".encodeToByteArray())
            }

        result.status shouldBe HttpStatusCode.NotFound
        val json = Json.parseToJsonElement(result.bodyAsText()).jsonObject
        json["request"]!!.jsonObject["method"]!!.jsonPrimitive.content shouldBe "POST"
        json["closestStub"]!!.jsonArray.filter {
            it.jsonObject["name"]!!.jsonPrimitive.content == "bytes-stub"
        } shouldHaveSize 1
    }

    @Test
    suspend fun `diagnostic response for multipart data mismatch`() {
        val path = "/multipart-data-mismatch-$seed"

        mokksy.post(StubConfiguration(name = "multipart-stub")) {
            path(path)
            body {
                multipart("multipart/mixed") {
                    part("part1") {
                        contentType("text/plain")
                        text { it == "expected" }
                    }
                }
            }
        } respondsWith {
            body = "ok"
        }

        val result =
            client.post(path) {
                setBody(
                    MultiPartFormDataContent(
                        formData { append("part1", "actual") },
                    ),
                )
            }

        result.status shouldBe HttpStatusCode.NotFound
        val json = Json.parseToJsonElement(result.bodyAsText()).jsonObject
        json["request"]!!.jsonObject["method"]!!.jsonPrimitive.content shouldBe "POST"
        json["closestStub"]!!.jsonArray.filter {
            it.jsonObject["name"]!!.jsonPrimitive.content == "multipart-stub"
        } shouldHaveSize 1
    }

    @Test
    suspend fun `diagnostic response prefers multipart data stub with partial part matches`() {
        val path = "/multipart-data-partial-score-$seed"
        val boundary = "PartialScoreBoundary"

        mokksy.post(StubConfiguration(name = "partial-multipart-stub")) {
            path(path)
            body {
                multipart("multipart/mixed") {
                    part("first") { text("one") }
                    part("second") { text("two") }
                    part("third") { text("expected") }
                }
            }
        } respondsWith {
            body = "partial"
        }

        mokksy.post(StubConfiguration(name = "less-relevant-multipart-data-stub")) {
            path(path)
            queryParam("hint", "less")
            body {
                multipart("multipart/mixed") {
                    part("missing") { text("value") }
                }
            }
        } respondsWith {
            body = "less"
        }

        val result =
            client.post("$path?hint=less") {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append(
                                "first",
                                "one".encodeToByteArray(),
                                Headers.build {
                                    append(
                                        HttpHeaders.ContentDisposition,
                                        "form-data; name=\"first\"",
                                    )
                                },
                            )
                            append(
                                "second",
                                "two".encodeToByteArray(),
                                Headers.build {
                                    append(
                                        HttpHeaders.ContentDisposition,
                                        "form-data; name=\"second\"",
                                    )
                                },
                            )
                            append(
                                "third",
                                "actual".encodeToByteArray(),
                                Headers.build {
                                    append(
                                        HttpHeaders.ContentDisposition,
                                        "form-data; name=\"third\"",
                                    )
                                },
                            )
                        },
                        boundary = boundary,
                        contentType =
                            ContentType.MultiPart.Mixed.withParameter(
                                "boundary",
                                boundary,
                            ),
                    ),
                )
            }

        result.status shouldBe HttpStatusCode.NotFound
        val json = Json.parseToJsonElement(result.bodyAsText()).jsonObject
        closestStubNames(json) shouldContainExactly listOf("partial-multipart-stub")
    }

    @Test
    suspend fun `diagnostic response for query parameter mismatch`() {
        val path = "/query-param-mismatch-$seed"

        mokksy.get(StubConfiguration(name = "query-stub")) {
            path(path)
            queryParam("q", "expected")
        } respondsWith {
            body = "ok"
        }

        val result = client.get("$path?q=actual")

        result.status shouldBe HttpStatusCode.NotFound
        val json = Json.parseToJsonElement(result.bodyAsText()).jsonObject
        json["request"]!!.jsonObject["method"]!!.jsonPrimitive.content shouldBe "GET"
        json["closestStub"]!!.jsonArray.filter {
            it.jsonObject["name"]!!.jsonPrimitive.content == "query-stub"
        } shouldHaveSize 1
    }

    private fun closestStubNames(json: kotlinx.serialization.json.JsonObject): List<String> =
        json["closestStub"]!!
            .jsonArray
            .map { it.jsonObject["name"]!!.jsonPrimitive.content }
}
