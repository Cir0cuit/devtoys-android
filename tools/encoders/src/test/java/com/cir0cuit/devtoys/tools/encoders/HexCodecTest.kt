package com.cir0cuit.devtoys.tools.encoders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HexCodecTest {

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
    }

    @Test
    fun decodeBasic() {
        assertEquals("Hello", hexDecode("48656c6c6f"))
        assertEquals("Hello", hexDecode("48656C6C6F"))
    }

    @Test
    fun decodeIgnoresCommonSeparatorsAndLeadingPrefix() {
        assertEquals("Hello", hexDecode("48 65 6c 6c 6f"))
        assertEquals("Hello", hexDecode("48:65:6c:6c:6f"))
        assertEquals("Hello", hexDecode("48-65-6c-6c-6f"))
        assertEquals("Hello", hexDecode("0x48656c6c6f"))
        assertEquals("Hello", hexDecode("0X48656c6c6f"))
        assertEquals("Hello", hexDecode("48 65\n6c\t6c 6f\n"))
    }

    @Test
    fun decodeUtf8MultiByte() {
        assertEquals("é€😀", hexDecode("c3a9e282acf09f9880"))
    }

    @Test
    fun decodeRoundTrip() {
        val s = "Grüße, мир! 😀 \u0000\u007F"
        assertEquals(s, hexDecode(hexEncode(s)))
    }

    @Test
    fun decodeOddLengthIsError() {
        assertTrue(hexDecode("abc").startsWith("Error"))
    }

    @Test
    fun decodeNonHexIsError() {
        assertTrue(hexDecode("zz").startsWith("Error"))
        assertTrue(hexDecode("4g").startsWith("Error"))
    }

    @Test
    fun decodeEmptyOrSeparatorsOnlyIsEmpty() {
        assertEquals("", hexDecode(""))
        assertEquals("", hexDecode("  \n"))
    }

    @Test
    fun decodeInvalidUtf8YieldsReplacementCharacter() {
        // Unicode ch.3 / WHATWG Encoding: an ill-formed byte decodes to U+FFFD
        assertEquals("�", hexDecode("ff"))
    }
}
