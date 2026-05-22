package dev.mokksy.mokksy

import dev.mokksy.mokksy.request.BodyPartKind
import dev.mokksy.mokksy.request.FormEncoding
import dev.mokksy.mokksy.request.RequestSpecificationBuilder
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlin.test.Test

class JavaBodySpecBuildersTest {
    private val delegate = RequestSpecificationBuilder(String::class)

    @Test
    fun `form configures form specs and returns this`() {
        val sut = JavaBodySpecBuilder(delegate)

        val result =
            sut.form { form ->
                form.field("name", "test")
            }
        sut.applyToDelegate()

        assertSoftly {
            result shouldBeSameInstanceAs sut
            delegate.build().formSpecs shouldHaveSize 1
        }
    }

    @Test
    fun `form with explicit encoding configures encoding`() {
        val sut = JavaBodySpecBuilder(delegate)

        sut.form(FormEncoding.URL_ENCODED) { form -> form.field("name", "test") }
        sut.applyToDelegate()

        delegate.build().formSpecs.single().encoding shouldBe FormEncoding.URL_ENCODED
    }

    @Test
    fun `multipart configures data part`() {
        val sut = JavaBodySpecBuilder(delegate)

        sut.multipart("multipart/mixed") { multipart ->
            multipart
                .boundary("WebAppBoundary")
                .part("metadata") { part -> part.textMatches { it?.contains("ok") == true } }
        }
        sut.applyToDelegate()

        assertSoftly(delegate.build().multipartSpecs.single()) {
            boundaryMatcher shouldNotBe null
            parts.single().kind shouldBe BodyPartKind.PART
        }
    }

    @Test
    fun `bytes and content type configure raw data body`() {
        val sut = JavaBodySpecBuilder(delegate)

        sut.bytes("payload".encodeToByteArray()).contentType("application/octet-stream")
        sut.applyToDelegate()

        assertSoftly(delegate.build().byteBodySpecs.single()) {
            contentTypeMatcher shouldNotBe null
            contentMatchers shouldHaveSize 1
        }
    }

    @Test
    fun `file configures filename and text matchers`() {
        val sut = JavaFormSpecBuilder()

        val result =
            sut.file("avatar") { file ->
                file.filename("photo.jpg").contentType("image/jpeg").text("payload")
            }

        assertSoftly {
            result shouldBeSameInstanceAs sut
            sut.build().single().filenameMatcher shouldNotBe null
            sut.build().single().contentMatchers shouldHaveSize 1
        }
    }
}
