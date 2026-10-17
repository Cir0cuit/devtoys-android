package com.cir0cuit.devtoys.tools.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class StringEscapeTest {
    private val sample = "Say \"hi\"\n\tto caf" + Char(0xE9) + " \\ $5 'quoted' " + String(Character.toChars(0x1F600)) + Char(1)

    @Test fun roundTripsEveryFormat() {
        EscapeFormat.entries.forEach { f ->
            assertEquals(f.label, sample, unescapeString(escapeString(sample, f), f))
        }
    }

    @Test fun jsonEscapes() {
        assertEquals("a\\\"b\\\\c\\nd\\u0001", escapeString("a\"b\\c\nd" + Char(1), EscapeFormat.JSON))
    }

    @Test fun kotlinEscapesDollar() {
        assertEquals("\\\$x", escapeString("\$x", EscapeFormat.KOTLIN))
        assertEquals("\$x", escapeString("\$x", EscapeFormat.JAVA))
    }

    @Test fun sqlShellCsv() {
        assertEquals("it''s", escapeString("it's", EscapeFormat.SQL))
        assertEquals("'it'\\''s'", escapeString("it's", EscapeFormat.SHELL))
        assertEquals("\"a \"\"b\"\"\"", escapeString("a \"b\"", EscapeFormat.CSV))
        assertEquals("a b\$c", unescapeString("\"a b\\\$c\"", EscapeFormat.SHELL))
    }

    @Test fun regexEscape() {
        assertEquals("a\\.b\\*\\(c\\)", escapeString("a.b*(c)", EscapeFormat.REGEX))
    }

    @Test fun unicodeEscapes() {
        val smile = String(Character.toChars(0x1F600))
        assertEquals("\\uD83D\\uDE00", escapeString(smile, EscapeFormat.UNICODE))
        assertEquals("\\u{1F600}", escapeString(smile, EscapeFormat.UNICODE_BRACES))
        assertEquals(smile + "x", unescapeString("\\u{1F600}x", EscapeFormat.JAVASCRIPT))
        assertEquals("A", unescapeString("\\x41", EscapeFormat.PYTHON))
    }

    @Test fun invalidEscapeReportsPosition() {
        try {
            unescapeString("ab\\q", EscapeFormat.JSON)
            fail()
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message, e.message!!.contains("position 3"))
        }
        assertFails { unescapeString("\\u12", EscapeFormat.JAVA) }
        assertFails { unescapeString("abc\\", EscapeFormat.JAVA) }
        assertFails { unescapeString("\"a\"b\"", EscapeFormat.CSV) }
    }

    private fun assertFails(block: () -> Unit) {
        try { block(); fail() } catch (e: IllegalArgumentException) {}
    }
}

class TextStatsTest {
    @Test fun countsBasics() {
        val s = computeTextStats("Hello world. Hello again!\r\nSecond line\r\n")
        assertEquals(6, s.words)
        assertEquals(2, s.lines)
        assertEquals("CRLF", s.lineEndings)
        assertEquals("hello" to 2, s.topWords.first())
        assertEquals(25, s.longestLine)
    }

    @Test fun graphemesAndBytes() {
        val s = computeTextStats("e" + Char(0x301) + String(Character.toChars(0x1F600)))
        assertEquals(4, s.chars)
        assertEquals(3, s.codePoints)
        assertEquals(2, s.graphemes)
        assertEquals(1 + 2 + 4, s.utf8Bytes)
        assertNull(s.latin1Bytes)
        assertEquals(1, computeTextStats("caf" + Char(0xE9)).words)
        assertEquals(4, computeTextStats("caf" + Char(0xE9)).latin1Bytes)
    }

    @Test fun emptyAndMixed() {
        val empty = computeTextStats("")
        assertEquals(0, empty.lines)
        assertEquals("None", empty.lineEndings)
        assertEquals("Mixed (1 LF, 1 CRLF)", computeTextStats("a\nb\r\nc").lineEndings)
    }
}

class UnicodeInspectorTest {
    @Test fun codePointDetails() {
        val info = codePointInfo(0x1F600)
        assertEquals("U+1F600", info.hex)
        assertEquals("GRINNING FACE", info.name)
        assertEquals("F0 9F 98 80", info.utf8)
        assertEquals("D83D DE00", info.utf16)
        assertTrue(info.escapes.contains("%F0%9F%98%80"))
        assertTrue(codePointInfo('A'.code).category.startsWith("Lu"))
    }

    @Test fun flagsInvisibleAndBidi() {
        listOf(0x200B, 0x200D, 0xFEFF, 0xA0, 0xAD, 0x202E, 0x2066, 0x2028, 0x0).forEach { assertTrue(Integer.toHexString(it), isInvisibleOrBidi(it)) }
        listOf('a'.code, ' '.code, '\n'.code, '\t'.code, 0x1F600).forEach { assertFalse(Integer.toHexString(it), isInvisibleOrBidi(it)) }
    }

    @Test fun cleansAndDetectsMixedScripts() {
        val s = "pay" + Char(0x200B) + "pal" + Char(0xA0) + Char(0x0430) + "pple" + Char(0x202E)
        assertEquals("paypal " + Char(0x0430) + "pple", removeInvisible(s))
        val inspection = inspect(s)
        assertEquals(3, inspection.suspiciousCount)
        assertEquals(listOf("Latin", "Cyrillic"), inspection.scripts)
    }

    @Test fun asciiTable() {
        assertEquals("NUL", asciiName(0))
        assertEquals("ESC", asciiName(27))
        assertEquals("SP", asciiName(32))
        assertEquals("DEL", asciiName(127))
        assertEquals(" 65  41   101  1000001  A", asciiRow(65))
    }

    @Test fun normalization() {
        val rows = normalizationRows("e" + Char(0x301))
        assertEquals(Char(0xE9).toString(), rows[0].second)
        assertTrue(rows[1].first.contains("same as input"))
    }
}

class ListCompareTest {
    @Test fun setOperations() {
        val r = compareLists("b\na\nc\na\n", " A\nd\nc", ignoreCase = true, trim = true, skipEmpty = true)
        assertEquals(listOf("a", "c"), r.both)
        assertEquals(listOf("b"), r.onlyA)
        assertEquals(listOf("d"), r.onlyB)
        assertEquals(listOf("b", "a", "c", "d"), r.union)
        assertEquals(listOf("b", "d"), r.eitherNotBoth)
    }

    @Test fun caseAndWhitespaceMatterWhenOff() {
        val r = compareLists("a\n", "A\n a", ignoreCase = false, trim = false, skipEmpty = false)
        assertEquals(listOf("a", ""), r.onlyA)
        assertEquals(listOf("A", " a"), r.onlyB)
    }
}

class FindReplaceTest {
    @Test fun literalReplaceIgnoresRegexSyntax() {
        val r = findAndReplace("a.b a.b axb", "a.b", "\$1", ReplaceOptions())
        assertEquals("\$1 \$1 axb", r.text)
        assertEquals(2, r.count)
        assertEquals(listOf(0..1, 3..4), r.highlights)
    }

    @Test fun regexBackreferencesAndNamedGroups() {
        assertEquals("[2023] [2024]", findAndReplace("2023 2024", "(\\d+)", "[$1]", ReplaceOptions(regex = true)).text)
        assertEquals("b-a", findAndReplace("a-b", "(?<x>a)-(?<y>b)", "\${y}-\${x}", ReplaceOptions(regex = true)).text)
    }

    @Test fun wholeWordAndIgnoreCase() {
        val r = findAndReplace("Cat concat cat", "cat", "dog", ReplaceOptions(ignoreCase = true, wholeWord = true))
        assertEquals("dog concat dog", r.text)
    }

    @Test fun deletionHasNoHighlights() {
        val r = findAndReplace("aXbX", "X", "", ReplaceOptions())
        assertEquals("ab", r.text)
        assertEquals(2, r.count)
        assertTrue(r.highlights.isEmpty())
    }

    @Test fun errors() {
        assertNotNull(findAndReplace("abc", "(a", "", ReplaceOptions(regex = true)).error)
        assertNotNull(findAndReplace("abc", "(a)", "$2", ReplaceOptions(regex = true)).error)
        assertNull(findAndReplace("abc", "", "x", ReplaceOptions()).error)
    }

    @Test fun invalidReplacementIsReportedWithoutAMatch() {
        val regex = ReplaceOptions(regex = true)
        assertNotNull(findAndReplace("xyz", "a", "a $", regex).error)
        assertNotNull(findAndReplace("xyz", "(a)", "$2", regex).error)
        assertNotNull(findAndReplace("", "(?<n>a)", "\${m}", regex).error)
        val ok = findAndReplace("xyz", "(a)", "[$1]", regex)
        assertNull(ok.error)
        assertEquals("xyz", ok.text)
        assertEquals(0, ok.count)
        assertNull(findAndReplace("xyz", "a", "a $", ReplaceOptions()).error)
    }
}

class TextToolFixesTest {
    @Test fun topWordsAlignCounts() {
        assertEquals("hello  2\ncafé   1", topWordsText(listOf("hello" to 2, "café" to 1)))
    }

    @Test fun blockAndScriptNamesKeepTitleCase() {
        assertEquals("Basic Latin", titleName(Character.UnicodeBlock.of('a'.code).toString()))
        assertEquals("General Punctuation", titleName(Character.UnicodeBlock.of(0x200B).toString()))
        assertEquals("CJK Unified Ideographs", titleName("CJK_UNIFIED_IDEOGRAPHS"))
        assertEquals("Halfwidth and Fullwidth Forms", titleName("HALFWIDTH_AND_FULLWIDTH_FORMS"))
        assertEquals("Old Italic", titleName(Character.UnicodeScript.OLD_ITALIC.name))
    }

    @Test fun replacementHintKeepsDoubleBackslashTogether() {
        val error = findAndReplace("abc", "b", "a $", ReplaceOptions(regex = true)).error!!
        assertTrue(error, error.endsWith("\\" + Char(0x2060) + "\\."))
    }

    @Test fun lineOutputTitleHasNoCountOnError() {
        assertEquals("Output", outputTitle(computeLines("a\nb", "Filter", LineOptions(pattern = "a(", regex = true))))
        assertEquals("Output, 1 of 2 lines", outputTitle(computeLines("a\nb", "Filter", LineOptions(pattern = "a"))))
    }
}
