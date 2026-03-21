package dev.mokksy.mokksy.jsonrpc

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.test.Test

class RequestIdSerializerTest {
    private val json = Json

    // region deserialize

    @Test
    fun `should deserialize string id`() {
        val result = json.decodeFromString(RequestIdSerializer, "\"abc-123\"")

        result.shouldBeInstanceOf<RequestId.StringId>()
        result.value shouldBe "abc-123"
    }

    @Test
    fun `should deserialize integer id`() {
        val result = json.decodeFromString(RequestIdSerializer, "42")

        result.shouldBeInstanceOf<RequestId.NumericId>()
        result.value shouldBe 42
    }

    @Test
    fun `should deserialize zero id`() {
        val result = json.decodeFromString(RequestIdSerializer, "0")

        result.shouldBeInstanceOf<RequestId.NumericId>()
        result.value shouldBe 0
    }

    @Test
    fun `should deserialize negative integer id`() {
        val result = json.decodeFromString(RequestIdSerializer, "-1")

        result.shouldBeInstanceOf<RequestId.NumericId>()
        result.value shouldBe -1
    }

    // endregion

    // region deserialize validation

    @Test
    fun `should reject float id`() {
        val exception =
            shouldThrow<SerializationException> {
                json.decodeFromString(RequestIdSerializer, "1.5")
            }
        exception.message shouldContain "not a valid string or integer"
    }

    @Test
    fun `should reject boolean id`() {
        val exception =
            shouldThrow<SerializationException> {
                json.decodeFromString(RequestIdSerializer, "true")
            }
        exception.message shouldContain "not a valid string or integer"
    }

    // endregion

    // region serialize

    @Test
    fun `should serialize string id`() {
        val result = json.encodeToString(RequestIdSerializer, RequestId("abc-123"))

        result shouldEqualJson "\"abc-123\""
    }

    @Test
    fun `should serialize integer id`() {
        val result = json.encodeToString(RequestIdSerializer, RequestId(42))

        result shouldEqualJson "42"
    }

    // endregion

    // region nullable serializer

    @Test
    fun `should deserialize null id`() {
        val result = json.decodeFromString(NullableRequestIdSerializer, "null")

        result shouldBe null
    }

    @Test
    fun `should serialize null id`() {
        val result = json.encodeToString(NullableRequestIdSerializer, null)

        result shouldEqualJson "null"
    }

    @Test
    fun `should deserialize string id with nullable serializer`() {
        val result = json.decodeFromString(NullableRequestIdSerializer, "\"req-1\"")

        result.shouldBeInstanceOf<RequestId.StringId>()
        result.value shouldBe "req-1"
    }

    @Test
    fun `should deserialize integer id with nullable serializer`() {
        val result = json.decodeFromString(NullableRequestIdSerializer, "7")

        result.shouldBeInstanceOf<RequestId.NumericId>()
        result.value shouldBe 7
    }

    // endregion

    // region roundtrip

    @Test
    fun `should roundtrip string id`() {
        val original = RequestId("roundtrip-id")
        val encoded = json.encodeToString(RequestIdSerializer, original)
        val decoded = json.decodeFromString(RequestIdSerializer, encoded)

        decoded shouldBe original
    }

    @Test
    fun `should roundtrip integer id`() {
        val original = RequestId(99)
        val encoded = json.encodeToString(RequestIdSerializer, original)
        val decoded = json.decodeFromString(RequestIdSerializer, encoded)

        decoded shouldBe original
    }

    // endregion
}
