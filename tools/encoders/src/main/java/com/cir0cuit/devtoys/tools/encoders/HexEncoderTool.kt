package com.cir0cuit.devtoys.tools.encoders

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolCategory
import com.cir0cuit.devtoys.tools.base.ToolDescriptor

internal fun hexEncode(input: String): String =
    input.toByteArray(Charsets.UTF_8).joinToString("") { "%02x".format(it) }

internal fun hexDecode(input: String): String {
    val cleaned = input.replace(Regex("""[\s:-]"""), "").removePrefix("0x").removePrefix("0X")
    return when {
        cleaned.isEmpty() -> ""
        !cleaned.matches(Regex("[0-9a-fA-F]+")) -> "Error: the input contains characters that are not hex digits (0-9, a-f)."
        cleaned.length % 2 != 0 -> "Error: hex needs an even number of digits. Add a leading 0 or remove a digit."
        else -> cleaned.chunked(2).map { it.toInt(16).toByte() }.toByteArray().toString(Charsets.UTF_8)
    }
}

class HexEncoderTool : Tool {
    override val descriptor = ToolDescriptor(
        id = "hex",
        title = "Hex Encoder / Decoder",
        description = "Convert text to hex bytes and back",
        icon = Icons.Default.Numbers,
        category = ToolCategory.ENCODERS_DECODERS,
        keywords = listOf("hex", "encode")
    )

    @Composable
    override fun Content(modifier: Modifier) {
        ToolScaffold(descriptor.title, modifier = modifier) {
            EncodeDecodePane(::hexEncode, ::hexDecode)
        }
    }
}
