package com.cir0cuit.devtoys.tools.encoders

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Filter1
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.MonoTextField
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolCategory
import com.cir0cuit.devtoys.tools.base.ToolDescriptor
import java.math.BigInteger

internal fun parseBig(s: String, radix: Int): BigInteger? {
    var t = s.trim().replace("_", "")
    when (radix) {
        16 -> t = t.removePrefix("0x").removePrefix("0X")
        2 -> t = t.removePrefix("0b").removePrefix("0B")
        8 -> t = t.removePrefix("0o").removePrefix("0O")
    }
    if (t.isEmpty() || t.length > 4096) return null
    return runCatching { BigInteger(t, radix) }.getOrNull()
}

class NumberBaseConverterTool : Tool {
    override val descriptor = ToolDescriptor(
        id = "base-converter",
        title = "Number Base Converter",
        description = "Convert numbers between bin, oct, dec and hex",
        icon = Icons.Default.Filter1,
        category = ToolCategory.CONVERTERS,
        keywords = listOf("base", "converter", "binary", "hex")
    )

    @Composable
    override fun Content(modifier: Modifier) {
        var input by rememberSaveable { mutableStateOf("42") }
        var radixIndex by rememberSaveable { mutableStateOf(0) }
        val radix = RADIXES[radixIndex].second
        val value = remember(input, radix) { parseBig(input, radix) }

        ToolScaffold(descriptor.title, modifier = modifier) {
            MonoTextField(input, { input = it }, "Number", minLines = 1)
            Spacer(Modifier.height(16.dp))
            Text("Input base", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            ModeSelector(RADIXES.map { it.first }, radixIndex, { i ->
                // Keep the same number when switching base, so the choice only changes how it is read.
                value?.let { input = format(it, RADIXES[i].second) }
                radixIndex = i
            })
            Spacer(Modifier.height(16.dp))
            if (input.isNotBlank() && value == null) {
                ErrorText("This is not a valid ${NAMES[radix]} number. ${HINTS[radix]}")
            } else {
                ValueSlab(RADIXES.map { (_, r) -> NAMES.getValue(r).replaceFirstChar { it.uppercase() } to (value?.let { format(it, r) } ?: "") })
            }
        }
    }
}

private val RADIXES = listOf("Dec" to 10, "Hex" to 16, "Bin" to 2, "Oct" to 8)
private val NAMES = mapOf(10 to "decimal", 16 to "hexadecimal", 2 to "binary", 8 to "octal")
private val HINTS = mapOf(
    10 to "Use digits 0-9.",
    16 to "Use digits 0-9 and A-F.",
    2 to "Use only 0 and 1.",
    8 to "Use digits 0-7."
)

private fun format(v: BigInteger, radix: Int): String = v.toString(radix).let { if (radix == 16) it.uppercase() else it }
