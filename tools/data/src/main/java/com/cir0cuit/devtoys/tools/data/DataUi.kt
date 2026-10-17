package com.cir0cuit.devtoys.tools.data

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.cir0cuit.devtoys.core.ui.OutputPanel
import kotlinx.coroutines.delay

/** Typing debounce that skips the first run, so a tool opened with saved input shows its result at once. */
internal class Debounce {
    private var started = false
    suspend fun await(millis: Long) {
        if (started) delay(millis) else started = true
    }
}

@Composable
internal fun rememberDebounce(): Debounce = remember { Debounce() }

@Composable
internal fun ErrorText(message: String) {
    Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
}

/** Result slab, or the parser's "Error: ..." message in the error color. */
@Composable
internal fun ResultPanel(output: String, label: String = "Output") {
    if (output.startsWith("Error: ")) ErrorText("Couldn't parse the input. ${output.removePrefix("Error: ")}")
    else OutputPanel(output, label = label)
}
