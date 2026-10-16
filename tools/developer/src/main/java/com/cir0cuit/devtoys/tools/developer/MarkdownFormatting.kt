package com.cir0cuit.devtoys.tools.developer

import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer

// Pure Markdown editing helpers behind MarkdownTool's toolbar and HTML export.

/** Text plus a selection (start <= end), independent of Compose so it can be unit tested. */
internal data class TextEdit(val text: String, val start: Int, val end: Int)

/**
 * Wraps the selection in [before]/[after] and keeps the inner text selected. Unwraps it when the markers are
 * already around the selection. With no selection, inserts [placeholder] and selects it.
 */
internal fun wrapSelection(e: TextEdit, before: String, after: String, placeholder: String): TextEdit {
    val s = minOf(e.start, e.end).coerceIn(0, e.text.length)
    val en = maxOf(e.start, e.end).coerceIn(0, e.text.length)
    val t = e.text
    if (s >= before.length && t.startsWith(before, s - before.length) && t.startsWith(after, en)) {
        val unwrapped = t.substring(0, s - before.length) + t.substring(s, en) + t.substring(en + after.length)
        return TextEdit(unwrapped, s - before.length, en - before.length)
    }
    val inner = t.substring(s, en).ifEmpty { placeholder }
    return TextEdit(t.substring(0, s) + before + inner + after + t.substring(en), s + before.length, s + before.length + inner.length)
}

/** Fenced code block around the selection, on lines of its own. */
internal fun wrapCodeBlock(e: TextEdit): TextEdit {
    val s = minOf(e.start, e.end)
    val lead = if (s == 0 || e.text[s - 1] == '\n') "" else "\n"
    return wrapSelection(e, "$lead```\n", "\n```\n", "code")
}

private val HEADING = Regex("^#{1,6} ")

/**
 * Adds [prefix] to every line the selection touches, or removes it when all of them already have it.
 * Headings replace an existing heading level instead of stacking. The affected lines end up selected.
 */
internal fun toggleLinePrefix(e: TextEdit, prefix: String): TextEdit {
    val t = e.text
    val s = minOf(e.start, e.end).coerceIn(0, t.length)
    val en = maxOf(e.start, e.end).coerceIn(0, t.length)
    val blockStart = t.lastIndexOf('\n', s - 1) + 1
    val blockEnd = t.indexOf('\n', en).let { if (it < 0) t.length else it }
    val lines = t.substring(blockStart, blockEnd).split('\n')
    val heading = HEADING.matches(prefix)
    val changed = if (lines.all { it.startsWith(prefix) }) {
        lines.map { it.removePrefix(prefix) }
    } else {
        lines.map { line -> prefix + if (heading) line.replaceFirst(HEADING, "") else line }
    }.joinToString("\n")
    return TextEdit(t.substring(0, blockStart) + changed + t.substring(blockEnd), blockStart, blockStart + changed.length)
}

/** Replaces the selection with [snippet] on its own line and puts the cursor after it. */
internal fun insertBlock(e: TextEdit, snippet: String): TextEdit {
    val t = e.text
    val s = minOf(e.start, e.end).coerceIn(0, t.length)
    val en = maxOf(e.start, e.end).coerceIn(0, t.length)
    val lead = if (s == 0 || t[s - 1] == '\n') "" else "\n"
    val inserted = "$lead$snippet\n"
    return TextEdit(t.substring(0, s) + inserted + t.substring(en), s + inserted.length, s + inserted.length)
}

private val MD_EXTENSIONS = listOf(TablesExtension.create(), StrikethroughExtension.create())

/** Renders Markdown (GFM tables and strikethrough) to an HTML fragment, or a standalone page with light styling. */
internal fun markdownToHtml(markdown: String, standalone: Boolean): String {
    val body = HtmlRenderer.builder().extensions(MD_EXTENSIONS).build()
        .render(Parser.builder().extensions(MD_EXTENSIONS).build().parse(markdown))
    if (!standalone) return body
    return """
        |<!doctype html>
        |<html>
        |<head>
        |<meta charset="utf-8">
        |<meta name="viewport" content="width=device-width, initial-scale=1">
        |<title>Document</title>
        |<style>
        |body { font-family: system-ui, sans-serif; line-height: 1.6; max-width: 46em; margin: 2em auto; padding: 0 1em; color: #1f2328; }
        |pre, code { font-family: ui-monospace, monospace; background: #f3f4f6; border-radius: 4px; }
        |code { padding: 0.1em 0.3em; } pre { padding: 1em; overflow-x: auto; } pre code { padding: 0; }
        |blockquote { margin: 0; padding-left: 1em; border-left: 4px solid #d0d7de; color: #59636e; }
        |table { border-collapse: collapse; } th, td { border: 1px solid #d0d7de; padding: 0.4em 0.8em; }
        |img { max-width: 100%; }
        |</style>
        |</head>
        |<body>
        |""".trimMargin() + body + "</body>\n</html>\n"
}
