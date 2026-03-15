package dev.mokksy.mokksy

import dev.mokksy.mokksy.request.RequestSpecification
import dev.mokksy.mokksy.response.ResponseDefinition
import dev.mokksy.mokksy.response.StreamResponseDefinition
import dev.mokksy.mokksy.utils.logger.HttpFormatter
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.testing.testApplication
import kotlin.test.Test

@OptIn(InternalMokksyApi::class)
class JavaBuildingStepTest {
    private val registeredStubs = mutableListOf<Stub<*, *>>()
    private val step =
        BuildingStep(
            name = "test",
            requestSpecification = RequestSpecification(requestType = String::class),
            registerStub = registeredStubs::add,
            requestType = String::class,
            formatter = HttpFormatter(),
        )
    private val sut = JavaBuildingStep(step)

    // region respondsWith

    @Test
    fun `respondsWith(Consumer) registers a stub and consumer body is set`() {
        sut.respondsWith { it.body("ok") }
        registeredStubs shouldHaveSize 1
        val supplier = registeredStubs.single().responseDefinitionSupplier
        testApplication {
            routing {
                get("/test") {
                    @Suppress("UNCHECKED_CAST")
                    val definition = supplier.invoke(call) as ResponseDefinition<*, *>
                    call.respondText("${definition.body}")
                }
            }
            client.get("/test").bodyAsText() shouldBe "ok"
        }
    }

    @Test
    fun `respondsWith(Class, Consumer) registers a typed stub and consumer body is set`() {
        sut.respondsWith(String::class.java) { it.body("typed") }
        registeredStubs shouldHaveSize 1
        val supplier = registeredStubs.single().responseDefinitionSupplier
        testApplication {
            routing {
                get("/test") {
                    @Suppress("UNCHECKED_CAST")
                    val definition = supplier.invoke(call) as ResponseDefinition<*, *>
                    call.respondText("${definition.body}")
                }
            }
            client.get("/test").bodyAsText() shouldBe "typed"
        }
    }

    // endregion

    // region respondsWithStream

    @Test
    fun `respondsWithStream(Consumer) registers a stub and consumer chunk is set`() {
        sut.respondsWithStream { it.chunk("stream-item") }
        registeredStubs shouldHaveSize 1
        val supplier = registeredStubs.single().responseDefinitionSupplier
        testApplication {
            routing {
                get("/test") {
                    @Suppress("UNCHECKED_CAST")
                    val definition = supplier.invoke(call) as StreamResponseDefinition<*, *>
                    call.respondText("${definition.chunks?.size}")
                }
            }
            client.get("/test").bodyAsText() shouldBe "1"
        }
    }

    @Test
    fun `respondsWithStream(Class, Consumer) registers a typed stub and consumer chunk is set`() {
        sut.respondsWithStream(String::class.java) { it.chunk("typed-stream") }
        registeredStubs shouldHaveSize 1
        val supplier = registeredStubs.single().responseDefinitionSupplier
        testApplication {
            routing {
                get("/test") {
                    @Suppress("UNCHECKED_CAST")
                    val definition = supplier.invoke(call) as StreamResponseDefinition<*, *>
                    call.respondText("${definition.chunks?.size}")
                }
            }
            client.get("/test").bodyAsText() shouldBe "1"
        }
    }

    // endregion
}
