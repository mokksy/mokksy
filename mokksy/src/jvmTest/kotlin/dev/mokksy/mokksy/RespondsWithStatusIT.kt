package dev.mokksy.mokksy

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

internal class RespondsWithStatusIT : AbstractIT() {
    @ParameterizedTest
    @MethodSource("statusCodes")
    suspend fun `respondsWithStatus returns the given status and empty body`(
        status: HttpStatusCode,
    ) {
        mokksy.get { path("/status-$seed") } respondsWithStatus status

        val result = client.get("/status-$seed")

        assertSoftly(result) {
            this.status shouldBe status
            bodyAsText() shouldBe ""
        }
    }

    @AfterEach
    fun afterEach() {
        mokksy.verifyNoUnexpectedRequests()
    }

    companion object {
        @JvmStatic
        fun statusCodes() =
            listOf(
                HttpStatusCode.OK,
                HttpStatusCode.Created,
                HttpStatusCode.NoContent,
                HttpStatusCode.NotFound,
                HttpStatusCode.InternalServerError,
            )
    }
}
