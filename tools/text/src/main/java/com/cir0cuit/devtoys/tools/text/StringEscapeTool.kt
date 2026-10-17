package com.cir0cuit.devtoys.tools.text

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.MonoTextField
import com.cir0cuit.devtoys.core.ui.OutputPanel
import com.cir0cuit.devtoys.core.ui.SelectField
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolCategory
import com.cir0cuit.devtoys.tools.base.ToolDescriptor

internal enum class EscapeFormat(val label: String) {
    JSON("JSON"),
    JAVA("Java"),
    KOTLIN("Kotlin"),
    JAVASCRIPT("JavaScript"),
    CSHARP("C#"),
    PYTHON("Python"),
    SQL("SQL string"),
    SHELL("Shell (POSIX)"),
    CSV("CSV field"),
    REGEX("Regex literal"),
    UNICODE("Unicode \\uXXXX"),
    UNICODE_BRACES("Unicode \\u{X}")
}

class StringEscapeTool : Tool {
    override val descriptor = ToolDescriptor(
        "string-escape", "String Escape", "Escape strings for code, SQL, shell or CSV",
        Icons.Default.FormatQuote, ToolCategory.TEXT,
        listOf("escape", "unescape", "string", "json", "java", "kotlin", "sql", "shell", "csv", "regex", "unicode", "quote")
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable override fun Content(modifier: Modifier) {
        var input by rememberSaveable(saver = boundedTextSaver) { mutableStateOf("Say \"hi\"\n\tto café") }
        var formatName by rememberSaveable { mutableStateOf(EscapeFormat.JSON.name) }
        var unescape by rememberSaveable { mutableStateOf(false) }
        val format = EscapeFormat.entries.firstOrNull { it.name == formatName } ?: EscapeFormat.JSON

        val result by rememberDebounced(Result.success(""), input, format, unescape) {
            runCatching { if (unescape) unescapeString(input, format) else escapeString(input, format) }
        }

        ToolScaffold(descriptor.title, modifier = modifier) {
            MonoTextField(input, { input = it }, "Input", minLines = 4)
            Spacer(Modifier.height(16.dp))
            SelectField("Format", format, EscapeFormat.entries, { formatName = it.name }, Modifier.fillMaxWidth()) { it.label }
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf("Escape", "Unescape").forEachIndexed { i, label ->
                    SegmentedButton(
                        selected = unescape == (i == 1),
                        onClick = { unescape = i == 1 },
                        shape = SegmentedButtonDefaults.itemShape(i, 2),
                        label = { Text(label, maxLines = 1) }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            val error = result.exceptionOrNull()
            if (error != null) {
                Text(error.message ?: "Could not unescape the text.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            } else {
                OutputPanel(result.getOrDefault(""), label = if (unescape) "Unescaped" else "Escaped")
            }
        }
    }
}

private fun hex4(c: Int) = Integer.toHexString(c).uppercase().padStart(4, '0')

/** Escapes [s] so it can be pasted inside a string literal (or field) of [format]. */
internal fun escapeString(s: String, format: EscapeFormat): String = when (format) {
    EscapeFormat.SQL -> s.replace("'", "''")
    EscapeFormat.SHELL -> "'" + s.replace("'", "'\\''") + "'"
    EscapeFormat.CSV -> "\"" + s.replace("\"", "\"\"") + "\""
    EscapeFormat.REGEX -> buildString { s.forEach { if (it in "\\^$.|?*+()[]{}") append('\\'); append(it) } }
    EscapeFormat.UNICODE -> buildString { s.forEach { if (it.code in 0x20..0x7E) append(it) else append("\\u").append(hex4(it.code)) } }
    EscapeFormat.UNICODE_BRACES -> buildString {
        s.codePoints().forEach { if (it in 0x20..0x7E) appendCodePoint(it) else append("\\u{").append(Integer.toHexString(it).uppercase()).append('}') }
    }
    else -> buildString {
        for (c in s) {
            when {
                c == '\\' -> append("\\\\")
                c == '"' -> append("\\\"")
                c == '\n' -> append("\\n")
                c == '\r' -> append("\\r")
                c == '\t' -> append("\\t")
                c == '\b' -> append("\\b")
                c == '\u000C' && format != EscapeFormat.KOTLIN -> append("\\f")
                c == '$' && format == EscapeFormat.KOTLIN -> append("\\$")
                c.code < 0x20 || c.code == 0x7F || c.code == 0x2028 || c.code == 0x2029 -> append("\\u").append(hex4(c.code))
                else -> append(c)
            }
        }
    }
}

/**
 * Reverses [escapeString]. The C-style formats share one lenient decoder that accepts the escapes of
 * all of them. Throws IllegalArgumentException naming the 1-based position of an invalid escape.
 */
internal fun unescapeString(s: String, format: EscapeFormat): String = when (format) {
    EscapeFormat.SQL -> s.replace("''", "'")
    EscapeFormat.SHELL -> unescapeShell(s)
    EscapeFormat.CSV -> unescapeCsv(s)
    EscapeFormat.REGEX -> Regex("\\\\([\\\\^$.|?*+()\\[\\]{}])").replace(s) { it.groupValues[1] }
    EscapeFormat.UNICODE, EscapeFormat.UNICODE_BRACES ->
        Regex("\\\\u\\{([0-9A-Fa-f]{1,6})\\}|\\\\u([0-9A-Fa-f]{4})|\\\\U([0-9A-Fa-f]{8})").replace(s) { m ->
            val hex = m.groupValues.drop(1).first { it.isNotEmpty() }
            codePointString(hex.toLong(16).toInt(), m.range.first)
        }
    else -> unescapeCStyle(s)
}

private fun codePointString(cp: Int, index: Int): String {
    require(Character.isValidCodePoint(cp)) { "Code point at position ${index + 1} is above U+10FFFF. Use at most 6 hex digits up to 10FFFF." }
    return String(Character.toChars(cp))
}

private fun unescapeCStyle(s: String): String = buildString {
    var i = 0
    fun hexAt(start: Int, len: Int): Int {
        val digits = s.substring(start, minOf(start + len, s.length))
        require(digits.length == len && digits.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
            "Invalid escape at position ${start - 1}: expected $len hex digits. Write a literal backslash as \\\\."
        }
        return digits.toLong(16).toInt()
    }
    while (i < s.length) {
        val c = s[i]
        if (c != '\\') { append(c); i++; continue }
        require(i + 1 < s.length) { "Lone backslash at position ${i + 1}. Write a literal backslash as \\\\." }
        val e = s[i + 1]
        i += 2
        when (e) {
            'n' -> append('\n'); 'r' -> append('\r'); 't' -> append('\t'); 'b' -> append('\b')
            'f' -> append('\u000C'); 'v' -> append('\u000B'); '0' -> append('\u0000'); 'a' -> append('\u0007')
            '\\', '"', '\'', '/', '$', '`', '?' -> append(e)
            '\n' -> {} // line continuation
            'x' -> { append(hexAt(i, 2).toChar()); i += 2 }
            'U' -> { append(codePointString(hexAt(i, 8), i - 2)); i += 8 }
            'u' -> if (i < s.length && s[i] == '{') {
                val close = s.indexOf('}', i)
                require(close > i + 1 && close - i <= 7) { "Invalid \\u{…} escape at position ${i - 1}. Use 1 to 6 hex digits between the braces." }
                append(codePointString(hexAt(i + 1, close - i - 1), i - 2))
                i = close + 1
            } else {
                append(hexAt(i, 4).toChar()); i += 4
            }
            else -> throw IllegalArgumentException("Unknown escape \\$e at position ${i - 1}. Write a literal backslash as \\\\.")
        }
    }
}

/** Decodes one POSIX shell word: single quotes, double quotes and backslashes. */
private fun unescapeShell(s: String): String = buildString {
    var i = 0
    val t = s.trim()
    while (i < t.length) {
        when (val c = t[i]) {
            '\'' -> {
                val end = t.indexOf('\'', i + 1)
                require(end >= 0) { "Unclosed single quote at position ${i + 1}. Add the closing '." }
                append(t, i + 1, end); i = end + 1
            }
            '"' -> {
                i++
                while (true) {
                    require(i < t.length) { "Unclosed double quote. Add the closing \"." }
                    val d = t[i]
                    if (d == '"') { i++; break }
                    if (d == '\\' && i + 1 < t.length && t[i + 1] in "$`\"\\\n") { append(t[i + 1]); i += 2 } else { append(d); i++ }
                }
            }
            '\\' -> { if (i + 1 < t.length) append(t[i + 1]); i += 2 }
            else -> { append(c); i++ }
        }
    }
}

private fun unescapeCsv(s: String): String {
    val t = s.trim()
    if (!t.startsWith('"')) return s
    require(t.length >= 2 && t.endsWith('"')) { "The field starts with a quote but does not end with one. Add the closing \"." }
    val inner = t.substring(1, t.length - 1)
    require('"' !in inner.replace("\"\"", "")) { "Unpaired quote inside the field. Double every quote inside a quoted field (\"\")." }
    return inner.replace("\"\"", "\"")
}
