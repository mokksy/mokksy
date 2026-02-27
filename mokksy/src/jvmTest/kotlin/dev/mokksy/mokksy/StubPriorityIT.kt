package dev.mokksy.mokksy

import io.kotest.matchers.equals.beEqual
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Test
import kotlin.test.AfterTest

internal class StubPriorityIT : AbstractIT() {
    @Test
    suspend fun `Should consider stub priority (desc order)`() {
        val path = "/stub-priority-$seed"
        mokksy
            .get {
                this.path = beEqual(path)
                this.priority = 1 // higher priority
            }.respondsWith(String::class) {
                body = "Expected response"
            }

        mokksy
            .get {
                this.path = beEqual(path)
                this.priority = 2 // lower priority
            }.respondsWith(String::class) {
                body = "Unexpected response"
            }

        // when
        val result =
            client.get(path)
        // then
        result.status shouldBe HttpStatusCode.OK
        result.bodyAsText() shouldBe "Expected response"
    }

    @Test
    suspend fun `Should consider stub priority (asc order)`() {
        mokksy
            .get {
                this.path = beEqual(path)
                this.priority = 2 // lower priority
            }.respondsWith(String::class) {
                body = "Unexpected response"
            }

        val path = "/stub-priority-$seed"
        mokksy
            .get {
                this.path = beEqual(path)
                this.priority = 1 // higher priority
            }.respondsWith(String::class) {
                body = "Expected response"
            }

        // when
        val result =
            client.get(path)
        // then
        with(result) {
            status shouldBe HttpStatusCode.OK
            bodyAsText() shouldBe "Expected response"
        }
    }

    @AfterTest
    fun afterEach() {
        mokksy.verifyNoUnexpectedRequests()
    }
}
