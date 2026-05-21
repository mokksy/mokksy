@file:OptIn(ExperimentalMokksyApi::class)

package dev.mokksy.mokksy

import io.kotest.matchers.equals.beEqual
import io.kotest.matchers.shouldBe
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.append
import io.ktor.client.request.forms.formData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.io.writeString
import org.junit.jupiter.api.Test

internal class FormDataFileMatchingIT : AbstractIT() {
    @Test
    suspend fun `should match file part by filename`() {
        val path = "/file-filename-$seed"

        mokksy.post {
            path(path)
            body {
                formData {
                    file("avatar") {
                        filename(beEqual("photo.jpg"))
                    }
                }
            }
        } respondsWith {
            body = "file-filename-ok"
        }

        val result =
            client.post(path) {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("avatar", "photo.jpg", ContentType.Image.JPEG) { }
                        },
                    ),
                )
            }

        result.status shouldBe HttpStatusCode.OK
        result.bodyAsText() shouldBe "file-filename-ok"
    }

    @Test
    suspend fun `should fail when file part filename does not match`() {
        val path = "/file-filename-mismatch-$seed"

        mokksy.post {
            path(path)
            body {
                formData {
                    file("avatar") {
                        filename(beEqual("expected.jpg"))
                    }
                }
            }
        } respondsWith {
            body = "file-filename-mismatch-ok"
        }

        val result =
            client.post(path) {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("avatar", "actual.jpg", ContentType.Image.JPEG) { }
                        },
                    ),
                )
            }

        result.status shouldBe HttpStatusCode.NotFound
    }

    @Test
    suspend fun `should match file part by content type`() {
        val path = "/file-content-type-$seed"

        mokksy.post {
            path(path)
            body {
                formData {
                    file("avatar") {
                        contentType(beEqual(ContentType.Image.JPEG))
                    }
                }
            }
        } respondsWith {
            body = "file-content-type-ok"
        }

        val result =
            client.post(path) {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("avatar", "photo.jpg", ContentType.Image.JPEG) { }
                        },
                    ),
                )
            }

        result.status shouldBe HttpStatusCode.OK
        result.bodyAsText() shouldBe "file-content-type-ok"
    }

    @Test
    suspend fun `should match binary part by content type`() {
        val path = "/binary-content-type-$seed"

        mokksy.post {
            path(path)
            body {
                formData {
                    file("data") {
                        contentType(beEqual(ContentType.Application.OctetStream))
                    }
                }
            }
        } respondsWith {
            body = "binary-content-type-ok"
        }

        val result =
            client.post(path) {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append(
                                "data",
                                Headers.build {
                                    append(
                                        HttpHeaders.ContentDisposition,
                                        "form-data; name=\"data\"",
                                    )
                                    append(HttpHeaders.ContentType, "application/octet-stream")
                                },
                            ) {
                                writeString("binary-data")
                            }
                        },
                    ),
                )
            }

        result.status shouldBe HttpStatusCode.OK
        result.bodyAsText() shouldBe "binary-content-type-ok"
    }

    @Test
    suspend fun `should fail when binary part content type does not match`() {
        val path = "/binary-content-type-mismatch-$seed"

        mokksy.post {
            path(path)
            body {
                formData {
                    file("data") {
                        contentType(beEqual(ContentType.Application.Json))
                    }
                }
            }
        } respondsWith {
            body = "binary-content-type-mismatch-ok"
        }

        val result =
            client.post(path) {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append(
                                "data",
                                Headers.build {
                                    append(
                                        HttpHeaders.ContentDisposition,
                                        "form-data; name=\"data\"",
                                    )
                                    append(HttpHeaders.ContentType, "application/octet-stream")
                                },
                            ) {
                                writeString("binary-data")
                            }
                        },
                    ),
                )
            }

        result.status shouldBe HttpStatusCode.NotFound
    }

    @Test
    suspend fun `should match binary part body content`() {
        val path = "/binary-body-$seed"

        mokksy.post {
            path(path)
            body {
                formData {
                    file("payload") {
                        body(beEqual("hello"))
                    }
                }
            }
        } respondsWith {
            body = "binary-body-ok"
        }

        val result =
            client.post(path) {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append(
                                "payload",
                                Headers.build {
                                    append(
                                        HttpHeaders.ContentDisposition,
                                        "form-data; name=\"payload\"",
                                    )
                                    append(HttpHeaders.ContentType, "text/plain")
                                },
                            ) {
                                writeString("hello")
                            }
                        },
                    ),
                )
            }

        result.status shouldBe HttpStatusCode.OK
        result.bodyAsText() shouldBe "binary-body-ok"
    }

    @Test
    suspend fun `should fail when binary part body does not match`() {
        val path = "/binary-body-mismatch-$seed"

        mokksy.post {
            path(path)
            body {
                formData {
                    file("payload") {
                        body(beEqual("expected"))
                    }
                }
            }
        } respondsWith {
            body = "binary-body-mismatch-ok"
        }

        val result =
            client.post(path) {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append(
                                "payload",
                                Headers.build {
                                    append(
                                        HttpHeaders.ContentDisposition,
                                        "form-data; name=\"payload\"",
                                    )
                                    append(HttpHeaders.ContentType, "text/plain")
                                },
                            ) {
                                writeString("actual")
                            }
                        },
                    ),
                )
            }

        result.status shouldBe HttpStatusCode.NotFound
    }

    @Test
    suspend fun `should fail when binary part has filename spec`() {
        val path = "/binary-filename-$seed"

        mokksy.post {
            path(path)
            body {
                formData {
                    file("data") {
                        filename(beEqual("ignored"))
                    }
                }
            }
        } respondsWith {
            body = "binary-filename-ok"
        }

        val result =
            client.post(path) {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append(
                                "data",
                                Headers.build {
                                    append(
                                        HttpHeaders.ContentDisposition,
                                        "form-data; name=\"data\"",
                                    )
                                    append(HttpHeaders.ContentType, "application/octet-stream")
                                },
                            ) {
                                writeString("binary-data")
                            }
                        },
                    ),
                )
            }

        result.status shouldBe HttpStatusCode.NotFound
    }
}
