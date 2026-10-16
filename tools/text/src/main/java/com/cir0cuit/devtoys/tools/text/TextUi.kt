package com.cir0cuit.devtoys.tools.text

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp

/** Section header: title on the left, optional Paste and Copy actions on the right. */
@Composable
internal fun PaneHeader(title: String, onPaste: ((String) -> Unit)? = null, copyText: String? = null) {
    val clipboard = LocalClipboardManager.current
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        if (onPaste != null) {
            TextButton(onClick = { clipboard.getText()?.text?.let(onPaste) }) {
                Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text("Paste")
            }
        }
        if (!copyText.isNullOrEmpty()) {
            IconButton(onClick = { clipboard.setText(AnnotatedString(copyText)) }) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy $title", modifier = Modifier.size(20.dp))
            }
        }
    }
}
