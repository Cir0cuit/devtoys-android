package com.cir0cuit.devtoys.tools.text

import com.cir0cuit.devtoys.tools.base.OnSharedText
import com.cir0cuit.devtoys.tools.base.ShareKind
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.designsystem.CodeTextStyle
import com.cir0cuit.devtoys.core.ui.NumberedTextField
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolCategory
import com.cir0cuit.devtoys.tools.base.ToolDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.regex.PatternSyntaxException

// Operation key (saved state, applyLineOperation) to menu label.
private val operations = listOf(
    "Sort" to "Sort A–Z",
    "Sort Desc" to "Sort Z–A",
    "Natural" to "Natural sort",
    "Sort Ci" to "Sort, ignoring case",
    "Sort Length" to "Sort by length",
    "Reverse" to "Reverse",
    "Shuffle" to "Shuffle",
    "Dedupe" to "Remove duplicates",
    "Dedupe Ci" to "Remove duplicates, ignoring case",
    "Duplicates" to "Keep only duplicates",
    "Count Each" to "Count occurrences",
    "Filter" to "Filter lines (grep)",
    "Join" to "Join lines into a list",
    "Split" to "Split into lines",
    "Remove Blank" to "Remove blank lines",
    "Trim" to "Trim lines",
    "Trim End" to "Strip trailing whitespace",
    "Number" to "Number lines",
    "Unnumber" to "Remove line numbers",
    "Tabs To Spaces" to "Tabs to spaces",
    "Spaces To Tabs" to "Leading spaces to tabs",
    "To LF" to "Line endings to LF",
    "To CRLF" to "Line endings to CRLF",
    "Remove BOM" to "Remove BOM"
)

private val quotes = listOf("" to "None", "'" to "'a'", "\"" to "\"a\"")
private val wraps = listOf("" to "None", "()" to "( )", "[]" to "[ ]", "{}" to "{ }")

/** Settings used by some operations; the defaults suit every other operation. */
internal data class LineOptions(
    val pattern: String = "",
    val regex: Boolean = false,
    val ignoreCase: Boolean = false,
    val invert: Boolean = false,
    val separator: String = ", ",
    val quote: String = "",
    val wrap: String = "",
    val delimiter: String = ",",
    val tabWidth: Int = 4
)

class LineToolsTool : Tool {
    override val descriptor = ToolDescriptor(
        "line-tools", "Line Tools", "Sort, filter, dedupe, join or fix lines",
        Icons.AutoMirrored.Filled.Sort, ToolCategory.TEXT,
        listOf("line", "sort", "dedupe", "count", "grep", "filter", "join", "split", "crlf", "uniq", "shuffle"),
        accepts = setOf(ShareKind.TEXT),
        fileTypes = setOf("txt", "log", "text")
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable override fun Content(modifier: Modifier) {
        var input by rememberSaveable(saver = boundedTextSaver) { mutableStateOf("banana\napple\ncherry\napple") }
        OnSharedText { input = it }
        var operation by rememberSaveable { mutableStateOf("Sort") }
        val op = operation.takeIf { saved -> operations.any { it.first == saved } } ?: "Sort"
        var pattern by rememberSaveable { mutableStateOf("") }
        var regex by rememberSaveable { mutableStateOf(false) }
        var ignoreCase by rememberSaveable { mutableStateOf(false) }
        var invert by rememberSaveable { mutableStateOf(false) }
        var separator by rememberSaveable { mutableStateOf(", ") }
        var quote by rememberSaveable { mutableStateOf("") }
        var wrap by rememberSaveable { mutableStateOf("") }
        var delimiter by rememberSaveable { mutableStateOf(",") }
        var tabWidth by rememberSaveable { mutableStateOf(4) }
        val options = LineOptions(pattern, regex, ignoreCase, invert, separator, quote, wrap, delimiter, tabWidth)

        var result by remember { mutableStateOf<LineResult?>(null) }
        LaunchedEffect(input, op, options) {
            if (result != null) delay(200)
            result = withContext(Dispatchers.Default) { computeLines(input, op, options) }
        }
        val output = result?.output.orEmpty()
        val error = result?.error

        ToolScaffold(descriptor.title, modifier = modifier, scrollable = false) {
            PaneHeader("Input", onPaste = { input = it })
            NumberedTextField(input, { input = it }, modifier = Modifier.weight(1f).fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OptionDropdown("Operation", op, operations, { operation = it }, Modifier.fillMaxWidth())
            when (op) {
                "Filter" -> {
                    OutlinedTextField(
                        value = pattern,
                        onValueChange = { pattern = it },
                        label = { Text(if (regex) "Regular expression" else "Text to find") },
                        singleLine = true,
                        textStyle = CodeTextStyle,
                        isError = error != null,
                        supportingText = if (error != null) { { Text(error) } } else null,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(regex, { regex = !regex }, label = { Text("Regex", maxLines = 1) })
                        FilterChip(ignoreCase, { ignoreCase = !ignoreCase }, label = { Text("Ignore case", maxLines = 1) })
                        FilterChip(invert, { invert = !invert }, label = { Text("Invert", maxLines = 1) })
                    }
                }
                "Join" -> Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = separator,
                        onValueChange = { separator = it },
                        label = { Text("Separator", maxLines = 1) },
                        singleLine = true,
                        textStyle = CodeTextStyle,
                        modifier = Modifier.weight(1f)
                    )
                    OptionDropdown("Quote", quote, quotes, { quote = it }, Modifier.weight(1f))
                    OptionDropdown("Wrap", wrap, wraps, { wrap = it }, Modifier.weight(1f))
                }
                "Split" -> Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = delimiter,
                        onValueChange = { delimiter = it },
                        label = { Text("Delimiter", maxLines = 1) },
                        singleLine = true,
                        textStyle = CodeTextStyle,
                        isError = error != null,
                        supportingText = { Text(error ?: "Leave empty to split on whitespace") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(regex, { regex = !regex }, label = { Text("Regex", maxLines = 1) }, modifier = Modifier.padding(start = 8.dp))
                }
                "Tabs To Spaces", "Spaces To Tabs" -> Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Tab width", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    SingleChoiceSegmentedButtonRow {
                        listOf(2, 4, 8).forEachIndexed { i, w ->
                            SegmentedButton(
                                selected = tabWidth == w,
                                onClick = { tabWidth = w },
                                shape = SegmentedButtonDefaults.itemShape(i, 3)
                            ) { Text("$w") }
                        }
                    }
                }
            }
            PaneHeader(outputTitle(result), copyText = output)
            NumberedTextField(output, { }, modifier = Modifier.weight(1f).fillMaxWidth(), readOnly = true)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/** Read-only dropdown over key-to-label pairs. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionDropdown(
    label: String,
    selected: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = modifier) {
        OutlinedTextField(
            value = options.firstOrNull { it.first == selected }?.second.orEmpty(),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label, maxLines = 1) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (key, text) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { onSelect(key); expanded = false })
            }
        }
    }
}

internal data class LineResult(val output: String, val inputLines: Int, val outputLines: Int, val filtered: Boolean, val error: String? = null)

private fun outputTitle(r: LineResult?): String {
    if (r == null) return "Output"
    val count = when {
        r.filtered -> "${r.outputLines} of ${r.inputLines} lines"
        r.outputLines == 1 -> "1 line"
        else -> "${r.outputLines} lines"
    }
    val ending = describeLineEnding(r.output)
    return if (ending.isEmpty()) "Output, $count" else "Output, $count, $ending"
}

internal fun computeLines(input: String, operation: String, options: LineOptions): LineResult = try {
    val out = applyLineOperation(input, operation, options)
    LineResult(out, countLines(input), countLines(out), operation == "Filter")
} catch (e: PatternSyntaxException) {
    LineResult("", countLines(input), 0, false, "Invalid regex: ${e.description}. Escape special characters such as ( [ . * with \\.")
}

/** Number of lines; a final line break ends the last line instead of starting a new one. */
internal fun countLines(s: String): Int =
    if (s.isEmpty()) 0 else s.count { it == '\n' } + if (s.endsWith("\n")) 0 else 1

/** "CRLF", "Mixed line endings" and/or "BOM"; empty for plain LF text without a BOM. */
internal fun describeLineEnding(s: String): String {
    val crlf = Regex("\r\n").findAll(s).count()
    val lf = s.count { it == '\n' } - crlf
    val ending = when {
        crlf > 0 && lf > 0 -> "Mixed line endings"
        crlf > 0 -> "CRLF"
        else -> null
    }
    return listOfNotNull(ending, "BOM".takeIf { s.startsWith(Char(0xFEFF)) }).joinToString(", ")
}

private val CHUNK = Regex("[0-9]+|[^0-9]+")

/** Compares digit runs by numeric value, so "file2" sorts before "file10". */
internal val naturalOrder = Comparator<String> { a, b ->
    val x = CHUNK.findAll(a).iterator()
    val y = CHUNK.findAll(b).iterator()
    while (x.hasNext() && y.hasNext()) {
        val p = x.next().value
        val q = y.next().value
        val c = if (p[0] in '0'..'9' && q[0] in '0'..'9') {
            val pt = p.trimStart('0')
            val qt = q.trimStart('0')
            if (pt.length != qt.length) pt.length - qt.length else pt.compareTo(qt)
        } else {
            p.compareTo(q)
        }
        if (c != 0) return@Comparator c
    }
    if (x.hasNext()) 1 else if (y.hasNext()) -1 else a.compareTo(b)
}

private val LINE_NUMBER = Regex("^\\s*\\d+[.:)|]?[ \\t]+")

/** Resolves \n, \t and \\ escapes typed into a separator field. */
private fun unescape(s: String) = s.replace("\\n", "\n").replace("\\t", "\t").replace("\\\\", "\\")

private fun expandTabs(line: String, width: Int) = buildString {
    line.forEach { c -> if (c == '\t') repeat(width - length % width) { append(' ') } else append(c) }
}

private fun leadingSpacesToTabs(line: String, width: Int): String {
    val spaces = line.length - line.trimStart(' ').length
    return "\t".repeat(spaces / width) + line.substring(spaces - spaces % width)
}

internal fun applyLineOperation(input: String, operation: String, options: LineOptions = LineOptions()): String {
    if (input.isEmpty()) return ""
    when (operation) {
        "To LF" -> return input.replace("\r\n", "\n")
        "To CRLF" -> return input.replace("\r\n", "\n").replace("\n", "\r\n")
        "Remove BOM" -> return input.removePrefix(Char(0xFEFF).toString())
    }
    val trailing = when {
        input.endsWith("\r\n") -> "\r\n"
        input.endsWith("\n") -> "\n"
        else -> ""
    }
    val sep = if (input.contains("\r\n")) "\r\n" else "\n"
    val lines = input.removeSuffix(trailing).lines()
    fun join(l: List<String>) = if (l.isEmpty()) "" else l.joinToString(sep) + trailing
    return when (operation) {
        "Sort" -> join(lines.sorted())
        "Sort Desc" -> join(lines.sortedDescending())
        "Natural" -> join(lines.sortedWith(naturalOrder))
        "Sort Ci" -> join(lines.sortedWith(String.CASE_INSENSITIVE_ORDER))
        "Sort Length" -> join(lines.sortedBy { it.length })
        "Reverse" -> join(lines.reversed())
        "Shuffle" -> join(lines.shuffled())
        "Dedupe" -> join(lines.distinct())
        "Dedupe Ci" -> join(lines.distinctBy { it.lowercase() })
        "Duplicates" -> join(lines.groupingBy { it }.eachCount().filterValues { it > 1 }.keys.toList())
        "Count Each" -> {
            // Like `sort | uniq -c | sort -rn`, ties kept in first-seen order.
            val counts = lines.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }
            val width = counts.first().value.toString().length
            join(counts.map { "${it.value.toString().padStart(width)} ${it.key}" })
        }
        "Filter" -> {
            val matches: (String) -> Boolean = if (options.regex) {
                val re = if (options.ignoreCase) Regex(options.pattern, RegexOption.IGNORE_CASE) else Regex(options.pattern)
                re::containsMatchIn
            } else {
                { it.contains(options.pattern, options.ignoreCase) }
            }
            join(lines.filter { matches(it) != options.invert })
        }
        "Join" -> {
            val open = options.wrap.take(1)
            val close = options.wrap.drop(1)
            lines.map { it.trim() }.filter { it.isNotEmpty() }
                .joinToString(unescape(options.separator), open, close) { options.quote + it + options.quote }
        }
        "Split" -> {
            val delimiter = when {
                options.delimiter.isEmpty() -> Regex("\\s+")
                options.regex -> Regex(options.delimiter)
                else -> Regex(Regex.escape(unescape(options.delimiter)))
            }
            join(lines.flatMap { delimiter.split(it) }.map { it.trim() }.filter { it.isNotEmpty() })
        }
        "Remove Blank" -> join(lines.filter { it.isNotBlank() })
        "Trim" -> join(lines.map { it.trim() })
        "Trim End" -> join(lines.map { it.trimEnd() })
        "Number" -> {
            val width = lines.size.toString().length
            join(lines.mapIndexed { i, l -> "${(i + 1).toString().padStart(width)}. $l" })
        }
        "Unnumber" -> join(lines.map { LINE_NUMBER.replaceFirst(it, "") })
        "Tabs To Spaces" -> join(lines.map { expandTabs(it, options.tabWidth) })
        "Spaces To Tabs" -> join(lines.map { leadingSpacesToTabs(it, options.tabWidth) })
        "Count" -> "Lines: ${lines.size}"
        else -> input
    }
}
