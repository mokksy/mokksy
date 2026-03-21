package dev.mokksy.mokksy.jsonrpc

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlin.test.Test

class JSONRPCTypedUseCaseTest {
    private val json = Json

    @Serializable
    data class SubtractParams(
        val subtrahend: Int,
        val minuend: Int,
    )

    private val messageSerializer =
        JSONRPCMessage
            .serializer(
                SubtractParams.serializer(),
            )
    private val responseSerializer =
        JSONRPCResponse
            .serializer(
                Int.serializer(),
                JsonElement.serializer(),
            )

    // region request with typed params — spec example §7 "named parameters"

    @Test
    fun `should serialize typed request to spec-compliant JSON`() {
        val request =
            JSONRPCRequest(
                id =
                    RequestId(3),
                method = "subtract",
                params = SubtractParams(subtrahend = 23, minuend = 42),
            )
        val result = json.encodeToString(messageSerializer, request)

        result shouldEqualJson """
            {
                "jsonrpc": "2.0",
                "id": 3,
                "method": "subtract",
                "params": {"subtrahend": 23, "minuend": 42}
            }
        """
    }

    @Test
    fun `should deserialize spec JSON into typed request`() {
        val message =
            json.decodeFromString(
                messageSerializer,
                """{"jsonrpc":"2.0","method":"subtract","params":{"subtrahend":23,"minuend":42},"id":3}""",
            )

        message
            .shouldBeInstanceOf<JSONRPCRequest<SubtractParams>>()
        assertSoftly(message) {
            id shouldBe
                RequestId(3)
            method shouldBe "subtract"
            params shouldBe SubtractParams(subtrahend = 23, minuend = 42)
        }
    }

    // endregion

    // region success response with typed result — spec example §7

    @Test
    fun `should serialize typed success response to spec-compliant JSON`() {
        val response: JSONRPCResponse<Int, JsonElement> =
            JSONRPCResponse.Success(
                id =
                    RequestId(
                        3,
                    ),
                result = 19,
            )
        val result = json.encodeToString(responseSerializer, response)

        result shouldEqualJson """
            {"jsonrpc": "2.0", "result": 19, "id": 3}
        """
    }

    @Test
    fun `should deserialize spec success JSON into typed response`() {
        val input = """{"jsonrpc": "2.0", "result": 19, "id": 3}"""
        val response = json.decodeFromString(responseSerializer, input)

        response
            .shouldBeInstanceOf<JSONRPCResponse.Success<Int>>()
        assertSoftly(response) {
            id shouldBe
                RequestId(3)
            result shouldBe 19
        }
    }

    // endregion

    // region error response — spec example §7 "non-existent method"

    @Test
    fun `should serialize error response to spec-compliant JSON`() {
        val response: JSONRPCResponse<Int, JsonElement> =
            JSONRPCResponse.Error(
                id =
                    RequestId("1"),
                error =
                    JSONRPCError
                        .methodNotFound(),
            )
        val result = json.encodeToString(responseSerializer, response)

        result shouldEqualJson """
            {"jsonrpc": "2.0", "error": {"code": -32601, "message": "Method not found"}, "id": "1"}
        """
    }

    @Test
    fun `should deserialize spec error JSON into typed response`() {
        val input =
            """{"jsonrpc":"2.0","error":{"code":-32601,"message":"Method not found"},"id":"1"}"""
        val response = json.decodeFromString(responseSerializer, input)

        response.shouldBeInstanceOf<JSONRPCResponse.Error<*>>()
        assertSoftly(response) {
            id shouldBe RequestId("1")
            error.code shouldBe -32601
            error.message shouldBe "Method not found"
        }
    }

    // endregion

    // region notification with typed params — spec example §7

    @Test
    fun `should serialize typed notification without id`() {
        val notification =
            JSONRPCNotification(
                method = "update",
                params = SubtractParams(subtrahend = 23, minuend = 42),
            )
        val result = json.encodeToString(messageSerializer, notification)

        result shouldEqualJson """
            {
                "jsonrpc": "2.0",
                "method": "update",
                "params": {"subtrahend": 23, "minuend": 42}
            }
        """
    }

    @Test
    fun `should deserialize typed notification`() {
        val input =
            """{"jsonrpc":"2.0","method":"update","params":{"subtrahend":23,"minuend":42}}"""
        val message = json.decodeFromString(messageSerializer, input)

        message.shouldBeInstanceOf<JSONRPCNotification<SubtractParams>>()
        assertSoftly(message) {
            method shouldBe "update"
            params shouldBe SubtractParams(subtrahend = 23, minuend = 42)
        }
    }

    // endregion

    // region full request-response cycle

    @Test
    fun `should handle full typed request-response cycle`() {
        // Client sends request
        val request =
            JSONRPCRequest(
                id = RequestId(4),
                method = "subtract",
                params = SubtractParams(subtrahend = 23, minuend = 42),
            )
        val requestJson = json.encodeToString(messageSerializer, request)

        requestJson shouldEqualJson """
            {"jsonrpc":"2.0","id":4,"method":"subtract","params":{"subtrahend":23,"minuend":42}}
        """

        // Server parses request
        val parsedRequest = json.decodeFromString(messageSerializer, requestJson)
        parsedRequest.shouldBeInstanceOf<JSONRPCRequest<SubtractParams>>()
        val params = parsedRequest.params.shouldNotBeNull()

        // Server computes result
        val computedResult = params.minuend - params.subtrahend

        // Server sends response
        val response: JSONRPCResponse<Int, JsonElement> =
            JSONRPCResponse.Success(id = parsedRequest.id.shouldNotBeNull(), result = computedResult)
        val responseJson = json.encodeToString(responseSerializer, response)

        responseJson shouldEqualJson """
            {"jsonrpc":"2.0","result":19,"id":4}
        """

        // Client parses response
        val parsedResponse = json.decodeFromString(responseSerializer, responseJson)
        parsedResponse.shouldBeInstanceOf<JSONRPCResponse.Success<Int>>()
        assertSoftly(parsedResponse) {
            id shouldBe RequestId(4)
            result shouldBe 19
        }
    }

    // endregion
}
