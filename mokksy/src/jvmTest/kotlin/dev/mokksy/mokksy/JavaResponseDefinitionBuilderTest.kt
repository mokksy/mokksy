@file:OptIn(InternalMokksyApi::class)

package dev.mokksy.mokksy

import dev.mokksy.mokksy.request.CapturedRequest
import dev.mokksy.mokksy.response.ResponseDefinitionBuilder
import dev.mokksy.mokksy.utils.logger.HttpFormatter
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.withCharset
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.text.Charsets.UTF_8
import kotlin.time.Duration.Companion.milliseconds

class JavaResponseDefinitionBuilderTest {
    private val formatter = HttpFormatter()

    private fun withBuilder(
        block: (
            ResponseDefinitionBuilder<String, String>,
            JavaResponseDefinitionBuilder<String, String>,
        ) -> Unit,
    ) {
        testApplication {
            routing {
                get("/test") {
                    val delegate =
                        ResponseDefinitionBuilder<String, String>(
                            request = CapturedRequest(call.request, String::class),
                            formatter = formatter,
                        )
                    val sut = JavaResponseDefinitionBuilder(delegate)
                    block(delegate, sut)
                    call.respondText("ok")
                }
            }
            client.get("/test")
        }
    }

    // region body

    @Test
    fun `body delegates to delegate and returns this`() =
        withBuilder { delegate, sut ->
            val result = sut.body("hello")
            delegate.body shouldBe "hello"
            result shouldBe sut
        }

    // endregion

    // region status

    @Test
    fun `status delegates to delegate and returns this`() =
        withBuilder { delegate, sut ->
            val result = sut.status(201)
            delegate.httpStatus shouldBe HttpStatusCode.fromValue(201)
            result shouldBe sut
        }

    // endregion

    // region header

    @Test
    fun `header delegates name and value to delegate and returns this`() =
        withBuilder { delegate, sut ->
            val result = sut.header("X-Custom", "value")
            delegate.build().headers shouldNotBe null
            result shouldBe sut
        }

    // endregion

    // region delayMillis

    @Test
    fun `delayMillis sets delay on delegate and returns this`() =
        withBuilder { delegate, sut ->
            val result = sut.delayMillis(250)
            delegate.delay shouldBe 250.milliseconds
            result shouldBe sut
        }

    // endregion

    // region contentType

    @Test
    fun `contentType with charset sets contentType on delegate and returns this`() =
        withBuilder { delegate, sut ->
            val result = sut.contentType("application/json; charset=UTF-8")
            delegate.contentType shouldBe ContentType.Application.Json.withCharset(UTF_8)
            result shouldBe sut
        }

    @Test
    fun `contentType parses and sets contentType on delegate and returns this`() =
        withBuilder { delegate, sut ->
            val result = sut.contentType("application/json")
            delegate.contentType shouldBe ContentType.Application.Json
            result shouldBe sut
        }

    // endregion

    // region chaining

    @Test
    fun `methods can be chained fluently`() =
        withBuilder { _, sut ->
            val result =
                sut
                    .body("ok")
                    .status(200)
                    .header("X-H", "v")
                    .delayMillis(0)
                    .contentType("text/plain")
            result shouldBe sut
        }

    // endregion
}
