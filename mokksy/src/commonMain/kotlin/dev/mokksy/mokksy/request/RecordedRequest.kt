package dev.mokksy.mokksy.request

import io.ktor.http.HttpMethod
import io.ktor.server.routing.RoutingRequest

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
 */
public class RecordedRequest internal constructor(
    public val method: HttpMethod,
    public val uri: String,
    public val headers: Map<String, List<String>>,
    public val matched: Boolean,
) {
    public companion object {
        /**
         * Creates a [RecordedRequest] snapshot from a [io.ktor.server.routing.RoutingRequest].
         */
        public fun from(
            request: RoutingRequest,
            matched: Boolean,
        ): RecordedRequest =
            RecordedRequest(
                method = request.local.method,
                uri = request.local.uri,
                headers = request.headers.entries().associate { it.key to it.value },
                matched = matched,
            )
    }

    override fun toString(): String = "${method.value} $uri"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RecordedRequest) return false

        if (matched != other.matched) return false
        if (method != other.method) return false
        if (uri != other.uri) return false
        if (headers != other.headers) return false

        return true
    }

    override fun hashCode(): Int {
        var result = matched.hashCode()
        result = 31 * result + method.hashCode()
        result = 31 * result + uri.hashCode()
        result = 31 * result + headers.hashCode()
        return result
    }
}
