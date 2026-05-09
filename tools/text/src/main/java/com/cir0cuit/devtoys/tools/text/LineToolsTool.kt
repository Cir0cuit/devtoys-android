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
        var input by rememberSaveable { mutableStateOf("") }
        var operation by rememberSaveable { mutableStateOf("Sort") }
        val output = remember(input, operation) {
            if (input.isEmpty()) "" else {
                val lines = input.lines()
                when (operation) {
                    "Sort" -> lines.sorted().joinToString("\n")
                    "Sort Desc" -> lines.sortedDescending().joinToString("\n")
                    "Dedupe" -> lines.distinct().joinToString("\n")
                    "Reverse" -> lines.reversed().joinToString("\n")
                    "Count" -> "Lines: ${lines.size}"
                    else -> input
                }
            }
        }
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