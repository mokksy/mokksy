package dev.mokksy.mokksy

import io.kotest.matchers.shouldBe
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.append
import io.ktor.client.request.forms.formData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import kotlinx.io.writeString
import org.junit.jupiter.api.Test

internal class FileMatchingIT : AbstractIT() {
    @Test
    suspend fun `should match multipart file`() {
        val path = "/file-upload-$seed"

        mokksy.post {
            path(path)
            body {
                form {
                    field("description", "Mokksy upload")
                    file("avatar") {}
                }
            }
        } respondsWith {
            body = "file-upload-ok"
        }

        val result =
            client.post(path) {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("description", "Mokksy upload")
                            append("avatar", "photo.png", ContentType.Image.PNG) {
                                writeString("payload")
                            }
                        },
                    ),
                )
            }

        result.status shouldBe HttpStatusCode.OK
        result.bodyAsText() shouldBe "file-upload-ok"
    }

    @Test
    suspend fun `should fail when file metadata does not match`() {
        val path = "/file-metadata-mismatch-$seed"

        mokksy.post {
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
            body = "file-metadata-mismatch-ok"
        }

        val result =
            client.post(path) {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("avatar", "actual.png", ContentType.Image.PNG) { }
                        },
                    ),
                )
            }

        result.status shouldBe HttpStatusCode.NotFound
    }

    @Test
    suspend fun `should fail when named file is missing`() {
        val path = "/file-name-mismatch-$seed"

        mokksy.post {
            path(path)
            body {
                form {
                    file("avatar") {}
                }
            }
        } respondsWith {
            body = "file-name-mismatch-ok"
        }

        val result =
            client.post(path) {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("document", "photo.png", ContentType.Image.PNG) {
                                writeString("payload")
                            }
                        },
                    ),
                )
            }

        result.status shouldBe HttpStatusCode.NotFound
    }
}
