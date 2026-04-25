package dev.mokksy.mokksy.config

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import dev.mokksy.mokksy.MokksyServer
import dev.mokksy.mokksy.SseEvent
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlin.time.Duration.Companion.milliseconds

private val VALID_METHODS = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS")

internal fun parseYamlConfig(text: String): MokksyFileConfig =
    Yaml(configuration = YamlConfiguration(strictMode = false))
        .decodeFromString(MokksyFileConfig.serializer(), text)

internal fun validateConfig(config: MokksyFileConfig) {
    config.stubs.forEachIndexed { index, stub ->
        val id = stub.name ?: "stubs[$index]"
        require(stub.path.isNotBlank()) { "$id: 'path' must not be blank" }
        require(stub.normalizedMethod in VALID_METHODS) {
            "$id: unknown HTTP method '${stub.method}'. Valid methods: ${VALID_METHODS.joinToString()}"
        }
        when (stub.response.type) {
            ResponseType.SSE, ResponseType.STREAM -> {
                require(stub.response.chunks.isNotEmpty()) {
                    "$id: response type '${stub.response.type.name.lowercase()}' requires at least one chunk"
                }
            }

            ResponseType.PLAIN -> {
                // noop
            }
        }
    }
}

internal fun MokksyServer.applyConfig(config: MokksyFileConfig) {
    config.stubs.forEach { stub -> registerFromConfig(stub) }
}

private fun MokksyServer.registerFromConfig(stub: StubConfig) {
    val buildingStep =
        method(
            name = stub.name,
            httpMethod = HttpMethod.parse(stub.normalizedMethod),
            requestType = String::class,
        ) {
            path(stub.path)
            stub.match.bodyContains.forEach { bodyContains(it) }
            stub.match.headers.forEach { (name, value) -> containsHeader(name, value) }
        }

    when (stub.response.type) {
        ResponseType.PLAIN -> {
            buildingStep.respondsWith {
                body = stub.response.body
                httpStatus = HttpStatusCode.fromValue(stub.response.status)
                stub.response.headers.forEach { (name, value) -> addHeader(name, value) }
                delay = stub.response.delayMs.milliseconds
            }
        }

        ResponseType.SSE -> {
            buildingStep.respondsWithSseStream {
                stub.response.chunks.forEach { chunk -> chunks += SseEvent.data(chunk) }
                delayBetweenChunks = stub.response.delayBetweenChunksMs.milliseconds
                delay = stub.response.delayMs.milliseconds
            }
        }

        ResponseType.STREAM -> {
            buildingStep.respondsWithStream {
                stub.response.chunks.forEach { chunk -> chunks += chunk }
                delayBetweenChunks = stub.response.delayBetweenChunksMs.milliseconds
                delay = stub.response.delayMs.milliseconds
                stub.response.contentType?.let { ct -> contentType = ContentType.parse(ct) }
            }
        }
    }
}
