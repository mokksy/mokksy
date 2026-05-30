@file:OptIn(dev.mokksy.mokksy.ExperimentalMokksyApi::class)

package dev.mokksy.it

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlin.test.Test

internal class RequestMatchingIT : MokksyIntegrationTest() {
    @Test
    fun `GET matches by header value`() =
        runIntegrationTest {
            mokksy.get {
                path("/secured")
                containsHeader("X-Api-Key", "secret")
            } respondsWith { body = "authorized" }

            val authorized =
                client.get(mokksy.baseUrl() + "/secured") {
                    header("X-Api-Key", "secret")
                }
            val unauthorized = client.get(mokksy.baseUrl() + "/secured")

            authorized.status shouldBe HttpStatusCode.OK
            unauthorized.status shouldBe HttpStatusCode.NotFound
        }

    @Test
    fun `GET matches by cookie predicate`() =
        runIntegrationTest {
            mokksy.get {
                path("/cookie-secured")
                cookie("session") { it?.startsWith("sess-") == true }
            } respondsWith { body = "cookie-authorized" }

            val authorized =
                client.get(mokksy.baseUrl() + "/cookie-secured") {
                    header("Cookie", "session=sess-abc")
                }
            val unauthorized =
                client.get(mokksy.baseUrl() + "/cookie-secured") {
                    header("Cookie", "session=wrong")
                }

            assertSoftly {
                authorized.status shouldBe HttpStatusCode.OK
                authorized.bodyAsText() shouldBe "cookie-authorized"
                unauthorized.status shouldBe HttpStatusCode.NotFound
            }
        }

    @Test
    fun `GET matches absent cookie`() =
        runIntegrationTest {
            mokksy.get {
                path("/cookie-absent")
                cookieAbsent("session")
            } respondsWith { body = "cookie-absent" }

            val matched = client.get(mokksy.baseUrl() + "/cookie-absent")
            val notMatched =
                client.get(mokksy.baseUrl() + "/cookie-absent") {
                    header("Cookie", "session=sess-abc")
                }

            assertSoftly {
                matched.status shouldBe HttpStatusCode.OK
                matched.bodyAsText() shouldBe "cookie-absent"
                notMatched.status shouldBe HttpStatusCode.NotFound
            }
        }

    @Test
    fun `POST matches by bodyContains and returns 404 when body does not match`() =
        runIntegrationTest {
            mokksy.post {
                path("/body-contains")
                bodyContains("expected-token")
            } respondsWith { body = "matched" }

            val matched =
                client.post(mokksy.baseUrl() + "/body-contains") {
                    setBody("""{"token": "expected-token"}""")
                }
            val notMatched =
                client.post(mokksy.baseUrl() + "/body-contains") {
                    setBody("""{"token": "other"}""")
                }

            matched.status shouldBe HttpStatusCode.OK
            notMatched.status shouldBe HttpStatusCode.NotFound
        }

    @Test
    fun `POST matches by body predicate`() =
        runIntegrationTest {
            mokksy.post {
                path("/body-predicate")
                bodyMatchesPredicate { it?.contains("match-me") == true }
            } respondsWith { body = "predicate-matched" }

            val matched =
                client.post(mokksy.baseUrl() + "/body-predicate") {
                    setBody("""{"value": "match-me"}""")
                }
            val notMatched =
                client.post(mokksy.baseUrl() + "/body-predicate") {
                    setBody("""{"value": "no"}""")
                }

            matched.status shouldBe HttpStatusCode.OK
            notMatched.status shouldBe HttpStatusCode.NotFound
        }
}
