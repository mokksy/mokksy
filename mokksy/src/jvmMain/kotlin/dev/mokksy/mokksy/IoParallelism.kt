package dev.mokksy.mokksy

/**
 * Controls the parallelism of Mokksy's IO dispatcher when using [IoThreadMode.PLATFORM].
 * Ignored when virtual threads are active (they scale automatically).
 *
 * Set via `mokksy.io.parallelism` in `mokksy.properties`:
 * ```properties
 * mokksy.io.parallelism=default
 * mokksy.io.parallelism=4
 * mokksy.io.parallelism=2c
 * ```
 *
 * @see MokksyProperties
 */
public sealed class IoParallelism {
    /**
     * Use the default parallelism of [kotlinx.coroutines.Dispatchers.IO].
     */
    public data object Default : IoParallelism()

    /**
     * Fixed thread count.
     *
     * @property count The exact number of threads.
     */
    public data class Fixed(
        val count: Int,
    ) : IoParallelism() {
        init {
            require(count > 0) { "parallelism count must be positive, got $count" }
        }
    }

    /**
     * Multiplier applied to [Runtime.getRuntime().availableProcessors][Runtime.availableProcessors].
     * For example, `ProcessorMultiplier(2f)` on an 8-core machine yields parallelism 16.
     *
     * @property multiplier The multiplier factor.
     */
    public data class ProcessorMultiplier(
        val multiplier: Float,
    ) : IoParallelism() {
        init {
            require(multiplier > 0) { "parallelism multiplier must be positive, got $multiplier" }
        }
    }

    public companion object {
        /**
         * Returns the [Default] parallelism.
         *
         * Kotlin callers can use [Default] directly; this factory is provided for Java ergonomics.
         */
        @JvmStatic
        public fun defaultParallelism(): IoParallelism = Default

        /**
         * Creates a [Fixed] parallelism with the given thread count.
         *
         * ```java
         * IoParallelism p = IoParallelism.fixed(4);
         * ```
         */
        @JvmStatic
        public fun fixed(count: Int): IoParallelism = Fixed(count)

        /**
         * Creates a [ProcessorMultiplier] parallelism.
         *
         * ```java
         * IoParallelism p = IoParallelism.processorMultiplier(2f); // 2× available processors
         * ```
         */
        @JvmStatic
        public fun processorMultiplier(multiplier: Float): IoParallelism =
            ProcessorMultiplier(multiplier)

        /**
         * Parses a parallelism value from a string.
         *
         * Accepted formats:
         * - `"default"` → [Default]
         * - `"4"` → [Fixed] with count 4
         * - `"2c"` → [ProcessorMultiplier] with multiplier 2
         *
         * @throws IllegalArgumentException if the value cannot be parsed.
         */
        @JvmStatic
        public fun of(value: String): IoParallelism {
            val trimmed = value.trim().lowercase()
            return when {
                trimmed == "default" -> {
                    Default
                }

                trimmed.endsWith("c") -> {
                    val multiplier =
                        requireNotNull(trimmed.dropLast(1).toFloatOrNull()) {
                            "Invalid parallelism multiplier: '$value'. Expected format: '<number>c' (e.g. '2c')"
                        }
                    require(multiplier > 0) {
                        "Invalid parallelism multiplier in '$value': must be positive, got $multiplier"
                    }
                    ProcessorMultiplier(multiplier)
                }

                else -> {
                    val count =
                        requireNotNull(trimmed.toIntOrNull()) {
                            "Invalid parallelism value: '$value'. Expected 'default', a number, or '<number>c'"
                        }
                    require(count > 0) {
                        "Invalid parallelism count in '$value': must be positive, got $count"
                    }
                    Fixed(count)
                }
            }
        }
    }
}
