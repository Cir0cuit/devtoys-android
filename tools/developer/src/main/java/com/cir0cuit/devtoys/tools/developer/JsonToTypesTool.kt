package com.cir0cuit.devtoys.tools.developer

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

        var output by remember { mutableStateOf("") }
        LaunchedEffect(input, lang, opts) {
            if (output.isNotEmpty()) delay(250)
            output = withContext(Dispatchers.Default) { convertJsonToTypes(input, lang, opts) }
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
            OutputPanel(output, label = lang.label)
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
