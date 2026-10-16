package com.cir0cuit.devtoys.tools.encoders

import org.junit.Assert.assertEquals
import org.junit.Test

/** Expectations follow RFC 3986 section 2 (percent-encoding, unreserved = ALPHA / DIGIT / "-" / "." / "_" / "~"). */
class UrlCodecTest {

    @Test
    fun unreservedCharactersAreNotEncoded() {
        val unreserved = "ABCXYZabcxyz0189-._~"
        assertEquals(unreserved, urlEncode(unreserved))
    }

    @Test
    fun spaceIsPercent20NotPlus() {
        assertEquals("Hello%20World", urlEncode("Hello World"))
    }

    @Test
    fun reservedCharactersArePercentEncoded() {
        assertEquals("%2A", urlEncode("*"))
        assertEquals("%2B", urlEncode("+"))
        assertEquals("%21%23%24%26%27%28%29%2C%2F%3A%3B%3D%3F%40%5B%5D", urlEncode("!#$&'(),/:;=?@[]"))
        assertEquals("%25", urlEncode("%"))
    }

    @Test
    fun nonAsciiIsUtf8PercentEncodedUppercase() {
        assertEquals("%C3%A9", urlEncode("é"))
        assertEquals("%F0%9F%98%80", urlEncode("😀"))
    }

    @Test
    fun literalPercent7EInputIsNotTurnedIntoTilde() {
        assertEquals("%257E", urlEncode("%7E"))
    }

    @Test
    fun decodeBasic() {
        assertEquals("a b", urlDecode("a%20b"))
        assertEquals("é", urlDecode("%C3%A9"))
        assertEquals("é", urlDecode("%c3%a9"))
    }

    @Test
    fun strayPercentIsKeptLiterally() {
        assertEquals("100%", urlDecode("100%"))
        assertEquals("%zz", urlDecode("%zz"))
        assertEquals("%4", urlDecode("%4"))
        assertEquals("50% off", urlDecode("50% off"))
        assertEquals("50% off", urlDecode("50%25 off"))
        assertEquals("%", urlDecode("%"))
    }

    @Test
    fun plusDecodesAsSpaceAndLiteralPlusRoundTrips() {
        // Product decision: match desktop DevToys / WebUtility.UrlDecode (form-urlencoded '+' = space).
        assertEquals("a b", urlDecode("a+b"))
        assertEquals("C  ", urlDecode("C++"))
        // A literal '+' survives because the encoder emits %2B.
        assertEquals("%2B", urlEncode("+"))
        assertEquals("C++", urlDecode(urlEncode("C++")))
        assertEquals("C++", urlDecode("C%2B%2B"))
    }

    @Test
    fun roundTrip() {
        val s = "a+b c~*é😀/?&=#%7E 100%"
        assertEquals(s, urlDecode(urlEncode(s)))
    }

    @Test
    fun emptyIsEmpty() {
        assertEquals("", urlEncode(""))
        assertEquals("", urlDecode(""))
    }

    @Test
    fun fullUrlModeKeepsStructure() {
        assertEquals("https://example.com/a%20b?q=%C3%A9&x=1#top", urlEncode("https://example.com/a b?q=é&x=1#top", UrlMode.FULL))
        assertEquals("%5B%5D%25", urlEncode("[]%", UrlMode.FULL))
    }

    @Test
    fun formModeUsesPlusForSpace() {
        assertEquals("a+b%26c%3Dd*", urlEncode("a b&c=d*", UrlMode.FORM))
    }

    @Test
    fun repeatedDecodeUnwrapsDoubleEncoding() {
        assertEquals("%20", urlDecode("%2520"))
        assertEquals(" ", urlDecodeRepeatedly("%2520"))
        assertEquals("a b", urlDecodeRepeatedly("a%252520b"))
        assertEquals("100%", urlDecodeRepeatedly("100%25"))
    }

    @Test
    fun parsesUrlComponents() {
        val p = parseUrl("https://user:pw@example.com:8080/p/a%20th?q=a+b&x=%C3%A9&flag#frag")!!
        assertEquals("https", p.scheme)
        assertEquals("user:pw", p.userInfo)
        assertEquals("example.com", p.host)
        assertEquals("8080", p.port)
        assertEquals("/p/a%20th", p.path)
        assertEquals("frag", p.fragment)
        assertEquals(listOf("q" to "a b", "x" to "é", "flag" to ""), p.params)
    }

    @Test
    fun parsesIpv6HostAndDeepLink() {
        val v6 = parseUrl("http://[::1]:80/")!!
        assertEquals("[::1]", v6.host)
        assertEquals("80", v6.port)
        val deep = parseUrl("myapp:open?id=7")!!
        assertEquals("myapp", deep.scheme)
        assertEquals(null, deep.host)
        assertEquals("open", deep.path)
        assertEquals(listOf("id" to "7"), deep.params)
    }

    @Test
    fun stripsTrackingParameters() {
        assertEquals("https://x.io/p?id=1#s", stripTracking("https://x.io/p?utm_source=a&id=1&fbclid=z#s"))
        assertEquals("https://x.io/p", stripTracking("https://x.io/p?utm_medium=b&gclid=c"))
        assertEquals("https://x.io/p?a=%20", stripTracking("https://x.io/p?a=%20"))
    }
}
