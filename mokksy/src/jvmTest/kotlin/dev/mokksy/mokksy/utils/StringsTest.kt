package dev.mokksy.mokksy.utils

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class StringsTest {

    @Test
    fun `ellipsizeMiddle returns null unchanged`() {
        null.ellipsizeMiddle(10) shouldBe null
    }

    @Test
    fun `ellipsizeMiddle returns string unchanged when shorter than maxLength`() {
        "hi".ellipsizeMiddle(10) shouldBe "hi"
    }

    @Test
    fun `ellipsizeMiddle returns string unchanged when equal to maxLength`() {
        "hello".ellipsizeMiddle(5) shouldBe "hello"
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 2, 3, 4])
    fun `ellipsizeMiddle returns string unchanged when maxLength less than 5`(maxLength: Int) {
        "this is a long string".ellipsizeMiddle(maxLength) shouldBe "this is a long string"
    }

    @ParameterizedTest
    @CsvSource(
        "abcdefghij, 5, a...j",
        "abcdefghij, 6, ab...j",
        "abcdefghij, 7, ab...ij",
        "abcdefghij, 8, abc...ij",
        "abcdefghij, 9, abc...hij",
    )
    fun `ellipsizeMiddle truncates middle with ellipsis`(
        input: String,
        maxLength: Int,
        expected: String,
    ) {
        input.ellipsizeMiddle(maxLength) shouldBe expected
    }
}
