package com.cir0cuit.devtoys.tools.text

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.NumberedTextField
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolCategory
import com.cir0cuit.devtoys.tools.base.ToolDescriptor

// Operation key (saved state, applyLineOperation) to chip label. The line count is always shown in the output header.
private val operations = listOf(
    "Sort" to "Sort A–Z",
    "Sort Desc" to "Sort Z–A",
    "Dedupe" to "Remove duplicates",
    "Reverse" to "Reverse"
)

class LineToolsTool : Tool {
    override val descriptor = ToolDescriptor(
        "line-tools", "Line Tools", "Sort, dedupe, reverse or count lines",
        Icons.AutoMirrored.Filled.Sort, ToolCategory.TEXT, listOf("line", "sort", "dedupe", "count")
    )

    @OptIn(ExperimentalLayoutApi::class)
    @Composable override fun Content(modifier: Modifier) {
        var input by rememberSaveable(saver = boundedTextSaver) { mutableStateOf("banana\napple\ncherry\napple") }
        var operation by rememberSaveable { mutableStateOf("Sort") }
        val op = operation.takeIf { saved -> operations.any { it.first == saved } } ?: "Sort"
        val output = remember(input, op) { applyLineOperation(input, op) }
        val lineCount = if (output.isEmpty()) 0 else output.lines().size - if (output.endsWith("\n")) 1 else 0

        ToolScaffold(descriptor.title, modifier = modifier, scrollable = false) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                operations.forEach { (key, label) ->
                    FilterChip(selected = op == key, onClick = { operation = key }, label = { Text(label, maxLines = 1) })
                }
            }
            PaneHeader("Input", onPaste = { input = it })
            NumberedTextField(input, { input = it }, modifier = Modifier.weight(1f).fillMaxWidth())
            PaneHeader(if (lineCount == 1) "Output, 1 line" else "Output, $lineCount lines", copyText = output)
            NumberedTextField(output, { }, modifier = Modifier.weight(1f).fillMaxWidth(), readOnly = true)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

internal fun applyLineOperation(input: String, operation: String): String {
    if (input.isEmpty()) return ""
    val trailing = when {
        input.endsWith("\r\n") -> "\r\n"
        input.endsWith("\n") -> "\n"
        else -> ""
    }
    val sep = if (input.contains("\r\n")) "\r\n" else "\n"
    val lines = input.removeSuffix(trailing).lines()
    fun join(l: List<String>) = l.joinToString(sep) + trailing
    return when (operation) {
        "Sort" -> join(lines.sorted())
        "Sort Desc" -> join(lines.sortedDescending())
        "Dedupe" -> join(lines.distinct())
        "Reverse" -> join(lines.reversed())
        "Count" -> "Lines: ${lines.size}"
        else -> input
    }
}
