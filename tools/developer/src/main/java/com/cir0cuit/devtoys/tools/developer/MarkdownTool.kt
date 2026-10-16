package com.cir0cuit.devtoys.tools.developer

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.provider.OpenableColumns
import android.util.DisplayMetrics
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.cir0cuit.devtoys.core.designsystem.CodeTextStyle
import com.cir0cuit.devtoys.core.designsystem.LocalSlabColor
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.ImagesPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_OPEN_BYTES = 256 * 1024

private val CHEATS = listOf(
    "Heading 1" to "# Heading 1",
    "Heading 2" to "## Heading 2",
    "Heading 3" to "### Heading 3",
    "Bold" to "**bold text**",
    "Italic" to "*italicized text*",
    "Strikethrough" to "~~crossed out~~",
    "Blockquote" to "> blockquote",
    "Ordered list" to "1. First item\n2. Second item",
    "Unordered list" to "- First item\n- Second item",
    "Task list" to "- [x] Done\n- [ ] Todo",
    "Code" to "`code`",
    "Code block" to "```\ncode block\n```",
    "Horizontal rule" to "---",
    "Link" to "[title](https://www.example.com)",
    "Image" to "![alt text](image.jpg)",
    "Table" to "| Header 1 | Header 2 |\n|----------|----------|\n| Cell 1 | Cell 2 |\n| Cell 3 | Cell 4 |",
    "HTML" to "<div>Raw HTML</div>",
    "Footnote" to "Here is a footnote reference,[^1]\n\n[^1]: Here is the footnote."
)

class MarkdownTool : Tool {
    override val descriptor = ToolDescriptor(
        id = "markdown",
        title = "Markdown Editor",
        description = "Write Markdown and preview it as you type",
        icon = Icons.Default.EditNote,
        category = ToolCategory.TEXT,
        keywords = listOf("markdown", "preview", "cheatsheet")
    )

    @Composable override fun Content(modifier: Modifier) {
        var tabIndex by rememberSaveable { mutableStateOf(0) }
        var input by rememberSaveable { mutableStateOf("# Hello World\n\nThis is **Markdown**.") }

        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        var openedUri by rememberSaveable { mutableStateOf<Uri?>(null) }
        var showSaveDialog by remember { mutableStateOf(false) }

        fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

        val openFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                scope.launch {
                    val result = withContext(Dispatchers.IO) { runCatching { readTextFile(context, uri) } }
                    result.onSuccess { text ->
                        input = text
                        openedUri = uri
                        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }
                    }.onFailure { e ->
                        toast("Could not open the file: ${e.message}")
                    }
                }
            }
        }

        val saveMdLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri: Uri? ->
            if (uri != null) {
                val ok = runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(input.toByteArray())
                    } != null
                }.getOrDefault(false)
                toast(if (ok) "Saved" else "Could not save the file. Try another location.")
            }
        }
        val savePdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri: Uri? ->
            if (uri != null) {
                val text = input
                scope.launch {
                    val ok = withContext(Dispatchers.Default) {
                        try { writeHeadlessPdf(context, text, uri) } catch (e: Exception) { false }
                    }
                    toast(if (ok) "Exported PDF" else "Could not export the PDF. Try another location.")
                }
            }
        }

        if (showSaveDialog) {
            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                title = { Text("Save changes") },
                text = { Text("Overwrite the opened file, or save a new copy?") },
                confirmButton = {
                    TextButton(onClick = {
                        showSaveDialog = false
                        openedUri?.let { uri ->
                            try {
                                context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
                                    stream.write(input.toByteArray())
                                }
                                toast("Saved")
                            } catch (e: Exception) {
                                toast("Could not overwrite the file. Use Save as instead.")
                            }
                        }
                    }) { Text("Overwrite") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showSaveDialog = false
                        saveMdLauncher.launch("document.md")
                    }) { Text("Save as") }
                }
            )
        }

        ToolScaffold(descriptor.title, modifier = modifier, scrollable = false) {
            TabRow(selectedTabIndex = tabIndex) {
                Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Code") })
                Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("Preview") })
                Tab(selected = tabIndex == 2, onClick = { tabIndex = 2 }, text = { Text("Cheatsheet") })
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.weight(1f)) {
                when (tabIndex) {
                    0 -> NumberedTextField(input, { input = it }, modifier = Modifier.fillMaxSize())
                    1 -> PreviewMode(input)
                    else -> CheatsheetMode()
                }
            }
            if (tabIndex == 0 || tabIndex == 1) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ActionButton(Icons.Default.FolderOpen, "Open") {
                        openFileLauncher.launch(arrayOf("text/*", "text/markdown", "application/octet-stream"))
                    }
                    ActionButton(Icons.Default.Save, "Save") {
                        if (openedUri != null) showSaveDialog = true else saveMdLauncher.launch("document.md")
                    }
                    Spacer(Modifier.weight(1f))
                    ActionButton(Icons.Default.PictureAsPdf, "Export PDF") { savePdfLauncher.launch("document.pdf") }
                }
            }
        }
    }

    @Composable private fun ActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
        TextButton(onClick = onClick, contentPadding = ButtonDefaults.TextButtonWithIconContentPadding) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text(label, maxLines = 1)
        }
    }

    private fun markwon(context: Context): Markwon = Markwon.builder(context)
        .usePlugin(TablePlugin.create(context))
        .usePlugin(StrikethroughPlugin.create())
        .usePlugin(TaskListPlugin.create(context))
        .usePlugin(HtmlPlugin.create())
        .usePlugin(ImagesPlugin.create())
        .usePlugin(LinkifyPlugin.create())
        .build()

    private fun readTextFile(context: Context, uri: Uri): String {
        val tooLarge = "File is too large. Open files up to ${MAX_OPEN_BYTES / 1024} KB."
        val size = context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else -1L
        } ?: -1L
        if (size > MAX_OPEN_BYTES) error(tooLarge)
        val stream = context.contentResolver.openInputStream(uri) ?: error("The file could not be read.")
        val bytes = stream.use { s ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val n = s.read(buffer)
                if (n < 0) break
                out.write(buffer, 0, n)
                if (out.size() > MAX_OPEN_BYTES) error(tooLarge)
            }
            out.toByteArray()
        }
        return String(bytes, Charsets.UTF_8)
    }

    private fun writeHeadlessPdf(context: Context, markdownText: String, uri: Uri): Boolean {
        val config = Configuration(context.resources.configuration)
        config.densityDpi = DisplayMetrics.DENSITY_DEFAULT
        config.fontScale = 1.0f
        val pdfContext = context.createConfigurationContext(config)

        val textView = TextView(pdfContext)
        textView.setTextColor(Color.BLACK)
        textView.setBackgroundColor(Color.WHITE)

        val paddingPx = 32
        textView.setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
        textView.textSize = 12f // 12sp scales exactly to 12pt due to 1.0 density

        markwon(pdfContext).setMarkdown(textView, renderFakeFootnotes(markdownText))

        val a4WidthPoints = 595
        textView.measure(
            View.MeasureSpec.makeMeasureSpec(a4WidthPoints, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        textView.layout(0, 0, a4WidthPoints, textView.measuredHeight)

        val a4HeightPoints = 842
        val textLayout = textView.layout ?: return false
        val layoutTop = textView.totalPaddingTop
        val usableHeight = a4HeightPoints - 2 * paddingPx
        val contentHeight = textLayout.height

        val pdfDocument = PdfDocument()
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
                val pageInfo = PdfDocument.PageInfo.Builder(a4WidthPoints, a4HeightPoints, pageNumber).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                canvas.drawColor(Color.WHITE)
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

    @Composable private fun PreviewMode(input: String) {
        val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
        Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            AndroidView(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                factory = { context -> TextView(context).apply { setTextColor(textColor) } },
                update = { textView ->
                    textView.setTextColor(textColor)
                    markwon(textView.context).setMarkdown(textView, renderFakeFootnotes(input))
                }
            )
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

    @Composable private fun CheatsheetMode() {
        val clipboardManager = LocalClipboardManager.current
        Surface(color = LocalSlabColor.current, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxSize()) {
            LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                items(CHEATS) { cheat ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(cheat.first, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(cheat.second, style = CodeTextStyle, maxLines = 4, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { clipboardManager.setText(AnnotatedString(cheat.second)) }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy ${cheat.first}", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}
