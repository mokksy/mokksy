@file:OptIn(ExperimentalMokksyApi::class)

package dev.mokksy.mokksy

import io.kotest.matchers.equals.beEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.startWith
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Test

internal class FormDataFieldMatchingIT : AbstractIT() {
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
            body = "multi-field-ok"
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
        result.bodyAsText() shouldBe "multi-field-ok"
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
            body = "missing-field-ok"
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
            body = "wrong-value-ok"
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
            body = "field-dsl-ok"
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
        result.bodyAsText() shouldBe "field-dsl-ok"
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
            body = "field-dsl-fail-ok"
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
                    !it.isNullOrEmpty()
                }
            }
        } respondsWith {
            body = "body-predicate-ok"
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
        result.bodyAsText() shouldBe "body-predicate-ok"
    }
}
