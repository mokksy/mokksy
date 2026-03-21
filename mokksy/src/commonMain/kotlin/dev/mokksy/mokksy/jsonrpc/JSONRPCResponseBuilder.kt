package dev.mokksy.mokksy.jsonrpc

import dev.mokksy.mokksy.MokksyDsl
import kotlin.jvm.JvmName

/**
 * DSL builder for constructing [JSONRPCResponse] instances.
 *
 * Set either [result] for a success response or [error] for a failure response.
 *
 * @param T The type of the result value.
 * @param E The type of the error data.
 *
 * Example:
 * ```kotlin
 * val success = jsonRPCResponse<Int> {
 *     id = RequestId(1)
 *     result = 42
 * }
 *
 * val failure = jsonRPCResponse<Nothing, Nothing> {
 *     id = RequestId(1)
 *     error = JSONRPCError.methodNotFound()
 * }
 * ```
 */
@MokksyDsl
public class JSONRPCResponseBuilder<T, E> {
    /** The request identifier to echo back. */
    public var id: RequestId? = null

    private var resultSet = false
    private var errorSet = false

    /** The result value for a successful response. Mutually exclusive with [error]. */
    public var result: T? = null
        set(value) {
            field = value
            resultSet = true
        }

    /** The error object for a failed response. Mutually exclusive with [result]. */
    public var error: JSONRPCError<E>? = null
        set(value) {
            field = value
            errorSet = true
        }

    /**
     * Builds the error inline using the [JSONRPCErrorBuilder] DSL.
     *
     * ```kotlin
     * jsonRPCResponse<Nothing, Nothing> {
     *     id = RequestId(1)
     *     error {
     *         code = JSONRPCError.ErrorCodes.INTERNAL_ERROR
     *         message = "Something went wrong"
     *     }
     * }
     * ```
     */
    public inline fun error(init: JSONRPCErrorBuilder<E>.() -> Unit) {
        error = jsonRPCError(init)
    }

    /**
     * Builds a [JSONRPCResponse] from the configured properties.
     *
     * @throws IllegalArgumentException if both [result] and [error] are set, or neither is set.
     */
    public fun build(): JSONRPCResponse<T, E> {
        require(resultSet || errorSet) { "Either 'result' or 'error' must be set" }
        require(!(resultSet && errorSet)) { "'result' and 'error' are mutually exclusive" }
        return if (resultSet) {
            @Suppress("UNCHECKED_CAST")
            JSONRPCResponse.Success(
                id = requireNotNull(id) { "Success response must have a non-null 'id'" },
                result = result as T,
            )
        } else {
            JSONRPCResponse.Error(id = id, error = requireNotNull(error))
        }
    }
}

/**
 * Creates a [JSONRPCResponse] using the DSL builder.
 *
 * @param T The type of the result value.
 * @param E The type of the error data.
 */
public inline fun <T, E> jsonRPCResponse(
    init: JSONRPCResponseBuilder<T, E>.() -> Unit,
): JSONRPCResponse<T, E> = JSONRPCResponseBuilder<T, E>().apply(init).build()

/**
 * Creates a [JSONRPCResponse] using the DSL builder, with error data type defaulting to [Nothing].
 *
 * Convenience overload for the common case where error data is not used.
 *
 * @param T The type of the result value.
 */
@JvmName("jsonRPCResponseResult")
public inline fun <T> jsonRPCResponse(
    init: JSONRPCResponseBuilder<T, Nothing>.() -> Unit,
): JSONRPCResponse<T, Nothing> = JSONRPCResponseBuilder<T, Nothing>().apply(init).build()
