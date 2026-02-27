package dev.mokksy.mokksy

import kotlin.jvm.JvmOverloads

/**
 * Configuration for a stub's lifecycle and logging behaviour.
 *
 * @property name Optional human-readable name used in log and error output. Defaults to `null`.
 * @property removeAfterMatch When `true`, the stub is removed from the registry after its first match.
 *           Defaults to `false`.
 * @property verbose Enables per-stub `DEBUG`-level logging when `true`. Defaults to `false`.
 */
public data class StubConfiguration
    @JvmOverloads
    constructor(
        val name: String? = null,
        val removeAfterMatch: Boolean = false,
        val verbose: Boolean = false,
    )
