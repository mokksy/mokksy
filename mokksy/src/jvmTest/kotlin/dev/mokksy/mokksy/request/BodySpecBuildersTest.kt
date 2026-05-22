package dev.mokksy.mokksy.request

import dev.mokksy.mokksy.ExperimentalMokksyApi
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.http.ContentType
import kotlin.test.Test

@OptIn(ExperimentalMokksyApi::class)
class BodySpecBuildersTest {
    @Test
    fun `form adds exact field and file specs`() {
        val sut = BodySpecBuilder<String>()

        sut.form {
            field("locale", "en")
            file("avatar") {
                filename("photo.jpg")
                contentType("image/jpeg")
                text("payload")
            }
        }

        val result = sut.build()

        assertSoftly {
            result.formSpecs shouldHaveSize 1
            result.formSpecs[0].parts shouldHaveSize 2
            result.formSpecs[0].parts[0].kind shouldBe BodyPartKind.FIELD
            result.formSpecs[0].parts[1].kind shouldBe BodyPartKind.FILE
            result.formSpecs[0].parts[1].filenameMatcher shouldNotBe null
        }
    }

    @Test
    fun `multipart adds boundary and data part specs`() {
        val sut = BodySpecBuilder<String>()

        sut.multipart(ContentType.MultiPart.Mixed) {
            boundary("WebAppBoundary")
            part("metadata") {
                contentType("application/json")
                text { it?.contains("ok") == true }
            }
        }

        val result = sut.build()

        assertSoftly {
            result.multipartSpecs shouldHaveSize 1
            result.multipartSpecs[0].boundaryMatcher shouldNotBe null
            result.multipartSpecs[0]
                .parts
                .single()
                .kind shouldBe BodyPartKind.PART
        }
    }

    @Test
    fun `bytes and content type create raw byte body spec`() {
        val sut = BodySpecBuilder<String>()

        sut.bytes("payload".encodeToByteArray())
        sut.contentType("application/octet-stream")

        val result = sut.build()

        assertSoftly {
            result.byteBodySpecs shouldHaveSize 1
            assertSoftly(result.byteBodySpecs[0]) {
                contentMatchers shouldHaveSize 1
                contentTypeMatcher shouldNotBe null
            }
        }
    }

    @Test
    fun `predicate adds typed body matcher`() {
        val sut = BodySpecBuilder<String>()

        sut.predicate("non-empty") { it?.isNotEmpty() == true }

        val result = sut.build()

        assertSoftly {
            result.predicateMatchers shouldHaveSize 1
            assertSoftly(result.predicateMatchers[0]) {
                test("hello").passed() shouldBe true
                test(null).passed() shouldBe false
            }
        }
    }
}
