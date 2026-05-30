package dev.mokksy.mokksy

import dev.mokksy.mokksy.request.RecordedRequest
import dev.mokksy.mokksy.response.AbstractResponseDefinition

/**
 * A listener invoked when a matched stub is about to send its response.
 *
 * This is a Java-friendly alternative to the suspend-lambda [MokksyServer.onResponseReady] overload.
 *
 * Example (Java):
 * ```java
 * mokksy.addListener((request, response) -> {
 *     assertThat(request.getUri()).isEqualTo("/path");
 * });
 * ```
 *
 * @see MokksyServer.addListener
 * @see MokksyServer.onResponseReady
 */
@ExperimentalMokksyApi
public fun interface RequestListener {
    /**
     * Called when a matched stub is about to send its response.
     *
     * The listener fires after response headers and status have been applied,
     * but before the response body is written.
     *
     * @param request The recorded incoming request.
     * @param response The response definition that will be sent.
     */
    public fun onResponseReady(
        request: RecordedRequest,
        response: AbstractResponseDefinition<*>,
    )
}
