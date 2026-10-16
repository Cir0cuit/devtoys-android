package com.cir0cuit.devtoys.tools.encoders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.designsystem.CodeTextStyle
import com.cir0cuit.devtoys.core.designsystem.LocalSlabColor
import com.cir0cuit.devtoys.core.ui.OutputPanel
import com.cir0cuit.devtoys.core.ui.rememberFileSaver

/** Full-width segmented choice; [options] should stay short (<= 4, one word each) to fit 360dp. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ModeSelector(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        options.forEachIndexed { i, label ->
            SegmentedButton(
                selected = selected == i,
                onClick = { onSelect(i) },
                shape = SegmentedButtonDefaults.itemShape(index = i, count = options.size),
                label = { Text(label, maxLines = 1) }
            )
        }
    }
}

/** One recessed slab of labeled values; tapping a row (or its icon) copies the value. */
@Composable
internal fun ValueSlab(rows: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    val clipboard = LocalClipboardManager.current
    Surface(modifier.fillMaxWidth(), color = LocalSlabColor.current, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(vertical = 4.dp)) {
            rows.forEach { (label, value) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(enabled = value.isNotEmpty()) { clipboard.setText(AnnotatedString(value)) }
                        .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(value.ifEmpty { "-" }, style = CodeTextStyle, color = MaterialTheme.colorScheme.onSurface)
                    }
                    IconButton(onClick = { clipboard.setText(AnnotatedString(value)) }, enabled = value.isNotEmpty()) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy $label", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
internal fun ErrorText(message: String, modifier: Modifier = Modifier) {
    Text(message, modifier, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
}

/** Read-only dropdown that picks one of [options]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T> SelectField(
    label: String,
    selected: T,
    options: List<T>,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    optionLabel: (T) -> String = { it.toString() }
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = modifier) {
        OutlinedTextField(
            value = optionLabel(selected),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(optionLabel(option)) }, onClick = { onSelect(option); expanded = false })
            }
        }
    }
}

private const val DUMP_MAX_BYTES = 4096

/**
 * Decoded bytes: shown as text when they are valid UTF-8, otherwise as a hex dump with the
 * detected file type and a button to save them as a file.
 */
@Composable
internal fun BytesOutput(bytes: ByteArray, label: String, modifier: Modifier = Modifier) {
    val text = remember(bytes) { utf8OrNull(bytes) }
    if (text != null) {
        OutputPanel(text, modifier, label = label)
    } else {
        BinaryView(bytes, modifier)
    }
}

@Composable
private fun BinaryView(bytes: ByteArray, modifier: Modifier) {
    val type = remember(bytes) { detectFileType(bytes) }
    val dump = remember(bytes) { hexDump(bytes.copyOf(minOf(bytes.size, DUMP_MAX_BYTES))) }
    val save = rememberFileSaver("application/octet-stream")
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Binary data · ${type?.first ?: "unknown type"} · ${formatSize(bytes.size)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            FilledTonalButton(onClick = { save("decoded.${type?.second ?: "bin"}", bytes) }) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Save", maxLines = 1)
            }
        }
        Spacer(Modifier.height(8.dp))
        OutputPanel(dump, label = if (bytes.size > DUMP_MAX_BYTES) "Hex dump (first 4 KB)" else "Hex dump")
    }
}
