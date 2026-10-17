package com.cir0cuit.devtoys.tools.developer

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.designsystem.CodeTextStyle
import com.cir0cuit.devtoys.core.designsystem.LocalSlabColor
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Inputs above this size are not saved across process death (Binder limit). */
private const val MAX_SAVED_INPUT = 50_000

private val CappedStringSaver = Saver<String, String>(save = { it.takeIf { s -> s.length <= MAX_SAVED_INPUT } }, restore = { it })

class JsonToTypesTool : Tool {
    override val descriptor = ToolDescriptor(
        id = "json-types",
        title = "JSON to Types",
        description = "Generate typed models from JSON",
        icon = Icons.Default.AccountTree,
        category = ToolCategory.CONVERTERS,
        keywords = listOf("json", "types", "kotlin", "typescript", "swift", "go", "java", "python", "csharp", "dart", "rust", "class", "model")
    )

    @Composable override fun Content(modifier: Modifier) {
        var input by rememberSaveable(stateSaver = CappedStringSaver) {
            mutableStateOf("{\n  \"id\": 1,\n  \"name\": \"Test\",\n  \"is_active\": true,\n  \"data\": {\"value\": 5}\n}")
        }
        var lang by rememberSaveable { mutableStateOf(TypeLang.KOTLIN) }
        var kotlinLib by rememberSaveable { mutableStateOf(KotlinLib.KOTLINX) }
        var javaLib by rememberSaveable { mutableStateOf(JavaLib.JACKSON) }
        var allNullable by rememberSaveable { mutableStateOf(false) }
        var mutable by rememberSaveable { mutableStateOf(false) }
        var camelCase by rememberSaveable { mutableStateOf(false) }
        val opts = TypeOptions(kotlinLib, javaLib, allNullable, mutable, camelCase)

        // Only typing is debounced; option changes regenerate at once.
        var debouncedInput by remember { mutableStateOf(input) }
        LaunchedEffect(input) {
            delay(250)
            debouncedInput = input
        }
        // Tagged with its language so code for the previous language is never shown under the new label.
        val result by produceState<Pair<TypeLang, TypesResult>?>(null, debouncedInput, lang, opts) {
            value = lang to withContext(Dispatchers.Default) { convertJsonToTypes(debouncedInput, lang, opts) }
        }

        ToolScaffold(descriptor.title, modifier = modifier) {
            MonoTextField(input, { input = it }, "JSON", minLines = 6)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectField("Language", lang, TypeLang.entries, { lang = it }, Modifier.weight(1f)) { it.label }
                when (lang) {
                    TypeLang.KOTLIN -> SelectField("Library", kotlinLib, KotlinLib.entries, { kotlinLib = it }, Modifier.weight(1f)) { it.label }
                    TypeLang.JAVA -> SelectField("Library", javaLib, JavaLib.entries, { javaLib = it }, Modifier.weight(1f)) { it.label }
                    else -> {}
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OptionChip("All nullable", allNullable) { allNullable = it }
                when (lang) {
                    TypeLang.KOTLIN, TypeLang.SWIFT, TypeLang.DART -> OptionChip("Mutable", mutable) { mutable = it }
                    TypeLang.TYPESCRIPT -> OptionChip("camelCase keys", camelCase) { camelCase = it }
                    else -> {}
                }
            }
            Spacer(Modifier.height(16.dp))
            val current = result?.takeIf { it.first == lang }?.second
            val error = current?.error
            if (error != null) {
                Text(error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            } else {
                CodePanel(current?.code.orEmpty(), lang.label)
            }
        }
    }
}

/** OutputPanel without soft wrap: long lines scroll sideways instead of breaking between words. */
@Composable
private fun CodePanel(code: String, label: String) {
    val clipboard = LocalClipboardManager.current
    Surface(Modifier.fillMaxWidth(), color = LocalSlabColor.current, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().heightIn(min = 40.dp).padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                if (code.isNotEmpty()) {
                    IconButton(onClick = { clipboard.setText(AnnotatedString(code)) }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy $label", modifier = Modifier.size(20.dp))
                    }
                }
            }
            SelectionContainer(Modifier.horizontalScroll(rememberScrollState())) {
                Text(
                    code.ifEmpty { "The result appears here." },
                    style = CodeTextStyle,
                    softWrap = false,
                    color = if (code.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionChip(label: String, selected: Boolean, onChange: (Boolean) -> Unit) {
    FilterChip(
        selected = selected,
        onClick = { onChange(!selected) },
        label = { Text(label, maxLines = 1) },
        leadingIcon = if (selected) { { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) } } else null
    )
}
