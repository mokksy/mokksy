@file:OptIn(InternalMokksyApi::class)

package dev.mokksy.mokksy.utils.highlight

import dev.mokksy.mokksy.InternalMokksyApi

internal object FormParamsHighlighter {
    /**
     * Applies ANSI color highlighting to URL-encoded form data.
     *
     * Splits the input string into key-value pairs separated by '&' and colors keys in yellow and values in green.
     * Pairs that do not contain a '=' are left unchanged.
     *
     * @param data The URL-encoded form data to highlight.
     * @param useColor Set explicitly should colorize or not
     * @return The highlighted form data as a string with ANSI color codes.
     */
    internal fun highlight(
        data: String,
        useColor: Boolean = isColorSupported(),
    ): String = buildString {
        var start = 0
        while (start < data.length) {
            if (start > 0) append('&')
            val amp = data.indexOf('&', startIndex = start)
            val pairEnd = if (amp == -1) data.length else amp
            val eq = data.indexOf('=', startIndex = start).let { if (it == -1 || it >= pairEnd) -1 else it }
            if (eq == -1) {
                append(data, start, pairEnd)
            } else {
                appendColorized(data, start, eq, AnsiColor.YELLOW, useColor)
                append('=')
                appendColorized(data, eq + 1, pairEnd, AnsiColor.GREEN, useColor)
            }
            start = pairEnd + 1
        }
    }

    private fun StringBuilder.appendColorized(
        data: String,
        startIndex: Int,
        endIndex: Int,
        color: AnsiColor,
        useColor: Boolean,
    ) {
        if (useColor) {
            append(AnsiColor.RESET.code)
            append(color.code)
            append(data, startIndex, endIndex)
            append(AnsiColor.RESET.code)
        } else {
            append(data, startIndex, endIndex)
        }
    }
}
