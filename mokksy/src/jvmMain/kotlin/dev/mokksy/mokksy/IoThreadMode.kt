package dev.mokksy.mokksy

/**
 * Controls which thread type Mokksy uses for internal IO work
 * (stub matching, response writing, journal recording).
 *
 * Netty's event loops are **not** affected — they always run on platform threads.
 *
 * Set via `mokksy.io.threads` in `mokksy.properties`:
 * ```properties
 * mokksy.io.threads=auto
 * ```
 *
 * @see MokksyProperties
 */
public enum class IoThreadMode {
    /**
     * Use virtual threads if the runtime supports them (Java 21+),
     * otherwise fall back to platform threads silently.
     */
    AUTO,

    /**
     * Force virtual threads. Fails fast at startup if the runtime
     * does not support them (Java < 21).
     */
    VIRTUAL,

    /**
     * Use platform threads via [kotlinx.coroutines.Dispatchers.IO].
     */
    PLATFORM,
}
