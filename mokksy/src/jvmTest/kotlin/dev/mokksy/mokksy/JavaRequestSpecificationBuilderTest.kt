package dev.mokksy.mokksy

import dev.mokksy.mokksy.request.RequestSpecificationBuilder
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.contain
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlin.test.Test

class JavaRequestSpecificationBuilderTest {
    private val delegate = RequestSpecificationBuilder(String::class)
    private val sut = JavaRequestSpecificationBuilder(delegate)

    // region path

    @Test
    fun `path(String) sets path matcher and returns this`() {
        val result = sut.path("/api/items")
        assertSoftly {
            result shouldBe sut
            delegate.build().path shouldNotBe null
        }
    }

    @Test
    fun `path(Matcher) sets path matcher and returns this`() {
        val matcher = contain("/api")
        val result = sut.path(matcher)
        assertSoftly {
            result shouldBe sut
            delegate.build().path shouldBeSameInstanceAs matcher
        }
    }

    // endregion

    // region bodyContains

    @Test
    fun `bodyContains adds body string matchers and returns this`() {
        val result = sut.bodyContains("foo", "bar")
        assertSoftly {
            result shouldBe sut
            delegate.build().bodyString shouldHaveSize 2
        }
    }

    // endregion

    // region bodyMatchesPredicate

    @Test
    fun `bodyMatchesPredicate adds body matcher and returns this`() {
        val result = sut.bodyMatchesPredicate { it.contains("x") }
        assertSoftly {
            result shouldBe sut
            delegate.build().body shouldHaveSize 1
        }
    }

    @Test
    fun `bodyMatchesPredicate null body does not match`() {
        sut.bodyMatchesPredicate { it.isNotEmpty() }
        val matcher = delegate.build().body.single()
        matcher.test(null).passed() shouldBe false
    }

    @Test
    fun `bodyMatchesPredicate with description adds body matcher and returns this`() {
        val result = sut.bodyMatchesPredicate("contains x") { it.contains("x") }
        assertSoftly {
            result shouldBe sut
            delegate.build().body shouldHaveSize 1
        }
    }

    // endregion

    // region containsHeader

    @Test
    fun `containsHeader adds header matcher and returns this`() {
        val result = sut.containsHeader("X-Api-Key", "secret")
        assertSoftly {
            result shouldBe sut
            delegate.build().headers shouldHaveSize 1
        }
    }

    // endregion

    // region priority

    @Test
    fun `priority sets priority and returns this`() {
        val result = sut.priority(5)
        assertSoftly {
            result shouldBe sut
            delegate.build().priority shouldBe 5
        }
    }

    // endregion
}
