package dev.mokksy.mokksy

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.util.Properties
import kotlin.test.Test

class MokksyPropertiesTest {

    // region defaults

    @Test
    fun `empty properties return auto and default`() {
        val result = MokksyProperties.fromProperties(Properties())
        assertSoftly(result) {
            ioThreadMode shouldBe IoThreadMode.AUTO
            ioParallelism shouldBe IoParallelism.Default
        }
    }

    // endregion

    // region mokksy.io.threads

    @Test
    fun `parses virtual thread mode`() {
        val props = Properties().apply { setProperty("mokksy.io.threads", "virtual") }
        MokksyProperties.fromProperties(props).ioThreadMode shouldBe IoThreadMode.VIRTUAL
    }

    @Test
    fun `parses platform thread mode`() {
        val props = Properties().apply { setProperty("mokksy.io.threads", "platform") }
        MokksyProperties.fromProperties(props).ioThreadMode shouldBe IoThreadMode.PLATFORM
    }

    @Test
    fun `parses auto thread mode case-insensitive`() {
        val props = Properties().apply { setProperty("mokksy.io.threads", "AUTO") }
        MokksyProperties.fromProperties(props).ioThreadMode shouldBe IoThreadMode.AUTO
    }

    @Test
    fun `invalid thread mode throws`() {
        val props = Properties().apply { setProperty("mokksy.io.threads", "bogus") }
        shouldThrow<IllegalArgumentException> {
            MokksyProperties.fromProperties(props)
        }
    }

    // endregion

    // region mokksy.io.parallelism

    @Test
    fun `parses fixed parallelism`() {
        val props = Properties().apply { setProperty("mokksy.io.parallelism", "8") }
        val result = MokksyProperties.fromProperties(props)
        result.ioParallelism.shouldBeInstanceOf<IoParallelism.Fixed>().count shouldBe 8
    }

    @Test
    fun `parses processor multiplier parallelism`() {
        val props = Properties().apply { setProperty("mokksy.io.parallelism", "2c") }
        val result = MokksyProperties.fromProperties(props)
        result.ioParallelism.shouldBeInstanceOf<IoParallelism.ProcessorMultiplier>().multiplier shouldBe 2f
    }

    // endregion

    // region load from classpath

    @Test
    fun `load returns defaults when no file on classpath`() {
        val result = MokksyProperties.load()
        assertSoftly(result) {
            ioThreadMode shouldBe IoThreadMode.AUTO
            ioParallelism shouldBe IoParallelism.Default
        }
    }

    // endregion
}
