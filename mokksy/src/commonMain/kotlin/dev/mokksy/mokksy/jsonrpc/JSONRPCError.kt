package dev.mokksy.mokksy.jsonrpc

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a JSON-RPC 2.0 error object.
 *
 * @param D The type of the optional [data] field carrying additional error details.
 * @property code The integer error code. Standard codes are defined in [ErrorCodes].
 * @property message A short human-readable description of the error.
 * @property data Optional structured value with additional information about the error.
 *
 * Example:
 * ```kotlin
 * val error = JSONRPCError.methodNotFound("tools/call")
 * val typed = JSONRPCError(
 *     code = -32000,
 *     message = "Custom server error",
 *     data = MyErrorDetail("extra info"),
 * )
 * ```
 *
 * @see <a href="https://www.jsonrpc.org/specification#error_object">JSON-RPC 2.0 Error Object</a>
 */
@Serializable
public data class JSONRPCError<out D>(
    @SerialName("code")
    val code: Int,
    @SerialName("message")
    val message: String,
    @SerialName("data")
    @Contextual
    val data: D? = null,
) {
    // region Error Codes

    /**
     * Well-known JSON-RPC 2.0 error codes.
     *
     * @see <a href="https://www.jsonrpc.org/specification#error_object">JSON-RPC 2.0 Error Object</a>
     */
    public object ErrorCodes {
        /** -32700 Parse error — invalid JSON was received by the server. */
        public const val PARSE_ERROR: Int = -32700

        /** -32600 Invalid Request — the JSON sent is not a valid Request object. */
        public const val INVALID_REQUEST: Int = -32600

        /** -32601 Method not found — the method does not exist or is not available. */
        public const val METHOD_NOT_FOUND: Int = -32601

        /** -32602 Invalid params — invalid method parameter(s). */
        public const val INVALID_PARAMS: Int = -32602

        /** -32603 Internal error — internal JSON-RPC error. */
        public const val INTERNAL_ERROR: Int = -32603

        private const val SERVER_ERROR_RANGE_START: Int = -32000
        private const val SERVER_ERROR_RANGE_END: Int = -32099

        /** Returns `true` if [code] is in the server-error range (-32000 to -32099). */
        public fun isServerError(code: Int): Boolean =
            code in SERVER_ERROR_RANGE_START downTo SERVER_ERROR_RANGE_END
    }

    // endregion

    // region Companion Factory Methods

    public companion object {
        /** Creates a Parse error (-32700). */
        public fun parseError(message: String = "Parse error"): JSONRPCError<Nothing> =
            JSONRPCError(code = ErrorCodes.PARSE_ERROR, message = message)

        /** Creates an Invalid Request error (-32600). */
        public fun invalidRequest(message: String = "Invalid Request"): JSONRPCError<Nothing> =
            JSONRPCError(code = ErrorCodes.INVALID_REQUEST, message = message)

        /** Creates a Method not found error (-32601). */
        public fun methodNotFound(method: String? = null): JSONRPCError<Nothing> =
            JSONRPCError(
                code = ErrorCodes.METHOD_NOT_FOUND,
                message = if (method != null) "Method not found: $method" else "Method not found",
            )

        /** Creates an Invalid params error (-32602). */
        public fun invalidParams(message: String = "Invalid params"): JSONRPCError<Nothing> =
            JSONRPCError(code = ErrorCodes.INVALID_PARAMS, message = message)

        /** Creates an Internal error (-32603). */
        public fun internalError(message: String = "Internal error"): JSONRPCError<Nothing> =
            JSONRPCError(code = ErrorCodes.INTERNAL_ERROR, message = message)
    }

    // endregion
}
