package dev.mokksy.mokksy

import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

/**
 * Configuration for a stub's lifecycle and logging behaviour.
 *
 * @property name Optional human-readable name used in log and error output. Defaults to `null`.
 * @property eventuallyRemove When `true`, the stub becomes ineligible for matching immediately after
 *           its first match and is removed from the registry asynchronously. Subsequent requests
 *           will not match this stub even if the physical removal has not yet been applied.
 *           Defaults to `false`.
 * @property verbose Enables per-stub `DEBUG`-level logging when `true`. Defaults to `false`.
 */
public data class StubConfiguration
    @JvmOverloads
    constructor(
        val name: String? = null,
        val eventuallyRemove: Boolean = false,
        val verbose: Boolean = false,
    ) {
    /**
     * Deprecated alias for [eventuallyRemove], preserved for binary and source compatibility.
     *
     * Existing compiled callers that reference `getRemoveAfterMatch()` on the JVM will continue
     * to work without `NoSuchMethodError`. Kotlin callers reading this property will receive a
     * deprecation warning and an IDE quick-fix to migrate to [eventuallyRemove].
     *
     * Note: the named constructor argument `StubConfiguration(removeAfterMatch = …)` cannot be
     * preserved — use `StubConfiguration(eventuallyRemove = …)` or the companion factory
     * [StubConfiguration.removeAfterMatch] for a deprecated named-argument form.
     */
    @Deprecated(
        "Renamed to eventuallyRemove",
        ReplaceWith("eventuallyRemove"),
        level = DeprecationLevel.WARNING,
    )
    val removeAfterMatch: Boolean
        get() = eventuallyRemove

    public companion object {
        /**
         * Deprecated factory for callers that previously used `StubConfiguration(removeAfterMatch = …)`.
         *
         * Migrate to the primary constructor: `StubConfiguration(eventuallyRemove = …)`.
         */
        @Deprecated(
            "Use StubConfiguration(name, eventuallyRemove, verbose) instead",
            ReplaceWith(
                "StubConfiguration(name = name, eventuallyRemove = removeAfterMatch, verbose = verbose)",
            ),
            level = DeprecationLevel.WARNING,
        )
        @JvmStatic
        @JvmOverloads
        public fun removeAfterMatch(
            name: String? = null,
            removeAfterMatch: Boolean = false,
            verbose: Boolean = false,
        ): StubConfiguration =
            StubConfiguration(name = name, eventuallyRemove = removeAfterMatch, verbose = verbose)
    }
}
