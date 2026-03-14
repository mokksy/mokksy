package dev.mokksy.mokksy

import dev.mokksy.mokksy.request.RequestSpecification
import dev.mokksy.mokksy.utils.logger.HttpFormatter
import io.kotest.matchers.collections.shouldHaveSize
import kotlin.test.Test

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
    fun `respondsWith(Consumer) registers a stub`() {
        sut.respondsWith { it.body("ok") }
        registeredStubs shouldHaveSize 1
    }

    @Test
    fun `respondsWith(Class, Consumer) registers a typed stub`() {
        sut.respondsWith(String::class.java) { it.body("typed") }
        registeredStubs shouldHaveSize 1
    }

    // endregion

    // region respondsWithStream

    @Test
    fun `respondsWithStream(Consumer) registers a stub`() {
        sut.respondsWithStream { }
        registeredStubs shouldHaveSize 1
    }

    @Test
    fun `respondsWithStream(Class, Consumer) registers a typed stub`() {
        sut.respondsWithStream(String::class.java) { }
        registeredStubs shouldHaveSize 1
    }

    // endregion
}
