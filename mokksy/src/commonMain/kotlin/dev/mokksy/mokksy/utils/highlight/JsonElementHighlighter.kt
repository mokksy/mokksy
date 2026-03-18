@file:OptIn(InternalMokksyApi::class)

package dev.mokksy.mokksy.utils.highlight

import dev.mokksy.mokksy.InternalMokksyApi
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Syntax highlighter for [JsonElement] trees using ANSI colors.
 *
 * Produces pretty-printed, colorized JSON output by walking the element tree directly —
 * no intermediate JSON string needed, no heuristic scanning.
 * Keys are colored magenta, string values green, numeric values blue, and
 * boolean/null values yellow.
 */
internal object JsonElementHighlighter {
    private val keyColor = AnsiColor.MAGENTA
    private val stringValColor = AnsiColor.GREEN
    private val numberValColor = AnsiColor.BLUE
    private val boolNullColor = AnsiColor.YELLOW

    fun highlight(
        element: JsonElement,
        useColor: Boolean,
    ): String = buildString { appendElement(element, useColor, 0) }

    private fun StringBuilder.appendElement(
        element: JsonElement,
        useColor: Boolean,
        depth: Int,
    ) {
        when (element) {
            is JsonObject -> appendObject(element, useColor, depth)
            is JsonArray -> appendArray(element, useColor, depth)
            is JsonPrimitive -> appendPrimitive(element, useColor)
        }
    }

    private fun StringBuilder.appendObject(
        obj: JsonObject,
        useColor: Boolean,
        depth: Int,
    ) {
        append('{')
        val entries = obj.entries.toList()
        entries.forEachIndexed { i, (key, value) ->
            appendLine()
            indent(depth + 1)
            appendKey(key, useColor)
            append(": ")
            appendElement(value, useColor, depth + 1)
            if (i < entries.size - 1) append(',')
        }
        if (entries.isNotEmpty()) {
            appendLine()
            indent(depth)
        }
        append('}')
    }

    private fun StringBuilder.appendArray(
        arr: JsonArray,
        useColor: Boolean,
        depth: Int,
    ) {
        append('[')
        arr.forEachIndexed { i, element ->
            appendLine()
            indent(depth + 1)
            appendElement(element, useColor, depth + 1)
            if (i < arr.size - 1) append(',')
        }
        if (arr.isNotEmpty()) {
            appendLine()
            indent(depth)
        }
        append(']')
    }

    private fun StringBuilder.appendPrimitive(
        primitive: JsonPrimitive,
        useColor: Boolean,
    ) {
        val color =
            when {
                primitive is JsonNull -> boolNullColor
                primitive.isString -> stringValColor
                primitive.content == "true" || primitive.content == "false" -> boolNullColor
                else -> numberValColor
            }
        colorize(primitive.toString(), color, useColor)
    }

    private fun StringBuilder.appendKey(
        key: String,
        useColor: Boolean,
    ) {
        // JsonPrimitive(key).toString() produces a properly quoted and escaped JSON string
        colorize(JsonPrimitive(key).toString(), keyColor, useColor)
    }

    private fun StringBuilder.indent(depth: Int) {
        repeat(depth) { append("    ") }
    }

    private fun StringBuilder.colorize(
        text: String,
        color: AnsiColor,
        useColor: Boolean,
    ) {
        if (useColor) {
            append(AnsiColor.RESET.code)
            append(color.code)
            append(text)
            append(AnsiColor.RESET.code)
        } else {
            append(text)
        }
    }
}
