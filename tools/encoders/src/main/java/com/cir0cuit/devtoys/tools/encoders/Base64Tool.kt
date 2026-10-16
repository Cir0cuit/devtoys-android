package com.cir0cuit.devtoys.tools.encoders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TextFormat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val MAX_SAVED_CHARS = 32_000

private val CappedTextSaver: Saver<MutableState<String>, String> = Saver(
    save = { state -> if (state.value.length <= MAX_SAVED_CHARS) state.value else "" },
    restore = { mutableStateOf(it) }
)

@Composable
internal fun rememberSaveableText(initial: String = ""): MutableState<String> =
    rememberSaveable(saver = CappedTextSaver) { mutableStateOf(initial) }

/** Result of one pane computation: [text] or [bytes], or an [error] to show instead. */
private class PaneOutput(val key: Any?, val text: String = "", val bytes: ByteArray? = null, val error: String? = null)

/**
 * Encode / decode modes, each with its own saved input and a live output slab.
 * [options] renders between input and output; [optionsKey] must change whenever an option the
 * codecs read changes. With [decodeBytes] the decoded result is shown as text when it is UTF-8 and
 * as a hex dump otherwise; it signals bad input by throwing IllegalArgumentException with a message.
 * [extraMode] adds a third segment rendering [extraContent]; the host scaffold must then be
 * non-scrollable, so the encode/decode body scrolls itself.
 */
@Composable
internal fun EncodeDecodePane(
    encode: (String) -> String,
    decode: (String) -> String = { "" },
    decodeBytes: ((String) -> ByteArray)? = null,
    optionsKey: Any? = null,
    options: @Composable (isEncode: Boolean) -> Unit = {},
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
        val key = listOf(input, isEncode, optionsKey)
        val output by produceState(PaneOutput(null), key) {
            if (input.length > 2_000) delay(150) // debounce typing in large inputs; the key change cancels this
            value = withContext(Dispatchers.Default) {
                when {
                    input.isEmpty() -> PaneOutput(key)
                    isEncode -> encode(input).let { if (it.startsWith("Error: ")) PaneOutput(key, error = it) else PaneOutput(key, it) }
                    decodeBytes != null -> try {
                        val bytes = decodeBytes(input)
                        utf8OrNull(bytes)?.let { PaneOutput(key, it) } ?: PaneOutput(key, bytes = bytes)
                    } catch (e: IllegalArgumentException) {
                        PaneOutput(key, error = e.message ?: "This input could not be decoded.")
                    }
                    else -> decode(input).let { if (it.startsWith("Error: ")) PaneOutput(key, error = it) else PaneOutput(key, it) }
                }
            }
        }
        val current = output.takeIf { it.key == key }
        Column(if (extraMode != null) Modifier.verticalScroll(rememberScrollState()) else Modifier) {
            MonoTextField(
                input,
                { if (isEncode) encodeInput = it else decodeInput = it },
                if (isEncode) "Text to encode" else "Text to decode",
                minLines = 5
            )
            Spacer(Modifier.height(16.dp))
            options(isEncode)
            val bytes = current?.bytes
            when {
                current?.error != null -> ErrorText(current.error.removePrefix("Error: ").replaceFirstChar { it.uppercase() })
                bytes != null -> BytesOutput(bytes, "Decoded")
                else -> {
                    val text = current?.text.orEmpty()
                    OutputPanel(text, label = if (isEncode) "Encoded" else "Decoded")
                    if (text.isNotEmpty()) {
                        // Round-trip: hand the result to the other mode.
                        TextButton(
                            onClick = {
                                if (isEncode) decodeInput = text else encodeInput = text
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
}

internal fun base64Encode(input: String, urlSafe: Boolean = false, padding: Boolean = true, wrap: Boolean = false): String {
    if (input.isEmpty()) return ""
    val encoder = (if (urlSafe) Base64.getUrlEncoder() else Base64.getEncoder()).let { if (padding) it else it.withoutPadding() }
    val out = encoder.encodeToString(input.toByteArray())
    return if (wrap) out.chunked(76).joinToString("\n") else out
}

/**
 * Lenient Base64 decoding: accepts the standard and URL-safe alphabets, missing padding,
 * line breaks and a data URI prefix.
 */
internal fun base64DecodeBytes(input: String): ByteArray {
    val cleaned = input.trim().let { if (it.startsWith("data:")) it.substringAfter(",") else it }
        .filterNot { it.isWhitespace() }
        .replace('-', '+').replace('_', '/')
        .trimEnd('=')
    try {
        return Base64.getDecoder().decode(cleaned)
    } catch (e: IllegalArgumentException) {
        throw IllegalArgumentException(
            if (cleaned.length % 4 == 1) "This Base64 is one character too long or too short. Check that it was copied completely."
            else "This is not valid Base64. Use only A-Z, a-z, 0-9, + / (or - _) and = padding."
        )
    }
}

class Base64Tool : Tool {
    override val descriptor = ToolDescriptor(
        id = "base64",
        title = "Base64 Encoder / Decoder",
        description = "Encode and decode Base64 text",
        icon = Icons.Default.TextFormat,
        category = ToolCategory.ENCODERS_DECODERS,
        keywords = listOf("base64", "encode", "decode", "url-safe", "mime")
    )

    @OptIn(ExperimentalMaterial3Api::class) // FilterChip
    @Composable
    override fun Content(modifier: Modifier) {
        var urlSafe by rememberSaveable { mutableStateOf(false) }
        var padding by rememberSaveable { mutableStateOf(true) }
        var wrap by rememberSaveable { mutableStateOf(false) }
        ToolScaffold(descriptor.title, modifier = modifier) {
            EncodeDecodePane(
                encode = { base64Encode(it, urlSafe, padding, wrap) },
                decodeBytes = ::base64DecodeBytes,
                optionsKey = Triple(urlSafe, padding, wrap),
                options = { isEncode ->
                    // Decoding accepts every variant, so the options only apply to encoding.
                    if (isEncode) {
                        ModeSelector(listOf("Standard", "URL-safe"), if (urlSafe) 1 else 0, { urlSafe = it == 1 })
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = padding, onClick = { padding = !padding }, label = { Text("= padding") })
                            FilterChip(selected = wrap, onClick = { wrap = !wrap }, label = { Text("Wrap at 76") })
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            )
        }
    }
}
