package dev.mokksy.mokksy.jsonrpc

import kotlinx.serialization.Serializable

/**
 * Represents a JSON-RPC 2.0 request identifier.
 *
 * Per the specification, an id can be a [String], an integer [Number][Long], or `null`.
 * The `null` case is represented by a nullable `RequestId?` at the usage site.
 *
 * Use factory functions [RequestId] to construct instances:
 * ```kotlin
 * val stringId: RequestId = RequestId("abc-123")
 * val intId: RequestId = RequestId(42)
 * ```
 *
 * @see <a href="https://www.jsonrpc.org/specification#request_object">JSON-RPC 2.0 Request Object</a>
 */
@Serializable(with = RequestIdSerializer::class)
public sealed interface RequestId {
    /** A string-valued request identifier. */
    public data class StringId(
        val value: String,
    ) : RequestId {
        override fun toString(): String = value
    }

    /** An integer-valued request identifier. */
    public data class NumericId(
        val value: Long,
    ) : RequestId {
        override fun toString(): String = value.toString()
    }
}

/** Creates a [RequestId] from a [String] value. */
public fun RequestId(value: String): RequestId = RequestId.StringId(value)

/** Creates a [RequestId] from a [Long] value. */
public fun RequestId(value: Long): RequestId = RequestId.NumericId(value)

/** Creates a [RequestId] from an [Int] value. */
public fun RequestId(value: Int): RequestId = RequestId.NumericId(value.toLong())
