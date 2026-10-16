package com.cir0cuit.devtoys.tools.data
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridOn

import androidx.compose.material3.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*

class CsvViewerTool : Tool {
    override val descriptor = ToolDescriptor("csv-viewer", com.cir0cuit.devtoys.tools.base.R.string.tool_csv_viewer_title, com.cir0cuit.devtoys.tools.base.R.string.tool_csv_viewer_desc, Icons.Default.GridOn, ToolCategory.CONVERTERS, listOf("csv", "table", "viewer", "html", "markdown"))
    @Composable override fun Content(modifier: Modifier) {
        var input by rememberSaveable { mutableStateOf("""id,name,age
1,Alice,30
2,Bob,25""".trimIndent()) }
        val context = androidx.compose.ui.platform.LocalContext.current
        fun save(uri: android.net.Uri?, content: () -> String) {
            if (uri == null) return
            try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(content().toByteArray(Charsets.UTF_8))
                } ?: throw java.io.IOException("Cannot open output stream")
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Save failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        val saveMdLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/markdown")
        ) { uri: android.net.Uri? ->
            save(uri) { toMarkdown(parseCsv(input)) }
        }
        val saveHtmlLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/html")
        ) { uri: android.net.Uri? ->
            save(uri) { toHtmlDocument(toHtmlTable(parseCsv(input))) }
        }
        ToolScaffold("CSV Viewer & Converter", modifier = modifier, scrollable = false) {
            MonoTextField(input, { input = it }, "CSV Input", minLines = 4, maxLines = 8)
            Spacer(modifier = Modifier.height(16.dp))
            val rows = remember(input) { parseCsv(input) }
            if (rows.isNotEmpty()) {
                val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                val mdString = remember(rows) { toMarkdown(rows) }
                val htmlString = remember(rows) { toHtmlTable(rows) }
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(mdString))
                        }, modifier = Modifier.weight(1f)) {
                            Text("Copy Markdown")
                        }
                        Button(onClick = {
                            saveMdLauncher.launch("table.md")
                        }, modifier = Modifier.weight(1f)) {
                            Text("Save Markdown")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(htmlString))
                        }, modifier = Modifier.weight(1f)) {
                            Text("Copy HTML")
                        }
                        Button(onClick = {
                            saveHtmlLauncher.launch("table.html")
                        }, modifier = Modifier.weight(1f)) {
                            Text("Save HTML")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    items(rows.size) { idx ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            rows[idx].forEach { col ->
                                Text(col, modifier = Modifier.weight(1f).padding(4.dp))
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
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
