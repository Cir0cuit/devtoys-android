package com.cir0cuit.devtoys.tools.data

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Difference
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolCategory
import com.cir0cuit.devtoys.tools.base.ToolDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Returns (summary, change list) or throws IllegalArgumentException naming the input that failed to parse. */
internal fun compareJson(original: String, changed: String, ignoreArrayOrder: Boolean): Pair<String, String> {
    fun parse(text: String, name: String) = try { parseJson(text) } catch (e: Exception) {
        throw IllegalArgumentException("$name JSON, ${describeJsonError(e).replaceFirstChar { it.lowercase() }}")
    }
    val changes = diffJson(parse(original, "Original"), parse(changed, "Changed"), ignoreArrayOrder)
    return changeSummary(changes) to describeChanges(changes).ifEmpty { "The documents are equal." }
}

class JsonDiffTool : Tool {
    override val descriptor = ToolDescriptor(
        "json-diff", "JSON Diff", "Compare two JSON documents by structure",
        Icons.Default.Difference, ToolCategory.TEXT, listOf("json", "diff", "compare")
    )

    @Composable override fun Content(modifier: Modifier) {
        var original by rememberCappedText("{\"name\":\"devtoys\",\"tags\":[\"json\"],\"stars\":42}")
        var changed by rememberCappedText("{\"stars\":43,\"name\":\"devtoys\",\"tags\":[\"json\",\"diff\"]}")
        var ignoreOrder by rememberSaveable { mutableStateOf(false) }
        var result by remember { mutableStateOf("" to "") }
        LaunchedEffect(original, changed, ignoreOrder) {
            if (original.isBlank() || changed.isBlank()) {
                result = "" to ""
                return@LaunchedEffect
            }
            delay(300)
            result = withContext(Dispatchers.Default) {
                try { compareJson(original, changed, ignoreOrder) } catch (e: Exception) { "" to "Error: ${e.message}" }
            }
        }
        ToolScaffold(descriptor.title, modifier = modifier) {
            FileInput(original, { original = it }, "Original JSON", minLines = 5)
            FileInput(changed, { changed = it }, "Changed JSON", minLines = 5)
            FilterChip(selected = ignoreOrder, onClick = { ignoreOrder = !ignoreOrder }, label = { Text("Ignore array order", maxLines = 1) })
            Spacer(Modifier.height(8.dp))
            val (summary, lines) = result
            if (lines.isNotEmpty()) ResultPanel(lines, label = summary.ifEmpty { "Differences" })
        }
    }
}
