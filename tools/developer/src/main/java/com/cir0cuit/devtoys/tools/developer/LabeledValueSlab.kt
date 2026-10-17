package com.cir0cuit.devtoys.tools.developer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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

private val BIT_GROUPS = Regex("[01]+([.:][01]+)+")
private val HEX_COLON_GROUPS = Regex("[0-9a-fA-F]*(:+[0-9a-fA-F]*)+")

/**
 * Dotted or colon-grouped binary (`11111111.11100000`) and colon-grouped hex (IPv6, MAC) wrap only
 * between groups: a zero-width space after each separator gives the line breaker a place to break,
 * so it does not cut inside a group or before a colon.
 */
internal fun wrapAtSeparators(value: String): String = when {
    BIT_GROUPS.matches(value) -> value.replace(".", ".\u200B").replace(":", ":\u200B")
    HEX_COLON_GROUPS.matches(value) -> value.replace(":", ":\u200B")
    else -> value
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
                        SelectionContainer { Text(wrapAtSeparators(value), style = CodeTextStyle) }
                    }
                    IconButton(onClick = { clipboard.setText(AnnotatedString(value)) }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy $label", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
