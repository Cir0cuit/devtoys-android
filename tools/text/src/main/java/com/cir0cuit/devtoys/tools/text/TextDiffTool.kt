package com.cir0cuit.devtoys.tools.text

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Difference
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.designsystem.CodeTextStyle
import com.cir0cuit.devtoys.core.designsystem.LocalSlabColor
import com.cir0cuit.devtoys.core.ui.NumberedTextField
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.core.ui.rememberFileSaver
import com.cir0cuit.devtoys.core.ui.rememberTextFileOpener
import com.cir0cuit.devtoys.core.ui.shareText
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolCategory
import com.cir0cuit.devtoys.tools.base.ToolDescriptor
import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import com.github.difflib.patch.Patch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.function.BiPredicate

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


private val diffModes = listOf("Words", "Chars", "Patch")

class TextDiffTool : Tool {
    override val descriptor = ToolDescriptor(
        "text-diff", "Text Comparer", "Compare two texts line by line",
        Icons.Default.Difference, ToolCategory.TEXT, listOf("diff", "compare", "patch", "unified")
    )

    @OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
    @Composable override fun Content(modifier: Modifier) {
        val context = LocalContext.current
        val clipboard = LocalClipboardManager.current
        var tabIndex by rememberSaveable { mutableStateOf(0) }
        var oldText by rememberSaveable(saver = boundedTextSaver) { mutableStateOf("") }
        var newText by rememberSaveable(saver = boundedTextSaver) { mutableStateOf("") }
        var mode by rememberSaveable { mutableStateOf(0) }
        var ignoreCase by rememberSaveable { mutableStateOf(false) }
        var trim by rememberSaveable { mutableStateOf(false) }
        var ignoreSpace by rememberSaveable { mutableStateOf(false) }
        var ignoreBlank by rememberSaveable { mutableStateOf(false) }
        val options = DiffOptions(ignoreCase, trim, ignoreSpace, ignoreBlank, charLevel = mode == 1)
        val patchMode = mode == 2

        fun loaded(text: String, set: (String) -> Unit) {
            if (text.contains('\u0000')) {
                Toast.makeText(context, "This looks like a binary file. Open a text file instead.", Toast.LENGTH_LONG).show()
            } else {
                set(text)
            }
        }
        val openOld = rememberTextFileOpener { _, text -> loaded(text) { oldText = it } }
        val openNew = rememberTextFileOpener { _, text -> loaded(text) { newText = it } }
        val savePatch = rememberFileSaver("text/x-patch")
        val swap = { val t = oldText; oldText = newText; newText = t }

        var result by remember { mutableStateOf<DiffResult?>(null) }
        var resultKey by remember { mutableStateOf<List<Any>?>(null) }
        val key = listOf(oldText, newText, options, patchMode)
        val ready = resultKey == key
        LaunchedEffect(key, tabIndex) {
            if (tabIndex != 2 || ready) return@LaunchedEffect
            result = withContext(Dispatchers.Default) {
                if (patchMode) unifiedDiffResult(oldText, newText, options) else computeDiff(oldText, newText, options)
            }
            resultKey = key
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
                    InputHeader("Old text", onPaste = { oldText = it }, onOpen = openOld, onSwap = swap)
                    NumberedTextField(oldText, { oldText = it }, modifier = paneModifier)
                }
                1 -> {
                    InputHeader("New text", onPaste = { newText = it }, onOpen = openNew, onSwap = swap)
                    NumberedTextField(newText, { newText = it }, modifier = paneModifier)
                }
                else -> {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        diffModes.forEachIndexed { i, label ->
                            SegmentedButton(
                                selected = mode == i,
                                onClick = { mode = i },
                                shape = SegmentedButtonDefaults.itemShape(i, diffModes.size)
                            ) { Text(label, maxLines = 1) }
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(ignoreCase, { ignoreCase = !ignoreCase }, label = { Text("Ignore case", maxLines = 1) })
                        FilterChip(trim, { trim = !trim }, label = { Text("Trim lines", maxLines = 1) })
                        FilterChip(ignoreSpace, { ignoreSpace = !ignoreSpace }, label = { Text("Ignore spaces", maxLines = 1) })
                        FilterChip(
                            ignoreBlank && !patchMode, { ignoreBlank = !ignoreBlank },
                            label = { Text("Ignore blank lines", maxLines = 1) },
                            enabled = !patchMode
                        )
                    }
                    val shown = result.takeIf { ready }
                    val text = shown?.view?.text.orEmpty()
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            shown?.summary ?: "Differences",
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (shown?.summary != null) {
                            IconButton(onClick = { clipboard.setText(AnnotatedString(text)) }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy differences", modifier = Modifier.size(20.dp))
                            }
                            if (patchMode) {
                                IconButton(onClick = { shareText(context, text, "changes.patch") }) {
                                    Icon(Icons.Default.Share, contentDescription = "Share patch", modifier = Modifier.size(20.dp))
                                }
                                IconButton(onClick = { savePatch("changes.patch", text.toByteArray()) }) {
                                    Icon(Icons.Default.Save, contentDescription = "Save patch", modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                    Surface(modifier = paneModifier, color = LocalSlabColor.current, shape = MaterialTheme.shapes.medium) {
                        Box(modifier = Modifier.padding(16.dp).horizontalScroll(rememberScrollState()).verticalScroll(rememberScrollState())) {
                            Text(shown?.view ?: AnnotatedString("Computing…"), style = CodeTextStyle)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/** Pane header with Paste, plus Open file and Swap old/new actions. */
@Composable
private fun InputHeader(title: String, onPaste: (String) -> Unit, onOpen: () -> Unit, onSwap: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) { PaneHeader(title, onPaste = onPaste) }
        IconButton(onClick = onOpen) {
            Icon(Icons.Default.FolderOpen, contentDescription = "Open file as ${title.lowercase()}", modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = onSwap) {
            Icon(Icons.Default.SwapVert, contentDescription = "Swap old and new text", modifier = Modifier.size(20.dp))
        }
    }
}

internal data class DiffOptions(
    val ignoreCase: Boolean = false,
    val trim: Boolean = false,
    val ignoreAllSpace: Boolean = false,
    val ignoreBlankLines: Boolean = false,
    val charLevel: Boolean = false
) {
    val plain get() = !ignoreCase && !trim && !ignoreAllSpace

    fun normalize(line: String): String {
        var s = if (ignoreAllSpace) WHITESPACE.replace(line, "") else if (trim) line.trim() else line
        if (ignoreCase) s = s.lowercase()
        return s
    }
}

/** [summary] is null when there is nothing to report (empty input or identical texts). */
internal data class DiffResult(val view: AnnotatedString, val summary: String?)

private val WHITESPACE = Regex("\\s+")
private val TOKEN = Regex("[\\p{L}\\p{N}_]+|\\s+|.")
private const val MAX_INLINE_TOKENS = 4_000
private val RED = Color(0xFFE53935)
private val GREEN = Color(0xFF43A047)

private fun summary(patch: Patch<String>): String {
    val added = patch.deltas.sumOf { it.target.lines.size }
    val removed = patch.deltas.sumOf { it.source.lines.size }
    val changes = patch.deltas.size
    return "+$added −$removed lines, $changes ${if (changes == 1) "change" else "changes"}"
}

/** Character ranges that differ between two lines, compared by word or by character. */
internal fun changedRanges(a: String, b: String, byChar: Boolean): Pair<List<IntRange>, List<IntRange>> {
    val ta = if (byChar) a.map { it.toString() } else TOKEN.findAll(a).map { it.value }.toList()
    val tb = if (byChar) b.map { it.toString() } else TOKEN.findAll(b).map { it.value }.toList()
    // ponytail: long lines fall back to whole-line highlighting; Myers is O(N*D) per line pair.
    if (ta.size + tb.size > MAX_INLINE_TOKENS) return emptyList<IntRange>() to emptyList()
    val offA = ta.runningFold(0) { acc, t -> acc + t.length }
    val offB = tb.runningFold(0) { acc, t -> acc + t.length }
    val ra = mutableListOf<IntRange>()
    val rb = mutableListOf<IntRange>()
    DiffUtils.diff(ta, tb).deltas.forEach { d ->
        val s = d.source
        val t = d.target
        if (s.lines.isNotEmpty()) ra += offA[s.position] until offA[s.position + s.lines.size]
        if (t.lines.isNotEmpty()) rb += offB[t.position] until offB[t.position + t.lines.size]
    }
    return ra to rb
}

internal fun buildDiff(oldText: String, newText: String, options: DiffOptions = DiffOptions()): AnnotatedString =
    computeDiff(oldText, newText, options).view

internal fun computeDiff(oldText: String, newText: String, options: DiffOptions = DiffOptions()): DiffResult {
    if (oldText.isEmpty() && newText.isEmpty()) return DiffResult(AnnotatedString("Please enter text to compare."), null)
    return try {
        val oldAll = oldText.lines()
        val newAll = newText.lines()
        // Indexes of the lines taking part in the comparison; skipped blank lines keep their original numbers.
        val oldIdx = oldAll.indices.filter { !options.ignoreBlankLines || oldAll[it].isNotBlank() }
        val newIdx = newAll.indices.filter { !options.ignoreBlankLines || newAll[it].isNotBlank() }
        val patch = DiffUtils.diff(oldIdx.map { options.normalize(oldAll[it]) }, newIdx.map { options.normalize(newAll[it]) })
        if (patch.deltas.isEmpty()) return DiffResult(AnnotatedString("Texts are identical."), null)

        val view = buildAnnotatedString {
            val deltas = patch.deltas
            var printed = 0
            var offset = 0
            fun gap() = withStyle(SpanStyle(color = Color.Gray)) { append(" ... |  ... | \n") }
            fun context(from: Int, to: Int) {
                for (i in from until to) {
                    append(String.format(Locale.ROOT, "%4d | %4d |   ", oldIdx[i] + 1, newIdx[i + offset] + 1))
                    append(oldAll[oldIdx[i]])
                    append("\n")
                }
            }
            fun changed(prefix: String, text: String, color: Color, ranges: List<IntRange>) {
                withStyle(SpanStyle(color = color, background = color.copy(alpha = 0.13f))) {
                    append(prefix)
                    val start = length
                    append(text)
                    ranges.forEach { addStyle(SpanStyle(background = color.copy(alpha = 0.4f)), start + it.first, start + it.last + 1) }
                    append("\n")
                }
            }

            deltas.forEachIndexed { index, delta ->
                val src = delta.source
                val tgt = delta.target
                val start = maxOf(printed, src.position - 3)
                if (start > printed) gap()
                context(start, src.position)

                val oldLines = src.lines.indices.map { oldAll[oldIdx[src.position + it]] }
                val newLines = tgt.lines.indices.map { newAll[newIdx[tgt.position + it]] }
                // Inline highlights pair the n-th removed line with the n-th added line.
                val inline = (0 until minOf(oldLines.size, newLines.size))
                    .map { changedRanges(oldLines[it], newLines[it], options.charLevel) }
                oldLines.forEachIndexed { i, line ->
                    val number = String.format(Locale.ROOT, "%4d | %4s | - ", oldIdx[src.position + i] + 1, "")
                    changed(number, line, RED, inline.getOrNull(i)?.first.orEmpty())
                }
                newLines.forEachIndexed { i, line ->
                    val number = String.format(Locale.ROOT, "%4s | %4d | + ", "", newIdx[tgt.position + i] + 1)
                    changed(number, line, GREEN, inline.getOrNull(i)?.second.orEmpty())
                }

                val after = src.position + src.lines.size
                offset = tgt.position + tgt.lines.size - after
                val end = minOf(after + 3, deltas.getOrNull(index + 1)?.source?.position ?: oldIdx.size)
                context(after, end)
                printed = end
            }
            if (printed < oldIdx.size) gap()
        }
        DiffResult(view, summary(patch))
    } catch (e: Exception) {
        DiffResult(AnnotatedString("Could not compare the texts: ${e.message}"), null)
    }
}

/** Unified diff (`diff -u`, `git apply` format) over the original lines; blank lines always take part. */
internal fun unifiedDiff(oldText: String, newText: String, options: DiffOptions = DiffOptions()): Pair<String, Patch<String>> {
    // A final line break ends the last line; it does not start an empty one.
    fun split(s: String) = s.lines().let { if (s.isEmpty() || s.endsWith("\n")) it.dropLast(1) else it }
    val a = split(oldText)
    val b = split(newText)
    val patch = if (options.plain) {
        DiffUtils.diff(a, b)
    } else {
        DiffUtils.diff(a, b, BiPredicate<String, String> { x, y -> options.normalize(x) == options.normalize(y) })
    }
    if (patch.deltas.isEmpty()) return "" to patch
    // java-diff-utils writes an empty range as "-1,0"; diff and git apply expect the line before it ("-0,0").
    val lines = UnifiedDiffUtils.generateUnifiedDiff("a/text", "b/text", a, patch, 3).map { line ->
        if (!line.startsWith("@@")) line
        else EMPTY_RANGE.replace(line) { "${it.groupValues[1]}${it.groupValues[2].toInt() - 1},0 " }
    }
    return lines.joinToString("\n", postfix = "\n") to patch
}

private val EMPTY_RANGE = Regex("([-+])(\\d+),0 ")

private fun unifiedDiffResult(oldText: String, newText: String, options: DiffOptions): DiffResult {
    if (oldText.isEmpty() && newText.isEmpty()) return DiffResult(AnnotatedString("Please enter text to compare."), null)
    val (text, patch) = try {
        unifiedDiff(oldText, newText, options)
    } catch (e: Exception) {
        return DiffResult(AnnotatedString("Could not compare the texts: ${e.message}"), null)
    }
    if (text.isEmpty()) return DiffResult(AnnotatedString("Texts are identical."), null)
    val view = buildAnnotatedString {
        text.removeSuffix("\n").split("\n").forEach { line ->
            val color = when {
                line.startsWith("+++") || line.startsWith("---") || line.startsWith("@@") -> Color.Gray
                line.startsWith("+") -> GREEN
                line.startsWith("-") -> RED
                else -> null
            }
            if (color != null) withStyle(SpanStyle(color = color)) { append(line) } else append(line)
            append("\n")
        }
    }
    return DiffResult(view, summary(patch))
}
