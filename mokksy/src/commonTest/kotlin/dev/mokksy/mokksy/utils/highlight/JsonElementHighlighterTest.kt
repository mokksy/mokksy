package dev.mokksy.mokksy.utils.highlight

import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test

private const val RESET = "\u001B[0m"
private const val MAGENTA = "\u001B[35m"
private const val GREEN = "\u001B[32m"
private const val BLUE = "\u001B[34m"
private const val YELLOW = "\u001B[33m"

// Each colorized fragment is preceded by a reset to clear any preceding attributes.
private const val RM = "$RESET$MAGENTA"
private const val RG = "$RESET$GREEN"
private const val RB = "$RESET$BLUE"
private const val RY = "$RESET$YELLOW"

class JsonElementHighlighterTest {
    @Test
    fun `Should highlight simple object`() {
        val element = JsonObject(mapOf("foo" to JsonPrimitive("bar")))
        val result = JsonElementHighlighter.highlight(element, useColor = true)
        result shouldBe "{\n    ${RM}\"foo\"$RESET: ${RG}\"bar\"$RESET\n}"
    }

    @Test
    fun `Should highlight object with number value`() {
        val element = JsonObject(mapOf("count" to JsonPrimitive(42)))
        val result = JsonElementHighlighter.highlight(element, useColor = true)
        result shouldBe "{\n    ${RM}\"count\"$RESET: ${RB}42$RESET\n}"
    }

    @Test
    fun `Should highlight object with boolean value`() {
        val element = JsonObject(mapOf("flag" to JsonPrimitive(true)))
        val result = JsonElementHighlighter.highlight(element, useColor = true)
        result shouldBe "{\n    ${RM}\"flag\"$RESET: ${RY}true$RESET\n}"
    }

    @Test
    fun `Should highlight object with null value`() {
        val element = JsonObject(mapOf("value" to JsonNull))
        val result = JsonElementHighlighter.highlight(element, useColor = true)
        result shouldBe "{\n    ${RM}\"value\"$RESET: ${RY}null$RESET\n}"
    }

    @Test
    fun `Should highlight nested object`() {
        val element =
            JsonObject(
                mapOf("outer" to JsonObject(mapOf("inner" to JsonPrimitive("val")))),
            )
        val result = JsonElementHighlighter.highlight(element, useColor = true)
        result shouldBe
            "{\n    ${RM}\"outer\"$RESET: {\n        ${RM}\"inner\"$RESET: ${RG}\"val\"$RESET\n    }\n}"
    }

    @Test
    fun `Should highlight array of primitives`() {
        val element =
            JsonObject(
                mapOf(
                    "items" to
                        JsonArray(
                            listOf(JsonPrimitive("a"), JsonPrimitive("b")),
                        ),
                ),
            )
        val result = JsonElementHighlighter.highlight(element, useColor = true)
        result shouldBe
            "{\n    ${RM}\"items\"$RESET: [\n        ${RG}\"a\"$RESET,\n        ${RG}\"b\"$RESET\n    ]\n}"
    }

    @Test
    fun `Should escape special characters in keys`() {
        val element = JsonObject(mapOf("ba\"z" to JsonPrimitive(1)))
        val result = JsonElementHighlighter.highlight(element, useColor = true)
        result shouldBe "{\n    ${RM}\"ba\\\"z\"$RESET: ${RB}1$RESET\n}"
    }

    @Test
    fun `Should produce plain output when color disabled`() {
        val element = JsonObject(mapOf("foo" to JsonPrimitive("bar")))
        val result = JsonElementHighlighter.highlight(element, useColor = false)
        result shouldBe "{\n    \"foo\": \"bar\"\n}"
    }

    @Test
    fun `Should highlight empty object`() {
        val element = JsonObject(emptyMap())
        val result = JsonElementHighlighter.highlight(element, useColor = true)
        result shouldBe "{}"
    }

    @Test
    fun `Should highlight empty array`() {
        val element = JsonArray(emptyList())
        val result = JsonElementHighlighter.highlight(element, useColor = true)
        result shouldBe "[]"
    }

    @Test
    fun `Should highlight multiple fields`() {
        val element =
            JsonObject(
                mapOf(
                    "name" to JsonPrimitive("Alice"),
                    "age" to JsonPrimitive(30),
                ),
            )
        val result = JsonElementHighlighter.highlight(element, useColor = true)
        result shouldBe
            "{\n    ${RM}\"name\"$RESET: ${RG}\"Alice\"$RESET,\n    ${RM}\"age\"$RESET: ${RB}30$RESET\n}"
    }
}
