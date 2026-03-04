package dev.mokksy.mokksy.serializers

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.test.Test

class StringOrListSerializerTest {
    private val json = Json

    // region deserialize

    @Test
    fun `should deserialize single string as single-element list`() {
        val result: List<String> = json.decodeFromString(StringOrListSerializer(), "\"hello\"")

        result shouldBe listOf("hello")
    }

    @Test
    fun `should deserialize empty string as single-element list with empty string`() {
        val result: List<String> = json.decodeFromString(StringOrListSerializer(), "\"\"")

        result shouldBe listOf("")
    }

    @Test
    fun `should deserialize array of strings as list`() {
        val result: List<String> =
            json.decodeFromString(
                StringOrListSerializer(),
                "[\"a\", \"b\", \"c\"]",
            )

        result shouldBe listOf("a", "b", "c")
    }

    @Test
    fun `should deserialize empty array as empty list`() {
        val result: List<String> = json.decodeFromString(StringOrListSerializer(), "[]")

        result shouldBe emptyList()
    }

    @Test
    fun `should deserialize array with single element as single-element list`() {
        val result: List<String> = json.decodeFromString(StringOrListSerializer(), "[\"only\"]")

        result shouldBe listOf("only")
    }

    @Test
    fun `should throw SerializationException when input is a JSON object`() {
        val exception =
            shouldThrow<SerializationException> {
                json.decodeFromString(StringOrListSerializer(), "{}")
            }

        exception.message shouldContain "Expected string or array of strings"
    }

    // endregion

    // region serialize

    @Test
    fun `should serialize single-element list as plain string`() {
        val result = json.encodeToString(StringOrListSerializer(), listOf("hello"))

        result shouldEqualJson "\"hello\""
    }

    @Test
    fun `should serialize multi-element list as JSON array`() {
        val result = json.encodeToString(StringOrListSerializer(), listOf("a", "b", "c"))

        result shouldEqualJson "[\"a\",\"b\",\"c\"]"
    }

    @Test
    fun `should serialize empty list as empty JSON array`() {
        val result = json.encodeToString(StringOrListSerializer(), emptyList())

        result shouldEqualJson "[]"
    }

    @Test
    fun `should serialize two-element list as JSON array`() {
        val result = json.encodeToString(StringOrListSerializer(), listOf("first", "second"))

        result shouldEqualJson "[\"first\",\"second\"]"
    }

    // endregion

    // region roundtrip

    @Test
    fun `should roundtrip single string`() {
        val original = listOf("roundtrip")
        val encoded = json.encodeToString(StringOrListSerializer(), original)
        val decoded = json.decodeFromString(StringOrListSerializer(), encoded)

        assertSoftly(decoded) {
            size shouldBe 1
            get(0) shouldBe "roundtrip"
        }
    }

    @Test
    fun `should roundtrip multiple strings`() {
        val original = listOf("one", "two", "three")
        val encoded = json.encodeToString(StringOrListSerializer(), original)
        val decoded = json.decodeFromString(StringOrListSerializer(), encoded)

        decoded shouldBe original
    }

    // endregion
}
