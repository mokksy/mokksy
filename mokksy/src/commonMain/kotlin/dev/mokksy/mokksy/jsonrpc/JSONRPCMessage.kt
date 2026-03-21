package dev.mokksy.mokksy.jsonrpc

import kotlinx.serialization.Contextual
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Base type for JSON-RPC 2.0 messages sent from client to server.
 *
 * A message is either a [JSONRPCRequest] (which carries an [id][JSONRPCRequest.id] and
 * expects a response) or a [JSONRPCNotification] (which has no id and expects no response).
 *
 * @param P The type of the [params] field.
 *
 * @see <a href="https://www.jsonrpc.org/specification#request_object">JSON-RPC 2.0 Request Object</a>
 */
@Serializable(with = JSONRPCMessageSerializer::class)
public sealed class JSONRPCMessage<out P> {
    /** The JSON-RPC version string. Always `"2.0"`. */
    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault
    public val jsonrpc: String = "2.0"

    /** The name of the method to be invoked. */
    public abstract val method: String

    /**
     * Structured parameter values for the method invocation.
     * May be `null` if the method takes no parameters.
     */
    @Contextual
    public abstract val params: P?
}

/**
 * A JSON-RPC 2.0 request that expects a response from the server.
 *
 * The [id] field correlates this request with its [JSONRPCResponse].
 *
 * @param P The type of the [params] field.
 * @property id The request identifier. The server MUST reply with the same value.
 *   Per the specification, the id can be a string, an integer, or `null`.
 * @property method The name of the method to invoke.
 * @property params Optional structured parameters for the method.
 *
 * Example:
 * ```kotlin
 * val request = JSONRPCRequest(
 *     id = RequestId(1),
 *     method = "tools/call",
 *     params = MyParams(toolName = "calculator"),
 * )
 * ```
 */
@Serializable
public data class JSONRPCRequest<out P>(
    val id: RequestId?,
    override val method: String,
    @Contextual override val params: P? = null,
) : JSONRPCMessage<P>()

/**
 * A JSON-RPC 2.0 notification — a request without an `id` field.
 *
 * The server MUST NOT reply to a notification.
 *
 * @param P The type of the [params] field.
 * @property method The name of the method to invoke.
 * @property params Optional structured parameters for the method.
 *
 * Example:
 * ```kotlin
 * val notification = JSONRPCNotification(
 *     method = "notifications/initialized",
 * )
 * ```
 */
@Serializable
public data class JSONRPCNotification<out P>(
    @SerialName("method")
    override val method: String,
    @Contextual
    @SerialName("params")
    override val params: P? = null,
) : JSONRPCMessage<P>()
