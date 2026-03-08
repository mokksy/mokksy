package dev.mokksy.mokksy

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

/**
 * Verifies that when multiple stubs match a request, the most specific stub wins —
 * i.e. the one with the highest total matcher score — regardless of registration order.
 */
internal class StubSpecificityIT : AbstractIT() {
    @Test
    suspend fun `specific stub wins over generic stub when registered first`() {
        val path = "/users-$seed"

        mokksy.post {
            path(path)
            bodyContains("admin")
        } respondsWith {
            body = "specific"
        }

        mokksy.post {
            path(path)
        } respondsWith {
            body = "generic"
        }

        val result =
            client.post(path) {
                contentType(ContentType.Text.Plain)
                setBody("""{"role":"admin"}""")
            }

        assertSoftly(result) {
            status shouldBe HttpStatusCode.OK
            bodyAsText() shouldBe "specific"
        }
    }

    @Test
    suspend fun `specific stub wins over generic stub when registered second`() {
        val path = "/users-$seed"

        mokksy.post {
            path(path)
        } respondsWith {
            body = "generic"
        }

        mokksy.post {
            path(path)
            bodyContains("admin")
        } respondsWith {
            body = "specific"
        }

        val result =
            client.post(path) {
                contentType(ContentType.Text.Plain)
                setBody("""{"role":"admin"}""")
            }

        assertSoftly(result) {
            status shouldBe HttpStatusCode.OK
            bodyAsText() shouldBe "specific"
        }
    }

    @Test
    suspend fun `generic stub matches when body does not satisfy specific stub condition`() {
        val path = "/users-$seed"

        mokksy.post {
            path(path)
            bodyContains("admin")
        } respondsWith {
            body = "specific"
        }

        mokksy.post {
            path(path)
        } respondsWith {
            body = "generic"
        }

        val result =
            client.post(path) {
                contentType(ContentType.Text.Plain)
                setBody("""{"role":"user"}""")
            }

        assertSoftly(result) {
            status shouldBe HttpStatusCode.OK
            bodyAsText() shouldBe "generic"
        }
    }

    @AfterEach
    fun afterEach() {
        mokksy.verifyNoUnexpectedRequests()
    }
}
