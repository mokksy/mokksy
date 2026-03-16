package dev.mokksy.mokksy.utils

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class Base64UrlsTest {

    // region ByteArray

    @Test
    fun `asBase64DataUrl produces correct data URL format`() {
        "hello".toByteArray().asBase64DataUrl("text/plain") shouldStartWith "data:text/plain;base64,"
    }

    @Test
    fun `asBase64DataUrl with empty bytes produces valid data URL`() {
        ByteArray(0).asBase64DataUrl("application/octet-stream") shouldBe "data:application/octet-stream;base64,"
    }

    // endregion

    // region File

    @Test
    fun `File asBase64DataUrl reads file bytes and encodes as data URL`(@TempDir dir: Path) {
        val file = File(dir.toFile(), "test.txt").also { it.writeText("test content") }
        val result = file.asBase64DataUrl("text/plain")
        result shouldBe "test content".toByteArray().asBase64DataUrl("text/plain")
    }

    @Test
    fun `File asBase64DataUrl infers mime type from extension`(@TempDir dir: Path) {
        val file = File(dir.toFile(), "image.png").also { it.writeBytes(ByteArray(4)) }
        file.asBase64DataUrl() shouldStartWith "data:image/png;base64,"
    }

    // endregion

    // region Path

    @Test
    fun `Path asBase64DataUrl reads file bytes and encodes as data URL`(@TempDir dir: Path) {
        val path = dir.resolve("test.txt").also { it.toFile().writeText("path content") }
        val result = path.asBase64DataUrl("text/plain")
        result shouldBe "path content".toByteArray().asBase64DataUrl("text/plain")
    }

    @Test
    fun `Path asBase64DataUrl infers mime type from extension`(@TempDir dir: Path) {
        val path = dir.resolve("data.json").also { it.toFile().writeText("{}") }
        path.asBase64DataUrl() shouldStartWith "data:application/json;base64,"
    }

    // endregion
}
