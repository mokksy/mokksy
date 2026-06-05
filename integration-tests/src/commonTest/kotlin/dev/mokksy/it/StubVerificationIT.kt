@file:OptIn(dev.mokksy.mokksy.ExperimentalMokksyApi::class)

package dev.mokksy.it

import dev.mokksy.mokksy.StubConfiguration
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import kotlin.test.Test

internal class StubVerificationIT : MokksyIntegrationTest() {
    @Test
    fun `findAllUnmatchedStubs returns unmatched stub`() =
        runIntegrationTest {
            mokksy.get { path("/unmatched-stub") } respondsWith { body = "never-called" }

            mokksy.findAllUnmatchedStubs() shouldHaveSize 1

            client.get(mokksy.baseUrl() + "/unmatched-stub")

            mokksy.findAllUnmatchedStubs().shouldBeEmpty()
        }

    @Test
    fun `findAllUnexpectedRequests returns unexpected request`() =
        runIntegrationTest {
            mokksy.findAllUnexpectedRequests().shouldBeEmpty()

            client.get(mokksy.baseUrl() + "/no-stub-here")

            mokksy.findAllUnexpectedRequests() shouldHaveSize 1
        }

    @Test
    fun `verifyNoUnmatchedStubs throws when stub never called`() =
        runIntegrationTest {
            mokksy.get { path("/never-called") } respondsWith { body = "unreachable" }

            shouldThrow<AssertionError> {
                mokksy.verifyNoUnmatchedStubs()
            }
        }

    @Test
    fun `verifyNoUnexpectedRequests throws when request had no stub`() =
        runIntegrationTest {
            client.get(mokksy.baseUrl() + "/unexpected")

            shouldThrow<AssertionError> {
                mokksy.verifyNoUnexpectedRequests()
            }
        }

    @Test
    fun `resetMatchState makes matched stubs unmatched again`() =
        runIntegrationTest {
            mokksy.get { path("/reset-test") } respondsWith { body = "ok" }

            client.get(mokksy.baseUrl() + "/reset-test")

            mokksy.verifyNoUnmatchedStubs()
            mokksy.verifyNoUnexpectedRequests()

            mokksy.resetMatchState()

            shouldThrow<AssertionError> {
                mokksy.verifyNoUnmatchedStubs()
            }
        }

    @Test
    fun `matchCount returns 0 initially`() =
        runIntegrationTest {
            val stub = mokksy.get { path("/match-count-zero") }.respondsWith { body = "ok" }

            stub.matchCount() shouldBe 0
        }

    @Test
    fun `matchCount increments on each request`() =
        runIntegrationTest {
            val stub = mokksy.get { path("/match-count-inc") }.respondsWith { body = "ok" }

            stub.matchCount() shouldBe 0
            client.get(mokksy.baseUrl() + "/match-count-inc")
            stub.matchCount() shouldBe 1
            client.get(mokksy.baseUrl() + "/match-count-inc")
            stub.matchCount() shouldBe 2
            client.get(mokksy.baseUrl() + "/match-count-inc")
            stub.matchCount() shouldBe 3
        }

    @Test
    fun `matchCount on eventuallyRemove stub is 1 after match`() =
        runIntegrationTest {
            val stub =
                mokksy
                    .get(StubConfiguration(name = "once-count", eventuallyRemove = true)) {
                        path("/once-count")
                    }.respondsWith { body = "First!" }

            stub.matchCount() shouldBe 0
            client.get(mokksy.baseUrl() + "/once-count")
            stub.matchCount() shouldBe 1
            client.get(mokksy.baseUrl() + "/once-count")
            stub.matchCount() shouldBe 1
        }

    @Test
    fun `verify resetMatchState`() =
        runIntegrationTest {
            val stub = mokksy.get { path("/match-count-reset") }.respondsWith { body = "ok" }

            client.get(mokksy.baseUrl() + "/match-count-reset")
            stub.matchCount() shouldBe 1

            mokksy.resetMatchState()
            stub.matchCount() shouldBe 0

            client.get(mokksy.baseUrl() + "/match-count-reset")
            stub.matchCount() shouldBe 1
        }

    @Test
    fun `findStubByName returns null for unknown name`() =
        runIntegrationTest {
            mokksy.findStubByName("nonexistent") shouldBe null
        }

    @Test
    fun `findStubByName returns handle for named stub`() =
        runIntegrationTest {
            val stub =
                mokksy
                    .get(StubConfiguration(name = "find-stub-by-name")) {
                        path("/find-stub-by-name")
                    }.respondsWith { body = "ok" }

            val found = mokksy.findStubByName("find-stub-by-name")
            found.shouldNotBeNull {
                name shouldBe "find-stub-by-name"
                matchCount() shouldBe 0
            }

            client.get(mokksy.baseUrl() + "/find-stub-by-name")
            stub.matchCount() shouldBe 1
            found?.matchCount() shouldBe 1
        }

    @Test
    fun `findAllStubs returns all registered stubs`() =
        runIntegrationTest {
            val beforeCount = mokksy.allStubs().size

            mokksy.get { path("/stub-a") }.respondsWith { body = "a" }
            mokksy.get { path("/stub-b") }.respondsWith { body = "b" }

            mokksy.allStubs() shouldHaveSize (beforeCount + 2)

            client.get(mokksy.baseUrl() + "/stub-a")
            client.get(mokksy.baseUrl() + "/stub-b")
        }

    @Test
    fun `verifyCalled atLeast scenarios`() =
        runIntegrationTest {
            val stub =
                mokksy
                    .get(StubConfiguration(name = "atleast-scenarios")) {
                        path("/atleast-scenarios")
                    }.respondsWith { body = "ok" }
            val url = mokksy.baseUrl() + "/atleast-scenarios"

            assertSoftly {
                shouldThrow<AssertionError> { stub.verifyCalled().atLeast(1) }

                client.get(url)
                stub.verifyCalled().atLeast(1)
                shouldThrow<AssertionError> { stub.verifyCalled().atLeast(2) }

                client.get(url)
                stub.verifyCalled().atLeast(2)
                stub.verifyCalled().atLeast(1)
            }
        }

    @Test
    fun `verifyCalled atMost scenarios`() =
        runIntegrationTest {
            val stub =
                mokksy
                    .get(StubConfiguration(name = "atmost-scenarios")) {
                        path("/atmost-scenarios")
                    }.respondsWith { body = "ok" }
            val url = mokksy.baseUrl() + "/atmost-scenarios"

            client.get(url)
            client.get(url)

            assertSoftly {
                stub.verifyCalled().atMost(5)
                stub.verifyCalled().atMost(2)
                shouldThrow<AssertionError> { stub.verifyCalled().atMost(1) }

                client.get(url)

                shouldThrow<AssertionError> { stub.verifyCalled().atMost(2) }
                stub.verifyCalled().atMost(3)
            }
        }

    @Test
    fun `verifyCalled exactly scenarios`() =
        runIntegrationTest {
            val stub =
                mokksy
                    .get(StubConfiguration(name = "exactly-scenarios")) {
                        path("/exactly-scenarios")
                    }.respondsWith { body = "ok" }
            val url = mokksy.baseUrl() + "/exactly-scenarios"

            assertSoftly {
                stub.verifyCalled().exactly(0)
                shouldThrow<AssertionError> { stub.verifyCalled().exactly(1) }

                client.get(url)
                stub.verifyCalled().exactly(1)
                shouldThrow<AssertionError> { stub.verifyCalled().exactly(0) }

                client.get(url)
                client.get(url)
                stub.verifyCalled().exactly(3)
                stub.verifyCalled(3)
                shouldThrow<AssertionError> { stub.verifyCalled().exactly(2) }

                shouldThrow<AssertionError> { stub.verifyCalled().never() }
            }
        }

    @Test
    fun `verifyCalled never scenario`() =
        runIntegrationTest {
            val stub =
                mokksy
                    .get(StubConfiguration(name = "never-scenarios")) {
                        path("/never-scenarios")
                    }.respondsWith { body = "never" }

            stub.verifyCalled().never()

            client.get(mokksy.baseUrl() + "/never-scenarios")

            shouldThrow<AssertionError> {
                stub.verifyCalled().never()
            }
        }
}
