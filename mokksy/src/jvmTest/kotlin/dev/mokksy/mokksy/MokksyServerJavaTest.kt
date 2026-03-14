package dev.mokksy.mokksy

import dev.mokksy.Mokksy
import dev.mokksy.mokksy.request.RecordedRequest
import dev.mokksy.mokksy.request.RequestSpecification
import dev.mokksy.mokksy.request.RequestSpecificationBuilder
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpMethod
import io.mockk.MockKMatcherScope
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.stream.Stream
import kotlin.reflect.KClass
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MokksyServerJavaTest {
    private val delegate = mockk<MokksyServer>(relaxed = true)
    private val sut = Mokksy(delegate)

    @BeforeEach
    fun resetMocks() {
        clearMocks(delegate)
    }

    // region Properties and lifecycle

    @Test
    fun `delegate property exposes the underlying MokksyServer`() {
        sut.delegate shouldBe delegate
    }

    @Test
    fun `port delegates to delegate`() {
        every { delegate.port() } returns 9090
        sut.port() shouldBe 9090
    }

    // endregion

    // region Verification

    @Test
    fun `resetMatchCounts delegates to delegate`() {
        sut.resetMatchState()
        verify { delegate.resetMatchState() }
    }

    @Test
    fun `findAllUnmatchedStubs returns delegate result`() {
        val spec = mockk<RequestSpecification<String>>()
        every { delegate.findAllUnmatchedStubs() } returns listOf(spec)
        sut.findAllUnmatchedStubs() shouldBe listOf(spec)
    }

    @Test
    fun `findAllUnexpectedRequests returns delegate result`() {
        val request = mockk<RecordedRequest>()
        every { delegate.findAllUnexpectedRequests() } returns listOf(request)
        sut.findAllUnexpectedRequests() shouldBe listOf(request)
    }

    // endregion

    // region HTTP method delegation

    // Typed helpers — resolve mockk's any() to the exact parameter types of method<String>().
    // The explicit return types prevent K2's type inference from failing with
    // "Cannot infer type for type parameter 'T'" on generic any() calls in verify/every blocks.
    private fun MockKMatcherScope.anyConfig(): StubConfiguration = any()

    private fun MockKMatcherScope.anyMethod(): HttpMethod = any()

    private fun MockKMatcherScope.anyKClass(): KClass<String> = any()

    private fun MockKMatcherScope.anySpecBlock(): RequestSpecificationBuilder<String>.() -> Unit =
        any()

    @ParameterizedTest(name = "{0} delegates with correct HttpMethod")
    @MethodSource("simpleMethodInvocations")
    suspend fun `delegates with correct HttpMethod`(
        @Suppress("UNUSED_PARAMETER") methodName: String,
        expectedMethod: HttpMethod,
        invoke: Consumer<Mokksy>,
    ) {
        invoke.accept(sut)

        eventually {
            verify { delegate.method(anyConfig(), expectedMethod, anyKClass(), anySpecBlock()) }
        }
    }

    @ParameterizedTest(name = "{0} with StubConfiguration forwards config to delegate")
    @MethodSource("configMethodInvocations")
    suspend fun `StubConfiguration is forwarded to delegate`(
        @Suppress("UNUSED_PARAMETER") methodName: String,
        invoke: BiConsumer<Mokksy, StubConfiguration>,
    ) {
        val config = StubConfiguration("stub")
        val capturedConfig = slot<StubConfiguration>()
        every {
            delegate.method(capture(capturedConfig), anyMethod(), anyKClass(), anySpecBlock())
        } returns mockk(relaxed = true)

        invoke.accept(sut, config)

        eventually { capturedConfig.captured shouldBe config }
    }

    @ParameterizedTest(name = "{0} with typed KClass forwards KClass to delegate")
    @MethodSource("typedMethodInvocations")
    suspend fun `typed KClass is forwarded to delegate`(
        @Suppress("UNUSED_PARAMETER") methodName: String,
        invoke: Consumer<Mokksy>,
    ) {
        val capturedType = slot<KClass<String>>()
        every {
            delegate.method(anyConfig(), anyMethod(), capture(capturedType), anySpecBlock())
        } returns mockk(relaxed = true)

        invoke.accept(sut)

        eventually {
            capturedType.captured shouldBe String::class
        }
    }

    @ParameterizedTest(
        name = "{0} with StubConfiguration and typed KClass forwards both to delegate",
    )
    @MethodSource("configTypedMethodInvocations")
    suspend fun `StubConfiguration and KClass are both forwarded to delegate`(
        @Suppress("UNUSED_PARAMETER") methodName: String,
        invoke: BiConsumer<Mokksy, StubConfiguration>,
    ) {
        val config = StubConfiguration("typed-stub")
        val capturedConfig = slot<StubConfiguration>()
        val capturedType = slot<KClass<String>>()
        every {
            delegate.method(
                capture(capturedConfig),
                anyMethod(),
                capture(capturedType),
                anySpecBlock(),
            )
        } returns mockk(relaxed = true)

        invoke.accept(sut, config)

        eventually {
            assertSoftly {
                capturedConfig.captured shouldBe config
                capturedType.captured shouldBe String::class
            }
        }
    }

    // endregion

    // region Verification delegation

    @Test
    fun `verifyNoUnmatchedStubs delegates to delegate`() {
        sut.verifyNoUnmatchedStubs()
        verify { delegate.verifyNoUnmatchedStubs() }
    }

    @Test
    fun `verifyNoUnexpectedRequests delegates to delegate`() {
        sut.verifyNoUnexpectedRequests()
        verify { delegate.verifyNoUnexpectedRequests() }
    }

    @Test
    fun `baseUrl delegates to delegate`() {
        every { delegate.baseUrl() } returns "http://127.0.0.1:9090"
        sut.baseUrl() shouldBe "http://127.0.0.1:9090"
    }

    // endregion

    fun simpleMethodInvocations(): Stream<Arguments> =
        Stream.of(
            Arguments.of("GET", HttpMethod.Get, Consumer<Mokksy> { it.get {} }),
            Arguments.of("POST", HttpMethod.Post, Consumer<Mokksy> { it.post {} }),
            Arguments.of("PUT", HttpMethod.Put, Consumer<Mokksy> { it.put {} }),
            Arguments.of("DELETE", HttpMethod.Delete, Consumer<Mokksy> { it.delete {} }),
            Arguments.of("PATCH", HttpMethod.Patch, Consumer<Mokksy> { it.patch {} }),
            Arguments.of("HEAD", HttpMethod.Head, Consumer<Mokksy> { it.head {} }),
            Arguments.of("OPTIONS", HttpMethod.Options, Consumer<Mokksy> { it.options {} }),
        )

    fun configMethodInvocations(): Stream<Arguments> =
        Stream.of(
            Arguments.of(
                "GET",
                BiConsumer<Mokksy, StubConfiguration> { s, cfg -> s.get(cfg) {} },
            ),
            Arguments.of(
                "POST",
                BiConsumer<Mokksy, StubConfiguration> { s, cfg -> s.post(cfg) {} },
            ),
            Arguments.of(
                "PUT",
                BiConsumer<Mokksy, StubConfiguration> { s, cfg -> s.put(cfg) {} },
            ),
            Arguments.of(
                "DELETE",
                BiConsumer<Mokksy, StubConfiguration> { s, cfg -> s.delete(cfg) {} },
            ),
            Arguments.of(
                "PATCH",
                BiConsumer<Mokksy, StubConfiguration> { s, cfg -> s.patch(cfg) {} },
            ),
            Arguments.of(
                "HEAD",
                BiConsumer<Mokksy, StubConfiguration> { s, cfg -> s.head(cfg) {} },
            ),
            Arguments.of(
                "OPTIONS",
                BiConsumer<Mokksy, StubConfiguration> { s, cfg -> s.options(cfg) {} },
            ),
        )

    fun typedMethodInvocations(): Stream<Arguments> =
        Stream.of(
            Arguments.of("GET", Consumer<Mokksy> { it.get(String::class) {} }),
            Arguments.of("POST", Consumer<Mokksy> { it.post(String::class) {} }),
            Arguments.of("PUT", Consumer<Mokksy> { it.put(String::class) {} }),
            Arguments.of("DELETE", Consumer<Mokksy> { it.delete(String::class) {} }),
            Arguments.of("PATCH", Consumer<Mokksy> { it.patch(String::class) {} }),
            Arguments.of("HEAD", Consumer<Mokksy> { it.head(String::class) {} }),
            Arguments.of("OPTIONS", Consumer<Mokksy> { it.options(String::class) {} }),
        )

    fun configTypedMethodInvocations(): Stream<Arguments> =
        Stream.of(
            Arguments.of(
                "GET",
                BiConsumer<Mokksy, StubConfiguration> { s, cfg ->
                    s.get(cfg) {}
                },
            ),
            Arguments.of(
                "POST",
                BiConsumer<Mokksy, StubConfiguration> { s, cfg ->
                    s.post(cfg) {}
                },
            ),
            Arguments.of(
                "PUT",
                BiConsumer<Mokksy, StubConfiguration> { s, cfg ->
                    s.put(cfg) {}
                },
            ),
            Arguments.of(
                "DELETE",
                BiConsumer<Mokksy, StubConfiguration> { s, cfg ->
                    s.delete(cfg) {}
                },
            ),
            Arguments.of(
                "PATCH",
                BiConsumer<Mokksy, StubConfiguration> { s, cfg ->
                    s.patch(cfg) {}
                },
            ),
            Arguments.of(
                "HEAD",
                BiConsumer<Mokksy, StubConfiguration> { s, cfg ->
                    s.head(cfg) {}
                },
            ),
            Arguments.of(
                "OPTIONS",
                BiConsumer<Mokksy, StubConfiguration> { s, cfg ->
                    s.options(cfg) {}
                },
            ),
        )
}
