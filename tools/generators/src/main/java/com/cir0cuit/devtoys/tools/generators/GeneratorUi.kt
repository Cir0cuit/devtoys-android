package com.cir0cuit.devtoys.tools.generators

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.PersistableBundle
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.designsystem.CodeTextStyle
import com.cir0cuit.devtoys.core.designsystem.LocalSlabColor

/** Copies [text] with the IS_SENSITIVE flag so keyboards and clipboard previews hide it (API 33+). */
internal fun copySensitive(context: Context, label: String, text: String) {
    val clip = ClipData.newPlainText(label, text)
    // Value of ClipDescription.EXTRA_IS_SENSITIVE (API 33); older versions ignore it.
    clip.description.extras = PersistableBundle().apply { putBoolean("android.content.extra.IS_SENSITIVE", true) }
    context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(clip)
}

@Composable
internal fun SensitiveCopyButton(enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled, contentPadding = ButtonDefaults.ButtonWithIconContentPadding) {
        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text("Copy")
    }
}

/** Segmented row for up to four options. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T> ChoiceRow(options: List<T>, selected: T, onSelect: (T) -> Unit, label: (T) -> String = { it.toString() }) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { i, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
                icon = {},
                label = { Text(label(option), maxLines = 1) }
            )
        }
    }
}

/** Output panel that keeps every line whole and scrolls sideways, so line-per-item results never wrap. */
@Composable
internal fun UnwrappedOutputPanel(content: String, label: String) {
    val clipboard = LocalClipboardManager.current
    Surface(Modifier.fillMaxWidth(), color = LocalSlabColor.current, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().heightIn(min = 40.dp).padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                IconButton(onClick = { clipboard.setText(AnnotatedString(content)) }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy $label", modifier = Modifier.size(20.dp))
                }
            }
            SelectionContainer(Modifier.horizontalScroll(rememberScrollState())) {
                Text(content, style = CodeTextStyle, softWrap = false, modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp))
            }
        }
    }
}

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

/** Single-line mono field for an ID or name. */
@Composable
internal fun SingleLineField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, isError: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        textStyle = CodeTextStyle,
        modifier = modifier.fillMaxWidth()
    )
}
