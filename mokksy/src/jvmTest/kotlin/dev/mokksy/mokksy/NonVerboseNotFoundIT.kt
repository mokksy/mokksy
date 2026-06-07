package dev.mokksy.mokksy

import io.kotest.matchers.shouldBe
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import org.junit.jupiter.api.Test

internal class NonVerboseNotFoundIT : AbstractIT(verbose = false) {

    @Test
    suspend fun `returns plain text 404 when verbose is false`() {
        val path = "/non-verbose-$seed"

        mokksy.post {
            path("/other-$seed")
            bodyContains("never-match")
        } respondsWith { body = "ok" }

        val result = client.post(path) {
            contentType(ContentType.Application.Json)
            setBody("""{"key": "value"}""")
        }

        result.status shouldBe HttpStatusCode.NotFound
        val body = result.bodyAsText()
        body.startsWith("No matched mapping for request") shouldBe true
        (body.first() != '{') shouldBe true
    }
}
