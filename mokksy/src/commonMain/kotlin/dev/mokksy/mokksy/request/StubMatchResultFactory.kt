@file:OptIn(InternalMokksyApi::class)

package dev.mokksy.mokksy.request

import dev.mokksy.mokksy.InternalMokksyApi
import dev.mokksy.mokksy.StubEvaluation
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path

internal fun StubEvaluation.toStubMatchResult(
    request: io.ktor.server.request.ApplicationRequest,
): StubMatchResult {
    val spec = stub.requestSpecification
    return StubMatchResult(
        name = stub.configuration.name ?: "Stub #${stub.creationOrder}",
        configuredMatchers = buildConfiguredMatchers(spec),
        failedMatchers =
            matchResult.failedMatchers.map { descriptor ->
                describeFailedMatcher(descriptor, spec, request)
            },
    )
}

private fun buildConfiguredMatchers(spec: RequestSpecification<*>): List<String> =
    buildList {
        spec.method?.let { add("method: $it") }
        spec.path?.let { add("path: $it") }
        addIfNotEmpty(spec.headers, "headers")
        addIfNotEmpty(spec.body, "body") { matcherLabel(it, "body") }
        addIfNotEmpty(spec.bodyString, "bodyString") { matcherLabel(it, "bodyString") }
        addIfNotEmpty(spec.cookies, "cookies")
        addIfNotEmpty(spec.queryParameters, "queryParams")
        addIfNotEmpty(spec.formSpecs, "form") { f ->
            val parts = f.parts.joinToString(", ") { it.name }
            "${f.encoding} parts=[$parts]"
        }
        addIfNotEmpty(spec.multipartSpecs, "multipart") { mp ->
            val parts = mp.parts.joinToString(", ") { it.name }
            "${mp.contentType} parts=[$parts]"
        }
        addIfNotEmpty(spec.byteBodySpecs, "bytes") { bb ->
            val ct = bb.contentTypeMatcher?.let { matcherLabel(it, "content-type") } ?: "any"
            "contentType=$ct"
        }
    }

private fun <T> MutableList<String>.addIfNotEmpty(
    items: List<T>,
    label: String,
    format: (T) -> String = { it.toString() },
) {
    if (items.isNotEmpty()) {
        add("$label: ${items.joinToString(", ", transform = format)}")
    }
}

internal fun matcherLabel(
    matcher: Any,
    fallback: String,
): String {
    val str = matcher.toString()
    return if (str.contains('@')) fallback else str
}

internal fun describeFailedMatcher(
    descriptor: FailedMatcherDescriptor,
    spec: RequestSpecification<*>,
    request: io.ktor.server.request.ApplicationRequest,
): String =
    when (descriptor) {
        is FailedMatcherDescriptor.Simple -> {
            when (descriptor.category) {
                MatcherCategory.METHOD -> {
                    "method: expected ${spec.method} but was ${request.httpMethod.value}"
                }

                MatcherCategory.PATH -> {
                    "path: expected ${spec.path} but was ${request.path()}"
                }

                else -> {
                    descriptor.category.toString()
                }
            }
        }

        is FailedMatcherDescriptor.Indexed -> {
            describeIndexedFailedMatcher(descriptor, spec, request)
        }
    }

@Suppress("CyclomaticComplexMethod", "LongMethod")
private fun describeIndexedFailedMatcher(
    descriptor: FailedMatcherDescriptor.Indexed,
    spec: RequestSpecification<*>,
    request: io.ktor.server.request.ApplicationRequest,
): String =
    when (descriptor.category) {
        MatcherCategory.HEADERS -> {
            val matcherStr = spec.headers.getOrNull(descriptor.index)?.toString()
            if (matcherStr == null) {
                "headers: <missing>"
            } else {
                val headerName = matcherStr.substringBefore(" = ").trim()
                val actualValue = request.headers[headerName]
                if (actualValue != null) {
                    "headers: expected $matcherStr but was '$actualValue'"
                } else {
                    "headers: expected $matcherStr but header was not present"
                }
            }
        }

        MatcherCategory.BODY -> {
            val str = spec.body.getOrNull(descriptor.index)?.let { matcherLabel(it, "body") }
            if (str != null) {
                "body: expected $str"
            } else {
                "body: <missing>"
            }
        }

        MatcherCategory.BODY_STRING -> {
            val str =
                spec.bodyString
                    .getOrNull(
                        descriptor.index,
                    )?.let { matcherLabel(it, "bodyString") }
            if (str != null) {
                "bodyString: expected $str"
            } else {
                "bodyString: <missing>"
            }
        }

        MatcherCategory.MULTIPART -> {
            val mp = spec.multipartSpecs.getOrNull(descriptor.index)
            if (mp != null) {
                val parts = mp.parts.joinToString(", ") { it.name }
                "multipart: ${mp.contentType} parts=[$parts]"
            } else {
                "multipart: <missing>"
            }
        }

        MatcherCategory.FORM -> {
            val f = spec.formSpecs.getOrNull(descriptor.index)
            if (f != null) {
                val parts = f.parts.joinToString(", ") { it.name }
                "form: ${f.encoding} parts=[$parts]"
            } else {
                "form: <missing>"
            }
        }

        MatcherCategory.BYTES -> {
            val bb = spec.byteBodySpecs.getOrNull(descriptor.index)
            if (bb != null) {
                "bytes: contentType=${
                    bb.contentTypeMatcher?.let { matcherLabel(it, "content-type") } ?: "any"
                }"
            } else {
                "bytes: <missing>"
            }
        }

        MatcherCategory.COOKIES -> {
            val matcherStr = spec.cookies.getOrNull(descriptor.index)?.toString()
            if (matcherStr == null) {
                "cookies: <missing>"
            } else {
                val cookieName = matcherStr.removePrefix("cookie('").removeSuffix("')")
                val actualValue = request.cookies[cookieName]
                if (actualValue != null) {
                    "cookies: expected $matcherStr but was '$actualValue'"
                } else {
                    "cookies: expected $matcherStr but cookie was not present"
                }
            }
        }

        MatcherCategory.QUERY_PARAMS -> {
            val matcherStr = spec.queryParameters.getOrNull(descriptor.index)?.toString()
            if (matcherStr == null) {
                "queryParams: <missing>"
            } else {
                val paramName = matcherStr.removePrefix("queryParam('").removeSuffix("')")
                val actualValue = request.queryParameters[paramName]
                if (actualValue != null) {
                    "queryParams: expected $matcherStr but was '$actualValue'"
                } else {
                    "queryParams: expected $matcherStr but parameter was not present"
                }
            }
        }

        else -> {
            descriptor.category.toString()
        }
    }
