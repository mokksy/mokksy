package dev.mokksy.mokksy

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledForJreRange
import org.junit.jupiter.api.condition.JRE

class IoDispatcherFactoryTest {
    // region PLATFORM mode

    @Test
    fun `platform with default parallelism returns null dispatcher and logs actual thread count`() {
        val result = IoDispatcherFactory.create(IoThreadMode.PLATFORM, IoParallelism.Default)
        val expectedCount = maxOf(64, Runtime.getRuntime().availableProcessors())
        result.dispatcher.shouldBeNull()
        result.description shouldContain "platform"
        result.description shouldContain expectedCount.toString()
    }

    @Test
    fun `platform with fixed parallelism returns limited dispatcher`() {
        val result = IoDispatcherFactory.create(IoThreadMode.PLATFORM, IoParallelism.Fixed(4))
        result.dispatcher.shouldNotBeNull()
        result.description shouldContain "4"
    }

    @Test
    fun `platform with processor multiplier returns limited dispatcher`() {
        val result =
            IoDispatcherFactory.create(
                IoThreadMode.PLATFORM,
                IoParallelism.ProcessorMultiplier(2f),
            )
        result.dispatcher.shouldNotBeNull()
        val expectedParallelism = Runtime.getRuntime().availableProcessors() * 2
        result.description shouldContain "$expectedParallelism"
    }

    // endregion

    // region AUTO mode

    @Test
    fun `auto mode does not throw`() {
        val result = IoDispatcherFactory.create(IoThreadMode.AUTO, IoParallelism.Default)
        if (IoDispatcherFactory.virtualThreadsAvailable) {
            result.dispatcher.shouldNotBeNull()
            result.description shouldContain "virtual"
            (result.dispatcher as? ExecutorCoroutineDispatcher)?.close()
        } else {
            result.dispatcher.shouldBeNull()
            result.description shouldContain "platform"
        }
    }

    // endregion

    // region VIRTUAL mode

    @Test
    @EnabledForJreRange(min = JRE.JAVA_21)
    fun `virtual mode on supported JVM returns executor dispatcher`() {
        val result = IoDispatcherFactory.create(IoThreadMode.VIRTUAL, IoParallelism.Default)
        result.dispatcher.shouldNotBeNull()
        (result.dispatcher is ExecutorCoroutineDispatcher) shouldBe true
        result.description shouldContain "virtual"
        (result.dispatcher as ExecutorCoroutineDispatcher).close()
    }

    // endregion
}
