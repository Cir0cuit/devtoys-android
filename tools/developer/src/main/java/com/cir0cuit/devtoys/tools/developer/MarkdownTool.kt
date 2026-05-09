package com.cir0cuit.devtoys.tools.developer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import android.widget.TextView
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*
import io.noties.markwon.Markwon
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.ImagesPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.font.FontWeight

class MarkdownTool : Tool {
    override val descriptor = ToolDescriptor("markdown", com.cir0cuit.devtoys.tools.base.R.string.tool_markdown_title, com.cir0cuit.devtoys.tools.base.R.string.tool_markdown_desc, Icons.Default.EditNote, ToolCategory.TEXT, listOf("markdown", "preview", "cheatsheet"))
    
    @Composable override fun Content(modifier: Modifier) {
        var tabIndex by rememberSaveable { mutableStateOf(0) }
        var input by rememberSaveable { mutableStateOf("# Hello World\n\nThis is **Markdown**.") }
        
        val context = androidx.compose.ui.platform.LocalContext.current
        var textToSave by remember { mutableStateOf("") }
        var openedUri by remember { mutableStateOf<android.net.Uri?>(null) }
        var showSaveDialog by remember { mutableStateOf(false) }

        val openFileLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
        ) { uri: android.net.Uri? ->
            if (uri != null) {
                openedUri = uri
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    input = stream.bufferedReader().readText()
                }
            }
        }

        val saveMdLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/markdown")
        ) { uri: android.net.Uri? ->
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(textToSave.toByteArray())
                }
                android.widget.Toast.makeText(context, "Saved Markdown", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        val savePdfLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/pdf")
        ) { uri: android.net.Uri? ->
            if (uri != null) {
                saveHeadlessPdf(context, textToSave, uri)
                android.widget.Toast.makeText(context, "Exported PDF", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        if (showSaveDialog) {
            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                title = { Text("Save Markdown") },
                text = { Text("Do you want to overwrite the opened file or save as a new file?") },
                confirmButton = {
                    TextButton(onClick = {
                        showSaveDialog = false
                        openedUri?.let { uri ->
                            try {
                                context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
                                    stream.write(input.toByteArray())
                                }
                                android.widget.Toast.makeText(context, "Overwritten successfully", android.widget.Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Error saving file", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) { Text("Overwrite") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showSaveDialog = false
                        textToSave = input
                        saveMdLauncher.launch("document.md")
                    }) { Text("Save As") }
                }
            )
        }
        
        ToolScaffold("Markdown Previewer & Editor", modifier = modifier, scrollable = false) {
            TabRow(selectedTabIndex = tabIndex) {
                Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Code") })
                Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("Preview") })
                Tab(selected = tabIndex == 2, onClick = { tabIndex = 2 }, icon = { Icon(Icons.Default.MenuBook, contentDescription = "Cheatsheet") })
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (tabIndex == 0) {
                    CodeMode(input = input, onInputChange = { input = it })
                } else if (tabIndex == 1) {
                    PreviewMode(input = input)
                } else {
                    CheatsheetMode()
                }
            }
            if (tabIndex == 0 || tabIndex == 1) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        openFileLauncher.launch(arrayOf("*/*"))
                    }, modifier = Modifier.weight(1f)) {
                        Text("Open")
                    }
                    Button(onClick = {
                        if (openedUri != null) {
                            showSaveDialog = true
                        } else {
                            textToSave = input
                            saveMdLauncher.launch("document.md")
                        }
                    }, modifier = Modifier.weight(1f)) {
                        Text("Save")
                    }
                    Button(onClick = {
                        textToSave = input
                        savePdfLauncher.launch("document.pdf")
                    }, modifier = Modifier.weight(1f)) {
                        Text("Export PDF")
                    }
                }
            }
        }
    }

    private fun saveHeadlessPdf(context: android.content.Context, markdownText: String, uri: android.net.Uri) {
        val config = android.content.res.Configuration(context.resources.configuration)
        config.densityDpi = android.util.DisplayMetrics.DENSITY_DEFAULT
        config.fontScale = 1.0f
        val pdfContext = context.createConfigurationContext(config)

        val textView = TextView(pdfContext)
        textView.setTextColor(android.graphics.Color.BLACK)
        textView.setBackgroundColor(android.graphics.Color.WHITE)
        
        val paddingPx = 32
        textView.setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
        textView.textSize = 12f // 12sp scales exactly to 12pt due to 1.0 density
        
        val markwon = Markwon.builder(pdfContext)
            .usePlugin(TablePlugin.create(pdfContext))
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TaskListPlugin.create(pdfContext))
            .usePlugin(HtmlPlugin.create())
            .usePlugin(ImagesPlugin.create())
            .usePlugin(LinkifyPlugin.create())
            .build()
        markwon.setMarkdown(textView, renderFakeFootnotes(markdownText))

        val a4WidthPoints = 595
        
        textView.measure(
            android.view.View.MeasureSpec.makeMeasureSpec(a4WidthPoints, android.view.View.MeasureSpec.EXACTLY),
            android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
        )
        val heightPx = textView.measuredHeight
        
        textView.layout(0, 0, a4WidthPoints, heightPx)
        
        val pdfDocument = android.graphics.pdf.PdfDocument()
        val pageHeightPoints = heightPx.coerceAtLeast(842)
        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(a4WidthPoints, pageHeightPoints, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        
        page.canvas.drawColor(android.graphics.Color.WHITE)
        textView.draw(page.canvas)
        
        pdfDocument.finishPage(page)
        
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            pdfDocument.writeTo(stream)
        }
        pdfDocument.close()
    }

    @Composable fun CodeMode(input: String, onInputChange: (String) -> Unit) {
        Column(modifier = Modifier.fillMaxSize()) {
            NumberedTextField(input, onInputChange, modifier = Modifier.fillMaxSize())
        }
    }

    @Composable fun PreviewMode(input: String) {
        val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
        val scrollState = androidx.compose.foundation.rememberScrollState()
        
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(scrollState)) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    factory = { context -> 
                        TextView(context).apply { setTextColor(textColor) }
                    },
                    update = { textView ->
                        textView.setTextColor(textColor)
                        val markwon = Markwon.builder(textView.context)
                            .usePlugin(TablePlugin.create(textView.context))
                            .usePlugin(StrikethroughPlugin.create())
                            .usePlugin(TaskListPlugin.create(textView.context))
                            .usePlugin(HtmlPlugin.create())
                            .usePlugin(ImagesPlugin.create())
                            .usePlugin(LinkifyPlugin.create())
                            .build()
                        markwon.setMarkdown(textView, renderFakeFootnotes(input))
                    }
                )
            }
        }
    }

    private fun renderFakeFootnotes(markdown: String): String {
        var processed = markdown
        val defRegex = Regex("""(?m)^\[\^([^\]]+)\]:\s*(.+)""")
        processed = defRegex.replace(processed) { matchResult ->
            val id = matchResult.groupValues[1]
            val text = matchResult.groupValues[2]
            "<p><sup id=\"fn$id\">$id.</sup> $text <a href=\"#ref$id\">↩</a></p>"
        }
        val refRegex = Regex("""\[\^([^\]]+)\]""")
        processed = refRegex.replace(processed) { matchResult ->
            val id = matchResult.groupValues[1]
            "<sup><a href=\"#fn$id\" id=\"ref$id\">$id</a></sup>"
        }
        return processed
    }

    @Composable fun CheatsheetMode() {
        val cheats = listOf(
            "Heading 1" to "# Heading 1",
            "Heading 2" to "## Heading 2",
            "Heading 3" to "### Heading 3",
            "Bold" to "**bold text**",
            "Italic" to "*italicized text*",
            "Strikethrough" to "~~crossed out~~",
            "Blockquote" to "> blockquote",
            "Ordered List" to "1. First item\n2. Second item",
            "Unordered List" to "- First item\n- Second item",
            "Task List" to "- [x] Done\n- [ ] Todo",
            "Code" to "`code`",
            "Code Block" to "```\ncode block\n```",
            "Horizontal Rule" to "---",
            "Link" to "[title](https://www.example.com)",
            "Image" to "![alt text](image.jpg)",
            "Table" to "| Header 1 | Header 2 |\n|----------|----------|\n| Cell 1 | Cell 2 |\n| Cell 3 | Cell 4 |",
            "HTML" to "<div>Raw HTML</div>",
            "Footnote" to "Here is a footnote reference,[^1]\n\n[^1]: Here is the footnote."
        )
        val clipboardManager = LocalClipboardManager.current
        
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(cheats) { cheat ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(cheat.first, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(cheat.second, style = MaterialTheme.typography.bodyMedium)
                        }
                        IconButton(onClick = { clipboardManager.setText(AnnotatedString(cheat.second)) }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                        }
                    }
                }
            }
        }
    }
}