@file:OptIn(InternalMokksyApi::class)

package dev.mokksy.mokksy.request

import dev.mokksy.mokksy.InternalMokksyApi
import dev.mokksy.mokksy.utils.highlight.AnsiColor
import dev.mokksy.mokksy.utils.highlight.colorize
import dev.mokksy.mokksy.utils.highlight.isColorSupported

@InternalMokksyApi
internal object DiagnosticLogger {
    fun format(
        stubResults: List<StubMatchResult>,
        useColor: Boolean = isColorSupported(),
    ): String =
        buildString {
            appendLine()
            val heading =
                if (stubResults.size == 1) "Closest stub:" else "Closest ${stubResults.size} stubs:"
            appendLine(heading.colorize(AnsiColor.STRONGER, useColor))
            for (stubResult in stubResults) {
                appendLine(
                    "Stub: ${stubResult.name}".colorize(AnsiColor.STRONGER, useColor),
                )
                for (configured in stubResult.configuredMatchers) {
                    val failed = findFailedMatcher(configured, stubResult.failedMatchers)
                    if (failed != null) {
                        appendLine("  ${"✗".colorize(AnsiColor.RED, useColor)} $configured")
                        appendLine("      $failed")
                    } else {
                        appendLine("  ${"✓".colorize(AnsiColor.GREEN, useColor)} $configured")
                    }
                }
            }
        }

    private fun findFailedMatcher(
        configured: String,
        failedMatchers: List<String>,
    ): String? {
        val prefix = configured.substringBefore(":")
        return failedMatchers.find { it.startsWith("$prefix:") }
    }
}
