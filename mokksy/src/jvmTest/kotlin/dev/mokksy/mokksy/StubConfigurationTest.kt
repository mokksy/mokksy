package dev.mokksy.mokksy

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.Test

class StubConfigurationTest {
    // region toString

    @Test
    fun `toString without name omits name field`() {
        val config = StubConfiguration(eventuallyRemove = true, verbose = false)
        config.toString() shouldBe "StubConfiguration(eventuallyRemove=true, verbose=false)"
    }

    @Test
    fun `toString with name includes name field`() {
        val config = StubConfiguration(name = "my-stub", eventuallyRemove = false, verbose = true)
        config.toString() shouldBe
            "StubConfiguration(name=my-stub, eventuallyRemove=false, verbose=true)"
    }

    @Test
    fun `toString default instance produces consistent format`() {
        val config = StubConfiguration()
        config.toString() shouldBe "StubConfiguration(eventuallyRemove=false, verbose=false)"
    }

    // endregion

    // region removeAfterMatch deprecated property

    @Suppress("DEPRECATION")
    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `removeAfterMatch property delegates to eventuallyRemove`(value: Boolean) {
        StubConfiguration(eventuallyRemove = value).removeAfterMatch shouldBe value
    }

    // endregion

    // region removeAfterMatch companion factory (deprecated)

    @Suppress("DEPRECATION")
    @Test
    fun `removeAfterMatch factory with no args creates default configuration`() {
        val config = StubConfiguration.removeAfterMatch()
        assertSoftly(config) {
            name shouldBe null
            eventuallyRemove shouldBe false
            verbose shouldBe false
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun `removeAfterMatch factory with name sets name`() {
        val config = StubConfiguration.removeAfterMatch("my-stub")
        assertSoftly(config) {
            name shouldBe "my-stub"
            eventuallyRemove shouldBe false
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun `removeAfterMatch factory with removeAfterMatch true sets eventuallyRemove`() {
        val config = StubConfiguration.removeAfterMatch("once", removeAfterMatch = true)
        assertSoftly(config) {
            name shouldBe "once"
            eventuallyRemove shouldBe true
        }
    }

    // endregion
}
