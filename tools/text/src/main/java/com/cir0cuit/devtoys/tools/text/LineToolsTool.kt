package com.cir0cuit.devtoys.tools.text
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ContentCopy

import androidx.compose.material3.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*

class LineToolsTool : Tool {
    override val descriptor = ToolDescriptor("line-tools", android.R.string.untitled, android.R.string.untitled, Icons.Default.Sort, ToolCategory.TEXT, listOf("line", "sort", "dedupe"))
    @Composable override fun Content(modifier: Modifier) {
        var input by rememberSaveable(saver = boundedTextSaver) { mutableStateOf("") }
        var operation by rememberSaveable { mutableStateOf("Sort") }
        val output = remember(input, operation) { applyLineOperation(input, operation) }
        ToolScaffold("Line Tools", modifier = modifier) {
            LazyRow {
                items(5) { idx ->
                    val op = listOf("Sort", "Sort Desc", "Dedupe", "Reverse", "Count")[idx]
                    FilterChip(
                        selected = operation == op,
                        onClick = { operation = op },
                        label = { Text(op) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Input", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            NumberedTextField(input, { input = it }, modifier = Modifier.weight(1f).fillMaxWidth())
            
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Output", style = MaterialTheme.typography.titleSmall)
                val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
                IconButton(onClick = { clipboard.setText(androidx.compose.ui.text.AnnotatedString(output)) }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy output")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            NumberedTextField(output, { }, modifier = Modifier.weight(1f).fillMaxWidth(), readOnly = true)
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
