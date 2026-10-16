package com.cir0cuit.devtoys.tools.reference

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ReferenceLogicTest {

    @Test
    fun placeholdersAreParsedAndFilled() {
        val cmd = "docker build -t <name>:<tag> . && echo <name>"
        assertEquals(listOf("name", "tag"), placeholdersIn(cmd))
        assertEquals("docker build -t app:<tag> . && echo app", fillPlaceholders(cmd, mapOf("name" to "app", "tag" to " ")))
        // Comparisons and redirects are not placeholders.
        assertEquals(emptyList<String>(), placeholdersIn("jq 'select(.a > 1)' | x < y"))
        assertEquals("a\$1b", fillPlaceholders("a<x>b", mapOf("x" to "\$1")))
    }

    @Test
    fun searchSpansAllCheatsheets() {
        val hits = searchCheatsheets(CHEATSHEETS, "worktree")
        assertTrue(hits.isNotEmpty())
        assertTrue(hits.all { it.name.startsWith("Git · ") })
        assertTrue(searchCheatsheets(CHEATSHEETS, "port").map { it.name.substringBefore(" · ") }.toSet().size > 1)
        assertTrue(searchCheatsheets(CHEATSHEETS, "zzzz-no-match").isEmpty())
    }

    @Test
    fun cheatsheetKeysAreUnique() {
        assertEquals(CHEATSHEETS.size, CHEATSHEETS.map { it.title }.toSet().size)
        CHEATSHEETS.forEach { sheet ->
            assertEquals(sheet.title, sheet.categories.size, sheet.categories.map { it.name }.toSet().size)
            sheet.categories.forEach { cat ->
                assertEquals("${sheet.title}/${cat.name}", cat.commands.size, cat.commands.map { it.title }.toSet().size)
            }
        }
    }

    @Test
    fun httpSpecSections() {
        assertEquals("RFC 9110 §15.5.5", httpSpec(404))
        assertEquals("RFC 9110 §15.4.9", httpSpec(308))
        assertEquals("RFC 9110 §15.5.21", httpSpec(422))
        assertEquals("RFC 9110 §15.6.4", httpSpec(503))
        assertEquals("RFC 6585", httpSpec(429))
        assertEquals("Not standardized", httpSpec(599))
    }

    @Test
    fun httpFlagsAndFilters() {
        assertTrue(!httpHasBody(204) && !httpHasBody(304) && !httpHasBody(101) && httpHasBody(404))
        assertEquals(listOf(404), filterStatuses("404").flatMap { it.second }.map { it.code })
        assertTrue(filterHeaders("access-control").flatMap { it.second }.size >= 7)
        assertEquals(HTTP_HEADERS.flatMap { it.second }.size, HTTP_HEADERS.flatMap { it.second }.map { it.name }.toSet().size)
    }

    @Test
    fun mimeLookup() {
        assertEquals("image/jpeg", lookupMime("Photo.JPG").first().type)
        assertEquals("image/png", lookupMime(".png").first().type)
        assertEquals("application/json", lookupMime("json").first().type)
        assertTrue(lookupMime("image/").all { it.type.startsWith("image/") })
        assertTrue(lookupMime("nope-nothing").isEmpty())
        assertEquals(MIME_TYPES.size, MIME_TYPES.map { it.type }.toSet().size)
    }

    @Test
    fun magicBytes() {
        fun bytes(vararg b: Int) = ByteArray(b.size) { b[it].toByte() }
        assertTrue(detectFileType(bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0))!!.startsWith("PNG"))
        assertTrue(detectFileType("\u0000\u0000\u0000\u0018ftypheic".toByteArray(Charsets.ISO_8859_1))!!.startsWith("HEIC"))
        assertTrue(detectFileType("\u0000\u0000\u0000\u0018ftypisom".toByteArray(Charsets.ISO_8859_1))!!.startsWith("MP4"))
        assertTrue(detectFileType("RIFF\u0000\u0000\u0000\u0000WEBPVP8 ".toByteArray(Charsets.ISO_8859_1))!!.startsWith("WebP"))
        assertTrue(detectFileType(bytes(0x50, 0x4B, 0x03, 0x04))!!.startsWith("ZIP"))
        assertNull(detectFileType(bytes(0x12, 0x34)))
        assertNull(detectFileType(ByteArray(0)))
        assertEquals("00 FF 10", toHex(bytes(0, 255, 16)))
    }

    @Test
    fun ports() {
        assertEquals(PORTS.size, PORTS.map { it.number }.toSet().size)
        assertEquals(5432, filterPorts("postgres").single().number)
        assertTrue(filterPorts("udp").all { "UDP" in it.protocol })
        val random = Random(42)
        repeat(1000) {
            val port = randomFreePort(random)
            assertTrue(port in 49152..65535)
            assertTrue(PORTS.none { it.number == port })
        }
    }
}
