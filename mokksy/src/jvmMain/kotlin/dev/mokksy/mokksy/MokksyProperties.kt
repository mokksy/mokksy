package dev.mokksy.mokksy

import java.util.Properties

/**
 * Loads Mokksy configuration from a `mokksy.properties` file on the classpath.
 *
 * If the file is absent, all properties fall back to their defaults.
 *
 * Supported properties:
 * | Property                | Values                          | Default   |
 * |-------------------------|---------------------------------|-----------|
 * | `mokksy.io.threads`     | `auto`, `virtual`, `platform`   | `auto`    |
 * | `mokksy.io.parallelism` | `default`, `<n>`, `<n>c`        | `default` |
 *
 * Example `mokksy.properties`:
 * ```properties
 * mokksy.io.threads=auto
 * mokksy.io.parallelism=2c
 * ```
 *
 * @see IoThreadMode
 * @see IoParallelism
 */
public class MokksyProperties private constructor(
    /**
     * The resolved IO thread mode.
     */
    public val ioThreadMode: IoThreadMode,
    /**
     * The resolved IO parallelism setting.
     */
    public val ioParallelism: IoParallelism,
) {
    public companion object {
        private const val FILE_NAME = "mokksy.properties"
        private const val KEY_IO_THREADS = "mokksy.io.threads"
        private const val KEY_IO_PARALLELISM = "mokksy.io.parallelism"

        /**
         * Loads configuration from the classpath `mokksy.properties` file.
         * Returns defaults if the file is not found.
         */
        @JvmStatic
        public fun load(): MokksyProperties {
            val props = Properties()
            val stream =
                Thread
                    .currentThread()
                    .contextClassLoader
                    ?.getResourceAsStream(FILE_NAME)
            stream?.use { props.load(it) }
            return fromProperties(props)
        }

        /**
         * Creates [MokksyProperties] from an existing [Properties] instance.
         * Useful for testing.
         */
        @JvmStatic
        public fun fromProperties(props: Properties): MokksyProperties {
            val threadModeValue = props.getProperty(KEY_IO_THREADS, "auto").trim().lowercase()
            val ioThreadMode =
                when (threadModeValue) {
                    "auto" -> IoThreadMode.AUTO

                    "virtual" -> IoThreadMode.VIRTUAL

                    "platform" -> IoThreadMode.PLATFORM

                    else -> throw IllegalArgumentException(
                        "Invalid value for $KEY_IO_THREADS: '$threadModeValue'. " +
                            "Expected: auto, virtual, platform",
                    )
                }

            val parallelismValue = props.getProperty(KEY_IO_PARALLELISM, "default")
            val ioParallelism = IoParallelism.of(parallelismValue)

            return MokksyProperties(
                ioThreadMode = ioThreadMode,
                ioParallelism = ioParallelism,
            )
        }
    }
}
