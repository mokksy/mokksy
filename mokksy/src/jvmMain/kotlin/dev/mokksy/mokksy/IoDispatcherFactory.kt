package dev.mokksy.mokksy

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.ExecutorService
import kotlin.math.roundToInt

/**
 * Result of [IoDispatcherFactory.create], bundling the dispatcher with a human-readable
 * description for startup logging.
 *
 * @property dispatcher The configured dispatcher, or `null` when Ktor's default is sufficient.
 * @property description Log-friendly summary, e.g. `"virtual threads"` or
 *           `"platform threads (parallelism=16)"`.
 */
internal data class IoDispatcherResult(
    val dispatcher: CoroutineDispatcher?,
    val description: String,
)

private const val MAX_PLATFORM_PARALLELISM_DEFAULT = 64

/**
 * Creates a [CoroutineDispatcher] based on [IoThreadMode] and [IoParallelism] configuration.
 *
 * When virtual threads are used, the returned dispatcher wraps an [ExecutorService].
 * Cast to [kotlinx.coroutines.ExecutorCoroutineDispatcher] and call `close()`
 * when the server shuts down.
 *
 * @see MokksyProperties
 */
internal object IoDispatcherFactory {
    val virtualThreadsAvailable: Boolean =
        try {
            Thread::class.java.getMethod("ofVirtual")
            true
        } catch (_: NoSuchMethodException) {
            false
        }

    /**
     * Creates a [IoDispatcherResult] for the given configuration.
     *
     * @throws IllegalStateException if [IoThreadMode.VIRTUAL] is requested but unavailable.
     */
    fun create(
        mode: IoThreadMode,
        parallelism: IoParallelism,
    ): IoDispatcherResult =
        when (mode) {
            IoThreadMode.AUTO -> createAutoResult(parallelism)
            IoThreadMode.VIRTUAL -> createVirtualResult()
            IoThreadMode.PLATFORM -> createPlatformResult(parallelism)
        }

    // region internal

    private fun createAutoResult(parallelism: IoParallelism): IoDispatcherResult =
        if (virtualThreadsAvailable) {
            createVirtualResult()
        } else {
            createPlatformResult(parallelism)
        }

    private fun createVirtualResult(): IoDispatcherResult {
        check(virtualThreadsAvailable) {
            "Virtual threads are not available. Requires Java 21+. " +
                "Set mokksy.io.threads=auto or mokksy.io.threads=platform to use platform threads."
        }
        val executor = newVirtualThreadPerTaskExecutor()
        return IoDispatcherResult(
            dispatcher = executor.asCoroutineDispatcher(),
            description = "virtual threads",
        )
    }

    @Suppress("InjectDispatcher") // Internal factory; tested via IoDispatcherResult output
    private fun createPlatformResult(parallelism: IoParallelism): IoDispatcherResult {
        val count = resolveParallelism(parallelism)
        val dispatcher = count?.let { Dispatchers.IO.limitedParallelism(it) }
        val effectiveCount = count ?: defaultIoParallelism()
        return IoDispatcherResult(
            dispatcher = dispatcher,
            description = "platform threads (parallelism=$effectiveCount)",
        )
    }

    private fun resolveParallelism(parallelism: IoParallelism): Int? =
        when (parallelism) {
            is IoParallelism.Default -> {
                null
            }

            is IoParallelism.Fixed -> {
                parallelism.count
            }

            is IoParallelism.ProcessorMultiplier -> {
                maxOf(
                    1,
                    (Runtime.getRuntime().availableProcessors() * parallelism.multiplier)
                        .roundToInt(),
                )
            }
        }

    /**
     * Mirrors kotlinx.coroutines' own default for [kotlinx.coroutines.Dispatchers.IO]:
     * the system property `kotlinx.coroutines.io.parallelism` when set, otherwise
     * `max(64, availableProcessors)`.
     */
    private fun defaultIoParallelism(): Int =
        System.getProperty("kotlinx.coroutines.io.parallelism")?.toIntOrNull()
            ?: maxOf(
                MAX_PLATFORM_PARALLELISM_DEFAULT,
                Runtime.getRuntime().availableProcessors(),
            )

    /**
     * Reflective call to `Executors.newVirtualThreadPerTaskExecutor()` (Java 21+).
     * The project targets JVM 17, so this API is not available at compile time.
     */
    private fun newVirtualThreadPerTaskExecutor(): ExecutorService {
        val executors = Class.forName("java.util.concurrent.Executors")
        val method = executors.getMethod("newVirtualThreadPerTaskExecutor")
        return method.invoke(null) as ExecutorService
    }

    // endregion
}
