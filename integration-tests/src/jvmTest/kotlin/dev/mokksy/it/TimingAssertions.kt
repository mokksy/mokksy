package dev.mokksy.it

import org.assertj.core.api.Assertions.assertThat
import java.time.Duration
import kotlin.time.measureTimedValue
import kotlin.time.toJavaDuration

/**
 * Functional interface for a block that produces a value and may throw a checked exception.
 * Supports SAM conversion from both Java and Kotlin lambdas.
 */
fun interface TimedBlock<T> {
    /**
     * Execute the block and produce its result.
     *
     * @return The value produced by the block.
     * @throws Exception If an error occurs during execution.
     */
    @Throws(Exception::class)
    fun execute(): T
}

/**
 * Executes [block] and asserts the elapsed wall-clock time is >= [minDuration].
 *
 * Java:
 * ```java
 * var response = TimingAssertions.takesAtLeast(150L, () -> get("/path"));
 * ```
 *
 * Kotlin:
 * ```kotlin
 * val response = TimingAssertions.takesAtLeast(150L) { get("/path") }
 * ```
 */
object TimingAssertions {

    /**
     * Asserts that executing the given block takes at least the specified wall-clock duration and returns the block's result.
     *
     * @param minDuration The minimum elapsed wall-clock duration required for the block's execution.
     * @param block The action to execute; its result is returned when the assertion passes.
     * @return The value produced by [block].
     */
    @JvmStatic
    fun <T> takesAtLeast(minDuration: Duration, block: TimedBlock<T>): T {
        val (result, elapsed) = measureTimedValue { block.execute() }
        assertThat(elapsed.toJavaDuration()).isGreaterThanOrEqualTo(minDuration)
        return result
    }

    /**
         * Asserts that executing the provided block takes at least the given number of milliseconds and returns the block's result.
         *
         * @param minMillis Minimum elapsed time in milliseconds required for the assertion to pass.
         * @param block The timed block to execute.
         * @return The value produced by the executed block.
         */
        @JvmStatic
    fun <T> takesAtLeast(minMillis: Long, block: TimedBlock<T>): T =
        takesAtLeast(Duration.ofMillis(minMillis), block)
}
