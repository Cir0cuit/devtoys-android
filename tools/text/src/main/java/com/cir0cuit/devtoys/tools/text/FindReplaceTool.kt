package com.cir0cuit.devtoys.tools.text

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.designsystem.CodeTextStyle
import com.cir0cuit.devtoys.core.designsystem.LocalSlabColor
import com.cir0cuit.devtoys.core.ui.MonoTextField
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.core.ui.shareText
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolCategory
import com.cir0cuit.devtoys.tools.base.ToolDescriptor
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

internal data class ReplaceOptions(
    val regex: Boolean = false,
    val ignoreCase: Boolean = false,
    val wholeWord: Boolean = false,
    val multiline: Boolean = false,
    val dotAll: Boolean = false
)

/** Result text, number of matches and the ranges of the inserted replacements in the result; or an error. */
internal data class ReplaceResult(val text: String, val count: Int, val highlights: List<IntRange>, val error: String? = null)

private const val MAX_HIGHLIGHTS = 2_000

class FindReplaceTool : Tool {
    override val descriptor = ToolDescriptor(
        "find-replace", "Find & Replace", "Replace text or regex matches",
        Icons.Default.FindReplace, ToolCategory.TEXT,
        listOf("find", "replace", "search", "regex", "substitute", "sed")
    )

    @OptIn(ExperimentalLayoutApi::class)
    @Composable override fun Content(modifier: Modifier) {
        var input by rememberSaveable(saver = boundedTextSaver) { mutableStateOf("The year 2023 and the year 2024") }
        var find by rememberSaveable { mutableStateOf("(\\d{4})") }
        var replace by rememberSaveable { mutableStateOf("[$1]") }
        var regex by rememberSaveable { mutableStateOf(true) }
        var ignoreCase by rememberSaveable { mutableStateOf(false) }
        var wholeWord by rememberSaveable { mutableStateOf(false) }
        var multiline by rememberSaveable { mutableStateOf(false) }
        var dotAll by rememberSaveable { mutableStateOf(false) }
        val options = ReplaceOptions(regex, ignoreCase, wholeWord, multiline, dotAll)

        val result by rememberDebounced(ReplaceResult(input, 0, emptyList()), input, find, replace, options) {
            findAndReplace(input, find, replace, options)
        }
        val highlight = SpanStyle(background = MaterialTheme.colorScheme.primaryContainer, color = MaterialTheme.colorScheme.onPrimaryContainer)
        val annotated = remember(result, highlight) {
            buildAnnotatedString {
                append(result.text)
                result.highlights.forEach { addStyle(highlight, it.first, it.last + 1) }
            }
        }
        val clipboard = LocalClipboardManager.current
        val context = LocalContext.current

        ToolScaffold(descriptor.title, modifier = modifier) {
            MonoTextField(input, { input = it }, "Text", minLines = 4, maxLines = 10)
            Spacer(Modifier.height(8.dp))
            MonoTextField(find, { find = it }, "Find", minLines = 1, maxLines = 3)
            Spacer(Modifier.height(8.dp))
            MonoTextField(replace, { replace = it }, if (regex) "Replace with ($1, \${name})" else "Replace with", minLines = 1, maxLines = 3)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = regex, onClick = { regex = !regex }, label = { Text("Regex", maxLines = 1) })
                FilterChip(selected = ignoreCase, onClick = { ignoreCase = !ignoreCase }, label = { Text("Ignore case", maxLines = 1) })
                FilterChip(selected = wholeWord, onClick = { wholeWord = !wholeWord }, label = { Text("Whole word", maxLines = 1) })
                if (regex) {
                    FilterChip(selected = multiline, onClick = { multiline = !multiline }, label = { Text("^$ per line", maxLines = 1) })
                    FilterChip(selected = dotAll, onClick = { dotAll = !dotAll }, label = { Text(". matches newline", maxLines = 1) })
                }
            }
            Spacer(Modifier.height(16.dp))
            val error = result.error
            if (error != null) {
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            } else {
                Surface(Modifier.fillMaxWidth(), color = LocalSlabColor.current, shape = MaterialTheme.shapes.medium) {
                    Column {
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 40.dp).padding(start = 16.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (result.count == 1) "1 match replaced" else "${result.count} matches replaced",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { input = result.text }, enabled = result.count > 0) {
                                Icon(Icons.Default.Upload, contentDescription = "Use result as input", modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { shareText(context, result.text) }) {
                                Icon(Icons.Default.Share, contentDescription = "Share result", modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { clipboard.setText(AnnotatedString(result.text)) }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy result", modifier = Modifier.size(20.dp))
                            }
                        }
                        SelectionContainer {
                            Text(annotated, style = CodeTextStyle, modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

internal fun findAndReplace(input: String, find: String, replace: String, options: ReplaceOptions, timeoutMs: Long = 2_000): ReplaceResult {
    if (find.isEmpty()) return ReplaceResult(input, 0, emptyList())
    val body = if (options.regex) find else Pattern.quote(find)
    val source = if (options.wholeWord) "(?<![\\p{L}\\p{N}_])(?:$body)(?![\\p{L}\\p{N}_])" else body
    var flags = 0
    if (options.ignoreCase) flags = flags or Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE
    if (options.regex && options.multiline) flags = flags or Pattern.MULTILINE
    if (options.regex && options.dotAll) flags = flags or Pattern.DOTALL
    val pattern = try {
        Pattern.compile(source, flags)
    } catch (e: PatternSyntaxException) {
        return ReplaceResult(input, 0, emptyList(), "Invalid pattern: ${e.description}" + (if (e.index >= 0) " near position ${e.index + 1}" else "") + ". Escape special characters with \\ or turn off Regex.")
    }
    val replacement = if (options.regex) replace else Matcher.quoteReplacement(replace)
    // ponytail: the deadline is checked between matches only. Android's ICU-backed Matcher copies the input
    // and matches natively, so one catastrophic match such as (a+)+$ cannot be interrupted; it ties up one
    // Dispatchers.Default thread until it ends. Run matching in a killable process if that ever matters.
    val deadline = System.nanoTime() + timeoutMs * 1_000_000
    val matcher = pattern.matcher(input)
    val out = StringBuffer()
    val highlights = ArrayList<IntRange>()
    var count = 0
    var lastEnd = 0
    try {
        while (matcher.find()) {
            if (System.nanoTime() > deadline) {
                return ReplaceResult(input, 0, emptyList(), "Matching took over ${timeoutMs / 1000} s. Use a more specific pattern or a shorter text.")
            }
            val replStart = out.length + (matcher.start() - lastEnd)
            matcher.appendReplacement(out, replacement)
            if (out.length > replStart && highlights.size < MAX_HIGHLIGHTS) highlights += replStart until out.length
            lastEnd = matcher.end()
            count++
        }
        matcher.appendTail(out)
    } catch (e: IllegalArgumentException) {
        return ReplaceResult(input, 0, emptyList(), "Invalid replacement: ${e.message}. Write a literal \$ as \\\$ and a literal \\ as \\\\.")
    } catch (e: IndexOutOfBoundsException) {
        return ReplaceResult(input, 0, emptyList(), "Invalid replacement: ${e.message}. Use a group number that exists in the pattern.")
    }
    return ReplaceResult(out.toString(), count, highlights)
}
