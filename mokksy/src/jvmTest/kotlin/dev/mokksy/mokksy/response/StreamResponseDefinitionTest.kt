@file:OptIn(InternalMokksyApi::class)

package dev.mokksy.mokksy.response

import dev.mokksy.mokksy.InternalMokksyApi
import dev.mokksy.mokksy.utils.logger.HttpFormatter
import io.kotest.matchers.shouldBe
import io.ktor.http.ContentType
import kotlinx.coroutines.flow.flowOf
import kotlin.test.Test
import kotlin.time.Duration.Companion.ZERO

internal class StreamResponseDefinitionTest {
    @Test
    fun `chunk log content type uses stream content type by default`() {
        val definition =
            StreamResponseDefinition(
                // language=json
                chunkFlow = flowOf("""{"value":1}"""),
                contentType = ContentType.parse("application/x-ndjson"),
                delay = ZERO,
                formatter = HttpFormatter(),
            )

        definition.chunkLogContentType("""{"value":1}""") shouldBe
            ContentType.parse("application/x-ndjson")
    }

    @Test
    fun `chunk log content type prefers explicit chunk content type`() {
        val definition =
            StreamResponseDefinition(
                chunkFlow = flowOf("""{"value":1}"""),
                contentType = ContentType.parse("application/x-ndjson"),
                chunkContentType = ContentType.Application.Json,
                delay = ZERO,
                formatter = HttpFormatter(),
            )

        definition.chunkLogContentType("""{"value":1}""") shouldBe ContentType.Application.Json
    }
}
