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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_OPEN_BYTES = 256 * 1024

class MarkdownTool : Tool {
    override val descriptor = ToolDescriptor("markdown", com.cir0cuit.devtoys.tools.base.R.string.tool_markdown_title, com.cir0cuit.devtoys.tools.base.R.string.tool_markdown_desc, Icons.Default.EditNote, ToolCategory.TEXT, listOf("markdown", "preview", "cheatsheet"))
    
    @Composable override fun Content(modifier: Modifier) {
        var tabIndex by rememberSaveable { mutableStateOf(0) }
        var input by rememberSaveable { mutableStateOf("# Hello World\n\nThis is **Markdown**.") }
        
        val context = androidx.compose.ui.platform.LocalContext.current
        val scope = rememberCoroutineScope()
        var openedUri by rememberSaveable { mutableStateOf<android.net.Uri?>(null) }
        var showSaveDialog by remember { mutableStateOf(false) }

        val openFileLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
        ) { uri: android.net.Uri? ->
            if (uri != null) {
                scope.launch {
                    val result = withContext(Dispatchers.IO) { runCatching { readTextFile(context, uri) } }
                    result.onSuccess { text ->
                        input = text
                        openedUri = uri
                        val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }
                    }.onFailure { e ->
                        android.widget.Toast.makeText(context, "Could not open file: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        val saveMdLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/markdown")
        ) { uri: android.net.Uri? ->
            if (uri != null) {
                val ok = runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(input.toByteArray())
                    } != null
                }.getOrDefault(false)
                android.widget.Toast.makeText(context, if (ok) "Saved Markdown" else "Error saving file", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        val savePdfLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/pdf")
        ) { uri: android.net.Uri? ->
            if (uri != null) {
                val text = input
                scope.launch {
                    val ok = withContext(Dispatchers.Default) { saveHeadlessPdf(context, text, uri) }
                    android.widget.Toast.makeText(context, if (ok) "Exported PDF" else "PDF export failed", android.widget.Toast.LENGTH_SHORT).show()
                }
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
                        openFileLauncher.launch(arrayOf("text/*", "text/markdown", "application/octet-stream"))
                    }, modifier = Modifier.weight(1f)) {
                        Text("Open")
                    }
                    Button(onClick = {
                        if (openedUri != null) {
                            showSaveDialog = true
                        } else {
                            saveMdLauncher.launch("document.md")
                        }
                    }, modifier = Modifier.weight(1f)) {
                        Text("Save")
                    }
                    Button(onClick = {
                        savePdfLauncher.launch("document.pdf")
                    }, modifier = Modifier.weight(1f)) {
                        Text("Export PDF")
                    }
                }
            }
        }
    }

    private fun readTextFile(context: android.content.Context, uri: android.net.Uri): String {
        val size = context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else -1L
        } ?: -1L
        if (size > MAX_OPEN_BYTES) error("File too large (max ${MAX_OPEN_BYTES / 1024} KB)")
        val stream = context.contentResolver.openInputStream(uri) ?: error("Cannot open file")
        val bytes = stream.use { s ->
            val out = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val n = s.read(buffer)
                if (n < 0) break
                out.write(buffer, 0, n)
                if (out.size() > MAX_OPEN_BYTES) error("File too large (max ${MAX_OPEN_BYTES / 1024} KB)")
            }
            out.toByteArray()
        }
        return String(bytes, Charsets.UTF_8)
    }

    private fun saveHeadlessPdf(context: android.content.Context, markdownText: String, uri: android.net.Uri): Boolean = try {
        writeHeadlessPdf(context, markdownText, uri)
    } catch (e: Exception) {
        false
    }

    private fun writeHeadlessPdf(context: android.content.Context, markdownText: String, uri: android.net.Uri): Boolean {
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
        
        val a4HeightPoints = 842
        val textLayout = textView.layout ?: return false
        val layoutTop = textView.totalPaddingTop
        val usableHeight = a4HeightPoints - 2 * paddingPx
        val contentHeight = textLayout.height

        val pdfDocument = android.graphics.pdf.PdfDocument()
        try {
            var startY = 0
            var pageNumber = 1
            do {
                var endY = startY + usableHeight
                if (endY < contentHeight) {
                    val lineTop = textLayout.getLineTop(textLayout.getLineForVertical(endY))
                    if (lineTop > startY) endY = lineTop
                } else {
                    endY = contentHeight
                }
                val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(a4WidthPoints, a4HeightPoints, pageNumber).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                canvas.drawColor(android.graphics.Color.WHITE)
                canvas.save()
                canvas.clipRect(0, paddingPx, a4WidthPoints, paddingPx + (endY - startY))
                canvas.translate(0f, (paddingPx - layoutTop - startY).toFloat())
                textView.draw(canvas)
                canvas.restore()
                pdfDocument.finishPage(page)
                startY = endY
                pageNumber++
            } while (startY < contentHeight)

            val stream = context.contentResolver.openOutputStream(uri) ?: return false
            stream.use { pdfDocument.writeTo(it) }
            return true
        } finally {
            pdfDocument.close()
        }
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

    internal fun renderFakeFootnotes(markdown: String): String {
        val stash = mutableListOf<String>()
        fun mask(regex: Regex, text: String) = regex.replace(text) { stash.add(it.value); "\u0000${stash.size - 1}\u0000" }
        fun esc(text: String) = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
        var processed = mask(Regex("""(?ms)^ {0,3}(`{3,}|~{3,})[^\n]*\n.*?(^ {0,3}\1[`~]*[ \t]*$|\z)"""), markdown.replace("\u0000", ""))
        // CommonMark 4.4 indented code: 4-space/tab lines after a blank line or at document start.
        // ponytail: also masks indented paragraphs inside list items; needs real block parsing to tell them apart.
        processed = mask(Regex("""(?:\A|\n[ \t]*\n)(?: {4}|\t)[^\n]*(?:\n(?:[ \t]*\n)*(?: {4}|\t)[^\n]*)*"""), processed)
        processed = mask(Regex("""(`+)(?!`).+?(?<!`)\1(?!`)""", RegexOption.DOT_MATCHES_ALL), processed)
        val defRegex = Regex("""(?m)^\[\^([^\]]+)\]:\s*(.+)""")
        processed = defRegex.replace(processed) { matchResult ->
            val id = esc(matchResult.groupValues[1])
            val text = matchResult.groupValues[2]
            "<p><sup id=\"fn$id\">$id.</sup> $text <a href=\"#ref$id\">↩</a></p>"
        }
        val refRegex = Regex("""\[\^([^\]]+)\]""")
        processed = refRegex.replace(processed) { matchResult ->
            val id = esc(matchResult.groupValues[1])
            "<sup><a href=\"#fn$id\" id=\"ref$id\">$id</a></sup>"
        }
        return Regex("\u0000(\\d+)\u0000").replace(processed) { it.groupValues[1].toIntOrNull()?.let(stash::getOrNull) ?: it.value }
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