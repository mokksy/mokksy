package dev.mokksy.mokksy.jsonrpc

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlin.test.Test

class JSONRPCErrorTest {
    private val json = Json
    private val jsonElementSerializer =
        JSONRPCError.Companion
            .serializer(
                JsonElement.serializer(),
            )
    private val stringDataSerializer =
        JSONRPCError.Companion
            .serializer(
                String.serializer(),
            )

    // region serialization

    @Test
    fun `should serialize error with code and message`() {
        val error =
            JSONRPCError<Nothing>(
                code = -32600,
                message = "Invalid Request",
            )
        val result = json.encodeToString(jsonElementSerializer, error)

        result shouldEqualJson """
            {
                "code": -32600,
                "message": "Invalid Request"
            }
        """
    }

    @Test
    fun `should serialize error with string data`() {
        val error =
            JSONRPCError(
                code = -32602,
                message = "Invalid params",
                data = "extra info",
            )
        val result = json.encodeToString(stringDataSerializer, error)

        result shouldEqualJson """
            {
                "code": -32602,
                "message": "Invalid params",
                "data": "extra info"
            }
        """
    }

    @Test
    fun `should deserialize error without data`() {
        val input = """{"code": -32601, "message": "Method not found"}"""
        val error = json.decodeFromString(jsonElementSerializer, input)

        assertSoftly(error) {
            code shouldBe -32601
            message shouldBe "Method not found"
            data shouldBe null
        }
    }

    @Test
    fun `should deserialize error with data`() {
        val input = """{"code": -32603, "message": "Internal error", "data": "stack trace"}"""
        val error = json.decodeFromString(stringDataSerializer, input)

        assertSoftly(error) {
            code shouldBe -32603
            message shouldBe "Internal error"
            data shouldBe "stack trace"
        }
    }

    // endregion

    // region factory methods

    @Test
    fun `should create parse error with correct code`() {
        val error =
            JSONRPCError
                .parseError()

        assertSoftly(error) {
            code shouldBe -32700
            message shouldBe "Parse error"
        }
    }

    @Test
    fun `should create invalid request with correct code`() {
        val error =
            JSONRPCError
                .invalidRequest()

        assertSoftly(error) {
            code shouldBe -32600
            message shouldBe "Invalid Request"
        }
    }

    @Test
    fun `should create method not found with method name`() {
        val error =
            JSONRPCError
                .methodNotFound(
                    "tools/call",
                )

        assertSoftly(error) {
            code shouldBe -32601
            message shouldBe "Method not found: tools/call"
        }
    }

    @Test
    fun `should create method not found without method name`() {
        val error =
            JSONRPCError
                .methodNotFound()

        assertSoftly(error) {
            code shouldBe -32601
            message shouldBe "Method not found"
        }
    }

    @Test
    fun `should create invalid params with correct code`() {
        val error =
            JSONRPCError
                .invalidParams()

        assertSoftly(error) {
            code shouldBe -32602
            message shouldBe "Invalid params"
        }
    }

    @Test
    fun `should create internal error with correct code`() {
        val error =
            JSONRPCError
                .internalError()

        assertSoftly(error) {
            code shouldBe -32603
            message shouldBe "Internal error"
        }
    }

    @Test
    fun `should create factory error with custom message`() {
        val error =
            JSONRPCError
                .parseError(
                    "Custom parse error message",
                )

        assertSoftly(error) {
            code shouldBe -32700
            message shouldBe "Custom parse error message"
        }
    }

    // endregion

    // region ErrorCodes

    @Test
    fun `should identify server error codes`() {
        assertSoftly {
            JSONRPCError.ErrorCodes.isServerError(
                -32000,
            ) shouldBe
                true
            JSONRPCError.ErrorCodes.isServerError(
                -32050,
            ) shouldBe
                true
            JSONRPCError.ErrorCodes.isServerError(
                -32099,
            ) shouldBe
                true
        }
    }

    @Test
    fun `should not identify non-server error codes`() {
        assertSoftly {
            JSONRPCError.ErrorCodes.isServerError(
                -32100,
            ) shouldBe
                false
            JSONRPCError.ErrorCodes.isServerError(
                -31999,
            ) shouldBe
                false
            JSONRPCError.ErrorCodes.isServerError(
                -32700,
            ) shouldBe
                false
            JSONRPCError.ErrorCodes.isServerError(
                0,
            ) shouldBe
                false
        }
    }

    // endregion

    // region roundtrip

    @Test
    fun `should roundtrip error without data`() {
        val original =
            JSONRPCError
                .methodNotFound(
                    "subtract",
                )
        val encoded = json.encodeToString(jsonElementSerializer, original)
        val decoded = json.decodeFromString(jsonElementSerializer, encoded)

        assertSoftly(decoded) {
            code shouldBe original.code
            message shouldBe original.message
            data shouldBe null
        }
    }

    // endregion
}
