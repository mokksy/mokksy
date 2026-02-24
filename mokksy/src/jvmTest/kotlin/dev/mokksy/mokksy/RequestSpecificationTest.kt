package dev.mokksy.mokksy

import dev.mokksy.mokksy.request.RequestSpecification
import io.kotest.matchers.Matcher
import io.kotest.matchers.equals.beEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.contain
import io.ktor.http.Headers
import io.ktor.http.HttpMethod
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.receive
import io.ktor.server.testing.TestApplicationCall
import io.ktor.server.testing.TestApplicationRequest
import io.ktor.server.testing.testApplication
import io.ktor.util.reflect.TypeInfo
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class RequestSpecificationTest {
    @Test
    suspend fun `should match when all conditions are satisfied`() {
        val request = mockk<ApplicationRequest>()
        val input = Input("Alice")

        every { request.httpMethod } returns HttpMethod.Get
        every { request.path() } returns "/test"
        coEvery<Input?> { request.call.receiveNullable(any<TypeInfo>()) } returns input
        coEvery { request.call.receive(String::class) } returns Json.encodeToString(input)
        every { request.headers } returns
            Headers.build {
                append("X-Request-ID", "RequestID")
            }
        val headersMatcher = mockk<Matcher<Headers>>(relaxed = true)
        every { headersMatcher.test(any()).passed() } returns true

        val spec =
            RequestSpecification(
                method = beEqual(HttpMethod.Get),
                path = contain("test"),
                headers = listOf(headersMatcher),
                body = listOf(beEqual(Input("Alice"))),
                bodyString = listOf(contain("Alice")),
                requestType = Input::class,
            )

        spec.matches(request) shouldBe Result.success(true)
    }

    @Test
    fun `should match when only headers are specified`() = testApplication {
        val request = TestApplicationRequest(
            method = HttpMethod.Post,
            call = TestApplicationCall(
                application = application,
                coroutineContext = currentCoroutineContext()
            ),
            closeRequest = false,
            uri = "/foo/test",
        )
        request.addHeader("X-Request-ID", "RequestID")

        val headersMatcher = mockk<Matcher<Headers>>()
        val spec =
            RequestSpecification(
                headers = listOf(headersMatcher),
                requestType = Input::class,
            )

        every { headersMatcher.test(any()).passed() } returns true

        spec.matches(request) shouldBe Result.success(true)
    }

    @Test
    suspend fun mismatchedMethod() {
        val request = mockk<ApplicationRequest>()
        every { request.httpMethod } returns HttpMethod.Get
        val spec =
            RequestSpecification(
                method = beEqual(HttpMethod.Post),
                path = contain("test"),
                requestType = Input::class,
            )

        spec.matches(request) shouldBe Result.success(false)
    }

    @Test
    fun mismatchedPath() = testApplication {
        val request = TestApplicationRequest(
            method = HttpMethod.Get,
            call = TestApplicationCall(
                application = application,
                coroutineContext = currentCoroutineContext()
            ),
            closeRequest = false,
            uri = "/other",
        )

        val spec =
            RequestSpecification(
                method = beEqual(HttpMethod.Get),
                path = contain("test"),
                requestType = Input::class,
            )

        spec.matches(request) shouldBe Result.success(false)
    }

    @Test
    suspend fun mismatchedHeaders() {
        val request = mockk<ApplicationRequest>()
        every { request.headers } returns
            Headers.build {
                append("X-Request-ID", "RequestID")
            }

        val headersMatcher = mockk<Matcher<Headers>>(relaxed = true)
        val spec =
            RequestSpecification(
                headers = listOf(headersMatcher),
                requestType = Input::class,
            )

        every { headersMatcher.test(any()).passed() } returns false

        spec.matches(request) shouldBe Result.success(false)
    }

    @Test
    suspend fun `should not match when bodyString differs`() {
        val request = mockk<ApplicationRequest>()
        coEvery { request.call.receive(String::class) } returns "Another body"

        val bodyMatcher = contain("expectedBody")
        val spec =
            RequestSpecification(
                bodyString = listOf(bodyMatcher),
                requestType = String::class,
            )

        spec.matches(request) shouldBe Result.success(false)
    }

    @Test
    suspend fun `should not match when body differs`() {
        val request = mockk<ApplicationRequest>()
        val input = Input("Alice")

        coEvery<Input?> { request.call.receiveNullable(any<TypeInfo>()) } returns input

        val bodyMatcher: Matcher<Input?> = beEqual(Input("Bob"))
        val spec =
            RequestSpecification(
                body = listOf(bodyMatcher),
                requestType = Input::class,
            )

        spec.matches(request) shouldBe Result.success(false)
    }
}
