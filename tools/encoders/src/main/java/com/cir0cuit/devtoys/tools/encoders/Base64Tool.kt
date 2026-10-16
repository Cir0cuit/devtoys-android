package com.cir0cuit.devtoys.tools.encoders

import android.util.Base64
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TextFormat

import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.MonoTextField
import com.cir0cuit.devtoys.core.ui.OutputPanel
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolCategory
import com.cir0cuit.devtoys.tools.base.ToolDescriptor

private const val MAX_SAVED_CHARS = 32_000

private val CappedTextSaver: Saver<MutableState<String>, String> = Saver(
    save = { state -> if (state.value.length <= MAX_SAVED_CHARS) state.value else "" },
    restore = { mutableStateOf(it) }
)

@Composable
internal fun rememberSaveableText(initial: String = ""): MutableState<String> =
    rememberSaveable(saver = CappedTextSaver) { mutableStateOf(initial) }

class Base64Tool : Tool {
    override val descriptor: ToolDescriptor = ToolDescriptor(
        id = "base64",
        titleRes = android.R.string.untitled, // Placeholder
        descriptionRes = android.R.string.untitled, // Placeholder
        icon = Icons.Default.TextFormat,
        category = ToolCategory.ENCODERS_DECODERS,
        keywords = listOf("base64", "encode", "decode")
    )

    @Composable
    override fun Content(modifier: Modifier) {
        var encodeInput by rememberSaveableText()
        var decodeInput by rememberSaveableText()
        var isEncodeMode by rememberSaveable { mutableStateOf(true) }

        val encodeOutput = remember(encodeInput) {
            try {
                if (encodeInput.isEmpty()) {
                    ""
                } else {
                    Base64.encodeToString(encodeInput.toByteArray(), Base64.NO_WRAP)
                }
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
        }
        
        val decodeOutput = remember(decodeInput) {
            try {
                if (decodeInput.isEmpty()) {
                    ""
                } else {
                    String(Base64.decode(decodeInput, Base64.NO_WRAP))
                }
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
        }

        ToolScaffold(
            title = "Base64 Encoder / Decoder",
            modifier = modifier
        ) {
            TabRow(selectedTabIndex = if (isEncodeMode) 0 else 1) {
                Tab(selected = isEncodeMode, onClick = { isEncodeMode = true }, text = { Text("Encode") })
                Tab(selected = !isEncodeMode, onClick = { isEncodeMode = false }, text = { Text("Decode") })
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            if (isEncodeMode) {
                MonoTextField(
                    value = encodeInput,
                    onValueChange = { encodeInput = it },
                    label = "Input to Encode"
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Output")
                OutputPanel(content = encodeOutput)
            } else {
                MonoTextField(
                    value = decodeInput,
                    onValueChange = { decodeInput = it },
                    label = "Input to Decode"
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Output")
                OutputPanel(content = decodeOutput)
            }
        }
    }
}
