package dev.mokksy.mokksy

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.test.runTest

internal class MokksyServerTest {

    @Test
    fun `should throw IllegalStateException when awaitStarted times out`() =
        runTest {
            val server = MokksyServer(configuration = ServerConfiguration())

            val exception = shouldThrow<IllegalStateException> {
                server.awaitStarted(timeout = 100.milliseconds)
            }

            exception.message shouldContain "100ms"
        }
}
