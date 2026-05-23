@file:OptIn(ExperimentalMokksyApi::class)
@file:Suppress("TooManyFunctions", "TooGenericExceptionCaught")

package dev.mokksy.mokksy.request

import dev.mokksy.mokksy.ExperimentalMokksyApi
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.log
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.contentType
import io.ktor.server.request.receiveChannel
import io.ktor.server.request.receiveMultipart
import io.ktor.server.request.receiveParameters
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.CancellationException
import kotlinx.io.Buffer
import kotlinx.io.readByteArray

private const val MAX_PART_SIZE: Long = 65535

internal suspend fun scoreFormMatchers(
    request: ApplicationRequest,
    formSpecs: List<FormBodySpec>,
): Pair<Int, List<String>> {
    var score = 0
    val failed = mutableListOf<String>()

    formSpecs.forEachIndexed { index, spec ->
        val (partScore, partFailed) = scoreFormMatcher(request, spec, "form[$index]")
        score += partScore
        failed += partFailed
    }

    return score to failed
}

private suspend fun scoreFormMatcher(
    request: ApplicationRequest,
    spec: FormBodySpec,
    label: String,
): Pair<Int, List<String>> {
    val contentType = request.contentType()
    return when {
        spec.encoding != FormEncoding.URL_ENCODED &&
            contentType.match(ContentType.MultiPart.FormData) -> {
            scoreMultipartParts(request, spec.parts, label)
        }

        spec.encoding != FormEncoding.MULTIPART &&
            contentType.match(ContentType.Application.FormUrlEncoded) -> {
            scoreUrlEncodedParts(request, spec.parts, label)
        }

        else -> {
            0 to spec.parts.indices.map { "$label[$it]" }
        }
    }
}

internal suspend fun scoreMultipartMatchers(
    request: ApplicationRequest,
    multipartSpecs: List<MultipartBodySpec>,
): Pair<Int, List<String>> {
    var score = 0
    val failed = mutableListOf<String>()

    multipartSpecs.forEachIndexed { index, spec ->
        val label = "multipart[$index]"
        val contentType = request.contentType()
        val boundaryPassed =
            spec.boundaryMatcher
                ?.test(contentType.parameter("boundary"))
                ?.passed() != false
        if (!contentType.match(spec.contentType) || !boundaryPassed) {
            failed += spec.parts.indices.map { "$label[$it]" }
        } else {
            val (partScore, partFailed) = scoreMultipartParts(request, spec.parts, label)
            score += partScore
            failed += partFailed
        }
    }

    return score to failed
}

internal suspend fun scoreByteBodyMatchers(
    request: ApplicationRequest,
    byteBodySpecs: List<ByteBodySpec>,
): Pair<Int, List<String>> {
    if (byteBodySpecs.isEmpty()) return 0 to emptyList()

    val body =
        try {
            request.call
                .receiveChannel()
                .readRemaining()
                .readByteArray()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            request.call.application.log
                .trace("Unable to read raw body for matching: ${e.message}")
            null
        }

    var score = 0
    val failed = mutableListOf<String>()
    val contentType =
        try {
            request.headers[HttpHeaders.ContentType]?.let {
                ContentType.parse(
                    it,
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }

    byteBodySpecs.forEachIndexed { index, spec ->
        val label = "bytes[$index]"
        if (body == null) {
            failed += label
            return@forEachIndexed
        }
        if (spec.contentTypeMatcher != null &&
            !spec.contentTypeMatcher.test(contentType).passed()
        ) {
            failed += label
            return@forEachIndexed
        }
        if (spec.contentMatchers.all { it.matches(body) }) {
            score++
        } else {
            failed += label
        }
    }

    return score to failed
}

private suspend fun scoreUrlEncodedParts(
    request: ApplicationRequest,
    specs: List<BodyPartSpec>,
    label: String,
): Pair<Int, List<String>> {
    val parameters =
        try {
            request.call.receiveParameters()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            request.call.application.log
                .trace("Unable to receive form parameters: ${e.message}")
            return 0 to specs.indices.map { "$label[$it]" }
        }

    var score = 0
    val failed = mutableListOf<String>()
    specs.forEachIndexed { index, spec ->
        if (matchesUrlEncodedPart(parameters, spec)) {
            score++
        } else {
            failed += "$label[$index]"
        }
    }

    return score to failed
}

@Suppress("ReturnCount")
private suspend fun scoreMultipartParts(
    request: ApplicationRequest,
    specs: List<BodyPartSpec>,
    label: String,
): Pair<Int, List<String>> {
    val multipart =
        try {
            request.call.receiveMultipart()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            request.call.application.log
                .trace("Unable to receive multipart body for matching: ${e.message}")
            return 0 to specs.indices.map { "$label[$it]" }
        }

    val parts = mutableListOf<Pair<PartData, ByteArray?>>()
    try {
        multipart.forEachPart { part ->
            val content = readPartBytes(part)
            parts.add(part to content)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        request.call.application.log
            .trace("Unable to read multipart parts: ${e.message}")
        return 0 to specs.indices.map { "$label[$it]" }
    }

    var score = 0
    val failed = mutableListOf<String>()
    try {
        specs.forEachIndexed { index, spec ->
            val matchingParts =
                parts.filter { (part, _) ->
                    val partName = part.name ?: part.contentDisposition?.parameter("name")
                    partName == spec.name
                }
            if (matchingParts.any { (part, content) -> matchMultipartPart(part, spec, content) }) {
                score++
            } else {
                failed += "$label[$index]"
            }
        }
    } finally {
        parts.forEach { (part, _) -> part.dispose() }
    }

    return score to failed
}

@Suppress("ReturnCount")
private suspend fun matchMultipartPart(
    part: PartData,
    spec: BodyPartSpec,
    content: ByteArray? = null,
): Boolean {
    if (!matchesPartKind(part, spec.kind)) return false
    if (!matchesFilename(part, spec)) return false
    if (!matchesContentType(part, spec)) return false
    if (!matchesContent(part, spec, content)) return false
    return true
}

private fun matchesFilename(
    part: PartData,
    spec: BodyPartSpec,
): Boolean {
    if (spec.filenameMatcher == null) return true
    val filename =
        when (part) {
            is PartData.FileItem -> part.originalFileName
            else -> part.contentDisposition?.parameter("filename")
        }
    return spec.filenameMatcher.test(filename).passed()
}

private fun matchesContentType(
    part: PartData,
    spec: BodyPartSpec,
): Boolean {
    if (spec.contentTypeMatcher == null) return true
    return spec.contentTypeMatcher.test(part.contentType).passed()
}

@Suppress("ReturnCount")
private suspend fun matchesContent(
    part: PartData,
    spec: BodyPartSpec,
    content: ByteArray? = null,
): Boolean {
    if (spec.contentMatchers.isEmpty()) return true
    val bytes = content ?: readPartBytes(part) ?: return false
    return spec.contentMatchers.all { it.matches(bytes) }
}

private fun matchesPartKind(
    part: PartData,
    kind: BodyPartKind,
): Boolean =
    when (kind) {
        BodyPartKind.FIELD -> part is PartData.FormItem
        BodyPartKind.FILE -> part !is PartData.FormItem
        BodyPartKind.PART -> true
    }

private suspend fun readPartBytes(part: PartData): ByteArray? =
    when (part) {
        is PartData.FormItem -> {
            part.value.encodeToByteArray()
        }

        is PartData.FileItem -> {
            part
                .provider()
                .readRemaining()
                .readByteArray()
        }

        is PartData.BinaryItem -> {
            val buffer = Buffer()
            part.provider().readAtMostTo(buffer, MAX_PART_SIZE)
            buffer.readByteArray()
        }

        is PartData.BinaryChannelItem -> {
            part
                .provider()
                .readRemaining()
                .readByteArray()
        }
    }
