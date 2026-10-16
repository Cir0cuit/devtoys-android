package com.cir0cuit.devtoys.tools.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        assertTrue(hasInternalDtdSubset("﻿<?xml version=\"1.0\"?>\n<!-- c -->\n<!DOCTYPE a [ ]><a/>"))
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
}
