package com.cir0cuit.devtoys.tools.encoders
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RotateRight

import androidx.compose.material3.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*

internal fun rotN(input: String, s: Int): String =
    input.map { c ->
        when {
            c in 'a'..'z' -> 'a' + (c - 'a' + s) % 26
            c in 'A'..'Z' -> 'A' + (c - 'A' + s) % 26
            else -> c
        }
    }.joinToString("")

class Rot13Tool : Tool {
    override val descriptor = ToolDescriptor("rot13", android.R.string.untitled, android.R.string.untitled, Icons.Default.RotateRight, ToolCategory.ENCODERS_DECODERS, listOf("rot13", "cipher", "caesar"))
    @Composable override fun Content(modifier: Modifier) {
        var input by rememberSaveableText()
        var shift by rememberSaveable { mutableStateOf(13f) }
        val output = remember(input, shift) { rotN(input, shift.toInt()) }
        ToolScaffold("ROT13 / Caesar Cipher", modifier = modifier) {
            Text("Shift: ${shift.toInt()}")
            Slider(value = shift, onValueChange = { shift = it }, valueRange = 1f..25f, steps = 23)
            Spacer(modifier = Modifier.height(16.dp))
            MonoTextField(input, { input = it }, "Input")
            Spacer(modifier = Modifier.height(16.dp))
            OutputPanel(output)
        }
    }
}