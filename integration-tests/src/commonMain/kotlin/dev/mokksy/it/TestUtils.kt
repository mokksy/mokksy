@file:OptIn(ExperimentalAtomicApi::class)

package dev.mokksy.it

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.withTimeout
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.update
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val integrationTestScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

private val jobsRef = AtomicReference(emptyList<Deferred<Unit>>())

/**
 * Executes an integration test within a coroutine scope.
 *
 * The provided block is run asynchronously,
 * allowing for the concurrent execution of test code.
 *
 * @param block A suspending function to define the test logic,
 * executed within a `CoroutineScope`.
 */
fun runIntegrationTest(block: suspend CoroutineScope.() -> Unit) {
    val job = integrationTestScope.async { block() }
    // Atomically add the job
    jobsRef.update { it + job }
}

/**
 * Cancels and waits for all running jobs to complete within a specified timeout period.
 *
 * If a job does not terminate within the timeout, a warning message is displayed.
 * After processing all jobs, the coroutine scope is cancelled.
 *
 * @param duration The maximum amount of time to wait for each job to cancel and complete.
 * The default value is one second.
 */
suspend fun shutdownTests(duration: Duration = 1.seconds) {
    val jobsCopy = jobsRef.load() // snapshot
    for (job in jobsCopy) {
        if (job.isActive) {
            runCatching {
                withTimeout(duration) { job.cancelAndJoin() }
            }.onFailure {
                if (it is TimeoutCancellationException) {
                    println("⚠️ Warning: job $job did not terminate within $duration")
                } else if (it is CancellationException) {
                    throw it
                }
            }
        }
    }
    integrationTestScope.cancel()
    jobsRef.store(emptyList())
}
