package dev.mokksy.mokksy.jsonrpc

import dev.mokksy.mokksy.serializers.SerializerUtils.shouldNotBeNull
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Content-based polymorphic serializer for [JSONRPCResponse].
 *
 * Discriminates between [JSONRPCResponse.Success] and [JSONRPCResponse.Error]
 * based on the presence of the `"error"` or `"result"` field in the JSON object.
 *
 * @param T The type of the result value in [JSONRPCResponse.Success].
 * @param E The type of the error data in [JSONRPCResponse.Error].
 * @param resultSerializer Serializer for the `result` field.
 * @param errorDataSerializer Serializer for the error's `data` field.
 */
public class JSONRPCResponseSerializer<T, E>(
    private val resultSerializer: KSerializer<T>,
    private val errorDataSerializer: KSerializer<E>,
) : KSerializer<JSONRPCResponse<T, E>> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("JSONRPCResponse")

    @Suppress("ThrowsCount")
    override fun deserialize(decoder: Decoder): JSONRPCResponse<T, E> {
        val jsonDecoder =
            (decoder as? JsonDecoder)
                .shouldNotBeNull("JSONRPCResponseSerializer requires JSON format")
        val jsonObject =
            (jsonDecoder.decodeJsonElement() as? JsonObject)
                .shouldNotBeNull("Expected JSON object for JSONRPCResponse")

        val jsonrpc = jsonObject["jsonrpc"]
        if (jsonrpc == null || jsonrpc !is JsonPrimitive || jsonrpc.content != "2.0") {
            throw SerializationException("Missing or invalid 'jsonrpc' field: expected \"2.0\"")
        }

        val idElement =
            jsonObject["id"].shouldNotBeNull(
                "Missing required 'id' field in JSONRPCResponse",
            )
        val id = jsonDecoder.json.decodeFromJsonElement(NullableRequestIdSerializer, idElement)

        val hasError = "error" in jsonObject
        val hasResult = "result" in jsonObject

        if (hasError && hasResult) {
            throw SerializationException(
                "JSON-RPC response MUST NOT contain both 'result' and 'error'",
            )
        }

        return if (hasError) {
            val errorSerializer = JSONRPCError.serializer(errorDataSerializer)
            val error =
                jsonDecoder.json.decodeFromJsonElement(
                    errorSerializer,
                    jsonObject["error"].shouldNotBeNull(
                        "`error` should be present in JSONRPCError",
                    ),
                )
            JSONRPCResponse.Error(id = id, error = error)
        } else if (hasResult) {
            val successId =
                id ?: throw SerializationException("Success response must have a non-null 'id'")
            val result =
                jsonDecoder.json.decodeFromJsonElement(
                    resultSerializer,
                    jsonObject["result"].shouldNotBeNull(
                        "`result` should be present in JSONRPCResult",
                    ),
                )
            JSONRPCResponse.Success(id = successId, result = result)
        } else {
            throw SerializationException(
                "JSONRPCResponse must contain either 'result' or 'error' field",
            )
        }
    }

    override fun serialize(
        encoder: Encoder,
        value: JSONRPCResponse<T, E>,
    ) {
        val jsonEncoder =
            (encoder as? JsonEncoder)
                .shouldNotBeNull("JSONRPCResponseSerializer requires JSON format")

        val jsonObject =
            buildJsonObject {
                put("jsonrpc", "2.0")

                val idElement =
                    when (val id = value.id) {
                        null -> JsonNull
                        is RequestId.NumericId -> JsonPrimitive(id.value)
                        is RequestId.StringId -> JsonPrimitive(id.value)
                    }
                put("id", idElement)

                when (value) {
                    is JSONRPCResponse.Success -> {
                        put(
                            "result",
                            jsonEncoder.json.encodeToJsonElement(resultSerializer, value.result),
                        )
                    }

                    is JSONRPCResponse.Error -> {
                        val errorSerializer = JSONRPCError.serializer(errorDataSerializer)

                        val typedError = value.error
                        put(
                            "error",
                            jsonEncoder.json.encodeToJsonElement(errorSerializer, typedError),
                        )
                    }
                }
            }
        jsonEncoder.encodeJsonElement(jsonObject)
    }
}

/**
 * A [JSONRPCResponse] with untyped result and error data, deserialized as raw [JsonElement].
 */
public typealias UntypedJSONRPCResponse = JSONRPCResponse<JsonElement, JsonElement>
