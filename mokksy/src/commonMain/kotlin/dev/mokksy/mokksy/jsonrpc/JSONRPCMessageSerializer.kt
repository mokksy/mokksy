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
 * Content-based polymorphic serializer for [JSONRPCMessage].
 *
 * Discriminates between [JSONRPCRequest] and [JSONRPCNotification] based on
 * the presence of the `"id"` field in the JSON object:
 * - `"id"` present &rarr; [JSONRPCRequest]
 * - `"id"` absent &rarr; [JSONRPCNotification]
 *
 * The `params` field is serialized as raw [JsonElement] by default.
 * For typed params, supply a concrete [paramsSerializer].
 *
 * @param P The type of the `params` field.
 * @param paramsSerializer Serializer for the `params` value.
 */
public class JSONRPCMessageSerializer<P>(
    private val paramsSerializer: KSerializer<P>,
) : KSerializer<JSONRPCMessage<P>> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("JSONRPCMessage")

    @Suppress("ThrowsCount")
    override fun deserialize(decoder: Decoder): JSONRPCMessage<P> {
        val jsonDecoder =
            (decoder as? JsonDecoder)
                .shouldNotBeNull("JSONRPCMessageSerializer requires JSON format")
        val jsonObject =
            (jsonDecoder.decodeJsonElement() as? JsonObject)
                .shouldNotBeNull("Expected JSON object for JSONRPCMessage")

        val jsonrpc = jsonObject["jsonrpc"]
        if (jsonrpc == null || jsonrpc !is JsonPrimitive || jsonrpc.content != "2.0") {
            throw SerializationException("Missing or invalid 'jsonrpc' field: expected \"2.0\"")
        }

        val methodElement = jsonObject["method"]
        if (methodElement == null || methodElement !is JsonPrimitive || !methodElement.isString) {
            throw SerializationException(
                "Missing or invalid 'method' field: must be a JSON string",
            )
        }
        val method = methodElement.content

        val params: P? =
            jsonObject["params"]?.let {
                jsonDecoder.json.decodeFromJsonElement(paramsSerializer, it)
            }

        return if ("id" in jsonObject) {
            val id =
                jsonDecoder.json.decodeFromJsonElement(
                    NullableRequestIdSerializer,
                    jsonObject["id"].shouldNotBeNull("`id` property should be present"),
                )
            JSONRPCRequest(
                id = id,
                method = method,
                params = params,
            )
        } else {
            JSONRPCNotification(method = method, params = params)
        }
    }

    override fun serialize(
        encoder: Encoder,
        value: JSONRPCMessage<P>,
    ) {
        val jsonEncoder =
            (encoder as? JsonEncoder).shouldNotBeNull(
                "JSONRPCMessageSerializer requires JSON format",
            )

        val jsonObject =
            buildJsonObject {
                put("jsonrpc", "2.0")
                when (value) {
                    is JSONRPCRequest -> {
                        val idElement = encodeRequestId(value.id)
                        put("id", idElement)
                    }

                    is JSONRPCNotification -> { // no id field
                    }
                }
                put("method", value.method)
                val params = value.params
                if (params != null) {
                    put(
                        "params",
                        jsonEncoder.json.encodeToJsonElement(paramsSerializer, params),
                    )
                }
            }
        jsonEncoder.encodeJsonElement(jsonObject)
    }

    private fun encodeRequestId(id: RequestId?): JsonElement =
        when (id) {
            null -> JsonNull
            is RequestId.NumericId -> JsonPrimitive(id.value)
            is RequestId.StringId -> JsonPrimitive(id.value)
        }
}
