package dev.mokksy.mokksy

import dev.mokksy.mokksy.request.RequestSpecification
import dev.mokksy.mokksy.utils.logger.HttpFormatter
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.log
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class BuildingStepTest {
    private lateinit var subject: BuildingStep<Input>

    private lateinit var name: String

    private lateinit var request: RequestSpecification<Input>
    private lateinit var expectedHttpStatus: HttpStatusCode

    private lateinit var stub: CapturingSlot<Stub<*, *>>

    private lateinit var addStubCallback: (
        stub: Stub<*, *>,
    ) -> Unit

    @BeforeTest
    fun before() {
        name = UUID.randomUUID().toString()
        request = mockk()
        addStubCallback = mockk()
        expectedHttpStatus = HttpStatusCode.fromValue(IntRange(100, 500).random())

        stub = slot<Stub<*, *>>()

        every {
            addStubCallback(capture(stub))
        } returns
            Unit

        subject =
            BuildingStep(
                name = name,
                requestSpecification = request,
                registerStub = addStubCallback,
                requestType = Input::class,
                formatter = HttpFormatter(),
            )
    }

    @Test
    fun `Should handle respondsWith`() {
        subject.respondsWith<Output> {
            httpStatus = expectedHttpStatus
        }

        verifyStub()
    }

    @Test
    fun `Should handle respondsWithStream`() {
        subject.respondsWithStream<OutputChunk> {
            httpStatus = expectedHttpStatus
        }
        verifyStub()
    }

    @Test
    fun `Should handle respondsWithSseStream`() {
        subject.respondsWithSseStream<Output> {
            httpStatus = expectedHttpStatus
        }
        verifyStub()
    }

    @Test
    fun `Should handle respondsWithStream with typed KClass overload`() {
        subject.respondsWithStream(OutputChunk::class) {
            httpStatus = expectedHttpStatus
        }
        verifyStub()
    }

    @Test
    fun `Should handle respondsWithSseStream with typed KClass overload`() {
        subject.respondsWithSseStream(Output::class) {
            httpStatus = expectedHttpStatus
        }
        verifyStub()
    }

    @Test
    fun `Should log and rethrow exception in respondsWithStream`() =
        testApplication {
            val exception = RuntimeException("boom")
            subject.respondsWithStream(OutputChunk::class) {
                throw exception
            }

            routing {
                get("/test") {
                    val rethrown =
                        shouldThrow<RuntimeException> {
                            stub.captured.responseDefinitionSupplier.invoke(call)
                        }

                    rethrown.message shouldBe "boom"
                    verify {
                        application.log.error(
                            match { it.contains("Failed to build streaming response") },
                            exception,
                        )
                    }
                }
            }

            client.get("/test")
        }

    private fun verifyStub() {
        assertSoftly(stub.captured) {
            configuration.name shouldBe name
            requestSpecification shouldBe request
            responseDefinitionSupplier shouldNotBeNull { }
        }
    }
}
