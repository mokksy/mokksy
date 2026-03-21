package dev.mokksy.mokksy.jsonrpc

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Serializer for non-nullable [RequestId].
 *
 * Handles the JSON-RPC 2.0 `id` field which can be a JSON string or integer.
 * - JSON string &rarr; [RequestId.StringId]
 * - JSON integer &rarr; [RequestId.NumericId]
 *
 * For nullable `RequestId?` properties, use [NullableRequestIdSerializer].
 */
public object RequestIdSerializer : KSerializer<RequestId> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("RequestId")

    override fun deserialize(decoder: Decoder): RequestId {
        val jsonDecoder =
            decoder as? JsonDecoder
                ?: throw SerializationException("RequestIdSerializer requires JSON format")
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonPrimitive -> element.toRequestId()
            else -> throw SerializationException("Unexpected JSON element for RequestId: $element")
        }
    }

    override fun serialize(
        encoder: Encoder,
        value: RequestId,
    ) {
        val jsonEncoder =
            encoder as? JsonEncoder
                ?: throw SerializationException("RequestIdSerializer requires JSON format")
        val jsonElement =
            when (value) {
                is RequestId.NumericId -> JsonPrimitive(value.value)
                is RequestId.StringId -> JsonPrimitive(value.value)
            }
        jsonEncoder.encodeJsonElement(jsonElement)
    }
}

/**
 * Serializer for nullable [RequestId]`?`.
 *
 * Extends [RequestIdSerializer] to handle `null` (JSON `null`) values,
 * used in response objects where the id may be unknown.
 */
public object NullableRequestIdSerializer : KSerializer<RequestId?> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("RequestId?")

    override fun deserialize(decoder: Decoder): RequestId? {
        val jsonDecoder =
            decoder as? JsonDecoder
                ?: throw SerializationException("NullableRequestIdSerializer requires JSON format")
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonNull -> null
            is JsonPrimitive -> element.toRequestId()
            else -> throw SerializationException("Unexpected JSON element for RequestId: $element")
        }
    }

    override fun serialize(
        encoder: Encoder,
        value: RequestId?,
    ) {
        val jsonEncoder =
            encoder as? JsonEncoder
                ?: throw SerializationException("NullableRequestIdSerializer requires JSON format")
        val jsonElement =
            when (value) {
                null -> JsonNull
                is RequestId.NumericId -> JsonPrimitive(value.value)
                is RequestId.StringId -> JsonPrimitive(value.value)
            }
        jsonEncoder.encodeJsonElement(jsonElement)
    }
}

/**
 * Converts a [JsonPrimitive] to a [RequestId], distinguishing
 * JSON strings from JSON numbers by checking [JsonPrimitive.isString].
 *
 * @throws SerializationException if the value is not a valid string or integer.
 */
private fun JsonPrimitive.toRequestId(): RequestId =
    if (isString) {
        RequestId.StringId(content)
    } else {
        val longValue =
            longOrNull
                ?: throw SerializationException(
                    "Invalid RequestId: '$content' is not a valid string or integer",
                )
        RequestId.NumericId(longValue)
    }
