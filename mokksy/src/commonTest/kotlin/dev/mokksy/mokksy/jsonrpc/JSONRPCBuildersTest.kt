package dev.mokksy.mokksy.jsonrpc

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test

class JSONRPCBuildersTest {
    // region JSONRPCErrorBuilder

    @Test
    fun `should build error with all fields`() {
        val error =
            jsonRPCError {
                code = -32600
                message = "Invalid Request"
                data = "missing method"
            }

        assertSoftly(error) {
            code shouldBe -32600
            message shouldBe "Invalid Request"
            data shouldBe "missing method"
        }
    }

    @Test
    fun `should build error without data`() {
        val error = JSONRPCError<Nothing>(code = -32601, message = "Method not found")

        assertSoftly(error) {
            code shouldBe -32601
            message shouldBe "Method not found"
            data shouldBe null
        }
    }

    @Test
    fun `should fail when code is missing`() {
        val exception =
            shouldThrow<IllegalArgumentException> {
                jsonRPCError {
                    message = "error"
                    data = "some data"
                }
            }

        exception.message shouldContain "code"
    }

    @Test
    fun `should fail when message is missing`() {
        val exception =
            shouldThrow<IllegalArgumentException> {
                jsonRPCError {
                    code = -32600
                    data = "some data"
                }
            }

        exception.message shouldContain "message"
    }

    // endregion

    // region JSONRPCResponseBuilder - success

    @Test
    fun `should build success response`() {
        val response =
            jsonRPCResponse {
                id =
                    RequestId(1)
                result = 42
            }

        response
            .shouldBeInstanceOf<JSONRPCResponse.Success<Int>>()
        assertSoftly(response) {
            id shouldBe
                RequestId(1)
            result shouldBe 42
        }
    }

    @Test
    fun `should build success response with string id`() {
        val response =
            jsonRPCResponse {
                id =
                    RequestId("abc")
                result = "hello"
            }

        response
            .shouldBeInstanceOf<JSONRPCResponse.Success<String>>()
        assertSoftly(response) {
            id shouldBe
                RequestId("abc")
            result shouldBe "hello"
        }
    }

    // endregion

    // region JSONRPCResponseBuilder - error

    @Test
    fun `should build error response with direct error assignment`() {
        val response =
            jsonRPCResponse<Nothing, Nothing> {
                id =
                    RequestId(1)
                error =
                    JSONRPCError
                        .methodNotFound()
            }

        response.shouldBeInstanceOf<JSONRPCResponse.Error<*>>()
        assertSoftly(response) {
            id shouldBe
                RequestId(1)
            error.code shouldBe -32601
        }
    }

    @Test
    fun `should build error response with nested DSL`() {
        val response =
            jsonRPCResponse<Nothing, String> {
                id =
                    RequestId(5)
                error {
                    code = -32603
                    message = "Internal error"
                    data = "stack trace"
                }
            }

        response.shouldBeInstanceOf<JSONRPCResponse.Error<*>>()
        assertSoftly(response) {
            id shouldBe
                RequestId(5)
            error.code shouldBe -32603
            error.message shouldBe "Internal error"
            error.data shouldBe "stack trace"
        }
    }

    @Test
    fun `should build error response with null id`() {
        val response =
            jsonRPCResponse<Nothing, Nothing> {
                error =
                    JSONRPCError
                        .parseError()
            }

        response.shouldBeInstanceOf<JSONRPCResponse.Error<*>>()
        assertSoftly(response) {
            id shouldBe null
            error.code shouldBe -32700
        }
    }

    // endregion

    // region JSONRPCResponseBuilder - validation

    @Test
    fun `should fail when neither result nor error is set`() {
        val exception =
            shouldThrow<IllegalArgumentException> {
                jsonRPCResponse<String> {
                    id =
                        RequestId(1)
                }
            }

        exception.message shouldContain "result"
    }

    @Test
    fun `should fail when both result and error are set`() {
        val exception =
            shouldThrow<IllegalArgumentException> {
                jsonRPCResponse {
                    id =
                        RequestId(1)
                    result = "hello"
                    error =
                        JSONRPCError
                            .internalError()
                }
            }

        exception.message shouldContain "mutually exclusive"
    }

    // endregion
}
