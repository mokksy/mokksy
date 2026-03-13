package dev.mokksy.mokksy

import kotlin.jvm.JvmOverloads

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
    )
