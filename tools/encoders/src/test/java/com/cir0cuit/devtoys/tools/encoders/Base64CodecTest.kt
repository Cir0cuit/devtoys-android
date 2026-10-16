package com.cir0cuit.devtoys.tools.encoders

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Base64CodecTest {

    private fun decode(s: String) = base64DecodeBytes(s).toString(Charsets.UTF_8)

    @Test
    fun rfc4648Vectors() {
        val vectors = listOf("" to "", "f" to "Zg==", "fo" to "Zm8=", "foo" to "Zm9v", "foob" to "Zm9vYg==", "fooba" to "Zm9vYmE=", "foobar" to "Zm9vYmFy")
        for ((plain, encoded) in vectors) {
            assertEquals(encoded, base64Encode(plain))
            assertEquals(plain, decode(encoded))
        }
    }

    @Test
    fun variants() {
        assertEquals("8J+YgMO/Pj8=", base64Encode("😀ÿ>?"))
        assertEquals("8J-YgMO_Pj8=", base64Encode("😀ÿ>?", urlSafe = true))
        assertEquals("8J-YgMO_Pj8", base64Encode("😀ÿ>?", urlSafe = true, padding = false))
        val wrapped = base64Encode("a".repeat(60), wrap = true)
        assertEquals(listOf(76, 4), wrapped.lines().map { it.length })
    }

    @Test
    fun decodeIsLenient() {
        assertEquals("😀ÿ>?", decode("8J-YgMO_Pj8"))
        assertEquals("😀ÿ>?", decode("8J+YgMO/Pj8="))
        assertEquals("foobar", decode(" Zm9v\nYmFy \r\n"))
        assertEquals("f", decode("Zg"))
        assertEquals("foobar", decode("data:text/plain;base64,Zm9vYmFy"))
        assertEquals("a".repeat(60), decode(base64Encode("a".repeat(60), wrap = true)))
    }

    @Test
    fun decodeKeepsBinary() {
        assertArrayEquals(byteArrayOf(-1, -2, 0), base64DecodeBytes("//4A"))
        assertNull(utf8OrNull(base64DecodeBytes("//4A")))
    }

    @Test
    fun invalidInputIsRejectedWithAMessage() {
        for (bad in listOf("Zm9vY", "Zm9v!", "Zm9=v")) {
            try {
                base64DecodeBytes(bad)
                throw AssertionError("expected '$bad' to be rejected")
            } catch (e: IllegalArgumentException) {
                assertTrue(e.message!!.isNotEmpty())
            }
        }
    }

    @Test
    fun detectsFileTypes() {
        assertEquals("png", detectFileType(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D))?.second)
        assertEquals("gz", detectFileType(byteArrayOf(0x1F, 0x8B.toByte(), 8))?.second)
        assertNull(detectFileType(byteArrayOf(1, 2, 3)))
    }
}
