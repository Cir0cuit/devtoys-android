package com.cir0cuit.devtoys.tools.encoders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Expectations follow the WHATWG HTML Standard, "named character references" and "numeric character reference end state". */
class HtmlEntityCodecTest {

    private fun encode(s: String) = HtmlEntityCodec.encode(s)
    private fun decode(s: String) = HtmlEntityCodec.decode(s)

    @Test
    fun encodesMarkupSignificantCharacters() {
        assertEquals("&lt;a href=&quot;x&quot;&gt;&#39;&amp;&#39;&lt;/a&gt;", encode("<a href=\"x\">'&'</a>"))
    }

    @Test
    fun leavesPlainAsciiUntouched() {
        val s = "Hello, World! 0123 ~`^{}[]|\\/\t\n"
        assertEquals(s, encode(s))
    }

    @Test
    fun encodesNonAsciiWithNamedReferenceWhenAvailable() {
        assertEquals("&eacute;", encode("é"))
        assertEquals("&copy;", encode("©"))
        assertEquals("&euro;", encode("€"))
    }

    @Test
    fun encodesSupplementaryCodePointAsSingleReference() {
        // one reference per code point, never one per UTF-16 surrogate
        assertEquals("&#128512;", encode("😀"))
        assertEquals("&#20013;", encode("中"))
    }

    @Test
    fun encodedOutputIsAsciiAndHasNoRawMarkupCharacters() {
        val out = encode("Tom & Jerry <3 \"q\" 'a' café   😀 中文 ∾̳")
        assertTrue(out.all { it.code < 128 })
        assertFalse(out.contains('<') || out.contains('>') || out.contains('"') || out.contains('\''))
    }

    @Test
    fun roundTrip() {
        val samples = listOf(
            "Tom & Jerry <3 \"quotes\" 'apos'",
            "café   © € ♥",
            "😀👍🇵🇱",
            "∾̳ ⁡ 中文",
            "&amp; literally &lt;",
            "a&b;c",
        )
        for (s in samples) assertEquals(s, decode(encode(s)))
    }

    @Test
    fun decodesNamedReferencesCaseSensitively() {
        assertEquals("<p>", decode("&lt;p&gt;"))
        assertEquals("&", decode("&AMP;"))
        assertEquals("'", decode("&apos;"))
        assertEquals(" ", decode("&nbsp;"))
        assertEquals("Éé", decode("&Eacute;&eacute;"))
    }

    @Test
    fun decodesDecimalAndHexNumericReferences() {
        assertEquals("'''", decode("&#39;&#x27;&#X27;"))
        assertEquals("😀", decode("&#128512;"))
        assertEquals("😀", decode("&#x1F600;"))
        assertEquals("A", decode("&#0065;"))
    }

    @Test
    fun decodingIsSinglePass() {
        assertEquals("&lt;", decode("&amp;lt;"))
    }

    @Test
    fun unknownOrUnterminatedReferencesStayLiteral() {
        assertEquals("&unknown;", decode("&unknown;"))
        assertEquals("AT&T", decode("AT&T"))
        assertEquals("& ;", decode("& ;"))
        assertEquals("&#;", decode("&#;"))
        assertEquals("&#xZZ;", decode("&#xZZ;"))
    }

    @Test
    fun nullNumericReferenceBecomesReplacementCharacter() {
        // WHATWG: 0x00 -> U+FFFD (null-character-reference parse error)
        assertEquals("�", decode("&#0;"))
        assertEquals("�", decode("&#x0;"))
    }

    @Test
    fun surrogateNumericReferenceBecomesReplacementCharacter() {
        // WHATWG: surrogate code point -> U+FFFD (surrogate-character-reference parse error)
        assertEquals("�", decode("&#xD800;"))
        assertEquals("�", decode("&#57343;"))
    }

    @Test
    fun outOfRangeNumericReferenceBecomesReplacementCharacter() {
        // WHATWG: > 0x10FFFF -> U+FFFD (character-reference-outside-unicode-range parse error)
        assertEquals("�", decode("&#x110000;"))
        assertEquals("�", decode("&#99999999999;"))
    }

    @Test
    fun c1ControlNumericReferencesUseWindows1252Mapping() {
        // WHATWG numeric character reference end state replacement table
        assertEquals("€", decode("&#128;"))
        assertEquals("’", decode("&#x92;"))
    }
}
