@file:OptIn(dev.mokksy.mokksy.ExperimentalMokksyApi::class)

package dev.mokksy.it

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.shouldBe
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.options
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.measureTimedValue

internal class HttpMethodsIT : MokksyIntegrationTest() {
    @Test
    fun `GET returns 200 with body`() =
        runIntegrationTest {
            mokksy.get { path("/hello") } respondsWith { body = "Hello, World!" }

            val response = client.get(mokksy.baseUrl() + "/hello")

            assertSoftly(response) {
                status shouldBe HttpStatusCode.OK
                bodyAsText() shouldBe "Hello, World!"
            }
        }

    @Test
    fun `GET returns 404 when no stub matches`() =
        runIntegrationTest {
            val response = client.get(mokksy.baseUrl() + "/no-stub")

            response.status shouldBe HttpStatusCode.NotFound
        }

    @Test
    fun `PUT returns 200 with body`() =
        runIntegrationTest {
            mokksy.put { path("/put-test") } respondsWith { body = "put-ok" }

            val response = client.put(mokksy.baseUrl() + "/put-test")

            assertSoftly(response) {
                status shouldBe HttpStatusCode.OK
                bodyAsText() shouldBe "put-ok"
            }
        }

    @Test
    fun `DELETE returns 200 with body`() =
        runIntegrationTest {
            mokksy.delete { path("/delete-test") } respondsWith { body = "deleted" }

            val response = client.delete(mokksy.baseUrl() + "/delete-test")

            assertSoftly(response) {
                status shouldBe HttpStatusCode.OK
                bodyAsText() shouldBe "deleted"
            }
        }

    @Test
    fun `PATCH returns 200 with body`() =
        runIntegrationTest {
            mokksy.patch { path("/patch-test") } respondsWith { body = "patched" }

            val response = client.patch(mokksy.baseUrl() + "/patch-test")

            assertSoftly(response) {
                status shouldBe HttpStatusCode.OK
                bodyAsText() shouldBe "patched"
            }
        }

    @Test
    fun `HEAD returns 200 with empty body`() =
        runIntegrationTest {
            mokksy.head { path("/head-test") } respondsWith { body = "ignored-by-protocol" }

            val response = client.head(mokksy.baseUrl() + "/head-test")

            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText() shouldBe ""
        }

    @Test
    fun `OPTIONS returns 200 with body`() =
        runIntegrationTest {
            mokksy.options { path("/options-test") } respondsWith { body = "OK" }

            val response = client.options(mokksy.baseUrl() + "/options-test")

            assertSoftly(response) {
                status shouldBe HttpStatusCode.OK
                bodyAsText() shouldBe "OK"
            }
        }

    @Test
    fun `POST returns 201 with Location header`() =
        runIntegrationTest {
            mokksy.post { path("/items") } respondsWith {
                body = """{"id":"42"}"""
                httpStatus = HttpStatusCode.Created
                addHeader("Location", "/items/42")
            }

            val response =
                client.post(mokksy.baseUrl() + "/items") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"name":"widget"}""")
                }

            assertSoftly(response) {
                status shouldBe HttpStatusCode.Created
                bodyAsText() shouldBe """{"id":"42"}"""
                headers["Location"] shouldBe "/items/42"
            }
        }

    @Test
    fun `respondsWithStatus returns status code and empty body`() =
        runIntegrationTest {
            mokksy.get { path("/status-only") } respondsWithStatus HttpStatusCode.NoContent

            val response = client.get(mokksy.baseUrl() + "/status-only")

            response.status shouldBe HttpStatusCode.NoContent
            response.bodyAsText() shouldBe ""
        }

    @Test
    fun `GET with delay should delay response`() =
        runIntegrationTest {
            mokksy.get { path("/delayed") } respondsWith {
                body = "ok"
                delay = 200.milliseconds
            }

            val (response, elapsed) =
                measureTimedValue {
                    client.get(mokksy.baseUrl() + "/delayed")
                }

            response.status shouldBe HttpStatusCode.OK
            elapsed shouldBeGreaterThanOrEqualTo 200.milliseconds
        }
}
