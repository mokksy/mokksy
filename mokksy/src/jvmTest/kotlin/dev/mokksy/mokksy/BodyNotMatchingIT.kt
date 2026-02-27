package dev.mokksy.mokksy

import io.kotest.matchers.shouldBe
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
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
    suspend fun `should fail when !bodyContains`() {
        // given
        mokksy
            .post(name = "predicate", Input::class) {
                path("/predicate")
                bodyContains(
                    Json.encodeToString(Input("wrong")),
                )
            }.respondsWith(Output::class) {
                body = expectedResponse
                httpStatus = HttpStatusCode.Created
                headers += "Foo" to "bar" // list style
            }
        // when
        val result =
            client.post("/predicate") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(input))
            }

        // then
        result.status shouldBe HttpStatusCode.NotFound
        mokksy.resetMatchCounts()
    }
}
