package com.cir0cuit.devtoys.tools.data

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material3.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*
import com.google.gson.JsonElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal enum class JsonMode(val label: String) { FORMAT("Format"), ESCAPE("Escape"), UNESCAPE("Unescape") }

/** Output text plus the parsed tree when there is one. */
private class JsonResult(val text: String, val tree: JsonElement? = null)

/**
 * Format: parse and re-indent. Escape: text to a JSON string literal.
 * Unescape: string literal to text, formatted when the text is itself a JSON object or array.
 * Throws IllegalArgumentException with a user-facing message.
 */
internal fun runJsonTool(input: String, mode: JsonMode, indent: JsonIndent, sort: Boolean, lenient: Boolean): Pair<String, JsonElement?> = when (mode) {
    JsonMode.ESCAPE -> escapeJsonString(input) to null
    JsonMode.UNESCAPE -> {
        val text = try { unescapeJsonString(input) } catch (e: Exception) {
            throw IllegalArgumentException("Not a valid JSON string. ${describeJsonError(e)}")
        }
        // Sorted before it is returned, so the tree view shows the same key order as the text.
        val nested = runCatching { parseJson(text, lenient) }.getOrNull()?.takeIf { it.isJsonObject || it.isJsonArray }
            ?.let { if (sort) sortKeys(it) else it }
        if (nested != null) formatJson(nested, indent) to nested else text to null
    }
    JsonMode.FORMAT -> try {
        val tree = parseJson(input, lenient).let { if (sort) sortKeys(it) else it }
        formatJson(tree, indent) to tree
    } catch (e: Exception) {
        val msg = describeJsonError(e, input)
        throw IllegalArgumentException(if (lenient) msg else "$msg Fix it there, or turn on Lenient to accept comments, single quotes and unquoted keys.")
    }
}

class JsonFormatterTool : Tool {
    override val descriptor = ToolDescriptor(
        "json-formatter", "JSON Formatter", "Format, validate and escape JSON", Icons.Default.DataObject,
        ToolCategory.FORMATTERS, listOf("json", "format", "minify", "validate", "escape", "stringify", "tree", "sort"),
        accepts = setOf(ShareKind.TEXT),
        fileTypes = setOf("json", "geojson", "har", "webmanifest")
    )

    @Composable override fun Content(modifier: Modifier) {
        var input by rememberCappedText("{\"name\":\"devtoys\",\"tags\":[\"json\",\"format\"],\"stars\":42}")
        OnSharedText { input = it }
        var mode by rememberSaveable { mutableStateOf(JsonMode.FORMAT) }
        var indent by rememberSaveable { mutableStateOf(JsonIndent.TWO) }
        var sort by rememberSaveable { mutableStateOf(false) }
        var lenient by rememberSaveable { mutableStateOf(false) }
        var tree by rememberSaveable { mutableStateOf(false) }
        var result by remember { mutableStateOf(JsonResult("")) }
        val debounce = rememberDebounce()
        LaunchedEffect(input, mode, indent, sort, lenient) {
            if (input.isBlank()) {
                result = JsonResult("")
                return@LaunchedEffect
            }
            debounce.await(300)
            result = withContext(Dispatchers.Default) {
                try {
                    runJsonTool(input, mode, indent, sort, lenient).let { (text, element) -> JsonResult(text, element) }
                } catch (e: Exception) {
                    JsonResult("Error: ${e.message}")
                }
            }
        }
        ToolScaffold(descriptor.title, modifier = modifier) {
            FileInput(input, { input = it }, if (mode == JsonMode.ESCAPE) "Text input" else "JSON input")
            Segmented(JsonMode.entries, mode, { it.label }, { mode = it })
            if (mode != JsonMode.ESCAPE) {
                Spacer(Modifier.height(12.dp))
                Text("Indentation", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Segmented(JsonIndent.entries, indent, { it.label }, { indent = it })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = sort, onClick = { sort = !sort }, label = { Text("Sort keys", maxLines = 1) })
                    FilterChip(selected = lenient, onClick = { lenient = !lenient }, label = { Text("Lenient", maxLines = 1) })
                    FilterChip(selected = tree, onClick = { tree = !tree }, label = { Text("Tree view", maxLines = 1) })
                }
            }
            Spacer(Modifier.height(12.dp))
            val r = result
            val root = r.tree
            if (tree && mode != JsonMode.ESCAPE && root != null) JsonTree(root)
            else ResultPanel(r.text)
            if (r.text.isNotEmpty() && !r.text.startsWith("Error: ")) {
                OutputActions(r.text, if (root != null) "formatted.json" else "output.txt", if (root != null) "application/json" else "text/plain")
            }
        }
    }
}
