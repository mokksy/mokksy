package dev.mokksy.mokksy.jsonrpc

import kotlinx.serialization.Contextual
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a JSON-RPC 2.0 response.
 *
 * Per the specification, a response contains either a [result][Success.result] on success
 * or an [error][Error.error] on failure — never both and never neither.
 * This sealed hierarchy makes that invariant unrepresentable at compile time.
 *
 * @param T The type of the successful result value.
 * @param E The type of the error's optional `data` field.
 *
 * Example:
 * ```kotlin
 * val success: JSONRPCResponse<Int, Nothing> = JSONRPCResponse.Success(
 *     id = RequestId(1),
 *     result = 42,
 * )
 * val failure: JSONRPCResponse<Nothing, Nothing> = JSONRPCResponse.Error(
 *     id = RequestId(1),
 *     error = JSONRPCError.methodNotFound("tools/call"),
 * )
 * ```
 *
 * @see <a href="https://www.jsonrpc.org/specification#response_object">JSON-RPC 2.0 Response Object</a>
 */
@Serializable(with = JSONRPCResponseSerializer::class)
public sealed class JSONRPCResponse<out T, out E> {
    /** The JSON-RPC version string. Always `"2.0"`. */
    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault
    @SerialName("jsonrpc")
    public val jsonrpc: String = "2.0"

    /** The request identifier echoed back from the original request. */
    @SerialName("id")
    public abstract val id: RequestId?

    /**
     * A successful JSON-RPC 2.0 response carrying a [result] value.
     *
     * @param T The type of the result.
     * @property id The request identifier.
     * @property result The result value determined by the invoked method.
     */
    public data class Success<out T>(
        @SerialName("id")
        override val id: RequestId,
        @Contextual
        @SerialName("result")
        val result: T,
    ) : JSONRPCResponse<T, Nothing>()

    /**
     * A failed JSON-RPC 2.0 response carrying an [error] object.
     *
     * @param E The type of the error's optional `data` field.
     * @property id The request identifier, or `null` if the id could not be determined.
     * @property error The error object describing the failure.
     */
    public data class Error<out E>(
        @SerialName("id")
        override val id: RequestId?,
        @SerialName("error")
        val error: JSONRPCError<E>,
    ) : JSONRPCResponse<Nothing, E>()
}
