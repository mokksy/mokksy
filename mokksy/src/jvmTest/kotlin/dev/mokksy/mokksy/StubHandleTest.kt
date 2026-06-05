@file:OptIn(ExperimentalMokksyApi::class)

package dev.mokksy.mokksy

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

class StubHandleTest {
    @Test
    fun `name mirrors stub configuration`() {
        StubHandle(
            createStub<String, String>(
                name = "named",
                requestType = String::class,
            ),
        ).name shouldBe "named"
        StubHandle(createStub<String, String>(requestType = String::class)).name shouldBe null
    }

    @Test
    fun `id delegates to stub id and is never null`() {
        val stub = createStub<String, String>(requestType = String::class)
        val handle = StubHandle(stub)
        handle.id shouldBe stub.id
        handle.id shouldNotBe null
    }

    @Test
    fun `id is auto-generated for every stub`() {
        val handle1 = StubHandle(createStub<String, String>(requestType = String::class))
        val handle2 = StubHandle(createStub<String, String>(requestType = String::class))
        handle1.id shouldNotBe handle2.id
    }

    @Test
    fun `matchCount delegates to stub state`() {
        val reusableStub =
            createStub<String, String>(name = "reusable", requestType = String::class)
        val onceStub =
            createStub<String, String>(
                name = "once",
                eventuallyRemove = true,
                requestType = String::class,
            )

        reusableStub.markMatched()
        reusableStub.markMatched()
        onceStub.claimMatch() shouldBe true
        onceStub.claimMatch() shouldBe false

        StubHandle(reusableStub).matchCount() shouldBe 2L
        StubHandle(onceStub).matchCount() shouldBe 1L
    }

    @Test
    fun `verification success paths return same handle`() {
        val exactStub = createMatchedHandle("exact", 2)
        val rangeStub = createMatchedHandle("range", 1)
        val neverStub = createMatchedHandle("never", 0)

        exactStub.verifyCalled(2) shouldBe exactStub
        rangeStub.verifyCalled().atLeast(1) shouldBe rangeStub
        rangeStub.verifyCalled().atMost(1) shouldBe rangeStub
        neverStub.verifyCalled().never() shouldBe neverStub
    }

    @Test
    fun `verification failures include expected counts`() {
        assertVerificationFailure(
            handle = createMatchedHandle("missing", 0),
            expectedMessage = "Stub 'missing' was called 0 time(s), expected at least 1",
        ) { verifyCalled().atLeast(1) }

        assertVerificationFailure(
            handle = createMatchedHandle("too-many", 2),
            expectedMessage = "Stub 'too-many' was called 2 time(s), expected at most 1",
        ) { verifyCalled().atMost(1) }

        assertVerificationFailure(
            handle = createMatchedHandle("wrong-count", 1),
            expectedMessage = "Stub 'wrong-count' was called 1 time(s), expected exactly 2",
        ) { verifyCalled().exactly(2) }
    }

    @Test
    fun `negative verification bounds are rejected`() {
        val handle = createMatchedHandle("negative", 0)

        shouldThrow<IllegalArgumentException> {
            handle.verifyCalled().atLeast(-1)
        }.message shouldContain "atLeast must be >= 0"

        shouldThrow<IllegalArgumentException> {
            handle.verifyCalled().atMost(-1)
        }.message shouldContain "atMost must be >= 0"

        shouldThrow<IllegalArgumentException> {
            handle.verifyCalled().exactly(-1)
        }.message shouldContain "exactly must be >= 0"
    }

    private fun createMatchedHandle(
        name: String,
        matches: Int,
    ): StubHandle {
        val stub = createStub<String, String>(name = name, requestType = String::class)
        repeat(matches) {
            stub.markMatched()
        }
        return StubHandle(stub)
    }

    private fun assertVerificationFailure(
        handle: StubHandle,
        expectedMessage: String,
        block: StubHandle.() -> Unit,
    ) {
        val error =
            shouldThrow<AssertionError> {
                handle.block()
            }

        error.message shouldContain expectedMessage
    }
}
