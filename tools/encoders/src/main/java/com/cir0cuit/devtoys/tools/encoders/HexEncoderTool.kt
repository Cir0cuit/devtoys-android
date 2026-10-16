package com.cir0cuit.devtoys.tools.encoders

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Numbers

import androidx.compose.material3.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*

internal fun hexEncode(input: String): String =
    try {
        if (input.isEmpty()) "" else input.toByteArray(Charsets.UTF_8).joinToString("") { "%02x".format(it) }
    } catch (e: Exception) { "Error: ${e.message}" }

internal fun hexDecode(input: String): String {
    val cleaned = input.replace(Regex("[\\s:-]"), "").removePrefix("0x").removePrefix("0X")
    return when {
        cleaned.isEmpty() -> ""
        !cleaned.matches(Regex("[0-9a-fA-F]+")) -> "Error: input contains non-hex characters"
        cleaned.length % 2 != 0 -> "Error: hex input must have an even number of digits"
        else -> cleaned.chunked(2).map { it.toInt(16).toByte() }.toByteArray().toString(Charsets.UTF_8)
    }
}

class HexEncoderTool : Tool {
    override val descriptor = ToolDescriptor("hex", android.R.string.untitled, android.R.string.untitled, Icons.Default.Numbers, ToolCategory.ENCODERS_DECODERS, listOf("hex", "encode"))
    @Composable override fun Content(modifier: Modifier) {
        var encodeInput by rememberSaveableText()
        var decodeInput by rememberSaveableText()
        var isEncodeMode by rememberSaveable { mutableStateOf(true) }

        val encodeOutput = remember(encodeInput) { hexEncode(encodeInput) }
        val decodeOutput = remember(decodeInput) { hexDecode(decodeInput) }
        
        ToolScaffold("Hex Encoder / Decoder", modifier = modifier) {
            TabRow(selectedTabIndex = if (isEncodeMode) 0 else 1) {
                Tab(selected = isEncodeMode, onClick = { isEncodeMode = true }, text = { Text("Encode") })
                Tab(selected = !isEncodeMode, onClick = { isEncodeMode = false }, text = { Text("Decode") })
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (isEncodeMode) {
                MonoTextField(encodeInput, { encodeInput = it }, "Input to Encode")
                Spacer(modifier = Modifier.height(16.dp))
                OutputPanel(encodeOutput)
            } else {
                MonoTextField(decodeInput, { decodeInput = it }, "Input to Decode")
                Spacer(modifier = Modifier.height(16.dp))
                OutputPanel(decodeOutput)
            }
        }
    }
}