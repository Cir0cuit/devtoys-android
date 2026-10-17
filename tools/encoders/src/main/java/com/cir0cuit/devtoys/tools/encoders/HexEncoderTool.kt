package com.cir0cuit.devtoys.tools.encoders

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.tools.base.ShareKind
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolCategory
import com.cir0cuit.devtoys.tools.base.ToolDescriptor

internal enum class HexFormat(val label: String) {
    PLAIN("Plain"), SPACED("Spaced"), COLONS("Colons"), ESCAPED("\\x escapes"), LIST("0x list"),
    C_ARRAY("C array"), KOTLIN("Kotlin bytes"), PYTHON("Python bytes"), DUMP("Hex dump")
}

internal fun hexEncode(input: String, format: HexFormat = HexFormat.PLAIN, upper: Boolean = false): String {
    val bytes = input.toByteArray(Charsets.UTF_8)
    if (bytes.isEmpty()) return ""
    val pattern = if (upper) "%02X" else "%02x"
    fun join(sep: String, prefix: String = "") = bytes.joinToString(sep) { prefix + pattern.format(it) }
    // Code formats break every 16 bytes so long arrays stay readable.
    fun lines(sep: String, item: (Byte) -> String) = bytes.toList().chunked(16).joinToString(",\n    ") { row -> row.joinToString(sep, transform = item) }
    return when (format) {
        HexFormat.PLAIN -> join("")
        HexFormat.SPACED -> join(" ")
        HexFormat.COLONS -> join(":")
        HexFormat.ESCAPED -> join("", "\\x")
        HexFormat.LIST -> join(", ", "0x")
        HexFormat.C_ARRAY -> "{\n    " + lines(", ") { "0x" + pattern.format(it) } + "\n}"
        HexFormat.KOTLIN -> "byteArrayOf(\n    " + lines(", ") { it.toString() } + "\n)"
        HexFormat.PYTHON -> "b'" + join("", "\\x") + "'"
        HexFormat.DUMP -> hexDump(bytes)
    }
}

/** Parses hex digits, ignoring whitespace, `:` `-` `,` separators, braces and `0x` / `\x` byte prefixes. */
internal fun hexDecodeBytes(input: String): ByteArray {
    val cleaned = input.replace(Regex("""0[xX]|\\x|[\s:,{}\-]"""), "")
    return when {
        cleaned.isEmpty() -> ByteArray(0)
        !cleaned.matches(Regex("[0-9a-fA-F]+")) -> throw IllegalArgumentException("The input contains characters that are not hex digits (0-9, a-f).")
        cleaned.length % 2 != 0 -> throw IllegalArgumentException("Hex needs an even number of digits. Add a leading 0 or remove a digit.")
        else -> ByteArray(cleaned.length / 2) { cleaned.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
    }
}

class HexEncoderTool : Tool {
    override val descriptor = ToolDescriptor(
        id = "hex",
        title = "Hex Encoder / Decoder",
        description = "Convert text to hex bytes and back",
        icon = Icons.Default.Numbers,
        category = ToolCategory.ENCODERS_DECODERS,
        keywords = listOf("hex", "encode", "hexdump", "bytes", "c array"),
        accepts = setOf(ShareKind.TEXT)
    )

    @OptIn(ExperimentalMaterial3Api::class) // FilterChip
    @Composable
    override fun Content(modifier: Modifier) {
        var format by rememberSaveable { mutableStateOf(HexFormat.PLAIN) }
        var upper by rememberSaveable { mutableStateOf(false) }
        ToolScaffold(descriptor.title, modifier = modifier) {
            EncodeDecodePane(
                encode = { hexEncode(it, format, upper) },
                decodeBytes = ::hexDecodeBytes,
                optionsKey = format to upper,
                options = { isEncode ->
                    if (isEncode) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SelectField("Format", format, HexFormat.entries, { format = it }, Modifier.weight(1f), optionLabel = { it.label })
                            Spacer(Modifier.width(8.dp))
                            FilterChip(selected = upper, onClick = { upper = !upper }, label = { Text("Uppercase") })
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            )
        }
    }
}
