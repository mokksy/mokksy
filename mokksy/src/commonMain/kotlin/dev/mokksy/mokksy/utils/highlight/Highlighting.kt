@file:OptIn(InternalMokksyApi::class)

package dev.mokksy.mokksy.utils.highlight

import dev.mokksy.mokksy.InternalMokksyApi
import dev.mokksy.mokksy.utils.highlight.Highlighting.highlightBody
import dev.mokksy.mokksy.utils.highlight.Highlighting.registerJsonContentType
import io.ktor.http.ContentType
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.serialization.json.JsonElement
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.update
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic
import kotlin.jvm.JvmSynthetic

@InternalMokksyApi
@OptIn(ExperimentalAtomicApi::class)
public object Highlighting {
    private val additionalJsonContentTypes: AtomicReference<Set<ContentType>> =
        AtomicReference(persistentSetOf())

    /**
     * Registers a custom content type to be treated as JSON for highlighting purposes.
     *
     * Example: `registerJsonContentType(ContentType("application/x-ndjson"))`
     *
     * @param contentType The content type to register.
     */
    @InternalMokksyApi
    @JvmStatic
    @JvmSynthetic
    public fun registerJsonContentType(contentType: ContentType) {
        additionalJsonContentTypes.update { it + contentType }
    }

    /**
     * Registers a custom content type (given as a string) to be treated as JSON.
     *
     * Java-friendly overload of [registerJsonContentType].
     *
     * Example: `registerJsonContentType("application/x-ndjson")`
     */
    @InternalMokksyApi
    @JvmStatic
    public fun registerJsonContentType(contentType: String) {
        registerJsonContentType(ContentType.parse(contentType))
    }

    /**
     * Applies ANSI color highlighting to an HTTP body string based on its content type.
     *
     * @param body The HTTP body content to highlight.
     * @param contentType The content type of the body.
     * @return The highlighted body string with ANSI color codes.
     */
    @JvmStatic
    @JvmSynthetic
    public fun highlightBody(
        body: String,
        contentType: ContentType,
        useColor: Boolean = isColorSupported(),
    ): String =
        when {
            isJsonContentType(contentType) -> {
                JsonStringHighlighter.highlight(
                    json = body,
                    useColor = useColor,
                )
            }

            contentType.match(ContentType.Application.FormUrlEncoded) -> {
                FormParamsHighlighter.highlight(
                    data = body,
                    useColor = useColor,
                )
            }

            else -> {
                body.colorize(AnsiColor.LIGHT_GRAY, enabled = useColor)
            }
        }

    /**
     * Applies ANSI color highlighting to a [JsonElement] for terminal output.
     *
     * Produces pretty-printed output by walking the element tree directly —
     * no intermediate JSON string, no heuristic scanning.
     *
     * @param body The [JsonElement] to highlight.
     * @param useColor Whether to apply ANSI color codes.
     * @return The highlighted JSON string.
     */
    @JvmStatic
    @JvmSynthetic
    public fun highlightBody(
        body: JsonElement,
        useColor: Boolean = isColorSupported(),
    ): String = JsonElementHighlighter.highlight(body, useColor)

    /**
     * Applies ANSI color highlighting to an HTTP body string for a content type given as a string.
     *
     * Java-friendly overload of [highlightBody] that accepts a content type string.
     *
     * Example: `highlightBody(body, "application/x-ndjson")`
     */
    @JvmStatic
    @JvmOverloads
    public fun highlightBody(
        body: String,
        contentType: String,
        useColor: Boolean = isColorSupported(),
    ): String = highlightBody(body, ContentType.parse(contentType), useColor)

    /**
     * Determines whether the given [contentType] should be highlighted as JSON.
     *
     * Returns `true` for `application/json`, any `+json` subtype,
     * and any content type registered via [registerJsonContentType].
     */
    @JvmStatic
    @JvmSynthetic
    internal fun isJsonContentType(contentType: ContentType): Boolean =
        contentType.match(ContentType.Application.Json) ||
            contentType.contentSubtype.endsWith("+json", ignoreCase = true) ||
            contentType in additionalJsonContentTypes.load()
}

@InternalMokksyApi
public enum class ColorTheme { LIGHT_ON_DARK, DARK_ON_LIGHT }

/**
 * Determines whether ANSI color output is supported on the current platform.
 *
 * @return `true` if ANSI color codes can be used for output; otherwise, `false`.
 */
internal expect fun isColorSupported(): Boolean

@InternalMokksyApi
public enum class AnsiColor(
    public val code: String,
) {
    RESET("\u001B[0m"),
    STRONGER("\u001B[1m"),
    PALE("\u001B[2m"),
    BLACK("\u001B[30m"),
    RED("\u001B[31m"),
    GREEN("\u001B[32m"),
    YELLOW("\u001B[33m"),
    BLUE("\u001B[34m"),
    MAGENTA("\u001B[35m"),
    CYAN("\u001B[36m"),
    WHITE("\u001B[37m"),
    LIGHT_GRAY("\u001B[37m"),
    LIGHT_GRAY_BOLD("\u001B[37;1m"),
    DARK_GRAY("\u001B[90m"),
}

/**
 * Wraps the String with the specified ANSI color code if coloring is enabled.
 *
 * @param color The ANSI color to apply.
 * @param enabled Whether to apply colorization; if false, returns the original text.
 * @return The colorized text if enabled, otherwise the original text.
 */
internal fun String.colorize(
    color: AnsiColor,
    enabled: Boolean = isColorSupported(),
): String =
    if (enabled) "${AnsiColor.RESET.code}${color.code}$this${AnsiColor.RESET.code}" else this
