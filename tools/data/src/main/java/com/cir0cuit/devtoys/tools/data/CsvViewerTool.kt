package com.cir0cuit.devtoys.tools.data

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.cir0cuit.devtoys.core.designsystem.CodeTextStyle
import com.cir0cuit.devtoys.core.designsystem.LocalSlabColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*
import java.io.IOException

class CsvViewerTool : Tool {
    override val descriptor = ToolDescriptor("csv-viewer", "CSV Viewer & Converter", "View CSV as a table, export Markdown or HTML", Icons.Default.GridOn, ToolCategory.CONVERTERS, listOf("csv", "table", "viewer", "html", "markdown"))
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable override fun Content(modifier: Modifier) {
        var input by rememberSaveable { mutableStateOf("id,name,age\n1,Alice,30\n2,Bob,25") }
        val context = LocalContext.current
        fun save(uri: Uri?, content: () -> String) {
            if (uri == null) return
            try {
                context.contentResolver.openOutputStream(uri)?.use { it.write(content().toByteArray(Charsets.UTF_8)) }
                    ?: throw IOException("it could not be opened for writing")
            } catch (e: Exception) {
                Toast.makeText(context, "Couldn't save the file: ${e.message}. Try another location.", Toast.LENGTH_SHORT).show()
            }
        }
        val saveMdLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
            save(uri) { toMarkdown(parseCsv(input)) }
        }
        val saveHtmlLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/html")) { uri ->
            save(uri) { toHtmlDocument(toHtmlTable(parseCsv(input))) }
        }
        val clipboardManager = LocalClipboardManager.current
        var html by rememberSaveable { mutableStateOf(false) }
        ToolScaffold(descriptor.title, modifier = modifier, scrollable = false) {
            PasteableInput(input, { input = it }, "CSV input", minLines = 4, maxLines = 8)
            val rows = remember(input) { parseCsv(input) }
            if (rows.isEmpty()) {
                Text("Enter CSV with a header row to see it as a table.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@ToolScaffold
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
                    listOf("Markdown", "HTML").forEachIndexed { i, label ->
                        SegmentedButton(
                            selected = html == (i == 1),
                            onClick = { html = i == 1 },
                            shape = SegmentedButtonDefaults.itemShape(i, 2)
                        ) { Text(label, maxLines = 1) }
                    }
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { clipboardManager.setText(AnnotatedString(if (html) toHtmlTable(rows) else toMarkdown(rows))) }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = if (html) "Copy HTML" else "Copy Markdown")
                }
                IconButton(onClick = { if (html) saveHtmlLauncher.launch("table.html") else saveMdLauncher.launch("table.md") }) {
                    Icon(Icons.Default.Save, contentDescription = if (html) "Save HTML file" else "Save Markdown file")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            val columns = rows.maxOf { it.size }
            Text(
                "${rows.size - 1} data rows, $columns columns",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Fixed per-column widths from the longest cell, so every row lines up under one shared horizontal scroll.
            val widths = remember(rows) {
                (0 until columns).map { c -> (rows.maxOf { it.getOrNull(c)?.length ?: 0 }.coerceIn(3, 24) * 9 + 24).dp }
            }
            val hScroll = rememberScrollState()
            Surface(color = LocalSlabColor.current, shape = MaterialTheme.shapes.medium, modifier = Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                    itemsIndexed(rows) { index, row ->
                        Row(Modifier.horizontalScroll(hScroll).padding(horizontal = 4.dp)) {
                            widths.forEachIndexed { c, w ->
                                Text(
                                    row.getOrNull(c) ?: "",
                                    style = if (index == 0) CodeTextStyle.copy(fontWeight = FontWeight.SemiBold) else CodeTextStyle,
                                    color = if (index == 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.width(w).padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                        if (index == 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

internal fun parseCsv(text: String): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    var row = mutableListOf<String>()
    val field = StringBuilder()
    var inQuotes = false
    var fieldStarted = false
    var i = 0
    while (i < text.length) {
        val c = text[i]
        if (inQuotes) {
            if (c == '"') {
                if (i + 1 < text.length && text[i + 1] == '"') {
                    field.append('"')
                    i++
                } else {
                    inQuotes = false
                }
            } else {
                field.append(c)
            }
        } else {
            when (c) {
                '"' -> if (fieldStarted) field.append(c) else { inQuotes = true; fieldStarted = true }
                ',' -> { row.add(field.toString()); field.clear(); fieldStarted = false }
                '\r' -> {}
                '\n' -> { row.add(field.toString()); field.clear(); fieldStarted = false; rows.add(row); row = mutableListOf() }
                else -> { field.append(c); fieldStarted = true }
            }
        }
        i++
    }
    if (field.isNotEmpty() || row.isNotEmpty()) {
        row.add(field.toString())
        rows.add(row)
    }
    return rows.filterNot { it.size == 1 && it[0].isEmpty() }
}

private val LINE_BREAK = Regex("\r\n|\r|\n")

internal fun mdEscape(s: String) = s.replace("\\", "\\\\").replace("|", "\\|").replace(LINE_BREAK, "<br>")

internal fun htmlEscape(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;")

internal fun toMarkdown(rows: List<List<String>>): String = buildString {
    rows.forEachIndexed { index, cols ->
        append("| " + cols.joinToString(" | ") { mdEscape(it) } + " |\n")
        if (index == 0) {
            append("|" + cols.joinToString("|") { "---" } + "|\n")
        }
    }
}

internal fun toHtmlTable(rows: List<List<String>>): String = buildString {
    append("<table>\n")
    rows.forEachIndexed { index, cols ->
        val tag = if (index == 0) "th" else "td"
        append("  <tr>")
        cols.forEach { append("<$tag>${htmlEscape(it)}</$tag>") }
        append("</tr>\n")
    }
    append("</table>")
}

private fun toHtmlDocument(table: String): String =
    "<!DOCTYPE html>\n<html><head><meta charset=\"utf-8\"><title>table</title></head><body>\n" + table + "\n</body></html>\n"
