package com.cir0cuit.devtoys.tools.encoders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Filter1
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.designsystem.CodeTextStyle
import com.cir0cuit.devtoys.core.ui.MonoTextField
import com.cir0cuit.devtoys.core.ui.SelectField
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolCategory
import com.cir0cuit.devtoys.tools.base.ToolDescriptor
import java.math.BigInteger

internal fun parseBig(s: String, radix: Int): BigInteger? {
    var t = s.filterNot { it.isWhitespace() || it == '_' }
    val negative = t.startsWith("-")
    t = t.removePrefix("-")
    when (radix) {
        16 -> t = t.removePrefix("0x").removePrefix("0X")
        2 -> t = t.removePrefix("0b").removePrefix("0B")
        8 -> t = t.removePrefix("0o").removePrefix("0O")
    }
    if (t.isEmpty() || t.length > 4096 || t.startsWith("-") || t.startsWith("+")) return null
    return runCatching { BigInteger(t, radix) }.getOrNull()?.let { if (negative) it.negate() else it }
}

/** The [bits]-wide two's complement bit pattern of [v], or null when [v] does not fit. */
internal fun toWidth(v: BigInteger, bits: Int): BigInteger? {
    val limit = BigInteger.ONE.shiftLeft(bits)
    if (v >= limit || v < BigInteger.ONE.shiftLeft(bits - 1).negate()) return null
    return v.and(limit - BigInteger.ONE)
}

/** Reads an unsigned [bits]-wide pattern as a signed two's complement number. */
internal fun signedOf(pattern: BigInteger, bits: Int): BigInteger =
    if (pattern.testBit(bits - 1)) pattern - BigInteger.ONE.shiftLeft(bits) else pattern

/** Formats [v] in [radix], zero-padded to [padTo] digits, optionally with `_` groups and a 0x / 0b / 0o prefix. */
internal fun formatRadix(v: BigInteger, radix: Int, group: Boolean = false, prefix: Boolean = false, padTo: Int = 0): String {
    var digits = v.abs().toString(radix).padStart(padTo, '0')
    if (radix == 16) digits = digits.uppercase()
    if (group) {
        val size = if (radix == 10 || radix == 8) 3 else 4
        digits = digits.reversed().chunked(size).joinToString("_").reversed()
    }
    val p = if (prefix) PREFIXES[radix].orEmpty() else ""
    return (if (v.signum() < 0) "-" else "") + p + digits
}

/** Hex bytes of a [bits]-wide pattern, big-endian or little-endian. */
internal fun byteOrder(pattern: BigInteger, bits: Int, littleEndian: Boolean): String =
    pattern.toString(16).uppercase().padStart(bits / 4, '0').chunked(2).let { if (littleEndian) it.reversed() else it }.joinToString(" ")

class NumberBaseConverterTool : Tool {
    override val descriptor = ToolDescriptor(
        id = "base-converter",
        title = "Number Base Converter",
        description = "Convert numbers between bin, oct, dec and hex",
        icon = Icons.Default.Filter1,
        category = ToolCategory.CONVERTERS,
        keywords = listOf("base", "converter", "binary", "hex", "two's complement", "signed", "endian", "radix")
    )

    @OptIn(ExperimentalMaterial3Api::class) // FilterChip
    @Composable
    override fun Content(modifier: Modifier) {
        var input by rememberSaveable { mutableStateOf("42") }
        var radixIndex by rememberSaveable { mutableStateOf(0) }
        var width by rememberSaveable { mutableStateOf(0) } // 0 = any
        var otherBase by rememberSaveable { mutableStateOf("36") }
        var group by rememberSaveable { mutableStateOf(false) }
        var prefix by rememberSaveable { mutableStateOf(false) }
        val radix = RADIXES[radixIndex].second
        val value = remember(input, radix) { parseBig(input, radix) }
        val pattern = remember(value, width) { if (width == 0) value else value?.let { toWidth(it, width) } }
        val other = otherBase.toIntOrNull()?.takeIf { it in 2..36 }

        ToolScaffold(descriptor.title, modifier = modifier) {
            MonoTextField(input, { input = it }, "Number", minLines = 1)
            Spacer(Modifier.height(16.dp))
            Text("Input base", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            ModeSelector(RADIXES.map { it.first }, radixIndex, { i ->
                // Keep the same number when switching base, so the choice only changes how it is read.
                (pattern ?: value)?.let { input = formatRadix(it, RADIXES[i].second) }
                radixIndex = i
            })
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SelectField("Bit width", width, WIDTHS, { width = it }, Modifier.weight(1f), optionLabel = { if (it == 0) "Any" else "$it-bit" })
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    otherBase, { otherBase = it.filter(Char::isDigit).take(2) },
                    label = { Text("Other base") },
                    singleLine = true,
                    isError = other == null,
                    textStyle = CodeTextStyle,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = group, onClick = { group = !group }, label = { Text("Group digits") })
                FilterChip(selected = prefix, onClick = { prefix = !prefix }, label = { Text("Prefix") })
            }
            Spacer(Modifier.height(16.dp))
            when {
                input.isNotBlank() && value == null -> ErrorText("This is not a valid ${NAMES[radix]} number. ${HINTS[radix]}")
                value != null && pattern == null -> ErrorText("This number does not fit in $width bits. Choose a wider bit width or Any.")
                else -> {
                    val rows = mutableListOf<Pair<String, String>>()
                    fun fmt(r: Int, padBits: Boolean) = pattern?.let {
                        formatRadix(it, r, group, prefix, if (padBits && width != 0) (width + bitsPerDigit(r) - 1) / bitsPerDigit(r) else 0)
                    } ?: ""
                    if (width == 0) {
                        rows += "Decimal" to fmt(10, false)
                    } else {
                        rows += "Unsigned" to fmt(10, false)
                        rows += "Signed" to (pattern?.let { formatRadix(signedOf(it, width), 10, group) } ?: "")
                    }
                    rows += "Hexadecimal" to fmt(16, true)
                    rows += "Binary" to fmt(2, true)
                    rows += "Octal" to fmt(8, false)
                    if (other != null) rows += "Base $other" to fmt(other, false)
                    if (width >= 16) {
                        rows += "Bytes, big-endian" to (pattern?.let { byteOrder(it, width, false) } ?: "")
                        rows += "Bytes, little-endian" to (pattern?.let { byteOrder(it, width, true) } ?: "")
                    }
                    ValueSlab(rows)
                }
            }
        }
    }
}

// Only hex and binary are padded to the bit width.
private fun bitsPerDigit(radix: Int) = if (radix == 16) 4 else 1

private val RADIXES = listOf("Dec" to 10, "Hex" to 16, "Bin" to 2, "Oct" to 8)
private val WIDTHS = listOf(0, 8, 16, 32, 64)
private val PREFIXES = mapOf(16 to "0x", 2 to "0b", 8 to "0o")
private val NAMES = mapOf(10 to "decimal", 16 to "hexadecimal", 2 to "binary", 8 to "octal")
private val HINTS = mapOf(
    10 to "Use digits 0-9.",
    16 to "Use digits 0-9 and A-F.",
    2 to "Use only 0 and 1.",
    8 to "Use digits 0-7."
)
