package com.cir0cuit.devtoys.tools.text

import org.junit.Assert.assertEquals
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
}
