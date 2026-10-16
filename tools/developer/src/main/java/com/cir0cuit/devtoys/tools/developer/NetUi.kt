package com.cir0cuit.devtoys.tools.developer

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Segmented row for up to four short options. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NetChoiceRow(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        options.forEachIndexed { i, label ->
            SegmentedButton(
                selected = selected == i,
                onClick = { onSelect(i) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            )
        }
    }
}

@Composable
internal fun NetError(message: String, modifier: Modifier = Modifier) {
    Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = modifier)
}

/**
 * Runs [compute] on Dispatchers.Default after a short debounce whenever [keys] change; a newer
 * key cancels the previous run. Null until the first result arrives.
 */
@Composable
internal fun <T> rememberNetResult(vararg keys: Any?, compute: () -> T): Result<T>? {
    val state by produceState<Result<T>?>(null, *keys) {
        delay(200)
        value = withContext(Dispatchers.Default) { runCatching(compute) }
    }
    return state
}
