package dev.mokksy.mokksy.request

import dev.mokksy.mokksy.Input
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.ktor.server.request.RequestCookies
import io.mockk.mockk
import kotlin.test.Test

class RequestMatchersKtTest {
    private val predicate: (Input?) -> Boolean =
        object : (Input?) -> Boolean {
            override fun invoke(p1: Input?): Boolean = (p1?.name == "foo")

            override fun toString(): String = "predicateToString"
        }

    @Test
    fun `Should test predicate matcher`() {
        val input = Input("foo")

        val matcher = predicateMatcher<Input>("my predicate description", predicate)

        matcher.toString() shouldBe "my predicate description"
        val testResult = matcher.test(input)

        assertSoftly(testResult) {
            passed() shouldBe true
            failureMessage() shouldBe
                "Object 'Input(name=foo, age=null)' should match predicate 'predicateToString'"
            negatedFailureMessage() shouldBe
                "Object 'Input(name=foo, age=null)' should NOT match predicate 'predicateToString'"
        }
    }

    @Test
    fun `Should test cookieMatcher`() {
        val cookies =
            object : RequestCookies(mockk()) {
                override fun fetchCookies(): Map<String, String> = mapOf("session" to "abc123")
            }

        val passMatcher = cookieMatcher("session") { it == "abc123" }
        passMatcher.toString() shouldBe "cookie('session')"

        assertSoftly(passMatcher.test(cookies)) {
            passed() shouldBe true
            failureMessage() shouldBe
                "Request cookie 'session' should match predicate, but was 'abc123'."
            negatedFailureMessage() shouldBe
                "Request cookie 'session' should NOT match predicate, but it does."
        }

        val failMatcher = cookieMatcher("session") { it == "wrong" }
        assertSoftly(failMatcher.test(cookies)) {
            passed() shouldBe false
            failureMessage() shouldBe
                "Request cookie 'session' should match predicate, but was 'abc123'."
            negatedFailureMessage() shouldBe
                "Request cookie 'session' should NOT match predicate, but it does."
        }

        val emptyCookies =
            object : RequestCookies(mockk()) {
                override fun fetchCookies(): Map<String, String> = emptyMap()
            }
        val missingMatcher = cookieMatcher("missing") { it == null }
        assertSoftly(missingMatcher.test(emptyCookies)) {
            passed() shouldBe true
            failureMessage() shouldBe
                "Request cookie 'missing' should match predicate, but was 'null'."
            negatedFailureMessage() shouldBe
                "Request cookie 'missing' should NOT match predicate, but it does."
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
