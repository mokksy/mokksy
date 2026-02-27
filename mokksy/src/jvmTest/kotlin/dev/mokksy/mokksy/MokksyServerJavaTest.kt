package dev.mokksy.mokksy

import dev.mokksy.mokksy.request.RecordedRequest
import dev.mokksy.mokksy.request.RequestSpecification
import dev.mokksy.mokksy.request.RequestSpecificationBuilder
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpMethod
import io.mockk.MockKMatcherScope
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.util.function.Consumer
import kotlin.reflect.KClass
import kotlin.test.Test

class MokksyServerJavaTest {
    private val delegate = mockk<MokksyServer>(relaxed = true)
    private val sut = MokksyServerJava(delegate)

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
        sut.resetMatchCounts()
        verify { delegate.resetMatchCounts() }
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

    // region HTTP method delegation — PUT, DELETE, PATCH, HEAD, OPTIONS
    // (GET and POST with String body are covered by MokksyJavaIT)

    // Typed helpers — resolve mockk's any() to the exact parameter types of method<String>().
    // The explicit return types prevent K2's type inference from failing with
    // "Cannot infer type for type parameter 'T'" on generic any() calls in verify/every blocks.
    private fun MockKMatcherScope.anyConfig(): StubConfiguration = any()
    private fun MockKMatcherScope.anyMethod(): HttpMethod = any()
    private fun MockKMatcherScope.anyKClass(): KClass<String> = any()
    private fun MockKMatcherScope.anySpecBlock(): RequestSpecificationBuilder<String>.() -> Unit = any()

    @Test
    fun `put delegates with PUT HttpMethod`() {
        sut.put(Consumer<RequestSpecificationBuilder<String>> {})

        verify { delegate.method<String>(anyConfig(), HttpMethod.Put, anyKClass(), anySpecBlock()) }
    }

    @Test
    fun `put with StubConfiguration passes the config to delegate`() {
        val config = StubConfiguration("put-stub")
        val capturedConfig = slot<StubConfiguration>()
        every {
            delegate.method<String>(capture(capturedConfig), anyMethod(), anyKClass(), anySpecBlock())
        } returns mockk(relaxed = true)

        sut.put(config, Consumer<RequestSpecificationBuilder<String>> {})

        capturedConfig.captured shouldBe config
    }

    @Test
    fun `delete delegates with DELETE HttpMethod`() {
        sut.delete(Consumer<RequestSpecificationBuilder<String>> {})

        verify { delegate.method<String>(anyConfig(), HttpMethod.Delete, anyKClass(), anySpecBlock()) }
    }

    @Test
    fun `patch delegates with PATCH HttpMethod`() {
        sut.patch(Consumer<RequestSpecificationBuilder<String>> {})

        verify { delegate.method<String>(anyConfig(), HttpMethod.Patch, anyKClass(), anySpecBlock()) }
    }

    @Test
    fun `head delegates with HEAD HttpMethod`() {
        sut.head(Consumer<RequestSpecificationBuilder<String>> {})

        verify { delegate.method<String>(anyConfig(), HttpMethod.Head, anyKClass(), anySpecBlock()) }
    }

    @Test
    fun `options delegates with OPTIONS HttpMethod`() {
        sut.options(Consumer<RequestSpecificationBuilder<String>> {})

        verify { delegate.method<String>(anyConfig(), HttpMethod.Options, anyKClass(), anySpecBlock()) }
    }

    // endregion

    // region Typed body overloads — verify KClass is forwarded correctly

    @Test
    fun `get with typed Class forwards KClass to delegate`() {
        val capturedType = slot<KClass<String>>()
        every {
            delegate.method<String>(anyConfig(), anyMethod(), capture(capturedType), anySpecBlock())
        } returns mockk(relaxed = true)

        sut.get(String::class.java, Consumer<RequestSpecificationBuilder<String>> {})

        capturedType.captured shouldBe String::class
    }

    @Test
    fun `post with typed Class and StubConfiguration forwards both to delegate`() {
        val config = StubConfiguration("typed-post")
        val capturedConfig = slot<StubConfiguration>()
        val capturedType = slot<KClass<String>>()
        every {
            delegate.method<String>(capture(capturedConfig), anyMethod(), capture(capturedType), anySpecBlock())
        } returns mockk(relaxed = true)

        sut.post(config, String::class.java, Consumer<RequestSpecificationBuilder<String>> {})

        assertSoftly {
            capturedConfig.captured shouldBe config
            capturedType.captured shouldBe String::class
        }
    }

    // endregion
}
