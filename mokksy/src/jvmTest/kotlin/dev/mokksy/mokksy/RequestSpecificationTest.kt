package dev.mokksy.mokksy

import dev.mokksy.mokksy.request.RequestSpecification
import io.kotest.matchers.Matcher
import io.kotest.matchers.equals.beEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.contain
import io.ktor.http.Headers
import io.ktor.http.HttpMethod
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.receive
import io.ktor.util.reflect.TypeInfo
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import kotlin.test.BeforeTest

@Execution(ExecutionMode.SAME_THREAD)
@ExtendWith(MockKExtension::class)
class RequestSpecificationTest {

    @MockK
    lateinit var request: ApplicationRequest

    @MockK(relaxed = true)
    lateinit var call: ApplicationCall

    @BeforeTest
    fun setup() {
        every { request.call } returns call
    }

    @Test
    suspend fun `should match when only headers are specified`() {
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

        every { headersMatcher.test(any()).passed() } returns true

        spec.matches(request) shouldBe Result.success(true)
    }

    @Test
    suspend fun mismatchedMethod() {
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
    suspend fun mismatchedPath() {
        every { request.httpMethod } returns HttpMethod.Get
        every { request.path() } returns "/other"

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
        coEvery { call.receive(String::class) } returns "Another body"

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
        val input = Input("Alice")

        coEvery<Input?> { call.receiveNullable(any<TypeInfo>()) } returns input
        coEvery { call.receive(Input::class) } returns input

        val bodyMatcher: Matcher<Input?> = beEqual(Input("Bob"))
        val spec =
            RequestSpecification(
                body = listOf(bodyMatcher),
                requestType = Input::class,
            )

        spec.matches(request) shouldBe Result.success(false)
    }
}
