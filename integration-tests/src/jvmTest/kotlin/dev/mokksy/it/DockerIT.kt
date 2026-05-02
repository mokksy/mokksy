package dev.mokksy.it

import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.MountableFile
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DockerIT {
    private val dockerImage =
        "${System.getProperty("dockerImageName", "mokksy/server-jvm")}:" +
            System.getProperty("dockerImageTag", "snapshot")

    private val container =
        GenericContainer(dockerImage)
            .withImagePullPolicy { false } // never pull remote image
            .withEnv("MOKKSY_CONFIG", "/config/it-stubs.yaml")
            .withCopyFileToContainer(
                MountableFile.forClasspathResource("/it-stubs.yaml"),
                "/config/it-stubs.yaml",
            ).withExposedPorts(8080)
            .waitingFor(Wait.forLogMessage(".*Responding at.*", 1))
            .withLogConsumer { println("🐳 ${it.utf8StringWithoutLineEnding}") }
            .withStartupTimeout(10.seconds.toJavaDuration())

    lateinit var httpClient: HttpClient

    @BeforeAll
    fun beforeAll() {
        container.start()

        httpClient =
            HttpClient {
                defaultRequest {
                    host = container.host
                    port = container.firstMappedPort
                }
            }
    }

    @AfterAll
    fun afterAll() {
        httpClient.close()
        container.stop()
    }

    @Test
    suspend fun testDocker() {
        val response = httpClient.get("/ping")
        response.bodyAsText() shouldBe """{"response":"Pong"}"""
    }
}
