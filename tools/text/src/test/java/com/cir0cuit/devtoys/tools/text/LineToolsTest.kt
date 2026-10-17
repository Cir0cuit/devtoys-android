package com.cir0cuit.devtoys.tools.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LineToolsTest {

    @Test
    fun emptyInputGivesEmptyOutputForEveryOperation() {
        for (op in listOf("Sort", "Sort Desc", "Dedupe", "Reverse", "Count")) {
            assertEquals(op, "", applyLineOperation("", op))
        }
    }

    @Test
    fun sortWithoutTrailingNewline() {
        assertEquals("a\nb\nc", applyLineOperation("c\na\nb", "Sort"))
    }

    @Test
    fun sortKeepsTrailingNewlineAtEndInsteadOfSortingAnEmptyLineToTop() {
        // POSIX: the final "\n" terminates the last line, it does not start an empty one.
        assertEquals("a\nb\nc\n", applyLineOperation("c\na\nb\n", "Sort"))
    }

    @Test
    fun sortKeepsGenuineBlankLines() {
        assertEquals("\na\nb\n", applyLineOperation("b\n\na\n", "Sort"))
    }

    @Test
    fun sortDescending() {
        assertEquals("c\nb\na\n", applyLineOperation("a\nc\nb\n", "Sort Desc"))
    }

    @Test
    fun sortIsOrdinalCodePointOrder() {
        assertEquals("B\na\nb", applyLineOperation("b\na\nB", "Sort"))
    }

    @Test
    fun crlfInputKeepsCrlfSeparatorsAndTerminator() {
        assertEquals("a\r\nb\r\nc\r\n", applyLineOperation("c\r\nb\r\na\r\n", "Sort"))
        assertEquals("a\r\nb", applyLineOperation("b\r\na", "Sort"))
    }

    @Test
    fun crlfInputDoesNotLeaveStrayCarriageReturnsOnLines() {
        val out = applyLineOperation("b\r\na\r\n", "Reverse")
        assertEquals("a\r\nb\r\n", out)
    }

    @Test
    fun dedupeKeepsFirstOccurrenceOrderAndTrailingNewline() {
        assertEquals("b\na\nc\n", applyLineOperation("b\na\nb\nc\na\n", "Dedupe"))
    }

    @Test
    fun dedupeDoesNotTreatTrailingNewlineAsAnExtraEmptyLine() {
        assertEquals("x\n", applyLineOperation("x\nx\n", "Dedupe"))
    }

    @Test
    fun reverseKeepsTrailingNewlineAtEnd() {
        assertEquals("3\n2\n1\n", applyLineOperation("1\n2\n3\n", "Reverse"))
        assertEquals("3\n2\n1", applyLineOperation("1\n2\n3", "Reverse"))
    }

    @Test
    fun countDoesNotCountTerminatingNewlineAsALine() {
        assertEquals("Lines: 3", applyLineOperation("a\nb\nc\n", "Count"))
        assertEquals("Lines: 3", applyLineOperation("a\nb\nc", "Count"))
        assertEquals("Lines: 2", applyLineOperation("a\r\nb\r\n", "Count"))
        assertEquals("Lines: 1", applyLineOperation("single", "Count"))
    }

    @Test
    fun countIncludesBlankLinesInTheMiddle() {
        assertEquals("Lines: 3", applyLineOperation("a\n\nb\n", "Count"))
    }

    @Test
    fun singleNewlineIsOneEmptyLine() {
        assertEquals("\n", applyLineOperation("\n", "Sort"))
        assertEquals("Lines: 1", applyLineOperation("\n", "Count"))
    }

    @Test
    fun unknownOperationReturnsInputUnchanged() {
        assertEquals("b\na\n", applyLineOperation("b\na\n", "Nope"))
    }

    @Test
    fun naturalCaseInsensitiveAndLengthSorts() {
        assertEquals("file2\nfile10\nfile010b", applyLineOperation("file10\nfile010b\nfile2", "Natural"))
        assertEquals("a\nB\nc", applyLineOperation("c\nB\na", "Sort Ci"))
        assertEquals("b\naa\nccc\n", applyLineOperation("ccc\naa\nb\n", "Sort Length"))
    }

    @Test
    fun shuffleKeepsTheSameLines() {
        val out = applyLineOperation("1\n2\n3\n4\n", "Shuffle")
        assertEquals(listOf("1", "2", "3", "4"), out.removeSuffix("\n").lines().sorted())
        assertTrue(out.endsWith("\n"))
    }

    @Test
    fun dedupeVariantsAndCounts() {
        assertEquals("Apple\npear", applyLineOperation("Apple\napple\npear", "Dedupe Ci"))
        assertEquals("a\nb", applyLineOperation("a\nb\na\nc\nb", "Duplicates"))
        assertEquals("3 a\n1 b", applyLineOperation("a\nb\na\na", "Count Each"))
        assertEquals("12 x\n 1 y", applyLineOperation((List(12) { "x" } + "y").joinToString("\n"), "Count Each"))
    }

    @Test
    fun trimBlankAndNumbering() {
        assertEquals("a\nb\n", applyLineOperation("a\n  \n\nb\n", "Remove Blank"))
        assertEquals("a\nb", applyLineOperation("  a \n\tb", "Trim"))
        assertEquals("  a\nb", applyLineOperation("  a  \nb\t", "Trim End"))
        val numbered = applyLineOperation((1..10).joinToString("\n") { "l$it" }, "Number")
        assertTrue(numbered, numbered.startsWith(" 1. l1\n"))
        assertTrue(numbered, numbered.endsWith("10. l10"))
        assertEquals((1..10).joinToString("\n") { "l$it" }, applyLineOperation(numbered, "Unnumber"))
    }

    @Test
    fun filterPlainRegexIgnoreCaseAndInvert() {
        val log = "E/App: boom\nI/App: ok\ne/Net: fail\n"
        assertEquals("E/App: boom\n", applyLineOperation(log, "Filter", LineOptions(pattern = "E/")))
        assertEquals("E/App: boom\ne/Net: fail\n", applyLineOperation(log, "Filter", LineOptions(pattern = "e/", ignoreCase = true)))
        assertEquals("I/App: ok\n", applyLineOperation(log, "Filter", LineOptions(pattern = "^[Ee]/", regex = true, invert = true)))
        assertEquals("", applyLineOperation(log, "Filter", LineOptions(pattern = "zzz")))
    }

    @Test
    fun filterResultCountsAndInvalidRegexError() {
        val r = computeLines("a\nb\nab\n", "Filter", LineOptions(pattern = "a"))
        assertEquals(2, r.outputLines)
        assertEquals(3, r.inputLines)
        val bad = computeLines("a", "Filter", LineOptions(pattern = "(", regex = true))
        assertTrue(bad.error!!.startsWith("Invalid regex"))
        for (p in listOf("[a-", "[a-z", "[]", "[^]a", "x[\\]")) {
            val msg = computeLines("a", "Filter", LineOptions(pattern = p, regex = true)).error
            assertTrue("$p: $msg", msg!!.startsWith("Invalid regex: Missing ] to close the character class."))
        }
        val reversed = computeLines("a", "Filter", LineOptions(pattern = "[z-a]", regex = true)).error!!
        assertTrue(reversed, !reversed.contains("Missing ]"))
    }

    @Test
    fun joinWithQuoteWrapAndEscapedSeparator() {
        assertEquals("('a', 'b')", applyLineOperation("a\n b \n\n", "Join", LineOptions(quote = "'", wrap = "()")))
        assertEquals("[\"a\",\"b\"]", applyLineOperation("a\nb", "Join", LineOptions(separator = ",", quote = "\"", wrap = "[]")))
        assertEquals("a\tb", applyLineOperation("a\nb", "Join", LineOptions(separator = "\t")))
    }

    @Test
    fun splitByDelimiterWhitespaceAndRegex() {
        assertEquals("a\nb\nc", applyLineOperation("a, b,,c", "Split"))
        assertEquals("a\nb\nc", applyLineOperation("a  b\tc", "Split", LineOptions(delimiter = "")))
        assertEquals("a\nb\nc", applyLineOperation("a1b22c", "Split", LineOptions(delimiter = """\d+""", regex = true)))
        assertEquals("a.b", applyLineOperation("a.b", "Split", LineOptions(delimiter = ";")))
    }

    @Test
    fun lineEndingAndWhitespaceFixes() {
        assertEquals("a\nb\n", applyLineOperation("a\r\nb\r\n", "To LF"))
        assertEquals("a\r\nb\r\n", applyLineOperation("a\nb\r\n", "To CRLF"))
        assertEquals("x", applyLineOperation("\uFEFFx", "Remove BOM"))
        assertEquals("a   b\n    c", applyLineOperation("a\tb\n\tc", "Tabs To Spaces"))
        assertEquals("a b\n  c", applyLineOperation("a\tb\n\tc", "Tabs To Spaces", LineOptions(tabWidth = 2)))
        assertEquals("\t\t  x\ny", applyLineOperation("          x\ny", "Spaces To Tabs"))
    }

    @Test
    fun describesLineEndingsAndBom() {
        assertEquals("", describeLineEnding("a\nb\n"))
        assertEquals("CRLF", describeLineEnding("a\r\nb"))
        assertEquals("Mixed line endings, BOM", describeLineEnding("\uFEFFa\r\nb\n"))
    }
}
