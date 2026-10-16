package com.cir0cuit.devtoys.tools.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

class XmlFormatterTest {

    private fun fmt(s: String) = formatXml(s).replace("\r\n", "\n")

    private fun parse(s: String) = DocumentBuilderFactory.newInstance()
        .newDocumentBuilder().parse(InputSource(StringReader(s)))

    @Test
    fun indentsNestedElementsWithTwoSpaces() {
        val out = fmt("<a><b>1</b><c x=\"1\"/></a>")
        assertTrue(out, out.contains("<a>\n  <b>1</b>\n  <c x=\"1\"/>\n</a>"))
    }

    @Test
    fun reformatsAlreadyIndentedInputWithoutBlankLines() {
        val out = fmt("<a>\n\n      <b>t</b>\n\n</a>")
        assertTrue(out, out.contains("<a>\n  <b>t</b>\n</a>"))
        assertFalse(out, out.contains("\n\n"))
    }

    @Test
    fun escapedCharactersStayEscaped() {
        val out = fmt("<a>&lt;&amp;&gt;</a>")
        assertTrue(out, out.contains("<a>&lt;&amp;&gt;</a>"))
    }

    @Test
    fun cdataIsPreserved() {
        val out = fmt("<a><![CDATA[x<y]]></a>")
        assertTrue(out, out.contains("<![CDATA[x<y]]>"))
    }

    @Test
    fun namespacesArePreserved() {
        val out = fmt("<r xmlns=\"urn:a\" xmlns:p=\"urn:p\"><p:c/></r>")
        assertTrue(out, out.contains("xmlns=\"urn:a\""))
        assertTrue(out, out.contains("<p:c/>"))
    }

    @Test
    fun mixedContentTextIsNotAltered() {
        // XML 1.0 sec 2.10: whitespace in mixed content is significant; a pretty printer
        // must not inject indentation into text that sits next to non-whitespace text.
        val out = fmt("<p>Hello <b>world</b> !</p>")
        assertFalse(out, out.startsWith("Error"))
        assertEquals("Hello world !", parse(out).documentElement.textContent)
    }

    @Test
    fun malformedInputReturnsError() {
        assertTrue(fmt("<a><b/>").startsWith("Error:"))
        assertTrue(fmt("not xml").startsWith("Error:"))
    }

    @Test
    fun internalDtdSubsetIsRejected() {
        assertEquals(
            "Error: DTD internal subsets are not supported",
            formatXml("<!DOCTYPE a [<!ENTITY x \"y\">]><a>&x;</a>")
        )
    }

    @Test
    fun externalEntityIsRejected() {
        val out = formatXml("<?xml version=\"1.0\"?>\n<!DOCTYPE a [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]><a>&xxe;</a>")
        assertTrue(out, out.startsWith("Error:"))
    }

    @Test
    fun billionLaughsIsRejected() {
        val bomb = "<!DOCTYPE lolz [<!ENTITY lol \"lol\"><!ENTITY lol2 \"&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;\">]><lolz>&lol2;</lolz>"
        assertTrue(formatXml(bomb).startsWith("Error:"))
    }

    @Test
    fun dtdSubsetDetectionSkipsPrologAndBom() {
        assertTrue(hasInternalDtdSubset("${Char(0xFEFF)}<?xml version=\"1.0\"?>\n<!-- c -->\n<!DOCTYPE a [ ]><a/>"))
        assertTrue(hasInternalDtdSubset("<!DOCTYPE a SYSTEM 'x.dtd' [<!ENTITY e 'v'>]><a/>"))
    }

    @Test
    fun dtdSubsetDetectionIgnoresBracketsInQuotesAndComments() {
        assertFalse(hasInternalDtdSubset("<!DOCTYPE a SYSTEM \"foo[1].dtd\"><a/>"))
        assertFalse(hasInternalDtdSubset("<!DOCTYPE a PUBLIC '-//X[y]//EN' 'z.dtd'><a/>"))
        assertFalse(hasInternalDtdSubset("<!-- [not a subset] --><!DOCTYPE a><a/>"))
        assertFalse(hasInternalDtdSubset("<a>[text]</a>"))
        assertFalse(hasInternalDtdSubset("<a><![CDATA[<!DOCTYPE x [ ]>]]></a>"))
    }

    @Test
    fun doctypeWithoutSubsetIsFormattedWithoutFetchingDtd() {
        val out = fmt("<!DOCTYPE a SYSTEM \"http://invalid.example/foo[1].dtd\"><a><b/></a>")
        assertFalse(out, out.startsWith("Error"))
        assertTrue(out, out.contains("<a>\n  <b/>\n</a>"))
    }

    @Test
    fun doctypeDeclarationIsKeptInOutput() {
        // A formatter must not drop the document type declaration (XML 1.0 sec 2.8).
        val out = fmt("<!DOCTYPE html SYSTEM \"about:legacy-compat\"><html/>")
        assertTrue(out, out.contains("<!DOCTYPE html"))
    }

    @Test
    fun indentsWithFourSpaces() {
        val out = formatXml("<a><b>1</b></a>", "    ").replace("\r\n", "\n")
        assertTrue(out, out.contains("<a>\n    <b>1</b>\n</a>"))
    }

    @Test
    fun minifyDropsCommentsAndIndentationButKeepsMixedText() {
        val out = formatXml("<a>\n  <!-- c -->\n  <b> x </b>\n  <p>Hi <i>u</i> !</p>\n</a>", null)
        assertTrue(out, out.endsWith("?><a><b> x </b><p>Hi <i>u</i> !</p></a>"))
    }

    @Test
    fun parseErrorReportsLineAndColumn() {
        val out = formatXml("<a>\n<b></a>")
        assertTrue(out, out.startsWith("Error: Line 2, column "))
    }

    @Test
    fun xpathListsMatchingAttributesAndElements() {
        val xml = "<r><i id=\"1\">a</i><i id=\"2\">b</i></r>"
        assertEquals(XPathResult("2 matches", "id=\"1\"\nid=\"2\""), evaluateXPath(xml, "//i/@id"))
        assertEquals(XPathResult("1 match", "<i id=\"1\">a</i>"), evaluateXPath(xml, "//i[1]"))
        assertEquals(XPathResult("No matches", ""), evaluateXPath(xml, "//x"))
    }

    @Test
    fun xpathScalarResults() {
        val xml = "<r><i>a</i><i>b</i></r>"
        assertEquals(XPathResult("Result", "2"), evaluateXPath(xml, "count(//i)"))
        assertEquals(XPathResult("Result", "true"), evaluateXPath(xml, "boolean(//i)"))
    }

    @Test
    fun xpathResolvesDocumentPrefixesAndDefaultNamespace() {
        val xml = "<r xmlns=\"urn:a\" xmlns:p=\"urn:p\"><p:c>v</p:c><i>1</i></r>"
        assertEquals("v", evaluateXPath(xml, "string(//p:c)")?.text)
        assertEquals("1", evaluateXPath(xml, "count(//d:i)")?.text)
    }

    @Test
    fun xpathInvalidExpressionAndInvalidXml() {
        assertTrue(evaluateXPath("<r/>", "//i[")?.error != null)
        assertNull(evaluateXPath("<r>", "//r"))
    }
}
