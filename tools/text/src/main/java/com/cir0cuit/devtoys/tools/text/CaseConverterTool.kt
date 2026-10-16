package com.cir0cuit.devtoys.tools.text

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.designsystem.CodeTextStyle
import com.cir0cuit.devtoys.core.designsystem.LocalSlabColor
import com.cir0cuit.devtoys.core.ui.MonoTextField
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolCategory
import com.cir0cuit.devtoys.tools.base.ToolDescriptor

private val WORD = Regex("\\p{Lu}+(?!\\p{Ll})|\\p{Lu}?\\p{Ll}+|\\p{N}+|\\p{L}+")
private val WORD_START = Regex("(?<![\\p{L}\\p{N}'])\\p{L}")
private val SENTENCE_START = Regex("(^|[.!?]\\s+)(\\p{L})")

private fun words(line: String) = WORD.findAll(line).map { it.value.lowercase() }.toList()
private fun String.cap() = replaceFirstChar { it.uppercase() }

/** Case name to conversion; identifier cases are applied per line. */
internal val caseConversions: List<Pair<String, (String) -> String>> = listOf(
    "Uppercase" to { s: String -> s.uppercase() },
    "Lowercase" to { s: String -> s.lowercase() },
    "Title case" to { s: String -> WORD_START.replace(s.lowercase()) { it.value.uppercase() } },
    "Sentence case" to { s: String -> SENTENCE_START.replace(s.lowercase()) { it.groupValues[1] + it.groupValues[2].uppercase() } },
    "camelCase" to perLine { w -> w.mapIndexed { i, x -> if (i == 0) x else x.cap() }.joinToString("") },
    "PascalCase" to perLine { w -> w.joinToString("") { it.cap() } },
    "snake_case" to perLine { w -> w.joinToString("_") },
    "kebab-case" to perLine { w -> w.joinToString("-") },
    "CONSTANT_CASE" to perLine { w -> w.joinToString("_").uppercase() }
)

private fun perLine(join: (List<String>) -> String): (String) -> String =
    { s -> s.lines().joinToString("\n") { join(words(it)) } }

class CaseConverterTool : Tool {
    override val descriptor = ToolDescriptor(
        "case", "Case Converter", "Convert text to upper, lower, title, camel, snake or kebab case",
        Icons.Default.FormatSize, ToolCategory.TEXT, listOf("case", "upper", "lower", "camel", "snake", "kebab", "title")
    )

    @Composable override fun Content(modifier: Modifier) {
        var input by rememberSaveable { mutableStateOf("The quick brown fox") }
        val clipboard = LocalClipboardManager.current

        ToolScaffold(descriptor.title, modifier = modifier) {
            MonoTextField(input, { input = it }, "Input", minLines = 3)
            Spacer(modifier = Modifier.height(16.dp))
            Surface(color = LocalSlabColor.current, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(vertical = 8.dp)) {
                    caseConversions.forEach { (name, convert) ->
                        val value = convert(input)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable(enabled = value.isNotEmpty()) { clipboard.setText(AnnotatedString(value)) }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(value, style = CodeTextStyle, maxLines = 3, overflow = TextOverflow.Ellipsis)
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
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
