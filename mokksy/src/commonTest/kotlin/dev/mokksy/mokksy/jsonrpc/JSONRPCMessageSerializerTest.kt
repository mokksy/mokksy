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
import kotlin.test.Test

class JSONRPCMessageSerializerTest {
    private val json = Json
    private val serializer =
        JSONRPCMessage
            .serializer(
                JsonElement.serializer(),
            )

    // region deserialize request

    @Test
    fun `should deserialize request with integer id`() {
        val input = """{"jsonrpc": "2.0", "method": "subtract", "params": [42, 23], "id": 1}"""
        val message = json.decodeFromString(serializer, input)

        message.shouldBeInstanceOf<JSONRPCRequest<JsonElement>>()
        assertSoftly(message) {
            id shouldBe
                RequestId(1)
            method shouldBe "subtract"
        }
    }

    @Test
    fun `should deserialize request with string id`() {
        val input = """{"jsonrpc": "2.0", "method": "foobar", "id": "req-1"}"""
        val message = json.decodeFromString(serializer, input)

        message.shouldBeInstanceOf<JSONRPCRequest<JsonElement>>()
        assertSoftly(message) {
            id shouldBe
                RequestId("req-1")
            method shouldBe "foobar"
        }
    }

    @Test
    fun `should deserialize request without params`() {
        val input = """{"jsonrpc": "2.0", "method": "get_data", "id": "9"}"""
        val message = json.decodeFromString(serializer, input)

        message.shouldBeInstanceOf<JSONRPCRequest<JsonElement>>()
        assertSoftly(message) {
            id shouldBe
                RequestId("9")
            method shouldBe "get_data"
            params shouldBe null
        }
    }

    // endregion

    // region deserialize notification

    @Test
    fun `should deserialize notification with params`() {
        val input = """{"jsonrpc": "2.0", "method": "update", "params": [1, 2, 3, 4, 5]}"""
        val message = json.decodeFromString(serializer, input)

        message
            .shouldBeInstanceOf<JSONRPCNotification<JsonElement>>()
        message.method shouldBe "update"
    }

    @Test
    fun `should deserialize notification without params`() {
        val input = """{"jsonrpc": "2.0", "method": "foobar"}"""
        val message = json.decodeFromString(serializer, input)

        message
            .shouldBeInstanceOf<JSONRPCNotification<JsonElement>>()
        assertSoftly(message) {
            method shouldBe "foobar"
            params shouldBe null
        }
    }

    // endregion

    // region deserialize validation

    @Test
    fun `should reject message without jsonrpc field`() {
        val input = """{"method": "subtract", "id": 1}"""

        val exception =
            shouldThrow<SerializationException> {
                json.decodeFromString(serializer, input)
            }
        exception.message shouldContain "jsonrpc"
    }

    @Test
    fun `should reject message with invalid jsonrpc version`() {
        val input = """{"jsonrpc": "1.0", "method": "subtract", "id": 1}"""

        val exception =
            shouldThrow<SerializationException> {
                json.decodeFromString(serializer, input)
            }
        exception.message shouldContain "jsonrpc"
    }

    @Test
    fun `should reject message with non-string method`() {
        val input = """{"jsonrpc": "2.0", "method": 123, "id": 1}"""

        val exception =
            shouldThrow<SerializationException> {
                json.decodeFromString(serializer, input)
            }
        exception.message shouldContain "method"
    }

    @Test
    fun `should reject message without method field`() {
        val input = """{"jsonrpc": "2.0", "id": 1}"""

        val exception =
            shouldThrow<SerializationException> {
                json.decodeFromString(serializer, input)
            }
        exception.message shouldContain "method"
    }

    // endregion

    // region serialize request

    @Test
    fun `should serialize request with integer id`() {
        val request =
            JSONRPCRequest<JsonElement>(
                id =
                    RequestId(1),
                method = "subtract",
            )
        val result = json.encodeToString(serializer, request)

        result shouldEqualJson """
            {
                "jsonrpc": "2.0",
                "id": 1,
                "method": "subtract"
            }
        """
    }

    @Test
    fun `should serialize request with string id`() {
        val request =
            JSONRPCRequest<JsonElement>(
                id =
                    RequestId("abc"),
                method = "tools/call",
            )
        val result = json.encodeToString(serializer, request)

        result shouldEqualJson """
            {
                "jsonrpc": "2.0",
                "id": "abc",
                "method": "tools/call"
            }
        """
    }

    @Test
    fun `should serialize request with params`() {
        val params = Json.parseToJsonElement("""{"name": "myself"}""")
        val request =
            JSONRPCRequest(
                id =
                    RequestId(3),
                method = "get_user",
                params = params,
            )
        val result = json.encodeToString(serializer, request)

        result shouldEqualJson """
            {
                "jsonrpc": "2.0",
                "id": 3,
                "method": "get_user",
                "params": {"name": "myself"}
            }
        """
    }

    // endregion

    // region serialize notification

    @Test
    fun `should serialize notification without id`() {
        val notification =
            JSONRPCNotification<JsonElement>(
                method = "notify_hello",
            )
        val result = json.encodeToString(serializer, notification)

        result shouldEqualJson """
            {
                "jsonrpc": "2.0",
                "method": "notify_hello"
            }
        """
    }

    @Test
    fun `should serialize notification with params`() {
        val params = Json.parseToJsonElement("[1, 2, 3]")
        val notification =
            JSONRPCNotification(
                method = "notify_sum",
                params = params,
            )
        val result = json.encodeToString(serializer, notification)

        result shouldEqualJson """
            {
                "jsonrpc": "2.0",
                "method": "notify_sum",
                "params": [1, 2, 3]
            }
        """
    }

    // endregion

    // region roundtrip

    @Test
    fun `should roundtrip request`() {
        val original =
            JSONRPCRequest(
                id =
                    RequestId(42),
                method = "subtract",
                params = Json.parseToJsonElement("[42, 23]"),
            )
        val encoded = json.encodeToString(serializer, original)
        val decoded = json.decodeFromString(serializer, encoded)

        decoded.shouldBeInstanceOf<JSONRPCRequest<JsonElement>>()
        assertSoftly(decoded) {
            id shouldBe original.id
            method shouldBe original.method
            params shouldBe original.params
        }
    }

    @Test
    fun `should roundtrip notification`() {
        val original =
            JSONRPCNotification(
                method = "update",
                params = Json.parseToJsonElement("[1, 2, 3]"),
            )
        val encoded = json.encodeToString(serializer, original)
        val decoded = json.decodeFromString(serializer, encoded)

        decoded
            .shouldBeInstanceOf<JSONRPCNotification<JsonElement>>()
        assertSoftly(decoded) {
            method shouldBe original.method
            params shouldBe original.params
        }
    }

    // endregion
}
