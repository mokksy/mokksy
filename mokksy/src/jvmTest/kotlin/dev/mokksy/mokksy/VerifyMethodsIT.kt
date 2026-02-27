package dev.mokksy.mokksy

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.string.shouldContain
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import org.junit.jupiter.api.Test
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertFailsWith

internal class VerifyMethodsIT {
    private lateinit var mokksy: Mokksy
    private lateinit var client: HttpClient

    @BeforeTest
    fun setUp() {
        mokksy = Mokksy()
        client = createKtorClient(mokksy.port())
    }

    @AfterTest
    fun tearDown() {
        mokksy.shutdown()
    }

    // region: verifyNoUnexpectedRequests

    @Test
    fun `verifyNoUnexpectedRequests - should pass when no requests were made`() {
        mokksy.verifyNoUnexpectedRequests()
    }

    @Test
    suspend fun `verifyNoUnexpectedRequests - should pass when all requests matched by stubs`() {
        mokksy
            .get {
                path("/matched")
            }.respondsWith(String::class) {
                body = "ok"
            }

        client.get("/matched")

        mokksy.verifyNoUnexpectedRequests()
    }

    @Test
    suspend fun `verifyNoUnexpectedRequests - should throw AssertionError stub not matching`() {
        val path = "/no-stub-path"
        client.get(path)

        val error =
            assertFailsWith<AssertionError> {
                mokksy.verifyNoUnexpectedRequests()
            }

        error.message shouldContain path
    }

    // endregion

    // region: verifyNoUnmatchedStubs

    @Test
    fun `verifyNoUnmatchedStubs - should pass when no stubs were registered`() {
        mokksy.verifyNoUnmatchedStubs()
    }

    @Test
    suspend fun `verifyNoUnmatchedStubs - should pass when stub was matched at least once`() {
        mokksy
            .get {
                path("/matched-stub")
            }.respondsWith(String::class) {
                body = "ok"
            }

        client.get("/matched-stub")

        mokksy.verifyNoUnmatchedStubs()
        mokksy.findAllUnmatchedStubs().shouldBeEmpty()
    }

    @Test
    fun `verifyNoUnmatchedStubs - error message should include stub path`() {
        val path = "/never-called-named"
        mokksy
            .get {
                path(path)
            }.respondsWith(String::class) {
                body = "ok"
            }

        val error =
            assertFailsWith<AssertionError> {
                mokksy.verifyNoUnmatchedStubs()
            }

        error.message shouldContain path
    }
    // endregion
}
