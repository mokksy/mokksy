package dev.mokksy.it

import dev.mokksy.mokksy.ExperimentalMokksyApi
import dev.mokksy.mokksy.Mokksy
import io.ktor.client.HttpClient
import kotlin.time.Duration.Companion.seconds

/**
 * Shared fixture for common integration tests.
 *
 * These tests intentionally create and start Mokksy once and never shut it down after each test.
 * This verifies a scenario when Mokksy is started once and used across multiple tests.
 */
@OptIn(ExperimentalMokksyApi::class)
internal object MokksyITFixture {
    val mokksy = Mokksy()

    val client = HttpClient()

    init {
        runIntegrationTest {
            mokksy.startSuspend()
            mokksy.awaitStarted()
        }

        runIntegrationTest {
            awaitIntegrationTests()

            mokksy.shutdownSuspend()
            shutdownTests(1.seconds)
        }
    }

    fun ensureInitialized() {
        // Intentionally empty: calling this forces object initialization before test jobs are enqueued.
    }
}

@OptIn(ExperimentalMokksyApi::class)
internal abstract class MokksyIntegrationTest {
    init {
        MokksyITFixture.ensureInitialized()
    }

    protected val mokksy
        get() = MokksyITFixture.mokksy

    protected val client: HttpClient
        get() = MokksyITFixture.client
}
