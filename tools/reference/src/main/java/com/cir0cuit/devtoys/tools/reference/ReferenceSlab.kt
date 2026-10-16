package com.cir0cuit.devtoys.tools.reference

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.designsystem.LocalSlabColor

/** Compact label-left rows in one recessed slab, for short facts that need no copy button. */
@Composable
internal fun LabeledSlab(rows: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    Surface(color = LocalSlabColor.current, shape = MaterialTheme.shapes.medium, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            rows.forEach { (label, value) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(
                        label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(0.4f)
                    )
                    Text(
                        value,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(0.6f)
                    )
                }
            }
        }
    }
}

@Composable
internal fun EmptyHint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 16.dp)
    )
}
