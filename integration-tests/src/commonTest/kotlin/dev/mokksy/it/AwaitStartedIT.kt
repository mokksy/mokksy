@file:OptIn(ExperimentalMokksyApi::class)

package dev.mokksy.it

import dev.mokksy.mokksy.ExperimentalMokksyApi
import dev.mokksy.mokksy.MokksyServer
import dev.mokksy.mokksy.ServerConfiguration
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

internal class AwaitStartedIT {
    @Test
    fun `awaitStarted with explicit timeout returns when server starts`() =
        runIntegrationTest {
            val server = MokksyServer(configuration = ServerConfiguration(verbose = false))

            coroutineScope {
                val job = async { server.startSuspend() }
                server.awaitStarted(timeout = 5.seconds)
                job.await()
            }

            server.port() shouldBe 0
        }

    @Test
    fun `awaitStarted without timeout returns when server starts`() =
        runIntegrationTest {
            val server = MokksyServer(configuration = ServerConfiguration(verbose = false))

            coroutineScope {
                val job = async { server.startSuspend() }
                server.awaitStarted()
                job.await()
            }

            server.port() shouldBe 0
        }

    @Test
    fun `awaitStarted without timeout returns immediately when server already started`() =
        runIntegrationTest {
            val server = MokksyServer(configuration = ServerConfiguration(verbose = false))

            server.startSuspend(wait = true)
            server.awaitStarted()
        }

    @Test
    fun `awaitStarted with explicit timeout returns immediately when server already started`() =
        runIntegrationTest {
            val server = MokksyServer(configuration = ServerConfiguration(verbose = false))

            server.startSuspend(wait = true)
            server.awaitStarted(timeout = 5.seconds)
        }
}
