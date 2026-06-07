@file:OptIn(dev.mokksy.mokksy.InternalMokksyApi::class)

package dev.mokksy.mokksy.request

import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlin.test.Test

internal class DiagnosticLoggerTest {
    private fun formatLog(stubResults: List<StubMatchResult>) =
        DiagnosticLogger.format(
            stubResults = stubResults,
            useColor = false,
        )

    @Test
    fun `shows failed matchers`() {
        val log =
            formatLog(
                stubResults =
                    listOf(
                        StubMatchResult(
                            name = "my-stub",
                            configuredMatchers = listOf("method: POST", "path: '/test'"),
                            failedMatchers = listOf("path: expected '/test' but was /actual"),
                        ),
                    ),
            )

        log shouldContain "Closest stub:"
        log shouldContain "Stub: my-stub"
        log shouldContain "  ✓ method: POST"
        log shouldContain "  ✗ path: '/test'"
        log shouldContain "path: expected '/test' but was /actual"
    }

    @Test
    fun `does not show received body on bodyString mismatch`() {
        val log =
            formatLog(
                stubResults =
                    listOf(
                        StubMatchResult(
                            name = "token-stub",
                            configuredMatchers =
                                listOf(
                                    "method: POST",
                                    "path: '/test'",
                                    "bodyString: contain(expected-token)",
                                ),
                            failedMatchers =
                                listOf(
                                    "bodyString: expected contain(expected-token)",
                                ),
                        ),
                    ),
            )

        log shouldContain "  ✓ method: POST"
        log shouldContain "  ✓ path: '/test'"
        log shouldContain "  ✗ bodyString: contain(expected-token)"
        log shouldContain "bodyString: expected contain(expected-token)"
        log shouldNotContain "received:"
    }

    @Test
    fun `does not show received body on body mismatch with plain text`() {
        val log =
            formatLog(
                stubResults =
                    listOf(
                        StubMatchResult(
                            name = "body-stub",
                            configuredMatchers = listOf("body: PredicateMatcher(...)"),
                            failedMatchers = listOf("body: expected PredicateMatcher(...)"),
                        ),
                    ),
            )

        log shouldContain "  ✗ body: PredicateMatcher(...)"
        log shouldContain "body: expected PredicateMatcher(...)"
        log shouldNotContain "received:"
    }

    @Test
    fun `shows header mismatch detail`() {
        val log =
            formatLog(
                stubResults =
                    listOf(
                        StubMatchResult(
                            name = "header-stub",
                            configuredMatchers =
                                listOf(
                                    "method: GET",
                                    "headers: Authorization = Bearer token",
                                ),
                            failedMatchers =
                                listOf(
                                    "headers: expected Authorization = Bearer token but header was not present",
                                ),
                        ),
                    ),
            )

        log shouldContain "  ✓ method: GET"
        log shouldContain "  ✗ headers: Authorization = Bearer token"
        log shouldContain
            "headers: expected Authorization = Bearer token but header was not present"
    }

    @Test
    fun `shows multiple closest stubs when they tie`() {
        val log =
            formatLog(
                stubResults =
                    listOf(
                        StubMatchResult(
                            name = "stub-a",
                            configuredMatchers = listOf("method: GET"),
                            failedMatchers = listOf("path: expected '/a' but was /test"),
                        ),
                        StubMatchResult(
                            name = "stub-b",
                            configuredMatchers = listOf("method: GET"),
                            failedMatchers = listOf("path: expected '/b' but was /test"),
                        ),
                    ),
            )

        log shouldContain "Closest 2 stubs:"
        log shouldContain "Stub: stub-a"
        log shouldContain "Stub: stub-b"
        log shouldContain "  ✓ method: GET"
    }

    @Test
    fun `shows cookie mismatch detail`() {
        val log =
            formatLog(
                stubResults =
                    listOf(
                        StubMatchResult(
                            name = "cookie-stub",
                            configuredMatchers =
                                listOf(
                                    "method: GET",
                                    "cookies: cookie('session')",
                                ),
                            failedMatchers =
                                listOf(
                                    "cookies: expected cookie('session') but cookie was not present",
                                ),
                        ),
                    ),
            )

        log shouldContain "  ✓ method: GET"
        log shouldContain "  ✗ cookies: cookie('session')"
        log shouldContain "cookies: expected cookie('session') but cookie was not present"
    }

    @Test
    fun `does not show received on cookie mismatch`() {
        val log =
            formatLog(
                stubResults =
                    listOf(
                        StubMatchResult(
                            name = "cookie-stub",
                            configuredMatchers =
                                listOf(
                                    "method: GET",
                                    "cookies: cookie('session')",
                                ),
                            failedMatchers =
                                listOf(
                                    "cookies: expected cookie('session') but cookie was not present",
                                ),
                        ),
                    ),
            )

        log shouldContain "  ✓ method: GET"
        log shouldContain "  ✗ cookies: cookie('session')"
        log shouldContain "cookies: expected cookie('session') but cookie was not present"
        log shouldNotContain "received:"
    }

    @Test
    fun `does not show received on header mismatch`() {
        val log =
            formatLog(
                stubResults =
                    listOf(
                        StubMatchResult(
                            name = "header-stub",
                            configuredMatchers = listOf("headers: X-Api-Key = secret"),
                            failedMatchers =
                                listOf(
                                    "headers: expected X-Api-Key = secret but header was not present",
                                ),
                        ),
                    ),
            )

        log shouldContain "  ✗ headers: X-Api-Key = secret"
        log shouldContain "headers: expected X-Api-Key = secret but header was not present"
        log shouldNotContain "received:"
    }
}
