package com.cir0cuit.devtoys.tools.text

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.designsystem.CodeTextStyle
import com.cir0cuit.devtoys.core.ui.MonoTextField
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolCategory
import com.cir0cuit.devtoys.tools.base.ToolDescriptor
import java.text.Normalizer

internal data class CodePointInfo(
    val glyph: String,
    val hex: String,
    val name: String,
    val category: String,
    val block: String,
    val utf8: String,
    val utf16: String,
    val escapes: String,
    val suspicious: Boolean
)

internal class Inspection(val codePoints: IntArray, val suspiciousCount: Int, val scripts: List<String>)

private val EMPTY_INSPECTION = Inspection(IntArray(0), 0, emptyList())

class UnicodeInspectorTool : Tool {
    override val descriptor = ToolDescriptor(
        "unicode-inspector", "Unicode Inspector", "Inspect code points and find hidden characters",
        Icons.Default.Translate, ToolCategory.TEXT,
        listOf("unicode", "ascii", "table", "code point", "invisible", "zero width", "bidi", "homoglyph", "normalize", "nfc", "utf-8", "character")
    )

    @Composable override fun Content(modifier: Modifier) {
        var tab by rememberSaveable { mutableStateOf(0) }
        var input by rememberSaveable(saver = boundedTextSaver) { mutableStateOf("pay" + Char(0x200B) + "pal " + Char(0x0430) + "pple") }

        ToolScaffold(descriptor.title, modifier = modifier, scrollable = false) {
            TabRow(selectedTabIndex = tab) {
                listOf("Inspect", "Normalize", "ASCII table").forEachIndexed { i, label ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(label, maxLines = 1) })
                }
            }
            Spacer(Modifier.height(8.dp))
            when (tab) {
                0 -> InspectPane(input) { input = it }
                1 -> NormalizePane(input) { input = it }
                else -> AsciiTable(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ColumnScope.InspectPane(input: String, onInput: (String) -> Unit) {
    val inspection by rememberDebounced(EMPTY_INSPECTION, input) { inspect(input) }
    MonoTextField(input, onInput, "Text", minLines = 2, maxLines = 4)
    val count = inspection.codePoints.size
    Text(
        if (count == 1) "1 code point" else "$count code points",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp)
    )
    if (inspection.suspiciousCount > 0) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${inspection.suspiciousCount} invisible or direction-changing characters, highlighted below.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { onInput(removeInvisible(input)) }) { Text("Remove", maxLines = 1) }
        }
    }
    if (inspection.scripts.size > 1) {
        Text(
            "Mixed scripts: ${inspection.scripts.joinToString()}. Look-alike letters may differ.",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }
    Spacer(Modifier.height(8.dp))
    LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
        items(inspection.codePoints.size) { i -> CodePointRow(codePointInfo(inspection.codePoints[i])) }
    }
}

@Composable
private fun CodePointRow(info: CodePointInfo) {
    val bg = if (info.suspicious) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.background
    Row(Modifier.fillMaxWidth().background(bg).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(info.glyph, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, modifier = Modifier.width(48.dp))
        SelectionContainer(Modifier.weight(1f)) {
            Column {
                Text("${info.hex}  ${info.name}", style = CodeTextStyle)
                Text(
                    "${info.category} · ${info.block}\nUTF-8 ${info.utf8} · UTF-16 ${info.utf16}\n${info.escapes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    HorizontalDivider()
}

@Composable
private fun ColumnScope.NormalizePane(input: String, onInput: (String) -> Unit) {
    val rows by rememberDebounced(emptyList<Pair<String, String>>(), input) { normalizationRows(input) }
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        MonoTextField(input, onInput, "Text", minLines = 3)
        Spacer(Modifier.height(16.dp))
        ValueRowsSlab(rows)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun AsciiTable(modifier: Modifier) {
    LazyColumn(modifier.fillMaxWidth()) {
        item {
            Text("Dec  Hex  Oct  Binary   Char", style = CodeTextStyle, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
            HorizontalDivider()
        }
        items(128) { cp ->
            SelectionContainer { Text(asciiRow(cp), style = CodeTextStyle, modifier = Modifier.padding(vertical = 4.dp)) }
        }
    }
}

private val CONTROL_NAMES = ("NUL SOH STX ETX EOT ENQ ACK BEL BS HT LF VT FF CR SO SI " +
    "DLE DC1 DC2 DC3 DC4 NAK SYN ETB CAN EM SUB ESC FS GS RS US").split(' ')

/** Short name for an ASCII code: control abbreviation, SP, DEL or the character itself. */
internal fun asciiName(cp: Int): String = when {
    cp < 32 -> CONTROL_NAMES[cp]
    cp == 32 -> "SP"
    cp == 127 -> "DEL"
    else -> cp.toChar().toString()
}

internal fun asciiRow(cp: Int): String =
    cp.toString().padStart(3) + "  " + Integer.toHexString(cp).uppercase().padStart(2, '0') + "   " +
        Integer.toOctalString(cp).padStart(3, '0') + "  " + Integer.toBinaryString(cp).padStart(7, '0') + "  " + asciiName(cp)

private val INVISIBLE_LETTERS = setOf(0x115F, 0x1160, 0x3164, 0xFFA0)

/** True for format (zero-width, bidi), separator and control characters other than tab and newlines. */
internal fun isInvisibleOrBidi(cp: Int): Boolean = when (Character.getType(cp)) {
    Character.FORMAT.toInt(), Character.LINE_SEPARATOR.toInt(), Character.PARAGRAPH_SEPARATOR.toInt() -> true
    Character.SPACE_SEPARATOR.toInt() -> cp != 0x20
    Character.CONTROL.toInt() -> cp != 0x09 && cp != 0x0A && cp != 0x0D
    else -> cp in INVISIBLE_LETTERS
}

/** Turns odd spaces into plain spaces and drops the other invisible or bidi characters. */
internal fun removeInvisible(s: String): String = buildString {
    s.codePoints().forEach { cp ->
        when {
            !isInvisibleOrBidi(cp) -> appendCodePoint(cp)
            Character.getType(cp) == Character.SPACE_SEPARATOR.toInt() -> append(' ')
            else -> {}
        }
    }
}

private fun hexByte(b: Byte) = Integer.toHexString(b.toInt() and 0xFF).uppercase().padStart(2, '0')

private fun constantName(s: String) = s.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

internal fun inspect(s: String): Inspection {
    val cps = s.codePoints().toArray()
    val scripts = cps.asSequence().filter { Character.isLetter(it) }.map { Character.UnicodeScript.of(it) }
        .filter { it != Character.UnicodeScript.COMMON && it != Character.UnicodeScript.INHERITED }
        .distinct().map { constantName(it.name) }.toList()
    return Inspection(cps, cps.count(::isInvisibleOrBidi), scripts)
}

internal fun codePointInfo(cp: Int): CodePointInfo {
    val type = Character.getType(cp)
    val category = CharCategory.entries.firstOrNull { it.value == type }
    val text = String(Character.toChars(cp))
    val utf8 = text.toByteArray(Charsets.UTF_8)
    val hex = Integer.toHexString(cp).uppercase().padStart(4, '0')
    val suspicious = isInvisibleOrBidi(cp)
    return CodePointInfo(
        glyph = when {
            type == Character.CONTROL.toInt() && cp < 0x20 -> String(Character.toChars(0x2400 + cp)) // Control Pictures block
            suspicious -> "·"
            type == Character.NON_SPACING_MARK.toInt() || type == Character.ENCLOSING_MARK.toInt() -> String(Character.toChars(0x25CC)) + text
            else -> text
        },
        hex = "U+$hex",
        name = Character.getName(cp) ?: "Unassigned",
        category = if (category == null) "Unknown category" else "${category.code} ${constantName(category.name)}",
        block = Character.UnicodeBlock.of(cp)?.let { constantName(it.toString()) } ?: "No block",
        utf8 = utf8.joinToString(" ") { hexByte(it) },
        utf16 = text.map { Integer.toHexString(it.code).uppercase().padStart(4, '0') }.joinToString(" "),
        escapes = text.map { "\\u" + Integer.toHexString(it.code).uppercase().padStart(4, '0') }.joinToString("") +
            " · \\u{$hex} · &#x$hex; · " + utf8.joinToString("") { "%" + hexByte(it) },
        suspicious = suspicious
    )
}

internal fun normalizationRows(s: String): List<Pair<String, String>> =
    listOf(Normalizer.Form.NFC, Normalizer.Form.NFD, Normalizer.Form.NFKC, Normalizer.Form.NFKD).map { form ->
        val out = Normalizer.normalize(s, form)
        val cps = out.codePointCount(0, out.length)
        val same = if (out == s) "same as input" else "differs from input"
        "${form.name}, $cps code points, $same" to out
    }
