@file:OptIn(InternalMokksyApi::class)

package dev.mokksy.mokksy.request

import dev.mokksy.mokksy.InternalMokksyApi
import dev.mokksy.mokksy.utils.highlight.Highlighting
import io.ktor.server.request.contentType
import io.ktor.server.request.httpMethod
import io.ktor.server.request.receiveText
import io.ktor.server.request.uri
import io.ktor.server.routing.RoutingContext
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/**
 * Captures the incoming request for the diagnostic 404 response body.
 *
 * Body is best-effort: requires the Ktor `DoubleReceive` plugin.
 * When absent or already consumed, [RequestInfo.body] will be `null`.
 * When the content type is JSON, the body is parsed into a [JsonElement] for
 * structured display; otherwise it is wrapped as a [JsonPrimitive].
 */
internal suspend fun extractRequestInfo(
    context: RoutingContext,
    json: Json,
): RequestInfo {
    val request = context.call.request
    val contentType = request.contentType()
    val isJsonBody = Highlighting.isJsonContentType(contentType)
    val bodyText =
        try {
            context.call.receiveText()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    val bodyElement: JsonElement? =
        bodyText?.ifBlank { null }?.let { text ->
            if (isJsonBody) {
                try {
                    json.parseToJsonElement(text)
                } catch (_: Exception) {
                    JsonPrimitive(text)
                }
            } else {
                JsonPrimitive(text)
            }
        }
    return RequestInfo(
        method = request.httpMethod.value,
        path = request.uri,
        headers =
            request.headers.entries().associate { (key, values) ->
                key to
                    values.joinToString(", ")
            },
        body = bodyElement,
    )
}
