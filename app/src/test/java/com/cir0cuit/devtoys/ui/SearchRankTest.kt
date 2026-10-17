package com.cir0cuit.devtoys.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchRankTest {
    private data class T(val title: String, val description: String = "", val keywords: List<String> = emptyList())

    private fun rank(t: T, query: String) = searchRank(t.title, t.description, t.keywords, query)

    private fun search(tools: List<T>, query: String) =
        tools.mapNotNull { t -> rank(t, query)?.let { t.title to it } }.sortedBy { it.second }.map { it.first }

    @Test
    fun titleMatchesRankAboveDescriptionAndKeywords() {
        val tools = listOf(
            T("MAC Address Lookup", "Find the vendor of a MAC address"),
            T("Find & Replace"),
        )
        assertEquals(listOf("Find & Replace", "MAC Address Lookup"), search(tools, "Find"))

        val escape = listOf(
            T("JSON Formatter", keywords = listOf("escape")),
            T("HTML Entity Encoder", "Escape HTML"),
            T("String Escape"),
        )
        assertEquals("String Escape", search(escape, "escape").first())
    }

    @Test
    fun prefixBeatsWordStartBeatsSubstring() {
        val tools = listOf(T("Rot13 Cipher"), T("Base Converter"), T("Number Base Converter"), T("Database"))
        assertEquals(listOf("Base Converter", "Number Base Converter", "Database"), search(tools, "base"))
    }

    @Test
    fun everyWordMustMatchSomewhere() {
        val ip = T("IP Address Converter", "Convert IPv4 addresses")
        assertEquals(1, rank(ip, "ip conv"))
        assertEquals(1, rank(ip, "  IP   conv "))
        assertEquals(3, rank(ip, "ipv4"))
        assertNull(rank(ip, "ip zzz"))
        assertNull(rank(ip, "   "))
    }

    @Test
    fun wordStartIgnoresPunctuationAndRegexCharacters() {
        assertEquals(1, rank(T("ROT13 / Classic Ciphers"), "classic"))
        assertEquals(1, rank(T("XML ↔ JSON"), "json"))
        assertEquals(2, rank(T("Find & Replace"), "& r"))
        assertNull(rank(T("Regex Tester"), "("))
    }
}
