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
        var textToSave by remember { mutableStateOf("") }
        val saveMdLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/markdown")
        ) { uri: android.net.Uri? ->
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(textToSave.toByteArray())
                }
            }
        }
        val saveHtmlLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/html")
        ) { uri: android.net.Uri? ->
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(textToSave.toByteArray())
                }
            }
        }
        ToolScaffold("CSV Viewer & Converter", modifier = modifier, scrollable = false) {
            MonoTextField(input, { input = it }, "CSV Input", minLines=4)
            Spacer(modifier = Modifier.height(16.dp))
            val lines = input.lines().filter { it.isNotEmpty() }
            if (lines.isNotEmpty()) {
                val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                val mdString = buildString {
                    lines.forEachIndexed { index, line ->
                        val cols = line.split(",")
                        append("| " + cols.joinToString(" | ") + " |\n")
                        if (index == 0) {
                            append("|" + cols.joinToString("|") { "---" } + "|\n")
                        }
                    }
                }
                val htmlString = buildString {
                    append("<table>\n")
                    lines.forEachIndexed { index, line ->
                        val cols = line.split(",")
                        if (index == 0) {
                            append("  <tr>")
                            cols.forEach { append("<th>$it</th>") }
                            append("</tr>\n")
                        } else {
                            append("  <tr>")
                            cols.forEach { append("<td>$it</td>") }
                            append("</tr>\n")
                        }
                    }
                    append("</table>")
                }
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(mdString))
                        }, modifier = Modifier.weight(1f)) {
                            Text("Copy Markdown")
                        }
                        Button(onClick = {
                            textToSave = mdString
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
                            textToSave = htmlString
                            saveHtmlLauncher.launch("table.html")
                        }, modifier = Modifier.weight(1f)) {
                            Text("Save HTML")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            val parsedLines = input.lines().filter { it.isNotEmpty() }
            if (lines.isNotEmpty()) {
                LazyColumn {
                    items(lines.size) { idx ->
                        val cols = lines[idx].split(",")
                        Row(modifier = Modifier.fillMaxWidth()) {
                            cols.forEach { col ->
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