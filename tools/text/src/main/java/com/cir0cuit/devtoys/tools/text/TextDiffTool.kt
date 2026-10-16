package com.cir0cuit.devtoys.tools.text

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Difference
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.designsystem.CodeTextStyle
import com.cir0cuit.devtoys.core.designsystem.LocalSlabColor
import com.cir0cuit.devtoys.core.ui.NumberedTextField
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolCategory
import com.cir0cuit.devtoys.tools.base.ToolDescriptor
import com.github.difflib.DiffUtils
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
    override val descriptor = ToolDescriptor(
        "text-diff", "Text Comparer", "Compare two texts line by line",
        Icons.Default.Difference, ToolCategory.TEXT, listOf("diff", "compare")
    )

    @Composable override fun Content(modifier: Modifier) {
        var tabIndex by rememberSaveable { mutableStateOf(0) }
        var oldText by rememberSaveable(saver = boundedTextSaver) { mutableStateOf("") }
        var newText by rememberSaveable(saver = boundedTextSaver) { mutableStateOf("") }

        var output by remember { mutableStateOf(AnnotatedString("")) }
        var diffInputs by remember { mutableStateOf<Pair<String, String>?>(null) }
        val diffReady = diffInputs == (oldText to newText)
        LaunchedEffect(oldText, newText, tabIndex) {
            if (tabIndex != 2 || diffReady) return@LaunchedEffect
            output = withContext(Dispatchers.Default) { buildDiff(oldText, newText) }
            diffInputs = oldText to newText
        }

        ToolScaffold(descriptor.title, modifier = modifier, scrollable = false) {
            TabRow(selectedTabIndex = tabIndex) {
                Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Old text") })
                Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("New text") })
                Tab(selected = tabIndex == 2, onClick = { tabIndex = 2 }, text = { Text("Differences") })
            }
            Spacer(modifier = Modifier.height(8.dp))
            val paneModifier = Modifier.weight(1f).fillMaxWidth()
            when (tabIndex) {
                0 -> {
                    PaneHeader("Old text", onPaste = { oldText = it })
                    NumberedTextField(oldText, { oldText = it }, modifier = paneModifier)
                }
                1 -> {
                    PaneHeader("New text", onPaste = { newText = it })
                    NumberedTextField(newText, { newText = it }, modifier = paneModifier)
                }
                else -> {
                    PaneHeader("Differences", copyText = if (diffReady) output.text else null)
                    Surface(modifier = paneModifier, color = LocalSlabColor.current, shape = MaterialTheme.shapes.medium) {
                        Box(modifier = Modifier.padding(16.dp).horizontalScroll(rememberScrollState()).verticalScroll(rememberScrollState())) {
                            Text(if (diffReady) output else AnnotatedString("Computing…"), style = CodeTextStyle)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
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
            append("Could not compare the texts: ${e.message}")
        }
    }
}
