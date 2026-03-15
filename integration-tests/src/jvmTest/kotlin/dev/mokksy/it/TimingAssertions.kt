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

    @JvmStatic
    fun <T> takesAtLeast(minDuration: Duration, block: TimedBlock<T>): T {
        val (result, elapsed) = measureTimedValue { block.execute() }
        assertThat(elapsed.toJavaDuration()).isGreaterThanOrEqualTo(minDuration)
        return result
    }

    @JvmStatic
    fun <T> takesAtLeast(minMillis: Long, block: TimedBlock<T>): T =
        takesAtLeast(Duration.ofMillis(minMillis), block)
}
