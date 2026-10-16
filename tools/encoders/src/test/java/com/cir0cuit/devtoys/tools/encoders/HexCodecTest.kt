package com.cir0cuit.devtoys.tools.encoders

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HexCodecTest {

    private fun hexDecode(s: String) = hexDecodeBytes(s).toString(Charsets.UTF_8)

    private fun assertRejected(s: String) {
        try {
            hexDecodeBytes(s)
        } catch (e: IllegalArgumentException) {
            return
        }
        throw AssertionError("expected '$s' to be rejected")
    }

    @Test
    fun encodeAsciiIsLowercaseTwoDigitsPerByte() {
        assertEquals("48656c6c6f", hexEncode("Hello"))
        assertEquals("0a00", hexEncode("\n\u0000"))
    }

    @Test
    fun encodeUsesUtf8Bytes() {
        // RFC 3629: U+00E9 -> C3 A9, U+20AC -> E2 82 AC, U+1F600 -> F0 9F 98 80
        assertEquals("c3a9", hexEncode("é"))
        assertEquals("e282ac", hexEncode("€"))
        assertEquals("f09f9880", hexEncode("😀"))
    }

    @Test
    fun encodeEmptyIsEmpty() {
        assertEquals("", hexEncode(""))
        assertEquals("", hexEncode("", HexFormat.C_ARRAY))
    }

    @Test
    fun encodeFormats() {
        assertEquals("48 69", hexEncode("Hi", HexFormat.SPACED))
        assertEquals("48:69", hexEncode("Hi", HexFormat.COLONS))
        assertEquals("C3:A9", hexEncode("é", HexFormat.COLONS, upper = true))
        assertEquals("\\x48\\x69", hexEncode("Hi", HexFormat.ESCAPED))
        assertEquals("0x48, 0x69", hexEncode("Hi", HexFormat.LIST))
        assertEquals("{\n    0x48, 0x69\n}", hexEncode("Hi", HexFormat.C_ARRAY))
        assertEquals("byteArrayOf(\n    -61, -87\n)", hexEncode("é", HexFormat.KOTLIN))
        assertEquals("b'\\x48\\x69'", hexEncode("Hi", HexFormat.PYTHON))
        assertEquals("00000000  48 69" + " ".repeat(3 * 14 + 1) + "  |Hi|", hexEncode("Hi", HexFormat.DUMP))
    }

    @Test
    fun codeFormatsBreakEvery16Bytes() {
        val c = hexEncode("a".repeat(17), HexFormat.C_ARRAY)
        assertEquals(4, c.lines().size)
    }

    @Test
    fun everyFormatExceptCodeOnesDecodesBack() {
        val s = "Grüße 😀"
        for (f in listOf(HexFormat.PLAIN, HexFormat.SPACED, HexFormat.COLONS, HexFormat.ESCAPED, HexFormat.LIST, HexFormat.C_ARRAY)) {
            assertEquals(f.name, s, hexDecode(hexEncode(s, f, upper = f.ordinal % 2 == 0)))
        }
    }

    @Test
    fun decodeBasic() {
        assertEquals("Hello", hexDecode("48656c6c6f"))
        assertEquals("Hello", hexDecode("48656C6C6F"))
    }

    @Test
    fun decodeIgnoresCommonSeparatorsAndPrefixes() {
        assertEquals("Hello", hexDecode("48 65 6c 6c 6f"))
        assertEquals("Hello", hexDecode("48:65:6c:6c:6f"))
        assertEquals("Hello", hexDecode("48-65-6c-6c-6f"))
        assertEquals("Hello", hexDecode("0x48656c6c6f"))
        assertEquals("Hello", hexDecode("0X48656c6c6f"))
        assertEquals("Hello", hexDecode("48 65\n6c\t6c 6f\n"))
        assertEquals("Hello", hexDecode("0x48, 0x65, 0x6c, 0x6c, 0x6f"))
        assertEquals("Hello", hexDecode("\\x48\\x65\\x6c\\x6c\\x6f"))
    }

    @Test
    fun decodeUtf8MultiByte() {
        assertEquals("é€😀", hexDecode("c3a9e282acf09f9880"))
    }

    @Test
    fun decodeKeepsRawBytes() {
        assertArrayEquals(byteArrayOf(-1, 0), hexDecodeBytes("ff00"))
        assertNull(utf8OrNull(hexDecodeBytes("ff")))
    }

    @Test
    fun decodeOddLengthIsError() {
        assertRejected("abc")
    }

    @Test
    fun decodeNonHexIsError() {
        assertRejected("zz")
        assertRejected("4g")
    }

    @Test
    fun decodeEmptyOrSeparatorsOnlyIsEmpty() {
        assertEquals("", hexDecode(""))
        assertEquals("", hexDecode("  \n"))
    }
}
