package com.cir0cuit.devtoys.tools.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class TextDiffTest {

    private fun diff(old: String, new: String) = buildDiff(old, new).text

    @Test
    fun bothEmptyAsksForInput() {
        assertEquals("Please enter text to compare.", diff("", ""))
    }

    @Test
    fun identicalTexts() {
        assertEquals("Texts are identical.", diff("a\nb\nc", "a\nb\nc"))
    }

    @Test
    fun singleLineChangeShowsRemovalAdditionAndContext() {
        val expected =
            "   1 |    1 |   a\n" +
            "   2 |      | - b\n" +
            "     |    2 | + X\n" +
            "   3 |    3 |   c\n"
        assertEquals(expected, diff("a\nb\nc", "a\nX\nc"))
    }

    @Test
    fun deletionShiftsNewSideLineNumbers() {
        val expected =
            "   1 |    1 |   a\n" +
            "   2 |      | - b\n" +
            "   3 |    2 |   c\n" +
            "   4 |    3 |   d\n"
        assertEquals(expected, diff("a\nb\nc\nd", "a\nc\nd"))
    }

    @Test
    fun insertionShiftsNewSideLineNumbers() {
        val expected =
            "   1 |    1 |   a\n" +
            "     |    2 | + N\n" +
            "   2 |    3 |   b\n"
        assertEquals(expected, diff("a\nb", "a\nN\nb"))
    }

    @Test
    fun oneSideEmpty() {
        assertEquals("     |    1 | + x\n", diff("", "x").substringAfter("- \n"))
        assertTrue(diff("x", "").contains("   1 |      | - x\n"))
    }

    @Test
    fun leadingContextIsLimitedToThreeLinesWithEllipsis() {
        val old = (1..10).joinToString("\n")
        val new = old.replace("\n8\n", "\nEIGHT\n")
        val out = diff(old, new)
        assertTrue(out.startsWith(" ... |  ... | \n"))
        assertTrue(out.contains("   5 |    5 |   5\n"))
        assertFalse(out.contains("   4 |    4 |   4\n"))
        assertTrue(out.contains("   8 |      | - 8\n"))
        assertTrue(out.contains("     |    8 | + EIGHT\n"))
        assertTrue(out.endsWith("  10 |   10 |   10\n"))
    }

    @Test
    fun trailingContextIsLimitedToThreeLinesWithEllipsis() {
        val old = (1..10).joinToString("\n")
        val new = old.replace("2\n", "TWO\n")
        val out = diff(old, new)
        assertTrue(out.contains("   5 |    5 |   5\n"))
        assertFalse(out.contains("   6 |    6 |   6\n"))
        assertTrue(out.endsWith(" ... |  ... | \n"))
    }

    @Test
    fun nonFinalHunkAlsoGetsThreeLinesOfTrailingContext() {
        // Unified-diff convention (GNU diff -U3): every hunk carries up to 3 lines of context
        // after the change, not only the last one.
        val old = (1..20).joinToString("\n")
        val new = old.replace("\n2\n", "\nTWO\n").replace("\n18\n", "\nEIGHTEEN\n")
        val out = diff(old, new)
        assertTrue(out, out.contains("   3 |    3 |   3\n"))
        assertTrue(out, out.contains("   5 |    5 |   5\n"))
        assertTrue(out, out.contains("  15 |   15 |   15\n"))
    }

    @Test
    fun lineNumbersUseAsciiDigitsRegardlessOfDefaultLocale() {
        val saved = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-EG-u-nu-arab"))
            val out = diff("a\nb\nc", "a\nX\nc")
            assertTrue(out, out.contains("   1 |    1 |   a\n"))
            assertFalse(out, out.any { it in '٠'..'٩' })
        } finally {
            Locale.setDefault(saved)
        }
    }

    @Test
    fun linesContainingPercentSignsAreNotTreatedAsFormatSpecifiers() {
        val out = diff("100%d\n%s", "100%d\n%n")
        assertTrue(out, out.contains("- %s\n"))
        assertTrue(out, out.contains("+ %n\n"))
    }
}
