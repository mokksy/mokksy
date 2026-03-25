package dev.mokksy.mokksy

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test

class IoParallelismTest {
    // region of — valid values

    @Test
    fun `parse 'default' returns Default`() {
        IoParallelism.of("default") shouldBe IoParallelism.Default
    }

    @Test
    fun `parse 'DEFAULT' is case-insensitive`() {
        IoParallelism.of("DEFAULT") shouldBe IoParallelism.Default
    }

    @Test
    fun `parse fixed number returns Fixed`() {
        val result = IoParallelism.of("4")
        assertSoftly(result.shouldBeInstanceOf<IoParallelism.Fixed>()) {
            count shouldBe 4
        }
    }

    @Test
    fun `parse processor multiplier returns ProcessorMultiplier`() {
        val result = IoParallelism.of("2c")
        assertSoftly(result.shouldBeInstanceOf<IoParallelism.ProcessorMultiplier>()) {
            multiplier shouldBe 2f
        }
    }

    @Test
    fun `parse fractional processor multiplier`() {
        val result = IoParallelism.of("1.5c")
        assertSoftly(result.shouldBeInstanceOf<IoParallelism.ProcessorMultiplier>()) {
            multiplier shouldBe 1.5f
        }
    }

    @Test
    fun `parse trims whitespace`() {
        IoParallelism.of("  default  ") shouldBe IoParallelism.Default
    }

    @Test
    fun `parse uppercase C multiplier`() {
        val result = IoParallelism.of("3C")
        assertSoftly(result.shouldBeInstanceOf<IoParallelism.ProcessorMultiplier>()) {
            multiplier shouldBe 3f
        }
    }

    // endregion

    // region of — invalid values

    @Test
    fun `parse invalid string throws`() {
        shouldThrow<IllegalArgumentException> {
            IoParallelism.of("abc")
        }
    }

    @Test
    fun `parse empty multiplier throws`() {
        shouldThrow<IllegalArgumentException> {
            IoParallelism.of("c")
        }
    }

    @Test
    fun `parse zero fixed throws`() {
        shouldThrow<IllegalArgumentException> {
            IoParallelism.of("0")
        }
    }

    @Test
    fun `parse negative fixed throws`() {
        shouldThrow<IllegalArgumentException> {
            IoParallelism.of("-1")
        }
    }

    @Test
    fun `parse zero multiplier throws`() {
        shouldThrow<IllegalArgumentException> {
            IoParallelism.of("0c")
        }
    }

    // endregion
}
