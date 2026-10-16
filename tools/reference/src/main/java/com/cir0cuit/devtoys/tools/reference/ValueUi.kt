package com.cir0cuit.devtoys.tools.reference

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.designsystem.CodeTextStyle
import com.cir0cuit.devtoys.core.designsystem.LocalSlabColor

/** One recessed slab of label-over-value rows, each value selectable and copyable. */
@Composable
internal fun LabeledValueSlab(rows: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    val clipboard = LocalClipboardManager.current
    Surface(modifier.fillMaxWidth(), color = LocalSlabColor.current, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(vertical = 8.dp)) {
            rows.forEach { (label, value) ->
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
                        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        SelectionContainer { Text(value, style = CodeTextStyle) }
                    }
                    IconButton(onClick = { clipboard.setText(AnnotatedString(value)) }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy $label", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
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
    optionLabel: (T) -> String = { it.toString() },
    selectedLabel: (T) -> String = optionLabel
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = modifier) {
        OutlinedTextField(
            value = selectedLabel(selected),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = { onSelect(option); expanded = false }
                )
            }
        }
    }
}

/** Error line: what is wrong and how to fix it. */
@Composable
internal fun ErrorText(message: String) {
    Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
}
