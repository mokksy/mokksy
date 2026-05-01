package dev.mokksy.mokksy.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class MokksyFileConfig(
    val stubs: List<StubConfig> = emptyList(),
)

@Serializable
internal data class StubConfig(
    val name: String? = null,
    val method: String = "GET",
    val path: String,
    val match: MatchConfig = MatchConfig(),
    val response: ResponseConfig,
) {
    val normalizedMethod: String get() = method.uppercase()
}

@Serializable
internal data class MatchConfig(
    val bodyContains: List<String> = emptyList(),
    val headers: Map<String, String> = emptyMap(),
)

@Serializable
internal data class ResponseConfig(
    val type: ResponseType = ResponseType.PLAIN,
    val body: String? = null,
    val status: Int = 200,
    val headers: Map<String, String> = emptyMap(),
    val delayMs: Long = 0,
    val chunks: List<String> = emptyList(),
    val delayBetweenChunksMs: Long = 0,
    val contentType: String? = null,
)

@Serializable
internal enum class ResponseType {
    @SerialName("plain")
    PLAIN,

    @SerialName("sse")
    SSE,

    @SerialName("stream")
    STREAM,
}
