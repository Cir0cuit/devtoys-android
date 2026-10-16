package com.cir0cuit.devtoys.tools.reference

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.designsystem.CodeTextStyle
import com.cir0cuit.devtoys.core.designsystem.LocalSlabColor
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolCategory
import com.cir0cuit.devtoys.tools.base.ToolDescriptor

/** Placeholder tokens such as `<name>`; no spaces inside, so `a > b < c` is not one. */
private val PLACEHOLDER = Regex("<([^<>\\s]+)>")

internal fun placeholdersIn(command: String): List<String> =
    PLACEHOLDER.findAll(command).map { it.groupValues[1] }.distinct().toList()

/** Replaces each placeholder that has a non-blank value; the others stay as `<name>`. */
internal fun fillPlaceholders(command: String, values: Map<String, String>): String =
    PLACEHOLDER.replace(command) { m -> values[m.groupValues[1]]?.takeIf { it.isNotBlank() } ?: m.value }

/** Categories whose commands match [query], titled "Sheet · Category" so results from every sheet stay apart. */
internal fun searchCheatsheets(sheets: List<Cheatsheet>, query: String): List<CheatCategory> =
    sheets.flatMap { sheet ->
        sheet.categories.mapNotNull { cat ->
            cat.commands.filter { cmd ->
                listOf(cmd.title, cmd.command, cmd.description).any { it.contains(query, ignoreCase = true) }
            }.takeIf { it.isNotEmpty() }?.let { CheatCategory("${sheet.title} · ${cat.name}", it) }
        }
    }

class CheatsheetTool : Tool {
    override val descriptor = ToolDescriptor(
        "cheatsheet",
        "Cheatsheets",
        "Look up common Git, shell and tool commands",
        Icons.AutoMirrored.Filled.MenuBook,
        ToolCategory.NETWORK_SYSTEM,
        listOf("cheat", "sheet", "commands", "reference", "ssh", "curl", "openssl", "tmux")
    )

    @Composable
    override fun Content(modifier: Modifier) {
        var selected by rememberSaveable { mutableStateOf(CHEATSHEETS.first().title) }
        var query by rememberSaveable { mutableStateOf("") }
        var collapsed by rememberSaveable { mutableStateOf(listOf<String>()) }
        // Values typed for placeholders, shared by name across commands (e.g. <host>).
        var placeholderValues by rememberSaveable { mutableStateOf(mapOf<String, String>()) }
        var editing by rememberSaveable { mutableStateOf<String?>(null) }
        val clipboardManager = LocalClipboardManager.current
        val context = LocalContext.current

        val activeSheet = CHEATSHEETS.find { it.title == selected } ?: CHEATSHEETS.first()
        val q = query.trim()
        // A query searches every sheet: people remember the command, not which sheet it is on.
        val categories = remember(q, activeSheet) {
            if (q.isEmpty()) activeSheet.categories.map { it.copy(name = "${activeSheet.title} · ${it.name}") }
            else searchCheatsheets(CHEATSHEETS, q)
        }
        val copy: (String) -> Unit = {
            clipboardManager.setText(AnnotatedString(it))
            confirmCopied(context, "Command")
        }

        ToolScaffold(descriptor.title, modifier = modifier, scrollable = false) {
            SearchField(query, { query = it }, "Search all cheatsheets")
            Spacer(Modifier.height(8.dp))
            SheetPicker(selected = activeSheet.title, enabled = q.isEmpty(), onSelect = { selected = it })

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (categories.isEmpty()) {
                    item {
                        Text(
                            "No command matches \"$q\" in any cheatsheet. Try a shorter word, such as a tool name.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                }
                categories.forEach { category ->
                    val key = category.name
                    // While searching every match is shown, so collapsing is disabled.
                    val isExpanded = q.isNotEmpty() || key !in collapsed
                    item(key = key) {
                        SectionHeader(
                            category.name,
                            isExpanded,
                            onToggle = if (q.isNotEmpty()) null else {
                                { collapsed = if (isExpanded) collapsed + key else collapsed - key }
                            }
                        )
                    }
                    if (isExpanded) {
                        items(category.commands, key = { "$key/${it.title}" }) { cmd ->
                            CommandRow(
                                cmd,
                                onCopy = { copy(cmd.command) },
                                onEdit = if (placeholdersIn(cmd.command).isEmpty()) null else {
                                    { editing = cmd.command }
                                }
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }

        editing?.let { command ->
            PlaceholderDialog(
                command = command,
                values = placeholderValues,
                onValueChange = { name, value -> placeholderValues = placeholderValues + (name to value) },
                onCopy = { copy(fillPlaceholders(command, placeholderValues)); editing = null },
                onDismiss = { editing = null }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SheetPicker(selected: String, enabled: Boolean, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded && enabled, onExpandedChange = { if (enabled) expanded = !expanded }) {
        OutlinedTextField(
            value = if (enabled) selected else "All sheets",
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            singleLine = true,
            label = { Text("Cheatsheet") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && enabled) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            CHEATSHEETS.forEach { sheet ->
                DropdownMenuItem(
                    text = { Text(sheet.title) },
                    onClick = { onSelect(sheet.title); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun PlaceholderDialog(
    command: String,
    values: Map<String, String>,
    onValueChange: (String, String) -> Unit,
    onCopy: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Fill in placeholders") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                placeholdersIn(command).forEach { name ->
                    OutlinedTextField(
                        value = values[name].orEmpty(),
                        onValueChange = { onValueChange(name, it) },
                        label = { Text(name) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                }
                Surface(color = LocalSlabColor.current, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        fillPlaceholders(command, values),
                        style = CodeTextStyle,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onCopy) { Text("Copy") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun CommandRow(cmd: CheatCommand, onCopy: () -> Unit, onEdit: (() -> Unit)?) {
    Column(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Text(cmd.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            cmd.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
        )
        Surface(
            color = LocalSlabColor.current,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth().clickable(onClickLabel = "Copy command", onClick = onCopy)
        ) {
            Row(Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    cmd.command,
                    style = CodeTextStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f).padding(vertical = 6.dp)
                )
                if (onEdit != null) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Fill in placeholders",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp).size(18.dp)
                    )
                }
            }
        }
    }
}
