package com.cir0cuit.devtoys.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Read-only dropdown that picks one of [options]. The menu is exactly as wide as the field and
 * capped in height, so it opens below the field and scrolls. Material3 1.2's ExposedDropdownMenu
 * caps the menu at 320dp and sometimes opens it over the field, so this uses a plain DropdownMenu.
 */
@Composable
fun <T> SelectField(
    label: String,
    selected: T,
    options: List<T>,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    textStyle: TextStyle? = null,
    optionLabel: (T) -> String = { it.toString() },
) {
    var expanded by remember { mutableStateOf(false) }
    var fieldWidth by remember { mutableIntStateOf(0) }
    var fieldBottom by remember { mutableIntStateOf(0) }
    var menuMaxHeight by remember { mutableStateOf(MenuMaxHeight) }
    val view = LocalView.current
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val menuScroll = rememberScrollState()
    // Open with the selected option in view; rows are at least 48dp, so this is close enough for long lists.
    LaunchedEffect(expanded) {
        if (!expanded) return@LaunchedEffect
        val target = with(density) { (options.indexOf(selected).coerceAtLeast(0) * 48).dp.roundToPx() }
        // The popup is laid out after this starts; wait until it can scroll (short lists never can).
        withTimeoutOrNull(500) { snapshotFlow { menuScroll.maxValue }.first { it in 1 until Int.MAX_VALUE } } ?: return@LaunchedEffect
        menuScroll.scrollTo(target)
    }
    Box(modifier.onGloballyPositioned { fieldWidth = it.size.width; fieldBottom = it.boundsInWindow().bottom.toInt() }) {
        OutlinedTextField(
            value = optionLabel(selected),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            singleLine = true,
            label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, Modifier.rotate(if (expanded) 180f else 0f)) },
            textStyle = textStyle ?: LocalTextStyle.current,
            modifier = Modifier.fillMaxWidth()
        )
        // Taps go to this overlay, not the read-only field, so the field never takes focus.
        Box(
            Modifier
                .matchParentSize()
                .clickable(enabled = enabled, role = Role.DropdownList) {
                    focusManager.clearFocus()
                    // DropdownMenu flips above the field unless the whole menu fits in the visible frame
                    // minus a 48dp margin, so shrink it to the room below when a few rows still fit.
                    val frame = android.graphics.Rect().also { view.getWindowVisibleDisplayFrame(it) }
                    val below = with(density) { (frame.height() - fieldBottom).toDp() } - 56.dp
                    menuMaxHeight = if (below >= 160.dp) minOf(below, MenuMaxHeight) else MenuMaxHeight
                    expanded = true
                }
                .semantics { contentDescription = "$label: ${optionLabel(selected)}" }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            scrollState = menuScroll,
            modifier = Modifier
                .width(with(density) { fieldWidth.toDp() })
                .heightIn(max = menuMaxHeight)
        ) {
            options.forEach { option ->
                Text(
                    optionLabel(option),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (option == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(option); expanded = false }
                        .heightIn(min = 48.dp)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    }
}

private val MenuMaxHeight: Dp = 280.dp
