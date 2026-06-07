@file:OptIn(InternalMokksyApi::class)

package dev.mokksy.mokksy.request

import dev.mokksy.mokksy.InternalMokksyApi
import dev.mokksy.mokksy.ServerConfiguration
import dev.mokksy.mokksy.Stub
import dev.mokksy.mokksy.StubLookupResult
import dev.mokksy.mokksy.StubRegistry
import dev.mokksy.mokksy.response.AbstractResponseDefinition
import dev.mokksy.mokksy.utils.logger.HttpFormatter
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.server.logging.toLogString
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.RoutingRequest
import kotlinx.coroutines.CancellationException

/**
 * Processes an incoming HTTP request by matching it against available stubs and handling the response.
 *
 * Attempts to find the best matching stub for the request.
 * If a match is found, processes the stub and optionally removes it based on configuration.
 * If no match is found, it logs the event and triggers a failure.
 *
 * @param context The routing context containing the request and response.
 * @param stubRegistry The registry of available stubs to match against.
 * @param configuration Server configuration settings that influence matching and logging behavior.
 * @param formatter Formats HTTP requests for logging and error messages.
 * @author Konstantin Pavlov
 */
@Suppress("LongParameterList")
internal suspend fun handleRequest(
    context: RoutingContext,
    application: Application,
    stubRegistry: StubRegistry,
    requestJournal: RequestJournal,
    configuration: ServerConfiguration,
    formatter: HttpFormatter,
    responseListener: (suspend (RecordedRequest, AbstractResponseDefinition<*>) -> Unit)? = null,
) {
    val request = context.call.request

    when (
        val result =
            stubRegistry.findMatchingStub(
                request = request,
                verbose = configuration.verbose,
                logger = application.log,
                formatter = formatter,
            )
    ) {
        is StubLookupResult.Matched -> {
            if (requestJournal.recordsMatched) {
                requestJournal.recordMatched(RecordedRequest.from(request, matched = true))
            }
            handleMatchedStub(
                matchedStub = result.stub,
                serverConfig = configuration,
                application = application,
                request = request,
                context = context,
                formatter = formatter,
                responseListener = responseListener,
            )
        }

        is StubLookupResult.NotMatched -> {
            if (requestJournal.recordsUnmatched) {
                requestJournal.recordUnmatched(RecordedRequest.from(request, matched = false))
            }
            val errorMessage = "No matched mapping for request: ${request.toLogString()}"
            if (configuration.verbose) {
                handleVerboseNotFound(
                    context,
                    application,
                    result,
                    configuration,
                    formatter,
                )
            } else {
                application.log.warn(
                    "No matched mapping for request:\n---\n${request.toLogString()}\n---",
                )
                context.call.respond(HttpStatusCode.NotFound, errorMessage)
            }
        }
    }
}

@Suppress("LongParameterList", "ThrowsCount")
private suspend fun handleVerboseNotFound(
    context: RoutingContext,
    application: Application,
    result: StubLookupResult.NotMatched,
    configuration: ServerConfiguration,
    formatter: HttpFormatter,
) {
    val request = context.call.request

    val bestScore = result.evaluations.maxOfOrNull { it.matchResult.score }
    val topEvals =
        if (bestScore != null) {
            result.evaluations.filter { it.matchResult.score == bestScore }
        } else {
            result.evaluations
        }

    val diagnosticData: Pair<RequestInfo, List<StubMatchResult>>? =
        try {
            val requestInfo = extractRequestInfo(context, configuration.json)
            val stubResults = topEvals.map { it.toStubMatchResult(request) }
            requestInfo to stubResults
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }

    val formattedRequest =
        try {
            formatter.formatRequest(request)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            "<Unable to format request for diagnostics>"
        }
    val formattedDiagnostics =
        if (diagnosticData != null) {
            try {
                DiagnosticLogger.format(diagnosticData.second, formatter.useColor)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                "<Unable to format matcher diagnostics>"
            }
        } else {
            "<Diagnostic response not available>"
        }
    application.log.warn(
        "\n=== No stub matched for request ===\n\n${
            formattedRequest
        }\n$formattedDiagnostics",
    )

    context.call.respond(
        HttpStatusCode.NotFound,
        diagnosticData?.let { (requestInfo, stubResults) ->
            DiagnosticResponse(request = requestInfo, stubEvaluations = stubResults)
        } ?: "No stub matched and diagnostic response could not be built",
    )
}

/**
 * Processes a matched stub by logging the match and sending the stubbed response.
 *
 * If verbose logging is enabled in either the server or stub configuration,
 * logs detailed information about the matched request and stub.
 */
@Suppress("LongParameterList")
private suspend fun handleMatchedStub(
    matchedStub: Stub<*, *>,
    serverConfig: ServerConfiguration,
    application: Application,
    request: RoutingRequest,
    context: RoutingContext,
    formatter: HttpFormatter,
    responseListener: (suspend (RecordedRequest, AbstractResponseDefinition<*>) -> Unit)? = null,
) {
    val config = matchedStub.configuration
    val verbose = serverConfig.verbose || config.verbose

    matchedStub.apply {
        if (verbose) {
            application.log.info(
                "Request matched:\n---\n${
                    formatter.formatRequest(
                        request,
                    )
                }\n---\n${this.toLogString()}",
            )
        }
        respond(
            call = context.call,
            verbose = verbose,
            routingRequest = request,
            responseListener = responseListener,
        )
    }
}
