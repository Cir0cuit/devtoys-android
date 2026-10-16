package com.cir0cuit.devtoys.tools.crypto

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.cir0cuit.devtoys.core.designsystem.CodeTextStyle

/** Full-width single-choice segmented row without the check icon, so short labels never wrap. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        options.forEachIndexed { i, label ->
            SegmentedButton(
                selected = selected == i,
                onClick = { onSelect(i) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
                icon = {},
                label = { Text(label, maxLines = 1) }
            )
        }
    }
}

/** Single-line mono field that hides its value until the eye button is pressed. */
@Composable
internal fun SecretField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    isError: Boolean = false,
    extraAction: (@Composable () -> Unit)? = null,
) {
    var show by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        textStyle = CodeTextStyle,
        singleLine = true,
        isError = isError,
        shape = MaterialTheme.shapes.medium,
        supportingText = if (supportingText == null) null else { { Text(supportingText) } },
        visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrect = false),
        trailingIcon = {
            Row {
                extraAction?.invoke()
                IconButton(onClick = { show = !show }) {
                    Icon(
                        if (show) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (show) "Hide $label" else "Show $label"
                    )
                }
            }
        }
    )
}
