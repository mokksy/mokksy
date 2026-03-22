package dev.mokksy.mokksy.request

import dev.mokksy.mokksy.InternalMokksyApi
import io.ktor.http.HttpMethod
import io.ktor.server.request.receiveText
import io.ktor.server.routing.RoutingRequest
import kotlin.jvm.JvmStatic

/**
 * Immutable snapshot of an HTTP request for recording purposes.
 *
 * Unlike [CapturedRequest], this class does not retain references to
 * the underlying request or connection, making it safe for long-term storage
 * (e.g., tracking requests that arrived with no matching stub).
 *
 * @property method The HTTP method of the request.
 * @property uri The URI of the request.
 * @property headers The headers of the request.
 * @property matched Whether the request was matched by a stub.
 * @property body The typed request body captured during stub matching, or `null`
 *               if no typed body matchers ran or deserialization failed.
 * @property bodyAsText The raw request body as text, or `null` if the body was
 *                      empty or binary.
 */
public class RecordedRequest internal constructor(
    public val method: HttpMethod,
    public val uri: String,
    public val headers: Map<String, List<String>>,
    public val matched: Boolean,
    public val body: Any? = null,
    public val bodyAsText: String? = null,
) {
    internal companion object {
        /**
         * Creates a [RecordedRequest] snapshot from a [RoutingRequest].
         *
         * Body capture:
         * - Typed body from [CAPTURED_TYPED_BODY] call attribute (set during matching) → [body]
         * - Raw text via [receiveText] → [bodyAsText] (requires `DoubleReceive` plugin)
         *
         * Both [body] and [bodyAsText] may be populated for the same request.
         */
        @InternalMokksyApi
        @JvmStatic
        internal suspend fun from(
            request: RoutingRequest,
            matched: Boolean,
        ): RecordedRequest {
            val typedBody = request.call.attributes.getOrNull(CAPTURED_TYPED_BODY)

            @Suppress("TooGenericExceptionCaught")
            val bodyText = try {
                request.call.receiveText().blankToNull()
            } catch (_: Exception) {
                null
            }

            return RecordedRequest(
                method = request.local.method,
                uri = request.local.uri,
                headers = request.headers.entries().associate { it.key to it.value },
                matched = matched,
                body = typedBody,
                bodyAsText = bodyText,
            )
        }
    }

    override fun toString(): String = buildString {
        append("${method.value} $uri")
        val preview = bodyAsText ?: body?.toString()
        if (!preview.isNullOrBlank()) {
            append("\nBody: ")
            append(preview)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RecordedRequest) return false

        if (matched != other.matched) return false
        if (method != other.method) return false
        if (uri != other.uri) return false
        if (headers != other.headers) return false
        if (bodyAsText != other.bodyAsText) return false

        return true
    }

    override fun hashCode(): Int {
        var result = matched.hashCode()
        result = 31 * result + method.hashCode()
        result = 31 * result + uri.hashCode()
        result = 31 * result + headers.hashCode()
        result = 31 * result + (bodyAsText?.hashCode() ?: 0)
        return result
    }
}

private fun String.blankToNull(): String? = ifBlank { null }
