package com.cir0cuit.devtoys.tools.encoders

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GzipCodecTest {

    private val text = "Hello, gzip! ".repeat(20).toByteArray()

    @Test
    fun roundTripAndDetectEveryFormat() {
        for (format in ZipFormat.entries) {
            val packed = compress(text, format)
            assertEquals(format, detectZipFormat(packed))
            val (detected, unpacked) = decompress(packed)
            assertEquals(format, detected)
            assertArrayEquals(text, unpacked)
        }
    }

    @Test
    fun headersMatchTheirFormats() {
        val gz = compress(text, ZipFormat.GZIP)
        assertEquals(0x1F, gz[0].toInt() and 0xFF)
        assertEquals(0x8B, gz[1].toInt() and 0xFF)
        assertEquals(0x78, compress(text, ZipFormat.ZLIB)[0].toInt() and 0xFF)
        assertTrue(compress(text, ZipFormat.GZIP).size < text.size)
    }

    @Test
    fun knownGzipFromBase64() {
        // printf 'hello' | gzip -n | base64
        val (format, out) = decompress(parseBinaryText("H4sIAAAAAAAAA8tIzcnJBwCGphA2BQAAAA=="))
        assertEquals(ZipFormat.GZIP, format)
        assertEquals("hello", out.toString(Charsets.UTF_8))
    }

    @Test
    fun hexInputIsAccepted() {
        val hex = compress(text, ZipFormat.ZLIB).joinToString(" ") { "%02x".format(it) }
        assertArrayEquals(text, decompress(parseBinaryText(hex)).second)
    }

    @Test
    fun truncatedOrGarbageInputIsRejected() {
        val gz = compress(text, ZipFormat.GZIP)
        for (bad in listOf(gz.copyOf(gz.size / 2), byteArrayOf(1, 2, 3, 4, 5))) {
            try {
                decompress(bad)
                throw AssertionError("expected rejection")
            } catch (e: IllegalArgumentException) {
                assertTrue(e.message!!.isNotEmpty())
            }
        }
    }

    @Test
    fun outputIsCapped() {
        val bomb = compress(ByteArray(100_000), ZipFormat.GZIP)
        try {
            decompress(bomb, maxBytes = 10_000)
            throw AssertionError("expected the size cap to trigger")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("larger than"))
        }
    }
}
