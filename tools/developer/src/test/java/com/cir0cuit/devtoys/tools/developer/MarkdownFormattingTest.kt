package com.cir0cuit.devtoys.tools.developer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownFormattingTest {
    @Test fun wrapsAndUnwrapsSelection() {
        val wrapped = wrapSelection(TextEdit("say hi now", 4, 6), "**", "**", "bold")
        assertEquals(TextEdit("say **hi** now", 6, 8), wrapped)
        assertEquals(TextEdit("say hi now", 4, 6), wrapSelection(wrapped, "**", "**", "bold"))
    }

    @Test fun insertsPlaceholderWithoutSelection() {
        assertEquals(TextEdit("a[link text](https://)", 2, 11), wrapSelection(TextEdit("a", 1, 1), "[", "](https://)", "link text"))
    }

    @Test fun codeBlockStartsOnItsOwnLine() {
        assertEquals("x\n```\ncode\n```\n", wrapCodeBlock(TextEdit("x", 1, 1)).text)
    }

    @Test fun togglesLinePrefixOnAllSelectedLines() {
        val listed = toggleLinePrefix(TextEdit("one\ntwo\nthree", 1, 5), "- ")
        assertEquals("- one\n- two\nthree", listed.text)
        assertEquals("one\ntwo\nthree", toggleLinePrefix(listed, "- ").text)
    }

    @Test fun headingReplacesLevel() {
        assertEquals("## Title", toggleLinePrefix(TextEdit("# Title", 3, 3), "## ").text)
        assertEquals("Title", toggleLinePrefix(TextEdit("## Title", 0, 0), "## ").text)
    }

    @Test fun insertsBlockOnNewLine() {
        assertEquals(TextEdit("ab\n---\n", 7, 7), insertBlock(TextEdit("ab", 2, 2), "---"))
    }

    @Test fun rendersHtmlWithGfm() {
        val html = markdownToHtml("# T\n\n~~x~~\n\n| a | b |\n|---|---|\n| 1 | 2 |", standalone = false)
        assertTrue(html, html.contains("<h1>T</h1>"))
        assertTrue(html, html.contains("<del>x</del>"))
        assertTrue(html, html.contains("<table>"))
        val page = markdownToHtml("hi", standalone = true)
        assertTrue(page.startsWith("<!doctype html>") && page.contains("<p>hi</p>") && page.endsWith("</html>\n"))
    }
}
