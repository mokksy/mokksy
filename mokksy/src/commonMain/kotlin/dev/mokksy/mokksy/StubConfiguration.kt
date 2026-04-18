package dev.mokksy.mokksy

import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmRecord
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
@JvmRecord
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
         * @deprecated Deprecated since 0.9.2
         */
        @Deprecated(
            "Renamed to eventuallyRemove",
            ReplaceWith("eventuallyRemove"),
            level = DeprecationLevel.WARNING,
        )
        public val removeAfterMatch: Boolean
            get() = eventuallyRemove

        public companion object {
            /**
             * Creates a [StubConfiguration] that is removed after its first match.
             *
             * Java-friendly alternative to `StubConfiguration(eventuallyRemove = true)`:
             *
             * ```java
             * mokksy.get(StubConfiguration.once("my-stub"), "/path")
             *     .respondsWith("one-time response");
             * ```
             *
             * @param name Optional human-readable name for log and error output.
             * @return A new [StubConfiguration] with [eventuallyRemove] set to `true`.
             */
            @JvmStatic
            @JvmOverloads
            public fun once(name: String? = null): StubConfiguration =
                StubConfiguration(name = name, eventuallyRemove = true)

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
                StubConfiguration(
                    name = name,
                    eventuallyRemove = removeAfterMatch,
                    verbose = verbose,
                )
        }

        /**
         * Secondary constructor for creating a [StubConfiguration] instance.
         *
         * This constructor is deprecated and should be replaced
         * with the `StubConfiguration(eventuallyRemove)` constructor.
         *
         * @param removeAfterMatch Indicates whether the stub should be eventually removed after it has been matched.
         * @deprecated Use `StubConfiguration(eventuallyRemove)` instead.
         */
        @Deprecated(
            message = "Use StubConfiguration(eventuallyRemove = removeAfterMatch) instead",
            replaceWith = ReplaceWith("StubConfiguration(eventuallyRemove = removeAfterMatch)"),
        )
        public constructor(
            removeAfterMatch: Boolean,
        ) : this(
            eventuallyRemove = removeAfterMatch,
        )

        override fun toString(): String =
            buildString {
                append("StubConfiguration(")
                if (name != null) append("name=$name, ")
                append("eventuallyRemove=$eventuallyRemove, ")
                append("verbose=$verbose")
                append(")")
            }
    }
