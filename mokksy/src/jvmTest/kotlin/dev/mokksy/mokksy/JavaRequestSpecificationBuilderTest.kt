package dev.mokksy.mokksy

import dev.mokksy.mokksy.request.RequestSpecificationBuilder
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.contain
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlin.test.Test

@OptIn(ExperimentalMokksyApi::class)
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

    // region body

    @Test
    fun `body with Consumer configures form and returns this`() {
        val result = sut.body { b -> b.form { form -> form.field("key", "value") } }
        assertSoftly {
            result shouldBe sut
            delegate.build().formSpecs shouldHaveSize 1
        }
    }

    @Test
    fun `body with Consumer configures predicate and returns this`() {
        val result = sut.body { b -> b.predicate { it.isNotEmpty() } }
        assertSoftly {
            result shouldBe sut
            delegate.build().body shouldHaveSize 1
        }
    }

    @Test
    fun `body with Consumer configures both form and predicate`() {
        sut.body { b ->
            b.form { form -> form.field("key", "value") }
            b.predicate { it.isNotEmpty() }
        }
        val built = delegate.build()
        assertSoftly {
            built.formSpecs shouldHaveSize 1
            built.body shouldHaveSize 1
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

    // region body (String/Predicate overloads)

    @Test
    fun `bodyTextMatches with Predicate adds body string matcher and returns this`() {
        val result = sut.bodyTextMatches { it != null && it.contains("token") }
        assertSoftly {
            result shouldBe sut
            delegate.build().bodyString shouldHaveSize 1
        }
    }

    @Test
    fun `bodyTextMatches with description adds body string matcher and returns this`() {
        val result =
            sut.bodyTextMatches("contains token") { it != null && it.contains("token") }
        assertSoftly {
            result shouldBe sut
            delegate.build().bodyString shouldHaveSize 1
        }
    }

    @Test
    fun `body with exact value matches exact string`() {
        sut.bodyText("hello world")
        val built = delegate.build()
        assertSoftly {
            built.bodyString shouldHaveSize 1
            built.bodyString[0].test("hello world").passed() shouldBe true
            built.bodyString[0].test("hello").passed() shouldBe false
            built.bodyString[0].test(null).passed() shouldBe false
        }
    }

    @Test
    fun `multiple body calls add multiple matchers`() {
        sut.bodyTextMatches { it != null && it.contains("foo") }
        sut.bodyTextMatches { it != null && it.contains("bar") }
        val built = delegate.build()
        assertSoftly {
            built.bodyString shouldHaveSize 2
            built.bodyString[0].test("foo bar").passed() shouldBe true
            built.bodyString[1].test("foo bar").passed() shouldBe true
            built.bodyString[0].test("only foo").passed() shouldBe true
            built.bodyString[1].test("only foo").passed() shouldBe false
        }
    }

    @Test
    fun `body Predicate null body does not match`() {
        sut.bodyTextMatches { it != null && it.isNotEmpty() }
        val built = delegate.build()
        built.bodyString
            .single()
            .test(null)
            .passed() shouldBe false
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

    // region cookies

    @Test
    fun `cookie exact value adds cookie matcher and returns this`() {
        val result = sut.cookie("session", "abc")
        assertSoftly {
            result shouldBe sut
            delegate.build().cookies shouldHaveSize 1
        }
    }

    @Test
    fun `cookieMatches adds cookie matcher and returns this`() {
        val result = sut.cookieMatches("session") { it?.startsWith("abc") == true }
        assertSoftly {
            result shouldBe sut
            delegate.build().cookies shouldHaveSize 1
        }
    }

    @Test
    fun `cookieAbsent adds cookie matcher and returns this`() {
        val result = sut.cookieAbsent("session")
        assertSoftly {
            result shouldBe sut
            delegate.build().cookies shouldHaveSize 1
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
