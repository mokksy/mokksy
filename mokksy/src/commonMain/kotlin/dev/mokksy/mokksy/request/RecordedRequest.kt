package dev.mokksy.mokksy.request

import dev.mokksy.mokksy.DEFAULT_MAX_BODY_CAPTURE_SIZE
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
 *                      empty, binary, or a typed body was captured instead.
 *                      Truncated to [dev.mokksy.mokksy.DEFAULT_MAX_BODY_CAPTURE_SIZE]
 *                      if the original body exceeds that limit.
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
         * Body capture priority:
         * 1. Typed body from [CAPTURED_TYPED_BODY] call attribute (set during matching) → [body]
         * 2. Body text from [CAPTURED_BODY_TEXT] call attribute (set during matching) → [bodyAsText]
         * 3. If neither attribute exists, falls back to [receiveText] → [bodyAsText]
         * 4. If typed body was captured, skips the text fallback to avoid redundant I/O
         */
        @InternalMokksyApi
        @JvmStatic
        internal suspend fun from(
            request: RoutingRequest,
            matched: Boolean,
            maxBodyCaptureSize: Int = DEFAULT_MAX_BODY_CAPTURE_SIZE,
        ): RecordedRequest {
            val attrs = request.call.attributes
            val typedBody = attrs.getOrNull(CAPTURED_TYPED_BODY)

            // Priority: use text from matching if available, else fall back to receiveText()
            // If typed body was captured, skip text read — typed representation is sufficient
            @Suppress("TooGenericExceptionCaught")
            val bodyText = when {
                attrs.contains(CAPTURED_BODY_TEXT) -> {
                    attrs[CAPTURED_BODY_TEXT].truncateOrNull(maxBodyCaptureSize)
                }
                typedBody != null -> null
                else -> try {
                    request.call.receiveText().truncateOrNull(maxBodyCaptureSize)
                } catch (_: Exception) {
                    null
                }
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

private fun String.truncateOrNull(maxSize: Int): String? = when {
    isBlank() -> null
    length > maxSize -> take(maxSize)
    else -> this
}
