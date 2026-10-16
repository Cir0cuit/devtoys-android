package com.cir0cuit.devtoys.tools.text
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Difference

import androidx.compose.material3.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*
import com.github.difflib.DiffUtils
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.ui.Alignment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

private const val MAX_SAVED_CHARS = 20_000
private const val MAX_RETAINED_TEXTS = 8

private object RetainedTexts {
    private var nextId = 0L
    // Access-ordered so entries that are re-saved or restored stay newest and are evicted last.
    private val texts = object : LinkedHashMap<Long, String>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, String>?) = size > MAX_RETAINED_TEXTS
    }

    @Synchronized fun put(text: String): Long {
        val existing = texts.entries.firstOrNull { it.value === text }?.key
        if (existing != null) {
            texts[existing]
            return existing
        }
        return nextId++.also { texts[it] = text }
    }

    @Synchronized fun get(id: Long): String = texts[id] ?: ""
}

internal val boundedTextSaver = Saver<MutableState<String>, Any>(
    save = { if (it.value.length <= MAX_SAVED_CHARS) it.value else RetainedTexts.put(it.value) },
    restore = { mutableStateOf(if (it is Long) RetainedTexts.get(it) else it as String) }
)

class TextDiffTool : Tool {
    override val descriptor = ToolDescriptor("text-diff", android.R.string.untitled, android.R.string.untitled, Icons.Default.Difference, ToolCategory.TEXT, listOf("diff", "compare"))

    @Composable override fun Content(modifier: Modifier) {
        val clipboardManager = LocalClipboardManager.current
        var tabIndex by rememberSaveable { mutableStateOf(0) }
        var oldText by rememberSaveable(saver = boundedTextSaver) { mutableStateOf("") }
        var newText by rememberSaveable(saver = boundedTextSaver) { mutableStateOf("") }

        var output by remember { mutableStateOf(AnnotatedString("Computing…")) }
        var diffInputs by remember { mutableStateOf<Pair<String, String>?>(null) }
        val diffReady = diffInputs?.let { it.first == oldText && it.second == newText } == true
        LaunchedEffect(oldText, newText, tabIndex) {
            if (tabIndex != 2 || diffReady) return@LaunchedEffect
            output = AnnotatedString("Computing…")
            output = withContext(Dispatchers.Default) { buildDiff(oldText, newText) }
            diffInputs = oldText to newText
        }

        ToolScaffold("Text Comparer", modifier = modifier, scrollable = false) {
            TabRow(selectedTabIndex = tabIndex) {
                Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Old Text") })
                Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("New Text") })
                Tab(selected = tabIndex == 2, onClick = { tabIndex = 2 }, text = { Text("Differences") })
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (tabIndex == 0) {
                    NumberedTextField(oldText, { oldText = it }, modifier = Modifier.fillMaxSize())
                } else if (tabIndex == 1) {
                    NumberedTextField(newText, { newText = it }, modifier = Modifier.fillMaxSize())
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Box(modifier = Modifier.padding(16.dp).horizontalScroll(rememberScrollState()).verticalScroll(rememberScrollState())) {
                                Text(if (diffReady) output else AnnotatedString("Computing…"), fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                            }
                        }
                        FloatingActionButton(
                            onClick = { if (diffReady) clipboardManager.setText(output) },
                            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                        }
                    }
                }
            }
        }
    }
}

internal fun buildDiff(oldText: String, newText: String): AnnotatedString = buildAnnotatedString {
    if (oldText.isEmpty() && newText.isEmpty()) {
        append("Please enter text to compare.")
    } else {
        try {
            val oldLines = oldText.lines()
            val newLines = newText.lines()
            val patch = DiffUtils.diff(oldLines, newLines)

            if (patch.deltas.isEmpty()) {
                append("Texts are identical.")
            } else {
                val deltas = patch.deltas
                var printed = 0
                var offset = 0
                fun gap() = withStyle(SpanStyle(color = Color.Gray)) { append(" ... |  ... | \n") }
                fun context(from: Int, to: Int) {
                    for (i in from until to) {
                        append(String.format(Locale.ROOT, "%4d | %4d |   %s\n", i + 1, i + offset + 1, oldLines[i]))
                    }
                }

                deltas.forEachIndexed { index, delta ->
                    val start = maxOf(printed, delta.source.position - 3)
                    if (start > printed) gap()
                    context(start, delta.source.position)

                    delta.source.lines.forEachIndexed { i, line ->
                        withStyle(SpanStyle(color = Color(0xFFE53935), background = Color(0x22E53935))) {
                            append(String.format(Locale.ROOT, "%4d | %4s | - %s\n", delta.source.position + i + 1, "", line))
                        }
                    }
                    delta.target.lines.forEachIndexed { i, line ->
                        withStyle(SpanStyle(color = Color(0xFF43A047), background = Color(0x2243A047))) {
                            append(String.format(Locale.ROOT, "%4s | %4d | + %s\n", "", delta.target.position + i + 1, line))
                        }
                    }

                    val after = delta.source.position + delta.source.lines.size
                    offset = delta.target.position + delta.target.lines.size - after
                    val end = minOf(after + 3, deltas.getOrNull(index + 1)?.source?.position ?: oldLines.size)
                    context(after, end)
                    printed = end
                }
                if (printed < oldLines.size) gap()
            }
        } catch (e: Exception) {
            append("Error: ${e.message}")
        }
    }
}
