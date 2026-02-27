package dev.mokksy.mokksy.kotest

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

internal class MatchersTest {
    // region doesNotContainIgnoringCase

    @Test
    fun `doesNotContainIgnoringCase - should pass when value is null or substring is absent`() {
        val matcher = doesNotContainIgnoringCase("foo")
        matcher.test(null).passed() shouldBe true
        matcher.test("bar baz").passed() shouldBe true
    }

    @Test
    fun `doesNotContainIgnoringCase - should fail and report error when substring is present`() {
        assertSoftly(doesNotContainIgnoringCase("foo").test("foobar")) {
            passed() shouldBe false
            failureMessage() shouldContain "should not contain"
            failureMessage() shouldContain "case insensitive"
        }
    }

    @Test
    fun `doesNotContainIgnoringCase - should fail when substring matches in different case`() {
        doesNotContainIgnoringCase("foo").test("FOObar").passed() shouldBe false
    }

    // endregion
    // region doesNotContain

    @Test
    fun `doesNotContain - should pass when value is null or substring is absent`() {
        val matcher = doesNotContain("foo")
        matcher.test(null).passed() shouldBe true
        matcher.test("bar baz").passed() shouldBe true
    }

    @Test
    fun `doesNotContain - should fail and report error when substring is present`() {
        assertSoftly(doesNotContain("foo").test("foobar")) {
            passed() shouldBe false
            failureMessage() shouldContain "should not contain"
            failureMessage() shouldContain "case sensitive"
        }
    }

    @Test
    fun `doesNotContain - should pass when substring matches only in different case`() {
        doesNotContain("foo").test("FOObar").passed() shouldBe true
    }

    // endregion
    // region objectEquals

    @Test
    fun `objectEquals - should pass for equal values and fail for different values`() {
        val matcher = objectEquals("expected", "param")
        matcher.test("expected").passed() shouldBe true
        matcher.test("actual").passed() shouldBe false
    }

    @Test
    fun `objectEquals - should handle null request`() {
        val matcher = objectEquals<String>(null, "param")
        matcher.test(null).passed() shouldBe true
        matcher.test("something").passed() shouldBe false
    }

    @Test
    fun `objectEquals - param name should appear in failure message and toString`() {
        val matcher = objectEquals("expected", "myParam")
        matcher.test("other").failureMessage() shouldContain "myParam"
        matcher.toString() shouldContain "myParam"
    }

    // endregion
}
