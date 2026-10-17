package com.cir0cuit.devtoys.tools.developer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.regex.Pattern

class RegexEvaluateTest {
    @Test fun listsAllMatchesWithPositions() {
        assertEquals("Match 1 (0-1): a\nMatch 2 (1-2): a\nMatch 3 (2-3): a", regexMatchText("a", "aaa"))
    }

    @Test fun noMatches() {
        assertEquals("No matches", regexMatchText("z", "abc"))
    }

    @Test fun capturingGroupsIncludingUnmatchedOptionalGroup() {
        assertEquals(
            "Match 1 (0-2): ab\n  Group 1 (0-1): a\n  Group 2 (1-2): b\nMatch 2 (3-4): a\n  Group 1 (3-4): a\n  Group 2: null",
            regexMatchText("(a)(b)?", "ab a")
        )
    }

    @Test fun namedGroupsAreLabeledByName() {
        assertEquals("Match 1 (0-3): a-b\n  Group x (0-1): a\n  Group 2 (2-3): b", regexMatchText("(?<x>a)-(?:)(b)", "a-b"))
    }

    @Test fun groupNamesSkipNonCapturingLookaroundAndEscapes() {
        assertEquals(mapOf(2 to "n", 3 to "m"), groupNames("""\((a)(?=x)(?<!y)[(](?<n>b)(?<m>c)"""))
    }

    @Test fun multilineAnchors() {
        assertEquals("Match 1 (2-3): b", regexMatchText("^b$", "a\nb\nc"))
    }

    @Test fun literalFlagQuotesThePattern() {
        assertEquals("Match 1 (1-4): a.b", regexMatchText("a.b", "xa.b axb", Pattern.LITERAL))
    }

    @Test fun invalidPatternReportsError() {
        assertTrue(regexMatchText("(", "x").startsWith("Error:"))
        assertTrue(regexMatchText("[a-", "x").startsWith("Error:"))
        assertTrue(runRegex(RegexMode.MATCH, "(", "x", 0).isError)
        assertTrue(runRegex(RegexMode.REPLACE, "a", "a", 0, "$3").isError)
        assertFalse(runRegex(RegexMode.MATCH, "a", "b", 0).isError)
    }

    @Test fun emptyMatchesAreReportedAtEveryPosition() {
        // Kotlin/Java semantics: an empty match is found before each char and at the end.
        assertEquals("Match 1 (0-0): \nMatch 2 (1-1): \nMatch 3 (2-2): ", regexMatchText("x*", "ab"))
    }

    @Test fun truncatesAfterThousandMatches() {
        val lines = regexMatchText("a", "a".repeat(1001)).lines()
        assertEquals(1001, lines.size)
        assertEquals("Match 1000 (999-1000): a", lines[999])
        assertEquals("... (truncated after 1000 matches)", lines[1000])
    }

    @Test fun exactlyThousandMatchesIsNotTruncated() {
        val out = regexMatchText("a", "a".repeat(1000))
        assertFalse(out.contains("truncated"))
        assertEquals(1000, out.lines().size)
    }

    @Test fun stackOverflowInMatcherDoesNotEscape() {
        // Alternation inside a repetition recurses per character in java.util.regex.
        val out = regexMatchText("(a|b)*", "ab".repeat(200_000))
        assertTrue(out.startsWith("Error:") || out.startsWith("Match 1"))
    }

    @Test fun replaceWithNumberedAndNamedGroups() {
        assertEquals("ada at example.com", runRegex(RegexMode.REPLACE, """(?<u>\w+)@(\S+)""", "ada@example.com", 0, "\${u} at $2").text)
    }

    @Test fun replaceWithMissingGroupExplainsTheFix() {
        assertTrue(runRegex(RegexMode.REPLACE, "a", "a", 0, "$3").text.startsWith("Error: No group 3"))
    }

    @Test fun splitKeepsTrailingEmptyParts() {
        assertEquals("1: a\n2: b\n3: ", runRegex(RegexMode.SPLIT, ",", "a,b,", 0).text)
    }

    @Test fun copyAsCode() {
        val f = Pattern.CASE_INSENSITIVE or Pattern.MULTILINE
        assertEquals("Regex(\"\"\"\\d+/\\w\"\"\", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))", regexAsCode("""\d+/\w""", f, "Kotlin"))
        assertEquals("Pattern.compile(\"\\\\d+\\\"\", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE)", regexAsCode("""\d+"""", f, "Java"))
        assertEquals("""/\d+\/x/gim""", regexAsCode("""\d+/x""", f, "JavaScript"))
        assertEquals("""re.compile(r'(?P<n>\d)(?<=a)\'', re.I | re.M)""", regexAsCode("""(?<n>\d)(?<=a)'""", f, "Python"))
        assertEquals("regexp.MustCompile(`(?im)\\d`)", regexAsCode("""\d""", f, "Go"))
        assertEquals("""grep -oPi 'it'\''s'""", regexAsCode("it's", f, "grep"))
        assertEquals("""Regex(""${'"'}a${'$'}{'${'$'}'}b""${'"'})""", regexAsCode("a\$b", 0, "Kotlin"))
        assertEquals("""/a\.b/g""", regexAsCode("a.b", Pattern.LITERAL, "JavaScript"))
        assertEquals("# no equivalent for: unix lines\nre.compile(r'a')", regexAsCode("a", Pattern.UNIX_LINES, "Python"))
    }
}
