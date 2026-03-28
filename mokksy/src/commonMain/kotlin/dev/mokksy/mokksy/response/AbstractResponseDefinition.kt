package dev.mokksy.mokksy.response

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.ResponseHeaders
import kotlin.time.Duration

/**
 * Represents a supplier function for generating response definitions.
 *
 * This type alias is used to define functions that take an [ApplicationCall]
 * as input and return an [AbstractResponseDefinition].
 * The function is marked as suspend to allow for asynchronous operations within the response definition.
 */
internal typealias ResponseDefinitionSupplier<T> = suspend (
    ApplicationCall,
) -> AbstractResponseDefinition<T>

/**
 * Represents the base definition of an HTTP response in a mapping between a request and its corresponding response.
 * Provides the required attributes and behavior for configuring HTTP responses, including status code, headers,
 * and content type. This class serves as the foundation for more specialized response definitions.
 *
 * @param T The type of the response data.
 * @property contentType The MIME type of the response content.
 * @property httpStatus The HTTP status code of the response. Defaults to HttpStatusCode.OK.
 * @property headers A lambda that configures the response headers. Defaults to `null`.
 * @property delay A delay applied before sending the response. Defaults to Duration.ZERO.
 * @author Konstantin Pavlov
 */
public abstract class AbstractResponseDefinition<T>(
    public val contentType: ContentType,
    public val httpStatus: HttpStatusCode = HttpStatusCode.OK,
    public val headers: (ResponseHeaders.() -> Unit)? = null,
    public open val delay: Duration = Duration.ZERO,
) {
    internal abstract suspend fun writeResponse(
        call: ApplicationCall,
        verbose: Boolean,
    )
}
