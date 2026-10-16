package com.cir0cuit.devtoys.tools.developer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.designsystem.CodeTextStyle
import com.cir0cuit.devtoys.core.designsystem.LocalSlabColor
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

/** Longest sample text shown with inline highlights; longer text only gets the match list. */
private const val MAX_HIGHLIGHT_CHARS = 20_000

class RegexTesterTool : Tool {
    override val descriptor = ToolDescriptor(
        id = "regex",
        title = "Regex Tester",
        description = "Test, replace and split text with a regex",
        icon = Icons.Default.FindInPage,
        category = ToolCategory.TEXT,
        keywords = listOf("regex", "tester", "replace", "split", "pattern")
    )

    @OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
    @Composable override fun Content(modifier: Modifier) {
        var regex by rememberSaveable { mutableStateOf("""(?<user>\w+)@(\w+\.\w+)""") }
        var input by rememberSaveable { mutableStateOf("Write to ada@example.com or grace@example.org.") }
        var flags by rememberSaveable { mutableIntStateOf(Pattern.MULTILINE) }
        var mode by rememberSaveable { mutableStateOf(RegexMode.MATCH) }
        var replacement by rememberSaveable { mutableStateOf("\${user} at $2") }
        var outcome by remember { mutableStateOf(RegexRun("")) }
        var copyMenu by remember { mutableStateOf(false) }
        val clipboard = LocalClipboardManager.current

        LaunchedEffect(regex, input, flags, mode, replacement) {
            if (regex.isEmpty()) {
                outcome = RegexRun("")
                return@LaunchedEffect
            }
            delay(250)
            val slowNotice = launch {
                delay(2000)
                outcome = RegexRun("Still matching. The pattern may backtrack heavily; simplify nested repeats like (a+)+.")
            }
            outcome = withContext(Dispatchers.IO) { runRegex(mode, regex, input, flags, replacement) }
            slowNotice.cancel()
        }

        ToolScaffold(descriptor.title, modifier = modifier) {
            MonoTextField(regex, { regex = it }, "Pattern", minLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Java regex engine. Lookbehind and possessive quantifiers differ from JS and PCRE.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Box {
                    TextButton(onClick = { copyMenu = true }) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                        Text("Copy as")
                    }
                    DropdownMenu(copyMenu, { copyMenu = false }) {
                        CODE_LANGUAGES.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text(lang) },
                                onClick = {
                                    clipboard.setText(AnnotatedString(regexAsCode(regex, flags, lang)))
                                    copyMenu = false
                                }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            MonoTextField(input, { input = it }, "Sample text", minLines = 4)
            Spacer(Modifier.height(16.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                REGEX_FLAGS.forEach { (flag, label) ->
                    FilterChip(flags and flag != 0, { flags = flags xor flag }, label = { Text(label) })
                }
            }
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                RegexMode.entries.forEachIndexed { i, m ->
                    SegmentedButton(
                        selected = mode == m,
                        onClick = { mode = m },
                        shape = SegmentedButtonDefaults.itemShape(i, RegexMode.entries.size),
                        label = { Text(m.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
            if (mode == RegexMode.REPLACE) {
                Spacer(Modifier.height(8.dp))
                MonoTextField(replacement, { replacement = it }, "Replace with (\$1, \${name}, \\\$)", minLines = 1)
            }
            Spacer(Modifier.height(16.dp))
            if (mode == RegexMode.MATCH && outcome.hits.isNotEmpty() && input.length <= MAX_HIGHLIGHT_CHARS) {
                HighlightedText(input, outcome.hits)
                Spacer(Modifier.height(16.dp))
            }
            OutputPanel(
                outcome.text,
                label = when (mode) { RegexMode.MATCH -> "Matches"; RegexMode.REPLACE -> "Result"; RegexMode.SPLIT -> "Parts" }
            )
        }
    }
}

/** Sample text with alternating tints per match and underlined capture groups. */
@Composable
private fun HighlightedText(text: String, hits: List<MatchHit>) {
    val even = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    val odd = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f)
    val annotated = remember(text, hits, even, odd) {
        buildAnnotatedString {
            append(text)
            hits.forEachIndexed { i, h ->
                if (h.end > h.start) addStyle(SpanStyle(background = if (i % 2 == 0) even else odd), h.start, h.end)
                h.groups.forEach { g ->
                    if (g.value != null && g.end > g.start) addStyle(SpanStyle(textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Bold), g.start, g.end)
                }
            }
        }
    }
    Surface(Modifier.fillMaxWidth(), color = LocalSlabColor.current, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(16.dp)) {
            Text("Highlighted", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            SelectionContainer { Text(annotated, style = CodeTextStyle) }
        }
    }
}
