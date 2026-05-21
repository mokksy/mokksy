package dev.mokksy.mokksy.request

import dev.mokksy.mokksy.ExperimentalMokksyApi
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equals.beEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.contain
import io.kotest.matchers.string.startWith
import io.ktor.http.ContentType
import kotlin.test.Test

@OptIn(ExperimentalMokksyApi::class)
class FormDataBuildersTest {
    // region FormDataSpecBuilder

    @Test
    fun `field with matcher creates spec with body matcher`() {
        val sut = FormDataSpecBuilder()
        sut.field("locale", contain("en"))
        val specs = sut.build()
        assertSoftly {
            specs shouldHaveSize 1
            specs[0].name shouldBe "locale"
            specs[0].bodyMatchers shouldHaveSize 1
            specs[0].bodyMatchers[0].test("en-US").passed() shouldBe true
        }
    }

    @Test
    fun `field with DSL block creates spec with body matcher`() {
        val sut = FormDataSpecBuilder()
        sut.field("locale") {
            body(startWith("en"))
        }
        val specs = sut.build()
        assertSoftly {
            specs shouldHaveSize 1
            specs[0].name shouldBe "locale"
            specs[0].bodyMatchers shouldHaveSize 1
            specs[0].bodyMatchers[0].test("en-US").passed() shouldBe true
            specs[0].bodyMatchers[0].test("fr").passed() shouldBe false
        }
    }

    @Test
    fun `file creates spec with filename matcher`() {
        val sut = FormDataSpecBuilder()
        sut.file("avatar") {
            filename(contain("photo"))
        }
        val specs = sut.build()
        assertSoftly {
            specs shouldHaveSize 1
            specs[0].name shouldBe "avatar"
            specs[0].filenameMatcher shouldNotBe null
        }
    }

    @Test
    fun `file creates spec with content type matcher`() {
        val sut = FormDataSpecBuilder()
        sut.file("avatar") {
            contentType(beEqual(ContentType.Image.JPEG))
        }
        val specs = sut.build()
        assertSoftly {
            specs shouldHaveSize 1
            specs[0].contentTypeMatcher shouldNotBe null
        }
    }

    @Test
    fun `file creates spec with body matcher`() {
        val sut = FormDataSpecBuilder()
        sut.file("document") {
            body(contain("hello"))
        }
        val specs = sut.build()
        assertSoftly {
            specs shouldHaveSize 1
            specs[0].bodyMatchers shouldHaveSize 1
        }
    }

    // endregion

    // region FormDataFieldSpecBuilder

    @Test
    fun `FormDataFieldSpecBuilder body sets body matcher`() {
        val sut = FormDataFieldSpecBuilder("name")
        sut.body(contain("test"))
        val spec = sut.build()
        assertSoftly {
            spec.name shouldBe "name"
            spec.bodyMatchers shouldHaveSize 1
            spec.bodyMatchers[0].test("test-value").passed() shouldBe true
        }
    }

    // endregion

    // region FormDataFileSpecBuilder

    @Test
    fun `FormDataFileSpecBuilder filename sets filename matcher`() {
        val sut = FormDataFileSpecBuilder("upload")
        sut.filename(contain("data"))
        val spec = sut.build()
        assertSoftly {
            spec.name shouldBe "upload"
            spec.filenameMatcher shouldNotBe null
        }
    }

    @Test
    fun `FormDataFileSpecBuilder contentType sets content type matcher`() {
        val sut = FormDataFileSpecBuilder("upload")
        sut.contentType(beEqual(ContentType.Application.Json))
        val spec = sut.build()
        assertSoftly {
            spec.contentTypeMatcher shouldNotBe null
        }
    }

    @Test
    fun `FormDataFileSpecBuilder body sets body matcher`() {
        val sut = FormDataFileSpecBuilder("upload")
        sut.body(contain("payload"))
        val spec = sut.build()
        assertSoftly {
            spec.bodyMatchers shouldHaveSize 1
            spec.bodyMatchers[0].test("payload data").passed() shouldBe true
        }
    }

    // endregion

    // region BodySpecBuilder

    @Test
    fun `formData adds form-data part specs`() {
        val sut = BodySpecBuilder<String>()
        sut.formData {
            field("key", contain("value"))
        }
        val result = sut.build()
        assertSoftly {
            result.formDataPartSpecs shouldHaveSize 1
            result.formDataPartSpecs[0].name shouldBe "key"
        }
    }

    @Test
    fun `predicate adds predicate matcher`() {
        val sut = BodySpecBuilder<String>()
        sut.predicate("non-empty") { it?.isNotEmpty() == true }
        val result = sut.build()
        assertSoftly {
            result.predicateMatchers shouldHaveSize 1
            result.predicateMatchers[0].test("hello").passed() shouldBe true
            result.predicateMatchers[0].test(null).passed() shouldBe false
        }
    }

    @Test
    fun `predicate with null description adds predicate matcher`() {
        val sut = BodySpecBuilder<String>()
        sut.predicate { it?.isNotEmpty() == true }
        val result = sut.build()
        assertSoftly {
            result.predicateMatchers shouldHaveSize 1
        }
    }

    // endregion
}
