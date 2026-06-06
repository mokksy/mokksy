package dev.mokksy.mokksy

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.random.Random

internal class BodyNotMatchingIT : AbstractIT() {
    private lateinit var name: String
    private lateinit var id: String
    private lateinit var expectedResponse: Output
    private lateinit var input: Input

    @BeforeEach
    fun setup() {
        name = Random.nextInt().toHexString()
        id = Random.nextInt().toString()
        input = Input(name)
        expectedResponse = Output(id)
    }

    @Test
    suspend fun `should fail when not bodyContains`() {
        val path = "/predicate-$seed"
        mokksy
            .post(name = "predicate", Input::class) {
                path(path)
                bodyContains(
                    Json.encodeToString(Input("wrong")),
                )
            }.respondsWith(Output::class) {
                body = expectedResponse
                httpStatus = HttpStatusCode.Created
                headers += "Foo" to "bar"
            }
        val result =
            client.post(path) {
                contentType(ContentType.Application.Json)
                setBody(input)
            }

        result.status shouldBe HttpStatusCode.NotFound

        val unexpectedForPath = mokksy.findAllUnexpectedRequests().filter { it.uri == path }
        unexpectedForPath shouldHaveSize 1

        mokksy.findAllUnmatchedStubs().shouldNotBeEmpty()
    }

    @Test
    suspend fun `server returns 404 and does not crash when stub body matcher throws exception`() {
        val path = "/failing-matcher-$seed"

        mokksy.post {
            path(path)
            bodyMatchesPredicate("always throws") {
                throw IllegalStateException("Matcher intentionally throws")
            }
        } respondsWith {
            body = "ok"
        }

        val result =
            client.post(path) {
                contentType(ContentType.Text.Plain)
                setBody("test body")
            }

        result.status shouldBe HttpStatusCode.NotFound
    }

    @Test
    suspend fun `returns verbose diagnostic JSON when no stub matches`() {
        val path = "/diagnostic-$seed"

        mokksy.post(StubConfiguration(name = "token-stub")) {
            path(path)
            bodyContains("expected-token")
        } respondsWith { body = "ok" }

        mokksy.get(StubConfiguration(name = "auth-stub")) {
            path("/other-$seed")
            containsHeader("X-Api-Key", "secret")
        } respondsWith { body = "authorized" }

        val result =
            client.post(path) {
                contentType(ContentType.Application.Json)
                setBody("""{"token": "wrong"}""")
            }

        result.status shouldBe HttpStatusCode.NotFound

        val actualObj = Json.parseToJsonElement(result.bodyAsText()).jsonObject
        val headersJson =
            Json.encodeToString(
                JsonElement.serializer(),
                actualObj["request"]!!.jsonObject["headers"]!!,
            )

        val ourEvals =
            actualObj["closestStub"]!!
                .jsonArray
                .filter { it.jsonObject["name"]!!.jsonPrimitive.content == "token-stub" }

        val filteredResponse =
            buildJsonObject {
                put("request", actualObj["request"]!!)
                put("closestStub", buildJsonArray { ourEvals.forEach { add(it) } })
            }

        Json.encodeToString(filteredResponse) shouldEqualJson """
        {
          "request": {
            "method": "POST",
            "path": "$path",
            "body": {"token": "wrong"},
            "headers": $headersJson
          },
            "closestStub": [
            {
              "name": "token-stub",
              "configuredMatchers": ["method: POST", "path: '$path'", "bodyString: contain('expected-token')"],
              "failedMatchers": ["bodyString: expected contain('expected-token')"]
            }
          ]
        }
        """
    }

    @Test
    suspend fun `diagnostic response lists per-stub failed matchers`() {
        val path = "/failed-matchers-$seed"

        mokksy.get(StubConfiguration(name = "secured-get")) {
            path("/wrong-path")
            containsHeader("Authorization", "Bearer token")
        } respondsWith { body = "protected" }

        val result = client.get(path)

        result.status shouldBe HttpStatusCode.NotFound

        val actualObj = Json.parseToJsonElement(result.bodyAsText()).jsonObject
        val headersJson =
            Json.encodeToString(
                JsonElement.serializer(),
                actualObj["request"]!!.jsonObject["headers"]!!,
            )

        val ourEvals =
            actualObj["closestStub"]!!
                .jsonArray
                .filter { it.jsonObject["name"]!!.jsonPrimitive.content == "secured-get" }

        val filteredResponse =
            buildJsonObject {
                put("request", actualObj["request"]!!)
                put("closestStub", buildJsonArray { ourEvals.forEach { add(it) } })
            }

        Json.encodeToString(filteredResponse) shouldEqualJson """
        {
          "request": {
            "method": "GET",
            "path": "$path",
            "headers": $headersJson
          },
            "closestStub": [
            {
              "name": "secured-get",
              "configuredMatchers": ["method: GET", "path: '/wrong-path'", "headers: Authorization = Bearer token"],
              "failedMatchers": ["path: expected '/wrong-path' but was $path", "headers: expected Authorization = Bearer token but header was not present"]
            }
          ]
        }
        """
    }

    @Test
    suspend fun `diagnostic response includes request info and stub evaluations`() {
        val path = "/concrete-$seed"

        mokksy.post(StubConfiguration(name = "concrete-stub")) {
            path(path)
            bodyContains("expected")
        } respondsWith { body = "ok" }

        val result =
            client.post(path) {
                contentType(ContentType.Application.Json)
                setBody("""{"actual": "value"}""")
            }

        result.status shouldBe HttpStatusCode.NotFound

        val actualObj = Json.parseToJsonElement(result.bodyAsText()).jsonObject
        val headersJson =
            Json.encodeToString(
                actualObj["request"]!!.jsonObject["headers"]!!,
            )

        val ourEvals =
            requireNotNull(actualObj["closestStub"])
                .jsonArray
                .filter { it.jsonObject["name"]!!.jsonPrimitive.content == "concrete-stub" }

        val filteredResponse =
            buildJsonObject {
                put("request", actualObj["request"]!!)
                put("closestStub", buildJsonArray { ourEvals.forEach { add(it) } })
            }

        Json.encodeToString(filteredResponse) shouldEqualJson """
        {
          "request": {
            "method": "POST",
            "path": "$path",
            "body": {"actual": "value"},
            "headers": $headersJson
          },
            "closestStub": [
            {
              "name": "concrete-stub",
              "configuredMatchers": ["method: POST", "path: '$path'", "bodyString: contain('expected')"],
              "failedMatchers": ["bodyString: expected contain('expected')"]
            }
          ]
        }
        """
    }
}
