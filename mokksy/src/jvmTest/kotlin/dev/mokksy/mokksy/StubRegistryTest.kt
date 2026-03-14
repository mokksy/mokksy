package dev.mokksy.mokksy

import dev.mokksy.mokksy.utils.logger.HttpFormatter
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.ktor.server.routing.RoutingRequest
import io.ktor.util.logging.Logger
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertFailsWith

@ExtendWith(MockKExtension::class)
class StubRegistryTest {
    @MockK
    lateinit var routingRequest: RoutingRequest

    @MockK(relaxed = true)
    lateinit var logger: Logger

    @MockK(relaxed = true)
    lateinit var formatter: HttpFormatter

    @Nested
    inner class AddAndGetAll {
        @Test
        fun `should add stubs and return sorted snapshot`() =
            runTest {
                val registry = StubRegistry()

                val s1 =
                    createStub<String, String>(
                        name = "s1",
                        priority = 10,
                        requestType = String::class,
                    )
                val s2 =
                    createStub<String, String>(
                        name = "s2",
                        priority = 1,
                        requestType = String::class,
                    )
                val s3 =
                    createStub<String, String>(
                        name = "s3",
                        priority = 10,
                        requestType = String::class,
                    )

                registry.add(s1)
                registry.add(s2)
                registry.add(s3)

                val all = registry.getAll().toList()

                // Expect ordering by priority asc, then creation order
                all shouldBe listOf(s2, s1, s3)
            }

        @Test
        fun `should throw on duplicate stub`() =
            runTest {
                val registry = StubRegistry()
                val s1 =
                    createStub<String, String>(
                        name = "dup",
                        priority = 5,
                        requestType = String::class,
                    )

                registry.add(s1)

                assertFailsWith<IllegalArgumentException> {
                    registry.add(s1)
                }
            }
    }

    @Nested
    inner class FindMatchingStub {
        @Test
        fun `should return best match by priority and mark it as matched`() =
            runTest {
                val registry = StubRegistry()
                val lowPrio =
                    createStub<String, String>(
                        name = "low",
                        priority = 100,
                        requestType = String::class,
                    )
                val highPrio =
                    createStub<String, String>(
                        name = "high",
                        priority = 1,
                        requestType = String::class,
                    )

                registry.add(lowPrio)
                registry.add(highPrio)

                val matched =
                    registry.findMatchingStub(
                        request = routingRequest,
                        verbose = false,
                        logger = mockk(relaxed = true),
                        formatter =
                            HttpFormatter(),
                    )

                matched shouldBe highPrio
                highPrio.hasBeenMatched() shouldBe true
                lowPrio.hasBeenMatched() shouldBe false
            }

        @Test
        fun `should break ties by creation order`() =
            runTest {
                val registry = StubRegistry()
                val first =
                    createStub<String, String>(
                        name = "first",
                        priority = 10,
                        requestType = String::class,
                    )
                val second =
                    createStub<String, String>(
                        name = "second",
                        priority = 10,
                        requestType = String::class,
                    )

                registry.add(first)
                registry.add(second)

                val matched =
                    registry.findMatchingStub(
                        request = routingRequest,
                        verbose = false,
                        logger = mockk(relaxed = true),
                        formatter =
                            HttpFormatter(),
                    )

                matched shouldBe first
            }

        @Test
        fun `should remove stub after match when configured`() =
            runTest {
                val registry = StubRegistry()
                val removable =
                    createStub<String, String>(
                        name = "once",
                        priority = 5,
                        eventuallyRemove = true,
                        requestType = String::class,
                    )

                registry.add(removable)

                val matched1 =
                    registry.findMatchingStub(
                        request = routingRequest,
                        verbose = false,
                        logger = mockk(relaxed = true),
                        formatter =
                            HttpFormatter(),
                    )
                matched1 shouldBe removable
                removable.hasBeenMatched() shouldBe true

                // Next time it should not be present
                val matched2 =
                    registry.findMatchingStub(
                        request = routingRequest,
                        verbose = false,
                        logger = mockk(relaxed = true),
                        formatter =
                            HttpFormatter(),
                    )
                matched2 shouldBe null
                registry.getAll().isEmpty() shouldBe true
            }

        @Test
        fun `eventuallyRemove stub is ineligible for matching once hasBeenMatched is true`() =
            runTest {
                val registry = StubRegistry()
                val stub =
                    createStub<String, String>(
                        name = "once",
                        priority = 5,
                        eventuallyRemove = true,
                        requestType = String::class,
                    )

                registry.add(stub)

                // Simulate the claim that the registry itself performs,
                // without going through findMatchingStub, to verify the predicate alone.
                stub.claimMatch()

                stub.hasBeenMatched() shouldBe true

                // Registry must not select it even though it is still physically present.
                val result =
                    registry.findMatchingStub(
                        request = routingRequest,
                        verbose = false,
                        logger = mockk(relaxed = true),
                        formatter = HttpFormatter(),
                    )

                result shouldBe null
            }
    }

    @Nested
    inner class Concurrency {
        @Test
        fun `should handle concurrent additions`() =
            runTest {
                val registry = StubRegistry()
                val count = 100
                val stubsToAdd =
                    (1..count).map {
                        createStub<String, String>(
                            name = "createStub-$it",
                            requestType = String::class,
                        )
                    }

                val jobs =
                    stubsToAdd.map { s ->
                        async {
                            registry.add(s)
                        }
                    }
                jobs.awaitAll()

                registry.getAll().size shouldBe count
            }

        @Test
        fun `should handle concurrent add and remove`() =
            runTest {
                val registry = StubRegistry()
                val count = 100
                val stubs =
                    (1..count).map {
                        createStub<String, String>(
                            name = "createStub-$it",
                            requestType = String::class,
                        )
                    }

                // Add all first
                stubs.forEach { registry.add(it) }

                registry.getAll().size shouldBe 100
            }

        @Test
        fun `should handle concurrent findMatchingStub with removal`() =
            runTest {
                val registry = StubRegistry()
                val count = 50
                // Use different priorities to avoid ties being the only thing tested
                val stubs =
                    (1..count).map {
                        createStub<String, String>(
                            name = "createStub-$it",
                            priority = it,
                            eventuallyRemove = true,
                            requestType = String::class,
                        )
                    }

                stubs.forEach { registry.add(it) }

                val jobs =
                    (1..count).map {
                        async {
                            registry.findMatchingStub(routingRequest, false, logger, formatter)
                        }
                    }
                val results = jobs.awaitAll()

                results.filterNotNull().size shouldBe count
                results.toSet().size shouldBe count // All unique
                registry.getAll().shouldBeEmpty()
            }
    }
}
