package dev.mokksy.mokksy

import dev.mokksy.mokksy.request.RequestSpecification
import dev.mokksy.mokksy.utils.logger.HttpFormatter
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.get
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class BuildingStepTest {
    private lateinit var subject: BuildingStep<Input>

    private lateinit var name: String

    private lateinit var request: RequestSpecification<Input>
    private lateinit var expectedHttpStatus: HttpStatusCode

    private val registeredStubs = mutableListOf<Stub<*, *>>()

    @BeforeTest
    fun before() {
        name = UUID.randomUUID().toString()
        request = RequestSpecification(requestType = Input::class)
        expectedHttpStatus = HttpStatusCode.fromValue(IntRange(100, 500).random())
        registeredStubs.clear()

        subject =
            BuildingStep(
                name = name,
                requestSpecification = request,
                registerStub = registeredStubs::add,
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

            val stub = registeredStubs.single()

            routing {
                get("/test") {
                    val rethrown =
                        shouldThrow<RuntimeException> {
                            stub.responseDefinitionSupplier.invoke(call)
                        }

                    rethrown.message shouldBe "boom"
                    call.response.status(HttpStatusCode.OK)
                }
            }

            client.get("/test")
        }

    private fun verifyStub() {
        val stub = registeredStubs.single()
        assertSoftly(stub) {
            configuration.name shouldBe name
            requestSpecification shouldBe request
        }
    }
}
