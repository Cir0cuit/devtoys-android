package com.cir0cuit.devtoys.tools.developer

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.designsystem.CodeTextStyle
import com.cir0cuit.devtoys.core.ui.MonoTextField

// Shared by ColorPickerTool, ColorPaletteTool and ContrastCheckerTool.

/** Copies text to the clipboard and confirms with "Copied <text>". */
@Composable
internal fun rememberCopy(): (String) -> Unit {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    return { text ->
        clipboard.setText(AnnotatedString(text))
        Toast.makeText(context, "Copied $text", Toast.LENGTH_SHORT).show()
    }
}

/**
 * 48dp swatch of the current color, a field that accepts any color notation (see [parseColor]) and a paste
 * button. [onColor] gets the parsed ARGB color; the field text is left as typed.
 */
@Composable
internal fun ColorHeader(
    color: Color,
    text: String,
    onTextChange: (String) -> Unit,
    onColor: (Int) -> Unit,
    label: String = "Color"
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    fun accept(value: String) {
        onTextChange(value)
        parseColor(value)?.let(onColor)
    }
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .padding(top = 8.dp, end = 12.dp) // align with the field box below its floating label
                    .size(48.dp)
                    .background(color, MaterialTheme.shapes.medium)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
            )
            MonoTextField(value = text, onValueChange = ::accept, label = label, modifier = Modifier.weight(1f), minLines = 1, maxLines = 1)
            IconButton(
                onClick = {
                    val pasted = clipboard.getText()?.text?.trim().orEmpty()
                    if (parseColor(pasted) != null) accept(pasted)
                    else Toast.makeText(context, "The clipboard has no color. Copy a hex code, rgb(), hsl() or a color name.", Toast.LENGTH_LONG).show()
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(Icons.Default.ContentPaste, contentDescription = "Paste $label")
            }
        }
        if (text.isNotBlank() && parseColor(text) == null) {
            Text(
                "Not a color. Use #RRGGBB, rgb(), hsl(), hwb(), oklch() or a CSS color name.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 60.dp, top = 4.dp)
            )
        }
    }
}

/** Slim 0-255 slider; the value at the end is editable for precise entry. Also used for alpha. */
@Composable
internal fun ColorChannelSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    var text by remember(value) { mutableStateOf(value.toInt().toString()) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(44.dp)
        )
        Slider(value = value, onValueChange = onValueChange, valueRange = 0f..255f, modifier = Modifier.weight(1f))
        BasicTextField(
            value = text,
            onValueChange = { input ->
                val digits = input.filter { it.isDigit() }.take(3)
                text = digits
                digits.toIntOrNull()?.let { v -> onValueChange(v.coerceIn(0, 255).toFloat()) }
            },
            textStyle = CodeTextStyle.copy(color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.End),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .padding(start = 12.dp)
                .width(36.dp)
                .onFocusChanged { if (!it.isFocused) text = value.toInt().toString() } // restore after clearing the field
                .semantics { contentDescription = "$label value" }
        )
    }
}
