@file:OptIn(ExperimentalMokksyApi::class)

package dev.mokksy.mokksy

import io.kotest.matchers.equals.beEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.startWith
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.append
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Test

internal class FormDataMatchingIT : AbstractIT() {
    @Test
    suspend fun `should match form-data fields`() {
        val path = "/form-data-$seed"

        mokksy.post {
            path(path)
            body {
                formData {
                    field("locale", beEqual("test"))
                }
            }
        } respondsWith {
            body = "OK"
            httpStatus = HttpStatusCode.OK
        }

        val result =
            client.post(path) {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("locale", "test")
                        },
                    ),
                )
            }

        result.status shouldBe HttpStatusCode.OK
        result.bodyAsText() shouldBe "OK"
    }

    @Test
    suspend fun `should match multiple form-data fields`() {
        val path = "/multi-field-$seed"

        mokksy.post {
            path(path)
            body {
                formData {
                    field("locale", beEqual("test"))
                    field("code", beEqual("123"))
                }
            }
        } respondsWith {
            body = "OK"
        }

        val result =
            client.post(path) {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("locale", "test")
                            append("code", "123")
                        },
                    ),
                )
            }

        result.status shouldBe HttpStatusCode.OK
    }

    @Test
    suspend fun `should fail when a form-data field is missing`() {
        val path = "/missing-field-$seed"

        mokksy.post {
            path(path)
            body {
                formData {
                    field("locale", beEqual("test"))
                    field("code", beEqual("123"))
                }
            }
        } respondsWith {
            body = "OK"
        }

        val result =
            client.post(path) {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("locale", "test")
                        },
                    ),
                )
            }

        result.status shouldBe HttpStatusCode.NotFound
    }

    @Test
    suspend fun `should fail when form-data field value does not match`() {
        val path = "/wrong-value-$seed"

        mokksy.post {
            path(path)
            body {
                formData {
                    field("locale", beEqual("expected"))
                }
            }
        } respondsWith {
            body = "OK"
        }

        val result =
            client.post(path) {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("locale", "wrong")
                        },
                    ),
                )
            }

        result.status shouldBe HttpStatusCode.NotFound
    }

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
            body = "OK"
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
            body = "OK"
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
            body = "OK"
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
    }

    @Test
    suspend fun `should not match when request is not multipart`() {
        val path = "/not-multipart-$seed"

        mokksy.post {
            path(path)
            body {
                formData {
                    field("locale", beEqual("test"))
                }
            }
        } respondsWith {
            body = "OK"
        }

        val result = client.get(path)

        result.status shouldBe HttpStatusCode.NotFound
    }

    @Test
    suspend fun `should prefer more specific form-data stub over less specific`() {
        val path = "/specificity-$seed"

        mokksy.post {
            path(path)
            body {
                formData {
                    field("role", beEqual("admin"))
                    field("token", beEqual("secret"))
                }
            }
        } respondsWith {
            body = "admin-ok"
        }

        mokksy.post {
            path(path)
            body {
                formData {
                    field("role", beEqual("admin"))
                }
            }
        } respondsWith {
            body = "default-ok"
        }

        val result =
            client.post(path) {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("role", "admin")
                            append("token", "secret")
                        },
                    ),
                )
            }

        result.status shouldBe HttpStatusCode.OK
        result.bodyAsText() shouldBe "admin-ok"
    }

    @Test
    suspend fun `should match field using DSL block builder`() {
        val path = "/field-dsl-block-$seed"

        mokksy.post {
            path(path)
            body {
                formData {
                    field("locale") {
                        body(startWith("te"))
                    }
                }
            }
        } respondsWith {
            body = "OK"
        }

        val result =
            client.post(path) {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("locale", "test")
                        },
                    ),
                )
            }

        result.status shouldBe HttpStatusCode.OK
        result.bodyAsText() shouldBe "OK"
    }

    @Test
    suspend fun `should fail when field DSL block matcher does not match`() {
        val path = "/field-dsl-block-fail-$seed"

        mokksy.post {
            path(path)
            body {
                formData {
                    field("locale") {
                        body(startWith("no"))
                    }
                }
            }
        } respondsWith {
            body = "OK"
        }

        val result =
            client.post(path) {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("locale", "test")
                        },
                    ),
                )
            }

        result.status shouldBe HttpStatusCode.NotFound
    }

    @Test
    suspend fun `should match body predicate inside body block`() {
        val path = "/body-predicate-$seed"

        mokksy.post<String> {
            path(path)
            body {
                formData {
                    field("key", beEqual("value"))
                }
                predicate("body is non-empty") {
                    it != null && it.isNotEmpty()
                }
            }
        } respondsWith {
            body = "OK"
        }

        val result =
            client.post(path) {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("key", "value")
                        },
                    ),
                )
            }

        result.status shouldBe HttpStatusCode.OK
        result.bodyAsText() shouldBe "OK"
    }
}
