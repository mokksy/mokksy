package dev.mokksy.mokksy.jsonrpc

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test

class JSONRPCResponseSerializerTest {
    private val json = Json
    private val serializer =
        JSONRPCResponse
            .serializer(
                JsonElement.serializer(),
                JsonElement.serializer(),
            )

    // region deserialize success

    @Test
    fun `should deserialize success response with integer result`() {
        val input = """{"jsonrpc": "2.0", "result": 19, "id": 1}"""
        val response = json.decodeFromString(serializer, input)

        response
            .shouldBeInstanceOf<JSONRPCResponse.Success<JsonElement>>()
        assertSoftly(response) {
            id shouldBe
                RequestId(1)
            result shouldBe JsonPrimitive(19)
        }
    }

    @Test
    fun `should deserialize success response with string id`() {
        val input = """{"jsonrpc": "2.0", "result": "hello", "id": "abc"}"""
        val response = json.decodeFromString(serializer, input)

        response
            .shouldBeInstanceOf<JSONRPCResponse.Success<JsonElement>>()
        assertSoftly(response) {
            id shouldBe
                RequestId("abc")
            result shouldBe JsonPrimitive("hello")
        }
    }

    @Test
    fun `should deserialize success response with array result`() {
        val input = """{"jsonrpc": "2.0", "result": ["hello", 5], "id": "9"}"""
        val response = json.decodeFromString(serializer, input)

        response
            .shouldBeInstanceOf<JSONRPCResponse.Success<JsonElement>>()
        assertSoftly(response) {
            id shouldBe RequestId("9")
            result shouldBe json.parseToJsonElement("""["hello", 5]""")
        }
    }

    // endregion

    // region deserialize error

    @Test
    fun `should deserialize error response`() {
        val input = """{"jsonrpc": "2.0", "error": {"code": -32601, "message": "Method not found"}, "id": "1"}"""
        val response = json.decodeFromString(serializer, input)

        response.shouldBeInstanceOf<JSONRPCResponse.Error<*>>()
        assertSoftly(response) {
            id shouldBe
                RequestId("1")
            error.code shouldBe -32601
            error.message shouldBe "Method not found"
        }
    }

    @Test
    fun `should deserialize parse error with null id`() {
        val input = """{"jsonrpc": "2.0", "error": {"code": -32700, "message": "Parse error"}, "id": null}"""
        val response = json.decodeFromString(serializer, input)

        response.shouldBeInstanceOf<JSONRPCResponse.Error<*>>()
        assertSoftly(response) {
            id shouldBe null
            error.code shouldBe -32700
            error.message shouldBe "Parse error"
        }
    }

    @Test
    fun `should deserialize invalid request error`() {
        val input = """{"jsonrpc": "2.0", "error": {"code": -32600, "message": "Invalid Request"}, "id": null}"""
        val response = json.decodeFromString(serializer, input)

        response.shouldBeInstanceOf<JSONRPCResponse.Error<*>>()
        assertSoftly(response) {
            id shouldBe null
            error.code shouldBe -32600
        }
    }

    @Test
    fun `should deserialize error with data field`() {
        val input = """
            {
                "jsonrpc": "2.0",
                "error": {"code": -32603, "message": "Internal error", "data": "details"},
                "id": 5
            }
        """
        val response = json.decodeFromString(serializer, input)

        response.shouldBeInstanceOf<JSONRPCResponse.Error<*>>()
        assertSoftly(response) {
            error.code shouldBe -32603
            error.data shouldBe JsonPrimitive("details")
        }
    }

    // endregion

    // region deserialize validation

    @Test
    fun `should reject response without jsonrpc field`() {
        val input = """{"result": 19, "id": 1}"""

        val exception = shouldThrow<SerializationException> {
            json.decodeFromString(serializer, input)
        }
        exception.message shouldContain "jsonrpc"
    }

    @Test
    fun `should reject response with invalid jsonrpc version`() {
        val input = """{"jsonrpc": "1.0", "result": 19, "id": 1}"""

        val exception = shouldThrow<SerializationException> {
            json.decodeFromString(serializer, input)
        }
        exception.message shouldContain "jsonrpc"
    }

    @Test
    fun `should reject response without id field`() {
        val input = """{"jsonrpc": "2.0", "result": 19}"""

        val exception = shouldThrow<SerializationException> {
            json.decodeFromString(serializer, input)
        }
        exception.message shouldContain "id"
    }

    @Test
    fun `should reject response with both result and error`() {
        val input = """{"jsonrpc": "2.0", "result": 19, "error": {"code": -32600, "message": "Invalid"}, "id": 1}"""

        val exception = shouldThrow<SerializationException> {
            json.decodeFromString(serializer, input)
        }
        exception.message shouldContain "MUST NOT contain both"
    }

    // endregion

    // region serialize success

    @Test
    fun `should serialize success response with integer id`() {
        val response =
            JSONRPCResponse.Success(
                id =
                    RequestId(1),
                result = JsonPrimitive(19),
            )
        val result = json.encodeToString(serializer, response)

        result shouldEqualJson """
            {
                "jsonrpc": "2.0",
                "id": 1,
                "result": 19
            }
        """
    }

    @Test
    fun `should serialize success response with string id`() {
        val response =
            JSONRPCResponse.Success(
                id =
                    RequestId("abc"),
                result = JsonPrimitive("hello"),
            )
        val result = json.encodeToString(serializer, response)

        result shouldEqualJson """
            {
                "jsonrpc": "2.0",
                "id": "abc",
                "result": "hello"
            }
        """
    }

    @Test
    fun `should serialize success response with array result`() {
        val arrayResult = Json.parseToJsonElement("""["hello", 5]""")
        val response =
            JSONRPCResponse.Success(
                id =
                    RequestId("9"),
                result = arrayResult,
            )
        val result = json.encodeToString(serializer, response)

        result shouldEqualJson """
            {
                "jsonrpc": "2.0",
                "id": "9",
                "result": ["hello", 5]
            }
        """
    }

    // endregion

    // region serialize error

    @Test
    fun `should serialize error response`() {
        val response =
            JSONRPCResponse.Error(
                id =
                    RequestId("1"),
                error =
                    JSONRPCError
                        .methodNotFound(),
            )
        val result = json.encodeToString(serializer, response)

        result shouldEqualJson """
            {
                "jsonrpc": "2.0",
                "id": "1",
                "error": {"code": -32601, "message": "Method not found"}
            }
        """
    }

    @Test
    fun `should serialize error response with null id`() {
        val response =
            JSONRPCResponse.Error(
                id = null,
                error =
                    JSONRPCError
                        .parseError(),
            )
        val result = json.encodeToString(serializer, response)

        result shouldEqualJson """
            {
                "jsonrpc": "2.0",
                "id": null,
                "error": {"code": -32700, "message": "Parse error"}
            }
        """
    }

    // endregion

    // region spec examples

    @Test
    fun `should match spec example - negative result`() {
        val input = """{"jsonrpc": "2.0", "result": -19, "id": 2}"""
        val response = json.decodeFromString(serializer, input)

        response
            .shouldBeInstanceOf<JSONRPCResponse.Success<JsonElement>>()
        assertSoftly(response) {
            id shouldBe
                RequestId(2)
            result shouldBe JsonPrimitive(-19)
        }
    }

    // endregion

    // region roundtrip

    @Test
    fun `should roundtrip success response`() {
        val original =
            JSONRPCResponse.Success(
                id =
                    RequestId(42),
                result = JsonPrimitive("computed"),
            )
        val encoded = json.encodeToString(serializer, original)
        val decoded = json.decodeFromString(serializer, encoded)

        decoded
            .shouldBeInstanceOf<JSONRPCResponse.Success<JsonElement>>()
        assertSoftly(decoded) {
            id shouldBe original.id
            result shouldBe original.result
        }
    }

    @Test
    fun `should roundtrip error response`() {
        val original =
            JSONRPCResponse.Error(
                id =
                    RequestId("err-1"),
                error =
                    JSONRPCError
                        .internalError(
                            "something failed",
                        ),
            )
        val encoded = json.encodeToString(serializer, original)
        val decoded = json.decodeFromString(serializer, encoded)

        decoded.shouldBeInstanceOf<JSONRPCResponse.Error<*>>()
        assertSoftly(decoded) {
            id shouldBe original.id
            error.code shouldBe original.error.code
            error.message shouldBe original.error.message
        }
    }

    // endregion
}
