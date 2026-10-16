package com.cir0cuit.devtoys.tools.data

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import com.cir0cuit.devtoys.core.ui.MonoTextField
import com.cir0cuit.devtoys.core.ui.OutputPanel

/** Mono input with a "Paste" action under it. */
@Composable
internal fun PasteableInput(value: String, onValueChange: (String) -> Unit, label: String, minLines: Int = 6, maxLines: Int = Int.MAX_VALUE) {
    val clipboard = LocalClipboardManager.current
    MonoTextField(value, onValueChange, label, minLines = minLines, maxLines = maxLines)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = { clipboard.getText()?.text?.let(onValueChange) }) {
            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text("Paste")
        }
    }
}

/** Result slab, or the parser's "Error: ..." message in the error color. */
@Composable
internal fun ResultPanel(output: String, label: String = "Output") {
    if (output.startsWith("Error: ")) {
        Text(
            "Couldn't parse the input. ${output.removePrefix("Error: ")}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
    } else {
        OutputPanel(output, label = label)
    }
}
