package com.cir0cuit.devtoys.tools.data

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class XmlJsonTest {

    private val book = "<book id=\"1\"><title>Dune</title><tag>sf</tag><tag>classic</tag></book>"

    @Test
    fun xmlMapsAttributesTextAndRepeatedSiblings() {
        assertEquals(
            JsonParser.parseString("{\"book\":{\"@id\":\"1\",\"title\":\"Dune\",\"tag\":[\"sf\",\"classic\"]}}"),
            JsonParser.parseString(xmlToJson(book))
        )
        assertEquals(
            JsonParser.parseString("{\"a\":{\"@x\":\"1\",\"#text\":\"hi\"}}"),
            JsonParser.parseString(xmlToJson("<a x=\"1\"> hi </a>"))
        )
        assertEquals(JsonParser.parseString("{\"a\":\"\"}"), JsonParser.parseString(xmlToJson("<a/>")))
    }

    @Test
    fun jsonToXmlRoundTrips() {
        val xml = jsonToXml(xmlToJson(book), "").replace("\r\n", "\n")
        assertTrue(xml, xml.contains("<book id=\"1\">\n  <title>Dune</title>\n  <tag>sf</tag>\n  <tag>classic</tag>\n</book>"))
    }

    @Test
    fun rootNameWrapsJsonAndArraysBecomeItems() {
        val xml = jsonToXml("{\"a\":1,\"b\":[true,null]}", "root").replace("\r\n", "\n")
        assertTrue(xml, xml.contains("<root>\n  <a>1</a>\n  <b>true</b>\n  <b/>\n</root>"))
        val list = jsonToXml("[1,2]", "list").replace("\r\n", "\n")
        assertTrue(list, list.contains("<list>\n  <item>1</item>\n  <item>2</item>\n</list>"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun severalTopLevelKeysNeedARootName() {
        jsonToXml("{\"a\":1,\"b\":2}", "")
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidElementNameIsReported() {
        jsonToXml("{\"bad name\":1}", "")
    }

    @Test
    fun xmlTextIsEscaped() {
        val xml = jsonToXml("{\"a\":\"<&>\"}", "")
        assertTrue(xml, xml.contains("<a>&lt;&amp;&gt;</a>"))
    }
}
