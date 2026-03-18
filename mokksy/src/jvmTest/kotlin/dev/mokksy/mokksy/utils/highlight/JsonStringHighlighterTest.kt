package dev.mokksy.mokksy.utils.highlight

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

private const val RESET = "\u001B[0m"
private const val MAGENTA = "\u001B[35m"
private const val GREEN = "\u001B[32m"
private const val BLUE = "\u001B[34m"
private const val YELLOW = "\u001B[33m"

// Each colorized fragment is preceded by a reset to clear any preceding attributes.
private val RM = "$RESET$MAGENTA"
private val RG = "$RESET$GREEN"
private val RB = "$RESET$BLUE"
private val RY = "$RESET$YELLOW"

@Suppress("MaxLineLength")
class JsonStringHighlighterTest {
    @Test
    fun `Should highlight simple Json`() {
        // language=json
        val input = """{"foo": "bar", "ba\"z": 1}"""
        val result = JsonStringHighlighter.highlight(input, useColor = true)
        // language=text
        result shouldBe
            "{${RM}\"foo\"$RESET: ${RG}\"bar\"$RESET, ${RM}\"ba\\\"z\"$RESET: ${RB}1$RESET}"
    }

    @Test
    fun `Should highlight Json with newline`() {
        // language=json
        val input = "{\n  \"foo\"\n  : \n  \"bar\"\n}"
        val result = JsonStringHighlighter.highlight(input, useColor = true)
        // language=text
        result shouldBe
            "{\n  ${RM}\"foo\"$RESET\n  : \n  ${RG}\"bar\"$RESET\n}"
    }

    @Test
    fun `Should highlight nested Json`() {
        // language=json
        val input = """{"foo": {"bar": "ba\"z"}}"""
        val result = JsonStringHighlighter.highlight(input, useColor = true)
        // language=text
        result shouldBe "{${RM}\"foo\"$RESET: {${RM}\"bar\"$RESET: ${RG}\"ba\\\"z\"$RESET}}"
    }

    @Test
    fun `Should highlight Json with array`() {
        // language=json
        val input = """{"foo": ["bar", "ba\"z"]}"""
        val result = JsonStringHighlighter.highlight(input, useColor = true)
        // language=text
        result shouldBe
            "{${RM}\"foo\"$RESET: [${RG}\"bar\"$RESET, ${RG}\"ba\\\"z\"$RESET]}"
    }

    @Test
    fun `Should highlight Json with array of objects`() {
        // language=json
        val input = """{"foo": [{"bar": "baz"}]}"""
        val result = JsonStringHighlighter.highlight(input, useColor = true)
        // language=text
        result shouldBe
            "{${RM}\"foo\"$RESET: [{${RM}\"bar\"$RESET: ${RG}\"baz\"$RESET}]}"
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "42",
            "-42",
            "0",
            "3.14",
            "-3.14",
            "0.5",
            "1.5e10",
            "1.5E10",
            "-1.5e10",
            "2.5e-5",
            "-2.5E-5",
        ],
    )
    fun `Should highlight different number types`(numberValue: String) {
        // language=json
        val input = """{"number": $numberValue}"""
        val result = JsonStringHighlighter.highlight(input, useColor = true)
        // language=text
        result shouldBe "{${RM}\"number\"$RESET: $RB$numberValue$RESET}"
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `Should highlight boolean value`(value: Boolean) {
        // language=json
        val input = """{"flag": $value}"""
        val result = JsonStringHighlighter.highlight(input, useColor = true)
        // language=text
        result shouldBe "{${RM}\"flag\"$RESET: ${RY}$value$RESET}"
    }

    @Test
    fun `Should highlight null value`() {
        // language=json
        val input = """{"value": null}"""
        val result = JsonStringHighlighter.highlight(input, useColor = true)
        // language=text
        result shouldBe "{${RM}\"value\"$RESET: ${RY}null$RESET}"
    }

    @ParameterizedTest
    @CsvSource(
        "true, true",
        "false, false",
        "null, null",
    )
    fun `Should highlight boolean and null values in arrays`(
        value1: String,
        value2: String,
    ) {
        // language=json
        val input = """{"values": [$value1, $value1]}"""
        val result = JsonStringHighlighter.highlight(json = input, useColor = true)
        // language=text
        result shouldBe "{${RM}\"values\"$RESET: [${RY}$value2$RESET, ${RY}$value2$RESET]}"
    }
}
