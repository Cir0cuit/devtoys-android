package com.cir0cuit.devtoys.tools.text

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.designsystem.CodeTextStyle
import com.cir0cuit.devtoys.core.designsystem.LocalSlabColor
import com.cir0cuit.devtoys.core.ui.MonoTextField
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.core.ui.shareText
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolCategory
import com.cir0cuit.devtoys.tools.base.ToolDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.Normalizer
import java.util.Locale

private val WORD = Regex("\\p{Lu}+(?!\\p{Ll})|\\p{Lu}?\\p{Ll}+|\\p{N}+|\\p{L}+")
private val WORD_START = Regex("(?<![\\p{L}\\p{N}'])\\p{L}")
private val SENTENCE_START = Regex("(^|[.!?]\\s+)(\\p{L})")
private val MARKS = Regex("\\p{Mn}+")
private val NON_SLUG = Regex("[^a-z0-9]+")

// Letters NFD does not decompose into a base letter plus a combining mark.
private val FOLD = mapOf(
    'ß' to "ss", 'æ' to "ae", 'Æ' to "AE", 'œ' to "oe", 'Œ' to "OE", 'ø' to "o", 'Ø' to "O",
    'ł' to "l", 'Ł' to "L", 'đ' to "d", 'Đ' to "D", 'ð' to "d", 'Ð' to "D", 'þ' to "th", 'Þ' to "Th", 'ı' to "i"
)

// Kotlin's uppercase()/lowercase() without a locale argument are locale-invariant (Locale.ROOT).
private fun words(line: String) = WORD.findAll(line).map { it.value.lowercase() }.toList()
private fun String.cap() = replaceFirstChar { it.uppercase() }

/** Removes diacritics and folds common ligatures and special letters to ASCII. */
internal fun asciiFold(s: String): String {
    val stripped = MARKS.replace(Normalizer.normalize(s, Normalizer.Form.NFD), "")
    return buildString(stripped.length) { stripped.forEach { c -> append(FOLD[c] ?: c) } }
}

internal fun slugify(s: String): String = NON_SLUG.replace(asciiFold(s).lowercase(), "-").trim('-')

/**
 * Case name to conversion. Identifier cases are applied per line and always locale-invariant;
 * [locale] only affects the prose cases (upper, lower, title, sentence).
 */
internal fun caseConversions(locale: Locale = Locale.ROOT): List<Pair<String, (String) -> String>> = listOf(
    "Uppercase" to { s: String -> s.uppercase(locale) },
    "Lowercase" to { s: String -> s.lowercase(locale) },
    "Title case" to { s: String -> WORD_START.replace(s.lowercase(locale)) { it.value.uppercase(locale) } },
    "Sentence case" to { s: String ->
        SENTENCE_START.replace(s.lowercase(locale)) { it.groupValues[1] + it.groupValues[2].uppercase(locale) }
    },
    "camelCase" to perLine { w -> w.mapIndexed { i, x -> if (i == 0) x else x.cap() }.joinToString("") },
    "PascalCase" to perLine { w -> w.joinToString("") { it.cap() } },
    "snake_case" to perLine { w -> w.joinToString("_") },
    "kebab-case" to perLine { w -> w.joinToString("-") },
    "CONSTANT_CASE" to perLine { w -> w.joinToString("_").uppercase() },
    "Train-Case" to perLine { w -> w.joinToString("-") { it.cap() } },
    "COBOL-CASE" to perLine { w -> w.joinToString("-").uppercase() },
    "dot.case" to perLine { w -> w.joinToString(".") },
    "path/case" to perLine { w -> w.joinToString("/") },
    "flatcase" to perLine { w -> w.joinToString("") },
    "UPPERFLATCASE" to perLine { w -> w.joinToString("").uppercase() },
    "Slug" to { s: String -> s.lines().joinToString("\n") { slugify(it) } },
    "ASCII fold" to ::asciiFold,
    "Swap case" to { s: String ->
        s.map { c -> if (c.isUpperCase()) c.lowercaseChar() else if (c.isLowerCase()) c.uppercaseChar() else c }.joinToString("")
    },
    "Alternating case" to { s: String ->
        var upper = false
        s.map { c -> if (c.isLetter()) { upper = !upper; if (upper) c.lowercaseChar() else c.uppercaseChar() } else c }.joinToString("")
    }
)

private fun perLine(join: (List<String>) -> String): (String) -> String =
    { s -> s.lines().joinToString("\n") { join(words(it)) } }

// Most specific first: a one-word input matches several patterns.
private val CASE_PATTERNS = listOf(
    "camelCase" to Regex("[a-z][a-z0-9]*(?:[A-Z][a-z0-9]*)+"),
    "PascalCase" to Regex("[A-Z][a-z0-9]+(?:[A-Z][a-z0-9]*)+"),
    "snake_case" to Regex("[a-z0-9]+(?:_[a-z0-9]+)+"),
    "CONSTANT_CASE" to Regex("[A-Z0-9]+(?:_[A-Z0-9]+)+"),
    "kebab-case" to Regex("[a-z0-9]+(?:-[a-z0-9]+)+"),
    "COBOL-CASE" to Regex("[A-Z0-9]+(?:-[A-Z0-9]+)+"),
    "Train-Case" to Regex("[A-Z][a-z0-9]*(?:-[A-Z][a-z0-9]*)+"),
    "dot.case" to Regex("[a-z0-9]+(?:\\.[a-z0-9]+)+"),
    "path/case" to Regex("[a-z0-9]+(?:/[a-z0-9]+)+"),
    "flatcase" to Regex("[a-z][a-z0-9]*"),
    "UPPERFLATCASE" to Regex("[A-Z][A-Z0-9]*")
)
private val TITLE = Regex("[A-Z]\\S*(?:\\s+[A-Z]\\S*)+")
private val SENTENCE = Regex("[A-Z](?:[^A-Z]|(?<=[.!?]\\s)[A-Z])*")

/** Names the case of the first non-blank line, or null when it does not match a known case. */
internal fun detectCase(text: String): String? {
    val line = text.lineSequence().firstOrNull { it.isNotBlank() }?.trim() ?: return null
    CASE_PATTERNS.firstOrNull { it.second.matches(line) }?.let { return it.first }
    return when {
        line == line.uppercase() && line != line.lowercase() -> "Uppercase"
        line == line.lowercase() && line != line.uppercase() -> "Lowercase"
        TITLE.matches(line) -> "Title case"
        SENTENCE.matches(line) -> "Sentence case"
        else -> null
    }
}

class CaseConverterTool : Tool {
    override val descriptor = ToolDescriptor(
        "case", "Case Converter", "Convert text to upper, lower, title, camel, snake or kebab case",
        Icons.Default.FormatSize, ToolCategory.TEXT,
        listOf("case", "upper", "lower", "camel", "snake", "kebab", "title", "slug", "slugify", "train", "dot")
    )

    @OptIn(ExperimentalFoundationApi::class)
    @Composable override fun Content(modifier: Modifier) {
        var input by rememberSaveable(saver = boundedTextSaver) { mutableStateOf("The quick brown fox") }
        var deviceLocale by rememberSaveable { mutableStateOf(false) }
        val clipboard = LocalClipboardManager.current
        val context = LocalContext.current

        // Detected case to (case name, converted text) rows.
        val result by produceState<Pair<String?, List<Pair<String, String>>>?>(null, input, deviceLocale) {
            if (value != null) delay(150)
            val locale = if (deviceLocale) Locale.getDefault() else Locale.ROOT
            value = withContext(Dispatchers.Default) {
                detectCase(input) to caseConversions(locale).map { (name, convert) -> name to convert(input) }
            }
        }

        ToolScaffold(descriptor.title, modifier = modifier) {
            PaneHeader("Input", onPaste = { input = it })
            MonoTextField(input, { input = it }, "Text", minLines = 3)
            result?.first?.let {
                Text(
                    "Detected: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            FilterChip(
                selected = deviceLocale,
                onClick = { deviceLocale = !deviceLocale },
                label = { Text("Use device locale for prose cases", maxLines = 1) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(color = LocalSlabColor.current, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(vertical = 8.dp)) {
                    result?.second.orEmpty().forEach { (name, value) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    enabled = value.isNotEmpty(),
                                    onLongClick = { shareText(context, value) },
                                    onLongClickLabel = "Share $name",
                                    onClick = { clipboard.setText(AnnotatedString(value)) }
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(value.ifEmpty { "–" }, style = CodeTextStyle, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            }
                            if (value.isNotEmpty()) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copy $name",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 8.dp).size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
            Text(
                "Tap a row to copy it, long-press to share.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
