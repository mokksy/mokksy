package dev.mokksy.mokksy

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test

internal class NearMissIT : AbstractIT() {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    suspend fun `should return JSON diagnostic body when no stub matches`() {
        val path = "/near-miss-$seed"
        mokksy
            .post(name = "body-check-$seed", Input::class) {
                path(path)
                bodyContains("wrong-value")
            }.respondsWith(Output::class) {
                body = Output("ok")
            }

        val result =
            client.post(path) {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(Input("actual-value")))
            }

        result.status shouldBe HttpStatusCode.NotFound
        result.contentType()?.match(ContentType.Application.Json) shouldBe true

        val body = json.parseToJsonElement(result.bodyAsText()).jsonObject
        body["message"]?.jsonPrimitive?.content shouldBe "No stub matched the incoming request"
        body["request"]?.jsonObject?.get("method")?.jsonPrimitive?.content shouldBe "POST"
        body["request"]?.jsonObject?.get("path")?.jsonPrimitive?.content shouldBe path

        val nearMisses = body["nearMisses"]?.jsonArray
        nearMisses shouldNotBe null

        val stubNames = nearMisses!!.map { it.jsonObject["name"]?.jsonPrimitive?.content }
        stubNames shouldContain "body-check-$seed"
    }

    @Test
    suspend fun `should list all stubs with their match results`() {
        val path = "/multi-stub-$seed"
        mokksy
            .post(StubConfiguration(name = "stub-a-$seed")) {
                path(path)
                bodyContains("expected-a")
            }.respondsWith {
                body = "a"
            }

        mokksy
            .post(StubConfiguration(name = "stub-b-$seed")) {
                path("/different-path-$seed")
            }.respondsWith {
                body = "b"
            }

        val result =
            client.post(path) {
                contentType(ContentType.Application.Json)
                setBody("""{"data":"something"}""")
            }

        result.status shouldBe HttpStatusCode.NotFound

        val body = json.parseToJsonElement(result.bodyAsText()).jsonObject
        val nearMisses = body["nearMisses"]?.jsonArray
        nearMisses shouldNotBe null
        nearMisses!!.size shouldBeGreaterThanOrEqualTo 2

        val stubNames = nearMisses.map { it.jsonObject["name"]?.jsonPrimitive?.content }
        stubNames shouldContain "stub-a-$seed"
        stubNames shouldContain "stub-b-$seed"
    }

    @Test
    suspend fun `should return valid near miss response for unregistered path`() {
        val result =
            client.get("/no-stubs-path-$seed")

        result.status shouldBe HttpStatusCode.NotFound

        val body = json.parseToJsonElement(result.bodyAsText()).jsonObject
        body["message"]?.jsonPrimitive?.content shouldBe "No stub matched the incoming request"
        body["request"] shouldNotBe null
        body["nearMisses"]?.jsonArray shouldNotBe null
    }

    @Test
    suspend fun `should include request body in diagnostic response`() {
        val path = "/body-echo-$seed"
        val requestBody = Json.encodeToString(Input("test-value"))

        mokksy
            .post(StubConfiguration(name = "body-echo-$seed")) {
                path(path)
                bodyContains("not-matching")
            }.respondsWith {
                body = "ok"
            }

        val result =
            client.post(path) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

        result.status shouldBe HttpStatusCode.NotFound

        val body = json.parseToJsonElement(result.bodyAsText()).jsonObject
        val requestJson = body["request"]?.jsonObject
        requestJson?.get("body")?.jsonPrimitive?.content shouldContain "test-value"
    }

    @Test
    suspend fun `should show which matchers passed and which failed`() {
        val path = "/diagnostics-$seed"
        mokksy
            .post(name = "diagnostic-stub-$seed", Input::class) {
                path(path)
                bodyContains("wrong-content")
            }.respondsWith(Output::class) {
                body = Output("ok")
            }

        val result =
            client.post(path) {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(Input("actual-content")))
            }

        result.status shouldBe HttpStatusCode.NotFound

        val body = json.parseToJsonElement(result.bodyAsText()).jsonObject
        val nearMisses = body["nearMisses"]?.jsonArray
        val stub =
            nearMisses?.firstOrNull {
                it.jsonObject["name"]?.jsonPrimitive?.content == "diagnostic-stub-$seed"
            }?.jsonObject
        stub shouldNotBe null

        // method and path should pass
        val passed = stub!!["passed"]!!.jsonArray.map { it.jsonPrimitive.content }
        passed shouldContain "method"
        passed shouldContain "path"

        // bodyString should fail
        val failed = stub["failed"]?.jsonArray
        failed shouldNotBe null
        val failedMatchers = failed!!.map { it.jsonObject["matcher"]?.jsonPrimitive?.content }
        failedMatchers shouldContain "bodyString[0]"

        // reason should contain the expected value info
        val bodyStringFailure =
            failed.first {
                it.jsonObject["matcher"]?.jsonPrimitive?.content == "bodyString[0]"
            }.jsonObject
        bodyStringFailure["reason"]?.jsonPrimitive?.content shouldContain "wrong-content"
    }
}
