package com.cir0cuit.devtoys.tools.crypto

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.Base64

internal fun ByteArray.toLowerHex(): String = joinToString("") { "%02x".format(it) }

/** Labels of [decodeInput] encodings, by index. */
internal val INPUT_ENCODINGS = listOf("Text", "Hex", "Base64")

/** Labels of [formatBytes] formats, by index. */
internal val OUTPUT_FORMATS = listOf("hex", "HEX", "Base64")

/** [text] as UTF-8 (0), hex (1) or Base64 (2) bytes; throws IllegalArgumentException naming [what] when it does not decode. */
internal fun decodeInput(text: String, encoding: Int, what: String = "Input"): ByteArray = when (encoding) {
    1 -> parseHex(text, what)
    2 -> parseBase64(text, what)
    else -> text.toByteArray()
}

/** Hex bytes; whitespace, colons and a 0x prefix are ignored. */
internal fun parseHex(text: String, what: String = "Input"): ByteArray {
    val clean = text.filterNot { it.isWhitespace() || it == ':' }.removePrefix("0x")
    require(clean.length % 2 == 0 && clean.all { it.digitToIntOrNull(16) != null }) {
        "$what is not valid hex. Use pairs of 0-9 and a-f."
    }
    return ByteArray(clean.length / 2) { clean.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
}

/** Standard or URL-safe Base64, padding optional, whitespace ignored. */
internal fun parseBase64(text: String, what: String = "Input"): ByteArray {
    val clean = text.filterNot { it.isWhitespace() }.replace('-', '+').replace('_', '/').trimEnd('=')
    return try {
        Base64.getDecoder().decode(clean.padEnd((clean.length + 3) / 4 * 4, '='))
    } catch (e: IllegalArgumentException) {
        throw IllegalArgumentException("$what is not valid Base64. Check for missing or extra characters.")
    }
}

/** [bytes] as lowercase hex (0), uppercase hex (1) or Base64 (2). */
internal fun formatBytes(bytes: ByteArray, format: Int): String = when (format) {
    1 -> bytes.toLowerHex().uppercase()
    2 -> Base64.getEncoder().encodeToString(bytes)
    else -> bytes.toLowerHex()
}

private const val MAX_SAVED_CHARS = 32_000

/** Saves text across recreation only while it is small enough for the saved-state bundle. */
private val CappedTextSaver: Saver<MutableState<String>, String> = Saver(
    save = { state -> if (state.value.length <= MAX_SAVED_CHARS) state.value else "" },
    restore = { mutableStateOf(it) }
)

@Composable
internal fun rememberSaveableText(initial: String = ""): MutableState<String> =
    rememberSaveable(saver = CappedTextSaver) { mutableStateOf(initial) }

/** A short label followed by a single-choice segmented row. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChoiceRow(label: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(72.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
            options.forEachIndexed { i, option ->
                SegmentedButton(
                    selected = i == selected,
                    onClick = { onSelect(i) },
                    shape = SegmentedButtonDefaults.itemShape(i, options.size),
                    icon = {},
                    label = { Text(option, maxLines = 1) }
                )
            }
        }
    }
}
