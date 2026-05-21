@file:OptIn(ExperimentalMokksyApi::class)

package dev.mokksy.mokksy

import io.kotest.matchers.equals.beEqual
import io.kotest.matchers.shouldBe
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Test

internal class FormDataMatchingIT : AbstractIT() {
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
            body = "not-multipart-ok"
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
            body = "specificity-admin-ok"
        }

        mokksy.post {
            path(path)
            body {
                formData {
                    field("role", beEqual("admin"))
                }
            }
        } respondsWith {
            body = "specificity-default-ok"
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
        result.bodyAsText() shouldBe "specificity-admin-ok"
    }
}
