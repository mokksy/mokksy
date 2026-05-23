package dev.mokksy.mokksy.request

import dev.mokksy.mokksy.ExperimentalMokksyApi
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.contain
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
    fun `form field with predicate creates spec`() {
        val sut = FormSpecBuilder()

        sut.field("locale") { it?.startsWith("en") == true }

        val specs = sut.build()
        assertSoftly {
            specs shouldHaveSize 1
            specs[0].name shouldBe "locale"
            specs[0].kind shouldBe BodyPartKind.FIELD
            specs[0].contentMatchers[0].matches("en-US".encodeToByteArray()) shouldBe true
            specs[0].contentMatchers[0].matches("fr".encodeToByteArray()) shouldBe false
        }
    }

    @Test
    fun `form field with Kotest matcher creates spec`() {
        val sut = FormSpecBuilder()

        sut.field("locale", contain("en"))

        val specs = sut.build()
        assertSoftly {
            specs shouldHaveSize 1
            specs[0].name shouldBe "locale"
            specs[0].contentMatchers[0].matches("en-US".encodeToByteArray()) shouldBe true
            specs[0].contentMatchers[0].matches("fr".encodeToByteArray()) shouldBe false
        }
    }

    @Test
    fun `form with URL_ENCODED encoding configures encoding`() {
        val sut = BodySpecBuilder<String>()

        sut.form(FormEncoding.URL_ENCODED) {
            field("key", "value")
        }

        val result = sut.build()
        result.formSpecs.single().encoding shouldBe FormEncoding.URL_ENCODED
    }

    @Test
    fun `form with MULTIPART encoding configures encoding`() {
        val sut = BodySpecBuilder<String>()

        sut.form(FormEncoding.MULTIPART) {
            field("key", "value")
        }

        val result = sut.build()
        result.formSpecs.single().encoding shouldBe FormEncoding.MULTIPART
    }

    @Test
    fun `form file with filename predicate creates spec`() {
        val sut = FormSpecBuilder()

        sut.file("avatar") {
            filename { it?.endsWith(".png") == true }
        }

        val specs = sut.build()
        assertSoftly {
            specs shouldHaveSize 1
            specs[0].name shouldBe "avatar"
            specs[0].kind shouldBe BodyPartKind.FILE
            specs[0].filenameMatcher!!.test("photo.png").passed() shouldBe true
            specs[0].filenameMatcher!!.test("photo.jpg").passed() shouldBe false
        }
    }

    @Test
    fun `form file with filename Kotest matcher creates spec`() {
        val sut = FormSpecBuilder()

        sut.file("avatar") {
            filename(contain("photo"))
        }

        val specs = sut.build()
        assertSoftly {
            specs shouldHaveSize 1
            specs[0].name shouldBe "avatar"
            specs[0].filenameMatcher!!.test("photo.png").passed() shouldBe true
            specs[0].filenameMatcher!!.test("image.jpg").passed() shouldBe false
        }
    }

    @Test
    fun `form file with bytes content matcher creates spec`() {
        val sut = FormSpecBuilder()

        sut.file("data") {
            bytes("payload".encodeToByteArray())
        }

        val specs = sut.build()
        assertSoftly {
            specs shouldHaveSize 1
            specs[0].contentMatchers shouldHaveSize 1
            specs[0].contentMatchers[0].matches("payload".encodeToByteArray()) shouldBe true
            specs[0].contentMatchers[0].matches("other".encodeToByteArray()) shouldBe false
        }
    }

    @Test
    fun `form file with bytes predicate creates spec`() {
        val sut = FormSpecBuilder()

        sut.file("data") {
            bytes { it?.isNotEmpty() == true }
        }

        val specs = sut.build()
        assertSoftly {
            specs shouldHaveSize 1
            specs[0].contentMatchers shouldHaveSize 1
            specs[0].contentMatchers[0].matches("data".encodeToByteArray()) shouldBe true
            specs[0].contentMatchers[0].matches(ByteArray(0)) shouldBe false
        }
    }

    @Test
    fun `multipart with string content type parses correctly`() {
        val sut = BodySpecBuilder<String>()

        sut.multipart("multipart/mixed") {
            part("p1") { text("v1") }
        }

        val result = sut.build()
        assertSoftly {
            result.multipartSpecs shouldHaveSize 1
            result.multipartSpecs[0].contentType shouldBe ContentType.MultiPart.Mixed
        }
    }

    @Test
    fun `multipart with boundary predicate creates spec`() {
        val sut = MultipartSpecBuilder(ContentType.MultiPart.Mixed)

        sut.boundary { it?.startsWith("BOUNDARY") == true }

        val spec = sut.build()
        spec.boundaryMatcher shouldNotBeNull {
            test("BOUNDARY_42").passed() shouldBe true
            test("OTHER").passed() shouldBe false
            test(null).passed() shouldBe false
        }
    }

    @Test
    fun `multipart part with Kotest text matcher creates spec`() {
        val sut = MultipartSpecBuilder(ContentType.MultiPart.Mixed)

        sut.part("note") { text(contain("hello")) }

        val spec = sut.build()
        assertSoftly(spec.parts.single()) {
            name shouldBe "note"
            contentMatchers shouldHaveSize 1
        }
    }

    @Test
    fun `DataPartSpecBuilder builds part spec`() {
        val sut = DataPartSpecBuilder("item")

        sut.contentType("text/plain")
        sut.text("expected")

        val spec = sut.build()
        assertSoftly {
            spec.name shouldBe "item"
            spec.kind shouldBe BodyPartKind.PART
            spec.contentTypeMatcher shouldNotBe null
            spec.contentMatchers shouldHaveSize 1
        }
    }

    @Test
    fun `FilePartSpecBuilder builds file spec`() {
        val sut = FilePartSpecBuilder("upload")

        sut.filename("data.csv")
        sut.contentType("text/csv")
        sut.text("header,value")

        val spec = sut.build()
        assertSoftly(spec) {
            name shouldBe "upload"
            kind shouldBe BodyPartKind.FILE
            filenameMatcher shouldNotBe null
            contentTypeMatcher shouldNotBe null
            contentMatchers shouldHaveSize 1
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

        assertSoftly(result) {
            multipartSpecs shouldHaveSize 1
            multipartSpecs[0].boundaryMatcher shouldNotBe null
            multipartSpecs[0]
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
    fun `bytes predicate adds byte array content matcher`() {
        val sut = BodySpecBuilder<String>()

        sut.bytes { it?.isNotEmpty() == true }

        val result = sut.build()
        assertSoftly {
            result.byteBodySpecs shouldHaveSize 1
            result.byteBodySpecs[0].contentMatchers shouldHaveSize 1
            result.byteBodySpecs[0].contentMatchers[0].matches("data".encodeToByteArray()) shouldBe
                true
            result.byteBodySpecs[0].contentMatchers[0].matches(ByteArray(0)) shouldBe false
        }
    }

    @Test
    fun `text adds string content matcher`() {
        val sut = BodySpecBuilder<String>()

        sut.text("hello")

        val result = sut.build()
        assertSoftly {
            result.byteBodySpecs shouldHaveSize 1
            result.byteBodySpecs[0].contentMatchers shouldHaveSize 1
            result.byteBodySpecs[0].contentMatchers[0].matches("hello".encodeToByteArray()) shouldBe
                true
            result.byteBodySpecs[0].contentMatchers[0].matches("world".encodeToByteArray()) shouldBe
                false
        }
    }

    @Test
    fun `text predicate adds string content matcher`() {
        val sut = BodySpecBuilder<String>()

        sut.text { it?.startsWith("ok") == true }

        val result = sut.build()
        assertSoftly {
            result.byteBodySpecs shouldHaveSize 1
            result.byteBodySpecs[0].contentMatchers shouldHaveSize 1
            result.byteBodySpecs[0].contentMatchers[0].matches("ok!".encodeToByteArray()) shouldBe
                true
            result.byteBodySpecs[0].contentMatchers[0].matches("no".encodeToByteArray()) shouldBe
                false
        }
    }

    @Test
    fun `contentType with ContentType object sets contentTypeMatcher`() {
        val sut = BodySpecBuilder<String>()

        sut.contentType(ContentType.Application.Json)

        val result = sut.build()
        assertSoftly {
            result.byteBodySpecs
                .single()
                .contentTypeMatcher!!
                .test(
                    ContentType.Application.Json,
                ).passed() shouldBe
                true
            result.byteBodySpecs
                .single()
                .contentTypeMatcher!!
                .test(
                    ContentType.Text.Plain,
                ).passed() shouldBe
                false
        }
    }

    @Test
    fun `predicate without description adds predicate matcher`() {
        val sut = BodySpecBuilder<String>()

        sut.predicate { it?.isNotEmpty() == true }

        val result = sut.build()
        assertSoftly {
            result.predicateMatchers shouldHaveSize 1
            result.predicateMatchers[0].test("hello").passed() shouldBe true
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

    @Test
    fun `empty builder produces empty result`() {
        val sut = BodySpecBuilder<String>()

        val result = sut.build()

        assertSoftly {
            result.formSpecs shouldHaveSize 0
            result.multipartSpecs shouldHaveSize 0
            result.byteBodySpecs shouldHaveSize 0
            result.predicateMatchers shouldHaveSize 0
        }
    }
}
