package com.cir0cuit.devtoys.tools.data

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.designsystem.CodeTextStyle
import com.cir0cuit.devtoys.core.designsystem.LocalSlabColor
import com.cir0cuit.devtoys.core.ui.MonoTextField
import com.cir0cuit.devtoys.core.ui.rememberFileSaver
import com.cir0cuit.devtoys.core.ui.rememberTextFileOpener
import com.cir0cuit.devtoys.core.ui.shareText
import com.google.gson.JsonElement

private const val MAX_SAVED_CHARS = 32_000
private const val MAX_SHARE_CHARS = 200_000
private const val MAX_TREE_ROWS = 500

// ponytail: text over 32k chars (e.g. an opened file) is not restored after process death or rotation; move to a ViewModel if that matters
private val CappedTextSaver = Saver<MutableState<String>, String>(
    save = { if (it.value.length <= MAX_SAVED_CHARS) it.value else "" },
    restore = { mutableStateOf(it) }
)

@Composable
internal fun rememberCappedText(initial: String): MutableState<String> =
    rememberSaveable(saver = CappedTextSaver) { mutableStateOf(initial) }

private val JSON_TYPES = arrayOf("application/json", "text/*", "application/octet-stream")

/** Mono input with "Open file" and "Paste" actions under it. */
@Composable
internal fun FileInput(value: String, onValueChange: (String) -> Unit, label: String, minLines: Int = 6, mimeTypes: Array<String> = JSON_TYPES) {
    val clipboard = LocalClipboardManager.current
    val open = rememberTextFileOpener(mimeTypes) { _, text -> onValueChange(text) }
    MonoTextField(value, onValueChange, label, minLines = minLines)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        IconTextButton(Icons.Default.FolderOpen, "Open file", open)
        IconTextButton(Icons.Default.ContentPaste, "Paste") { clipboard.getText()?.text?.let(onValueChange) }
    }
}

/** "Save as" and "Share" for a result. */
@Composable
internal fun OutputActions(text: String, fileName: String, mimeType: String) {
    val context = LocalContext.current
    val save = rememberFileSaver(mimeType)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        IconTextButton(Icons.Default.Save, "Save as") { save(fileName, text.toByteArray()) }
        IconTextButton(Icons.Default.Share, "Share") {
            if (text.length > MAX_SHARE_CHARS) Toast.makeText(context, "Too large to share. Use Save as instead.", Toast.LENGTH_LONG).show()
            else shareText(context, text)
        }
    }
}

@Composable
private fun IconTextButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text(label, maxLines = 1)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T> Segmented(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        options.forEachIndexed { i, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
                icon = {}
            ) { Text(label(option), maxLines = 1) }
        }
    }
}

/** Collapsible tree: tap a container to expand it or a value to copy it; long-press copies the JSONPath. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun JsonTree(root: JsonElement, modifier: Modifier = Modifier) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    // Paths whose open/closed state differs from the default (first two levels open).
    var toggled by remember(root) { mutableStateOf(emptySet<String>()) }
    val rows = remember(root, toggled) { jsonRows(root, { path, depth -> (depth < 2) != (path in toggled) }, MAX_TREE_ROWS + 1) }
    fun copy(text: String, what: String) {
        clipboard.setText(AnnotatedString(text))
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) Toast.makeText(context, "Copied $what", Toast.LENGTH_SHORT).show()
    }
    val keyColor = MaterialTheme.colorScheme.primary
    Surface(modifier.fillMaxWidth(), color = LocalSlabColor.current, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(vertical = 8.dp)) {
            rows.take(MAX_TREE_ROWS).forEach { row ->
                key(row.path) {
                    val container = row.value.isJsonObject || row.value.isJsonArray
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (container) toggled = if (row.path in toggled) toggled - row.path else toggled + row.path
                                    else copy(if (row.value.isJsonPrimitive) row.value.asString else "null", "value")
                                },
                                onLongClick = { copy(row.path, "path") }
                            )
                            .padding(start = (8 + row.depth * 16).dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (container) Icon(
                            Icons.Default.ExpandMore,
                            contentDescription = if (row.expanded) "Collapse" else "Expand",
                            modifier = Modifier.size(16.dp).rotate(if (row.expanded) 0f else -90f)
                        ) else Spacer(Modifier.width(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            buildAnnotatedString {
                                withStyle(SpanStyle(color = keyColor)) { append(row.label) }
                                append(": ")
                                append(summarize(row.value))
                            },
                            style = CodeTextStyle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            val hint = if (rows.size > MAX_TREE_ROWS) "Showing the first $MAX_TREE_ROWS rows. Collapse nodes to see the rest."
            else "Tap to expand or copy a value. Long-press to copy its path."
            Text(
                hint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}
