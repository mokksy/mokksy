@file:OptIn(InternalMokksyApi::class)

package dev.mokksy.mokksy

import dev.mokksy.mokksy.request.NearMissFailedMatcher
import dev.mokksy.mokksy.request.NearMissRequest
import dev.mokksy.mokksy.request.NearMissResponse
import dev.mokksy.mokksy.request.NearMissStub
import dev.mokksy.mokksy.request.RecordedRequest
import dev.mokksy.mokksy.request.RequestJournal
import dev.mokksy.mokksy.utils.logger.HttpFormatter
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.server.logging.toLogString
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.RoutingRequest
import kotlinx.serialization.encodeToString

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
) {
    val request = context.call.request

    val findResult =
        stubRegistry.findMatchingStub(
            request = request,
            verbose = configuration.verbose,
            logger = application.log,
            formatter = formatter,
        )

    when (findResult) {
        is FindResult.Matched -> {
            val matchedStub = findResult.stub
            val recorded = RecordedRequest.from(request, matched = true)
            requestJournal.recordMatched(recorded)
            handleMatchedStub(
                matchedStub = matchedStub,
                serverConfig = configuration,
                application = application,
                request = request,
                context = context,
                formatter = formatter,
            )
        }

        is FindResult.NoMatch -> {
            val recorded = RecordedRequest.from(request, matched = false)
            requestJournal.recordUnmatched(recorded)

            if (configuration.verbose) {
                val availableStubs = stubRegistry.getAll()
                val availableStubsMessage =
                    if (availableStubs.isNotEmpty()) {
                        val stubsInfo = availableStubs.joinToString("\n---\n") { it.toLogString() }
                        "Available stubs:\n$stubsInfo"
                    } else {
                        "No stubs are available."
                    }
                application.log.warn(
                    "NO STUBS FOUND for the request:\n---\n${
                        formatter.formatRequest(request)
                    }\n---\n$availableStubsMessage\n",
                )
            } else {
                application.log.warn(
                    "No matched mapping for request:\n---\n${request.toLogString()}\n---",
                )
            }

            val nearMissResponse = buildNearMissResponse(context, findResult.evaluations)
            val jsonBody = configuration.json.encodeToString(nearMissResponse)
            context.call.respondText(
                text = jsonBody,
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.NotFound,
            )
        }
    }
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
        respond(context.call, verbose)
    }
}

/**
 * Builds a [NearMissResponse] from the request and per-stub evaluation results.
 */
@Suppress("TooGenericExceptionCaught")
private suspend fun buildNearMissResponse(
    context: RoutingContext,
    evaluations: List<StubEvaluation>,
): NearMissResponse {
    val request = context.call.request
    val bodyText =
        try {
            context.call.receiveText().ifBlank { null }
        } catch (_: Exception) {
            null
        }

    val nearMissRequest =
        NearMissRequest(
            method = request.local.method.value,
            path = request.local.uri,
            headers = request.headers.entries().associate { it.key to it.value },
            body = bodyText,
        )

    val nearMissStubs =
        evaluations.map { (stub, matchResult) ->
            NearMissStub(
                name = stub.configuration.name,
                passed = matchResult.diagnostics.filter { it.matched }.map { it.label },
                failed =
                    matchResult.diagnostics
                        .filter { !it.matched }
                        .map { NearMissFailedMatcher(matcher = it.label, reason = it.reason) },
            )
        }

    return NearMissResponse(
        message = "No stub matched the incoming request",
        request = nearMissRequest,
        nearMisses = nearMissStubs,
    )
}
