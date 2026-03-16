package dev.mokksy.it

import dev.mokksy.mokksy.MokksyServer
import dev.mokksy.mokksy.mokksy
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BasicAuthCredentials
import io.ktor.client.plugins.auth.providers.basic
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.basic
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.doublereceive.DoubleReceive
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE
import io.ktor.server.testing.testApplication
import kotlin.test.Test

class MokksyKtorExtensionsIT {
    // region Application.mokksy

    @Test
    fun `Application - GET returns stubbed response`() {
        val server = MokksyServer()
        server.get { path("/hello") } respondsWith { body = "Hello, World!" }

        testApplication {
            application { mokksy(server) }

            val response = client.get("/hello")
            assertSoftly(response) {
                status shouldBe HttpStatusCode.OK
                bodyAsText() shouldBe "Hello, World!"
            }
        }
    }

    @Test
    fun `Application - returns 404 when no stub matches`() {
        val server = MokksyServer()

        testApplication {
            application { mokksy(server) }

            client.get("/no-stub").status shouldBe HttpStatusCode.NotFound
        }
    }

    @Test
    fun `Application - POST with body matching returns stubbed response`() {
        val server = MokksyServer()
        server.post {
            path("/items")
            bodyContains("widget")
        } respondsWith {
            body = """{"id":"1"}"""
            httpStatus = HttpStatusCode.Created
        }

        testApplication {
            application { mokksy(server) }

            val response = client.post("/items") { setBody("""{"name":"widget"}""") }
            assertSoftly(response) {
                status shouldBe HttpStatusCode.Created
                bodyAsText() shouldBe """{"id":"1"}"""
            }
        }
    }

    @Test
    fun `Application - multiple stubs are matched independently`() {
        val server = MokksyServer()
        server.get { path("/a") } respondsWith { body = "A" }
        server.get { path("/b") } respondsWith { body = "B" }

        testApplication {
            application { mokksy(server) }

            client.get("/a").bodyAsText() shouldBe "A"
            client.get("/b").bodyAsText() shouldBe "B"
        }
    }

    // endregion

    // region Route.mokksy

    @Test
    fun `Route - Mokksy stubs coexist with custom routes`() {
        val server = MokksyServer()
        server.get { path("/stub") } respondsWith { body = "stubbed" }

        testApplication {
            application {
                install(SSE)
                install(DoubleReceive)
                install(ContentNegotiation) { json() }
                routing {
                    get("/health") { call.respondText("OK") }
                    mokksy(server)
                }
            }

            client.get("/health").bodyAsText() shouldBe "OK"
            client.get("/stub").bodyAsText() shouldBe "stubbed"
        }
    }

    @Test
    fun `Route - returns 404 for unmatched request within scope`() {
        val server = MokksyServer()

        testApplication {
            application {
                install(SSE)
                install(DoubleReceive)
                install(ContentNegotiation) { json() }
                routing {
                    mokksy(server)
                }
            }

            client.get("/missing").status shouldBe HttpStatusCode.NotFound
        }
    }

    @Test
    fun `Route - stubs are accessible only to authenticated requests`() {
        val server = MokksyServer()
        server.get { path("/secret") } respondsWith { body = "top-secret" }

        testApplication {
            application {
                install(SSE)
                install(DoubleReceive)
                install(ContentNegotiation) { json() }
                install(Authentication) {
                    basic("auth-basic") {
                        validate { credentials ->
                            if (credentials.name == "user" && credentials.password == "pass") {
                                UserIdPrincipal(credentials.name)
                            } else {
                                null
                            }
                        }
                    }
                }
                routing {
                    authenticate("auth-basic") {
                        mokksy(server)
                    }
                }
            }

            val authedClient =
                createClient {
                    install(Auth) {
                        basic {
                            credentials { BasicAuthCredentials("user", "pass") }
                            sendWithoutRequest { true }
                        }
                    }
                }

            authedClient.get("/secret").bodyAsText() shouldBe "top-secret"
            client.get("/secret").status shouldBe HttpStatusCode.Unauthorized
        }
    }

    // endregion
}
