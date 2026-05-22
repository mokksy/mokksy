package dev.mokksy.mokksy

import io.kotest.matchers.shouldBe
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import org.junit.jupiter.api.Test

internal class DataMatchingIT : AbstractIT() {
    @Test
    suspend fun `should match raw binary body`() {
        val path = "/raw-bytes-$seed"
        val payload = "binary-data".encodeToByteArray()

        mokksy.post {
            path(path)
            body {
                bytes(payload)
                contentType("application/octet-stream")
            }
        } respondsWith {
            body = "raw-bytes-ok"
        }

        val result =
            client.post(path) {
                contentType(ContentType.Application.OctetStream)
                setBody(payload)
            }

        result.status shouldBe HttpStatusCode.OK
        result.bodyAsText() shouldBe "raw-bytes-ok"
    }

    @Test
    suspend fun `should match custom multipart data parts`() {
        val path = "/multipart-mixed-$seed"

        mokksy.post {
            path(path)
            body {
                multipart("multipart/mixed") {
                    boundary("WebAppBoundary")
                    part("metadata") {
                        contentType("application/json")
                        text { it?.contains("Ktor logo") == true }
                    }
                    part("image") {
                        contentType("image/png")
                        bytes { it?.isNotEmpty() == true }
                    }
                }
            }
        } respondsWith {
            body = "multipart-mixed-ok"
        }

        val result =
            client.post(path) {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append(
                                "metadata",
                                """{"description":"Ktor logo"}""".encodeToByteArray(),
                                Headers.build {
                                    append(
                                        HttpHeaders.ContentDisposition,
                                        "form-data; name=\"metadata\"",
                                    )
                                    append(HttpHeaders.ContentType, "application/json")
                                },
                            )
                            append(
                                "image",
                                "png-data".encodeToByteArray(),
                                Headers.build {
                                    append(
                                        HttpHeaders.ContentDisposition,
                                        "form-data; name=\"image\"",
                                    )
                                    append(HttpHeaders.ContentType, "image/png")
                                },
                            )
                        },
                        boundary = "WebAppBoundary",
                        contentType =
                            ContentType.MultiPart.Mixed.withParameter(
                                "boundary",
                                "WebAppBoundary",
                            ),
                    ),
                )
            }

        result.status shouldBe HttpStatusCode.OK
        result.bodyAsText() shouldBe "multipart-mixed-ok"
    }

    @Test
    suspend fun `should fail when named multipart part is missing`() {
        val path = "/multipart-mixed-name-mismatch-$seed"

        mokksy.post {
            path(path)
            body {
                multipart("multipart/mixed") {
                    part("metadata") {
                        text("expected")
                    }
                }
            }
        } respondsWith {
            body = "multipart-mixed-name-mismatch-ok"
        }

        val result =
            client.post(path) {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append(
                                "other",
                                "expected".encodeToByteArray(),
                                Headers.build {
                                    append(
                                        HttpHeaders.ContentDisposition,
                                        "form-data; name=\"other\"",
                                    )
                                },
                            )
                        },
                        contentType = ContentType.MultiPart.Mixed,
                    ),
                )
            }

        result.status shouldBe HttpStatusCode.NotFound
    }
}
