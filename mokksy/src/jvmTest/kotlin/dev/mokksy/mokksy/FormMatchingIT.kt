package dev.mokksy.mokksy

import io.kotest.matchers.shouldBe
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.parameters
import org.junit.jupiter.api.Test

internal class FormMatchingIT : AbstractIT() {
    @Test
    suspend fun `should match url encoded form fields`() {
        val path = "/form-url-encoded-$seed"

        mokksy.post {
            path(path)
            body {
                form {
                    field("username", "JetBrains")
                    field("email") { it?.endsWith("@jetbrains.com") == true }
                }
            }
        } respondsWith {
            body = "form-url-encoded-ok"
        }

        val result =
            client.submitForm(
                url = path,
                formParameters =
                    parameters {
                        append("username", "JetBrains")
                        append("email", "example@jetbrains.com")
                    },
            )

        result.status shouldBe HttpStatusCode.OK
        result.bodyAsText() shouldBe "form-url-encoded-ok"
    }

    @Test
    suspend fun `should match multipart form fields`() {
        val path = "/form-multipart-fields-$seed"

        mokksy.post {
            path(path)
            body {
                form {
                    field("locale", "test")
                    field("code", "123")
                }
            }
        } respondsWith {
            body = "form-multipart-fields-ok"
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
        result.bodyAsText() shouldBe "form-multipart-fields-ok"
    }

    @Test
    suspend fun `should prefer more specific form stub`() {
        val path = "/form-specificity-$seed"

        mokksy.post {
            path(path)
            body {
                form {
                    field("role", "admin")
                    field("token", "secret")
                }
            }
        } respondsWith {
            body = "specific-form-ok"
        }

        mokksy.post {
            path(path)
            body {
                form {
                    field("role", "admin")
                }
            }
        } respondsWith {
            body = "default-form-ok"
        }

        val result =
            client.submitForm(
                url = path,
                formParameters =
                    parameters {
                        append("role", "admin")
                        append("token", "secret")
                    },
            )

        result.status shouldBe HttpStatusCode.OK
        result.bodyAsText() shouldBe "specific-form-ok"
    }

    @Test
    suspend fun `should not match plain body as form`() {
        val path = "/form-not-body-$seed"

        mokksy.post {
            path(path)
            body {
                form {
                    field("locale", "test")
                }
            }
        } respondsWith {
            body = "plain-body-ok"
        }

        val result = client.get(path)

        result.status shouldBe HttpStatusCode.NotFound
    }
}
