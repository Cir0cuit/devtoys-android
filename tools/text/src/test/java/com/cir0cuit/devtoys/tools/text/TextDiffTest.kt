package com.cir0cuit.devtoys.tools.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class TextDiffTest {

    private fun diff(old: String, new: String, options: DiffOptions = DiffOptions()) = buildDiff(old, new, options).text

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

    @Test
    fun ignoreOptionsTreatNormalizedLinesAsEqual() {
        assertEquals("Texts are identical.", diff("Foo\nbar", "foo\nBAR", DiffOptions(ignoreCase = true)))
        assertEquals("Texts are identical.", diff("  a\nb  ", "a\nb", DiffOptions(trim = true)))
        assertEquals("Texts are identical.", diff("f(a, b)", "f( a,b )", DiffOptions(ignoreAllSpace = true)))
        assertTrue(diff("a\nb", "A\nb").contains("- a\n"))
    }

    @Test
    fun ignoringBlankLinesKeepsOriginalLineNumbers() {
        assertEquals("Texts are identical.", diff("a\n\nb", "a\nb\n\n", DiffOptions(ignoreBlankLines = true)))
        val out = diff("a\n\nb\nc", "a\nb\nX", DiffOptions(ignoreBlankLines = true))
        assertTrue(out, out.contains("   3 |    2 |   b\n"))
        assertTrue(out, out.contains("   4 |      | - c\n"))
        assertTrue(out, out.contains("     |    3 | + X\n"))
    }

    @Test
    fun inlineRangesMarkOnlyTheChangedWordOrCharacter() {
        val (a, b) = changedRanges("val count = 10", "val total = 10", byChar = false)
        assertEquals(listOf(4..8), a)
        assertEquals(listOf(4..8), b)
        val (c, d) = changedRanges("color", "colour", byChar = true)
        assertTrue(c.isEmpty())
        assertEquals(listOf(4..4), d)
    }

    @Test
    fun inlineHighlightIsAddedToChangedLines() {
        val styled = buildDiff("a\nhello world\nc", "a\nhello there\nc")
        assertTrue(styled.spanStyles.size > 2)
    }

    @Test
    fun summaryCountsAddedRemovedAndChanges() {
        assertEquals("+2 \u22121 lines, 2 changes", computeDiff("a\nb\nc\nd\ne\nf\ng\nh", "a\nB\nc\nd\ne\nf\ng\nh\ni").summary)
        assertEquals(null, computeDiff("a", "a").summary)
    }

    @Test
    fun unifiedDiffIsGitApplyCompatible() {
        val expected = "--- a/text\n+++ b/text\n@@ -1,3 +1,3 @@\n a\n-b\n+X\n c\n"
        assertEquals(expected, unifiedDiff("a\nb\nc\n", "a\nX\nc\n").first)
        assertEquals("--- a/text\n+++ b/text\n@@ -0,0 +1,1 @@\n+x\n", unifiedDiff("", "x\n").first)
        assertEquals("", unifiedDiff("same\n", "same\n").first)
        assertEquals("", unifiedDiff("A \n", "a\n", DiffOptions(ignoreCase = true, trim = true)).first)
    }
}
