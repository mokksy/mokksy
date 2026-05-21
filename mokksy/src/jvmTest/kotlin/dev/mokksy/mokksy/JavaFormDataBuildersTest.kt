@file:OptIn(ExperimentalMokksyApi::class)

package dev.mokksy.mokksy

import dev.mokksy.mokksy.request.RequestSpecificationBuilder
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlin.test.Test

class JavaFormDataBuildersTest {
    private val delegate = RequestSpecificationBuilder(String::class)

    // region JavaBodySpecBuilder

    @Test
    fun `formData configures form-data specs and returns this`() {
        val sut = JavaBodySpecBuilder(delegate)
        val result =
            sut.formData { fd ->
                fd.field("name", "test")
            }
        assertSoftly {
            result shouldBe sut
            delegate.build().formDataPartSpecs shouldHaveSize 1
        }
    }

    @Test
    fun `predicate adds body matcher and returns this`() {
        val sut = JavaBodySpecBuilder(delegate)
        val result = sut.predicate { it.isNotEmpty() }
        assertSoftly {
            result shouldBe sut
            delegate.build().body shouldHaveSize 1
        }
    }

    @Test
    fun `predicate null body does not match`() {
        val sut = JavaBodySpecBuilder(delegate)
        sut.predicate { it.isNotEmpty() }
        val matcher = delegate.build().body.single()
        matcher.test(null).passed() shouldBe false
    }

    // endregion

    // region JavaFormDataSpecBuilder

    @Test
    fun `field creates spec with exact string matcher`() {
        val sut = JavaFormDataSpecBuilder()
        val result = sut.field("locale", "en")
        val specs = sut.build()
        assertSoftly {
            result shouldBeSameInstanceAs sut
            specs shouldHaveSize 1
            specs[0].name shouldBe "locale"
            specs[0].bodyMatchers shouldHaveSize 1
        }
    }

    @Test
    fun `fieldMatches creates spec with predicate matcher`() {
        val sut = JavaFormDataSpecBuilder()
        val result = sut.fieldMatches("locale") { v -> v != null && v.startsWith("en") }
        val specs = sut.build()
        assertSoftly {
            result shouldBeSameInstanceAs sut
            specs shouldHaveSize 1
            specs[0].name shouldBe "locale"
            specs[0].bodyMatchers shouldHaveSize 1
            specs[0].bodyMatchers[0].test("en-US").passed() shouldBe true
            specs[0].bodyMatchers[0].test("fr").passed() shouldBe false
        }
    }

    @Test
    fun `file configures file spec and adds to parts`() {
        val sut = JavaFormDataSpecBuilder()
        val result =
            sut.file("avatar") { f ->
                f.filename("photo.jpg")
            }
        val specs = sut.build()
        assertSoftly {
            result shouldBeSameInstanceAs sut
            specs shouldHaveSize 1
            specs[0].name shouldBe "avatar"
            specs[0].filenameMatcher shouldNotBe null
        }
    }

    // endregion

    // region JavaFormDataFileSpecBuilder

    @Test
    fun `filename sets filename matcher and returns this`() {
        val sut = JavaFormDataFileSpecBuilder("upload")
        val result = sut.filename("data.txt")
        val spec = sut.build()
        assertSoftly {
            result shouldBeSameInstanceAs sut
            spec.filenameMatcher shouldNotBe null
        }
    }

    @Test
    fun `contentType sets content type matcher and returns this`() {
        val sut = JavaFormDataFileSpecBuilder("upload")
        val result = sut.contentType("image/jpeg")
        val spec = sut.build()
        assertSoftly {
            result shouldBeSameInstanceAs sut
            spec.contentTypeMatcher shouldNotBe null
        }
    }

    @Test
    fun `body sets body matcher and returns this`() {
        val sut = JavaFormDataFileSpecBuilder("upload")
        val result = sut.body("hello world")
        val spec = sut.build()
        assertSoftly {
            result shouldBeSameInstanceAs sut
            spec.bodyMatchers shouldHaveSize 1
        }
    }

    @Test
    fun `bodyMatches sets predicate body matcher and returns this`() {
        val sut = JavaFormDataFileSpecBuilder("upload")
        val result = sut.bodyMatches { v -> v != null && v.contains("hello") }
        val spec = sut.build()
        assertSoftly {
            result shouldBeSameInstanceAs sut
            spec.bodyMatchers shouldHaveSize 1
            spec.bodyMatchers[0].test("hello world").passed() shouldBe true
            spec.bodyMatchers[0].test("goodbye").passed() shouldBe false
        }
    }

    // endregion
}
