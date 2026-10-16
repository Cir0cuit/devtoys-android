package com.cir0cuit.devtoys.tools.developer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownFootnotesTest {
    private val tool = MarkdownTool()
    private fun render(s: String) = tool.renderFakeFootnotes(s)

    @Test fun referenceAndDefinition() {
        assertEquals(
            "Text<sup><a href=\"#fn1\" id=\"ref1\">1</a></sup>\n\n" +
                "<p><sup id=\"fn1\">1.</sup> Note <a href=\"#ref1\">↩</a></p>",
            render("Text[^1]\n\n[^1]: Note")
        )
    }

    @Test fun plainTextUnchanged() {
        val s = "# Title\n\nplain *text* with a [link](http://x) and [ref][1]\n\n[1]: http://y"
        assertEquals(s, render(s))
    }

    @Test fun footnoteInsideFencedCodeIsLiteral() {
        val s = "```\nx[^1]\n[^1]: def\n```\n"
        assertEquals(s, render(s))
    }

    @Test fun footnoteInsideTildeFenceIsLiteral() {
        val s = "~~~md\n[^a]\n~~~"
        assertEquals(s, render(s))
    }

    @Test fun unclosedFenceRunsToEndOfDocument() {
        // CommonMark 4.5: an unclosed fenced block extends to the end of the document.
        val s = "intro\n```\n[^1]\nmore"
        assertEquals(s, render(s))
    }

    @Test fun footnoteInsideInlineCodeIsLiteral() {
        assertEquals("use `[^1]` syntax", render("use `[^1]` syntax"))
        assertEquals("use ``a`[^1]`` syntax", render("use ``a`[^1]`` syntax"))
    }

    @Test fun footnoteOutsideCodeStillRenderedWhenCodePresent() {
        val out = render("`code` and[^n]")
        assertEquals("`code` and<sup><a href=\"#fnn\" id=\"refn\">n</a></sup>", out)
    }

    @Test fun footnoteInsideIndentedCodeBlockIsLiteral() {
        // CommonMark 4.4: indented code block content is literal text.
        val s = "para\n\n    x[^1]\n"
        assertEquals(s, render(s))
    }

    @Test fun footnoteIdIsHtmlEscaped() {
        val out = render("x[^<b\"&>]")
        assertFalse(out.contains("<b"))
        assertTrue(out.contains("&lt;b&quot;&amp;&gt;"))
    }

    @Test fun nulCannotForgePlaceholders() {
        // A raw "\u0000<n>\u0000" in user input must not be expanded into stashed code content.
        val out = render("`secret`\u00000\u0000")
        assertFalse(out.contains('\u0000'))
        assertEquals(1, Regex("`secret`").findAll(out).count())
    }

    @Test fun nulIsNotPassedThrough() {
        assertFalse(render("a\u0000b").contains('\u0000'))
    }
}
