@file:OptIn(InternalMokksyApi::class)

package dev.mokksy.mokksy.utils.logger

import dev.mokksy.mokksy.InternalMokksyApi
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.string.shouldContain
import io.ktor.http.ContentType
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlin.test.Test

internal class HttpFormatterTest {
    @Serializable
    private data class SimpleDto(
        val name: String,
        val age: Int,
    )

    private data class ContextualDto(
        val value: String,
    )

    private object ContextualDtoSerializer : KSerializer<ContextualDto> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("ContextualDto", PrimitiveKind.STRING)

        override fun serialize(
            encoder: Encoder,
            value: ContextualDto,
        ) {
            encoder.encodeString(value.value)
        }

        override fun deserialize(decoder: Decoder): ContextualDto =
            ContextualDto(decoder.decodeString())
    }

    @Test
    fun `formatBody encodes Serializable class to JSON`() {
        val formatter = HttpFormatter(useColor = false)
        val result =
            formatter.formatBody(
                SimpleDto("Alice", 30),
                ContentType.Application.Json,
            )
        assertSoftly(result) {
            this shouldContain "\"name\""
            this shouldContain "\"Alice\""
            this shouldContain "\"age\""
            this shouldContain "30"
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `formatBody encodes contextual type using configured serializersModule`() {
        val json =
            Json {
                serializersModule =
                    SerializersModule {
                        contextual(ContextualDto::class, ContextualDtoSerializer)
                    }
            }
        val formatter = HttpFormatter(useColor = false, json = json)
        val result = formatter.formatBody(ContextualDto("hello"), ContentType.Application.Json)
        result shouldContain "hello"
    }

    @Test
    fun `formatBody returns toString for unregistered type with non-JSON content type`() {
        val formatter = HttpFormatter(useColor = false)
        val dto = ContextualDto("world")
        val result = formatter.formatBody(dto, ContentType.Text.Plain)
        result shouldContain dto.toString()
    }
}
