package com.cir0cuit.devtoys.tools.encoders

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.designsystem.CodeTextStyle
import com.cir0cuit.devtoys.core.ui.MonoTextField
import com.cir0cuit.devtoys.core.ui.OutputPanel
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolCategory
import com.cir0cuit.devtoys.tools.base.ToolDescriptor

internal fun rotN(input: String, s: Int): String =
    input.map { c ->
        when (c) {
            in 'a'..'z' -> 'a' + (c - 'a' + s) % 26
            in 'A'..'Z' -> 'A' + (c - 'A' + s) % 26
            else -> c
        }
    }.joinToString("")

class Rot13Tool : Tool {
    override val descriptor = ToolDescriptor(
        id = "rot13",
        title = "ROT13 / Caesar Cipher",
        description = "Shift letters with ROT13 or a Caesar cipher",
        icon = Icons.AutoMirrored.Filled.RotateRight,
        category = ToolCategory.ENCODERS_DECODERS,
        keywords = listOf("rot13", "cipher", "caesar")
    )

    @Composable
    override fun Content(modifier: Modifier) {
        var input by rememberSaveableText()
        var shift by rememberSaveable { mutableStateOf(13f) }
        val output = remember(input, shift) { rotN(input, shift.toInt()) }
        ToolScaffold(descriptor.title, modifier = modifier) {
            MonoTextField(input, { input = it }, "Text", minLines = 4)
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Shift", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(shift.toInt().toString(), style = CodeTextStyle)
                // ROT13 is the common case; offer a way back after sliding away from it.
                TextButton(onClick = { shift = 13f }, enabled = shift.toInt() != 13) { Text("ROT13") }
            }
            Slider(value = shift, onValueChange = { shift = it }, valueRange = 1f..25f, steps = 23)
            Spacer(Modifier.height(16.dp))
            OutputPanel(output, label = "Shifted")
        }
    }
}
