package com.cir0cuit.devtoys.tools.developer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RegexEvaluateTest {
    private val tool = RegexTesterTool()

    @Test fun listsAllMatches() {
        assertEquals("Match 1: a\nMatch 2: a\nMatch 3: a", tool.evaluate("a", "aaa"))
    }

    @Test fun noMatches() {
        assertEquals("No matches", tool.evaluate("z", "abc"))
    }

    @Test fun capturingGroupsIncludingUnmatchedOptionalGroup() {
        assertEquals(
            "Match 1: ab\n  Group 1: a\n  Group 2: b\nMatch 2: a\n  Group 1: a\n  Group 2: null",
            tool.evaluate("(a)(b)?", "ab a")
        )
    }

    @Test fun multilineAnchors() {
        assertEquals("Match 1: b", tool.evaluate("^b$", "a\nb\nc"))
    }

    @Test fun invalidPatternReportsError() {
        assertTrue(tool.evaluate("(", "x").startsWith("Error:"))
        assertTrue(tool.evaluate("[a-", "x").startsWith("Error:"))
    }

    @Test fun emptyMatchesAreReportedAtEveryPosition() {
        // Kotlin/Java semantics: an empty match is found before each char and at the end.
        assertEquals("Match 1: \nMatch 2: \nMatch 3: ", tool.evaluate("x*", "ab"))
    }

    @Test fun truncatesAfterThousandMatches() {
        val out = tool.evaluate("a", "a".repeat(1001))
        val lines = out.lines()
        assertEquals(1001, lines.size)
        assertEquals("Match 1000: a", lines[999])
        assertEquals("... (truncated after 1000 matches)", lines[1000])
    }

    @Test fun exactlyThousandMatchesIsNotTruncated() {
        val out = tool.evaluate("a", "a".repeat(1000))
        assertFalse(out.contains("truncated"))
        assertEquals(1000, out.lines().size)
    }

    @Test fun stackOverflowInMatcherDoesNotEscape() {
        // Alternation inside a repetition recurses per character in java.util.regex.
        val out = tool.evaluate("(a|b)*", "ab".repeat(200_000))
        assertTrue(out.startsWith("Error:") || out.startsWith("Match 1:"))
    }
}
