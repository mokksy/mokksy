package dev.mokksy.mokksy.request

import dev.mokksy.mokksy.Input
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class RequestMatchersKtTest {
    private val predicate: (Input?) -> Boolean = { it?.name == "foo" }

    @Test
    fun `Should test predicate matcher`() {
        val input = Input("foo")

        val matcher = predicateMatcher<Input>("my predicate description", predicate)

        matcher.toString() shouldBe "my predicate description"
        val testResult = matcher.test(input)

        assertSoftly(testResult) {
            passed() shouldBe true
            failureMessage() shouldStartWith
                "Object 'Input(name=foo, age=null)' should match predicate '"
            negatedFailureMessage() shouldStartWith
                "Object 'Input(name=foo, age=null)' should NOT match predicate '"
        }
    }

    @Test
    fun `Should test cookieMatcher`() =
        runTest {
            testApplication {
                application {
                    routing {
                        get("/pass") {
                            val matcher = cookieMatcher("session") { it == "abc123" }
                            val result = matcher.test(call.request.cookies)
                            call.respondText(
                                "${result.passed()}|${result.failureMessage()}|${result.negatedFailureMessage()}",
                            )
                        }
                        get("/fail") {
                            val matcher = cookieMatcher("session") { it == "wrong" }
                            val result = matcher.test(call.request.cookies)
                            call.respondText(
                                "${result.passed()}|${result.failureMessage()}|${result.negatedFailureMessage()}",
                            )
                        }
                        get("/missing") {
                            val matcher = cookieMatcher("missing") { it == null }
                            val result = matcher.test(call.request.cookies)
                            call.respondText(
                                "${result.passed()}|${result.failureMessage()}|${result.negatedFailureMessage()}",
                            )
                        }
                    }
                }

                cookieMatcher("session") { it == "abc123" }.toString() shouldBe "cookie('session')"

                val passBody =
                    client
                        .get("/pass") {
                            header(HttpHeaders.Cookie, "session=abc123")
                        }.bodyAsText()
                val passParts = passBody.split("|")
                assertSoftly(passParts) {
                    this[0] shouldBe "true"
                    this[1] shouldBe
                        "Request cookie 'session' should match predicate, but was 'abc123'."
                    this[2] shouldBe
                        "Request cookie 'session' should NOT match predicate, but it does."
                }

                val failBody =
                    client
                        .get("/fail") {
                            header(HttpHeaders.Cookie, "session=abc123")
                        }.bodyAsText()
                val failParts = failBody.split("|")
                assertSoftly(failParts) {
                    this[0] shouldBe "false"
                    this[1] shouldBe
                        "Request cookie 'session' should match predicate, but was 'abc123'."
                    this[2] shouldBe
                        "Request cookie 'session' should NOT match predicate, but it does."
                }

                val missingBody = client.get("/missing").bodyAsText()
                val missingParts = missingBody.split("|")
                assertSoftly(missingParts) {
                    this[0] shouldBe "true"
                    this[1] shouldBe
                        "Request cookie 'missing' should match predicate, but was 'null'."
                    this[2] shouldBe
                        "Request cookie 'missing' should NOT match predicate, but it does."
                }
            }
        }

    @Test
    fun `Should test successCallMatcher`() {
        val input = Input("foo")

        val matcher = successCallMatcher<Input>("Should not be null") { input.shouldNotBeNull() }

        matcher.toString() shouldStartWith "Should not be null"
        val testResult = matcher.test(input)

        assertSoftly(testResult) {
            passed() shouldBe true
            failureMessage() shouldStartWith
                "Object 'Input(name=foo, age=null)' should satisfy '"
            negatedFailureMessage() shouldStartWith
                "Object 'Input(name=foo, age=null)' should NOT satisfy '"
        }
    }
}
