@file:OptIn(dev.mokksy.mokksy.ExperimentalMokksyApi::class)

package dev.mokksy.it

import dev.mokksy.mokksy.StubConfiguration
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlin.test.Test

internal class StubSelectionIT : MokksyIntegrationTest() {
    @Test
    fun `removeAfterMatch returns 404 on second request`() =
        runIntegrationTest {
            mokksy.get(StubConfiguration("once-only", eventuallyRemove = true)) {
                path("/once")
            } respondsWith { body = "First!" }

            val first = client.get(mokksy.baseUrl() + "/once")
            val second = client.get(mokksy.baseUrl() + "/once")

            first.status shouldBe HttpStatusCode.OK
            second.status shouldBe HttpStatusCode.NotFound
        }

    @Test
    fun `higher priority value should win`() =
        runIntegrationTest {
            mokksy.get {
                path("/priority")
                priority(1)
            } respondsWith { body = "low-priority" }

            mokksy.get {
                path("/priority")
                priority(10)
            } respondsWith { body = "high-priority" }

            val response = client.get(mokksy.baseUrl() + "/priority")

            assertSoftly(response) {
                status shouldBe HttpStatusCode.OK
                bodyAsText() shouldBe "high-priority"
            }
        }

    @Test
    fun `default priority vs positive - positive should win`() =
        runIntegrationTest {
            mokksy.get { path("/priority-default-vs-positive") } respondsWith {
                body = "default-priority"
            }

            mokksy.get {
                path("/priority-default-vs-positive")
                priority(1)
            } respondsWith { body = "positive-priority" }

            val response = client.get(mokksy.baseUrl() + "/priority-default-vs-positive")

            response.bodyAsText() shouldBe "positive-priority"
        }

    @Test
    fun `default priority vs negative - default should win`() =
        runIntegrationTest {
            mokksy.get {
                path("/priority-default-vs-negative")
                priority(-1)
            } respondsWith { body = "negative-priority" }

            mokksy.get { path("/priority-default-vs-negative") } respondsWith {
                body = "default-priority"
            }

            val response = client.get(mokksy.baseUrl() + "/priority-default-vs-negative")

            response.bodyAsText() shouldBe "default-priority"
        }

    @Test
    fun `catch-all fallback pattern with priority`() =
        runIntegrationTest {
            mokksy.post {
                path("/v1/chat/completions")
                priority(-1)
            } respondsWith {
                body = """{"error":"unsupported request"}"""
                httpStatus = HttpStatusCode.BadRequest
            }

            mokksy.post {
                path("/v1/chat/completions")
                bodyContains("gpt-4")
                priority(1)
            } respondsWith {
                body = """{"model":"gpt-4"}"""
            }

            val specificResponse =
                client.post(mokksy.baseUrl() + "/v1/chat/completions") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"model":"gpt-4"}""")
                }
            assertSoftly(specificResponse) {
                status shouldBe HttpStatusCode.OK
                bodyAsText() shouldBe """{"model":"gpt-4"}"""
            }

            val fallbackResponse =
                client.post(mokksy.baseUrl() + "/v1/chat/completions") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"model":"other"}""")
                }
            assertSoftly(fallbackResponse) {
                status shouldBe HttpStatusCode.BadRequest
                bodyAsText() shouldBe """{"error":"unsupported request"}"""
            }
        }
}
