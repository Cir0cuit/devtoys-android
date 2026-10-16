package com.cir0cuit.devtoys.tools.encoders

import android.util.Base64
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TextFormat
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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

/**
 * Encode / decode modes, each with its own saved input and a live output slab.
 * [extraMode] adds a third segment rendering [extraContent]; the host scaffold must then be
 * non-scrollable, so the encode/decode body scrolls itself.
 */
@Composable
internal fun EncodeDecodePane(
    encode: (String) -> String,
    decode: (String) -> String,
    extraMode: String? = null,
    extraContent: @Composable () -> Unit = {}
) {
    var encodeInput by rememberSaveableText()
    var decodeInput by rememberSaveableText()
    var mode by rememberSaveable { mutableStateOf(0) }

    ModeSelector(listOfNotNull("Encode", "Decode", extraMode), mode, { mode = it })
    Spacer(Modifier.height(16.dp))
    if (mode == 2) {
        extraContent()
    } else {
        val isEncode = mode == 0
        val input = if (isEncode) encodeInput else decodeInput
        val output = remember(input, isEncode) { if (isEncode) encode(input) else decode(input) }
        Column(if (extraMode != null) Modifier.verticalScroll(rememberScrollState()) else Modifier) {
            MonoTextField(
                input,
                { if (isEncode) encodeInput = it else decodeInput = it },
                if (isEncode) "Text to encode" else "Text to decode",
                minLines = 5
            )
            Spacer(Modifier.height(16.dp))
            if (output.startsWith("Error: ")) {
                ErrorText(output.removePrefix("Error: ").replaceFirstChar { it.uppercase() })
            } else {
                OutputPanel(output, label = if (isEncode) "Encoded" else "Decoded")
                if (output.isNotEmpty()) {
                    // Round-trip: hand the result to the other mode.
                    TextButton(
                        onClick = {
                            if (isEncode) decodeInput = output else encodeInput = output
                            mode = 1 - mode
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.SwapVert, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (isEncode) "Decode this" else "Encode this")
                    }
                }
            }
        }
    }
}

internal fun base64Encode(input: String): String =
    if (input.isEmpty()) "" else Base64.encodeToString(input.toByteArray(), Base64.NO_WRAP)

internal fun base64Decode(input: String): String =
    try {
        if (input.isEmpty()) "" else String(Base64.decode(input, Base64.NO_WRAP))
    } catch (e: IllegalArgumentException) {
        "Error: this is not valid Base64. Check that the text is complete and the = padding is correct."
    }

class Base64Tool : Tool {
    override val descriptor = ToolDescriptor(
        id = "base64",
        title = "Base64 Encoder / Decoder",
        description = "Encode and decode Base64 text",
        icon = Icons.Default.TextFormat,
        category = ToolCategory.ENCODERS_DECODERS,
        keywords = listOf("base64", "encode", "decode")
    )

    @Composable
    override fun Content(modifier: Modifier) {
        ToolScaffold(descriptor.title, modifier = modifier) {
            EncodeDecodePane(::base64Encode, ::base64Decode)
        }
    }
}
