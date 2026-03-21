package dev.mokksy.mokksy.jsonrpc

import dev.mokksy.mokksy.MokksyDsl

/**
 * DSL builder for constructing [JSONRPCError] instances.
 *
 * @param D The type of the error's optional [data] field.
 *
 * Example:
 * ```kotlin
 * val error = jsonRPCError {
 *     code = -32600
 *     message = "Invalid Request"
 *     data = "Missing 'method' field"
 * }
 * ```
 */
@MokksyDsl
public class JSONRPCErrorBuilder<D> {
    /** The integer error code. See [JSONRPCError.ErrorCodes] for standard values. */
    public var code: Int? = null

    /** A short human-readable description of the error. */
    public var message: String? = null

    /** Optional structured data with additional error information. */
    public var data: D? = null

    /** Builds a [JSONRPCError] from the configured properties. */
    public fun build(): JSONRPCError<D> =
        JSONRPCError(
            code = requireNotNull(code) { "JSONRPCError.code must be provided" },
            message = requireNotNull(message) { "JSONRPCError.message must be provided" },
            data = data,
        )
}

/**
 * Creates a [JSONRPCError] using the DSL builder.
 *
 * ```kotlin
 * val error = jsonRPCError {
 *     code = JSONRPCError.ErrorCodes.INVALID_PARAMS
 *     message = "Invalid params"
 *     data = "Expected integer, got string"
 * }
 * ```
 */
public inline fun <D> jsonRPCError(init: JSONRPCErrorBuilder<D>.() -> Unit): JSONRPCError<D> =
    JSONRPCErrorBuilder<D>().apply(init).build()
