package dev.mokksy.mokksy

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.server.application.log
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal open class AbstractIT(
    private val clientSupplier: (Int) -> HttpClient = {
        createKtorClient(it)
    },
) {
    val mokksy: Mokksy =
        Mokksy(verbose = true) {
            it.log.info("Running Mokksy server with ${it.engine} engine")
        }

    protected lateinit var client: HttpClient

    protected val logger = KotlinLogging.logger(name = this::class.simpleName!!)

    /**
     * Represents a seed value is used for random number generation in tests.
     * Initialized to `-1` by default, it is updated before each test execution to a random value.
     * This ensures variability and uniqueness for random-based operations during every test run.
     */
    protected var seed: Int = -1

    @BeforeAll
    suspend fun initServerAndClent() {
        mokksy.startSuspend()
        client = clientSupplier(mokksy.port())
    }

    @BeforeEach
    fun beforeEach() {
        seed = Random.nextInt(42, 100500)
    }

    @AfterAll
    suspend fun closeServerAndClient() {
        mokksy.startSuspend()
        client.close()
    }
}
