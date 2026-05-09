package com.cir0cuit.devtoys.tools.text
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Difference

import androidx.compose.material3.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*
import com.github.difflib.DiffUtils
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.ui.Alignment

class TextDiffTool : Tool {
    override val descriptor = ToolDescriptor("text-diff", android.R.string.untitled, android.R.string.untitled, Icons.Default.Difference, ToolCategory.TEXT, listOf("diff", "compare"))
    
    @Composable override fun Content(modifier: Modifier) {
        val clipboardManager = LocalClipboardManager.current
        var tabIndex by rememberSaveable { mutableStateOf(0) }
        var oldText by rememberSaveable { mutableStateOf("") }
        var newText by rememberSaveable { mutableStateOf("") }
        
        val output = remember(oldText, newText) {
            buildAnnotatedString {
                if (oldText.isEmpty() && newText.isEmpty()) {
                    append("Please enter text to compare.")
                } else {
                    try {
                        val oldLines = oldText.lines()
                        val newLines = newText.lines()
                        val patch = DiffUtils.diff(oldLines, newLines)
                        
                        if (patch.deltas.isEmpty()) {
                            append("Texts are identical.")
                        } else {
                            var lastOldLine = 0
                            var lastNewLine = 0
                            patch.deltas.forEach { rawDelta ->
                                val delta = rawDelta as com.github.difflib.patch.AbstractDelta<String>
                                val start = maxOf(lastOldLine, delta.source.position - 3)
                                if (start > lastOldLine) {
                                    withStyle(SpanStyle(color = Color.Gray)) {
                                        append(" ... |  ... | \n")
                                    }
                                }
                                
                                var oldLineNum = start + 1
                                var newLineNum = delta.target.position - (delta.source.position - start) + 1
                                
                                for (i in start until delta.source.position) {
                                    append(String.format("%4d | %4d |   %s\n", oldLineNum++, newLineNum++, oldLines[i]))
                                }
                                
                                delta.source.lines.forEach { line ->
                                    withStyle(SpanStyle(color = Color(0xFFE53935), background = Color(0x22E53935))) {
                                        append(String.format("%4d | %4s | - %s\n", oldLineNum++, "", line))
                                    }
                                }
                                
                                delta.target.lines.forEach { line ->
                                    withStyle(SpanStyle(color = Color(0xFF43A047), background = Color(0x2243A047))) {
                                        append(String.format("%4s | %4d | + %s\n", "", newLineNum++, line))
                                    }
                                }
                                
                                lastOldLine = delta.source.position + delta.source.lines.size
                                lastNewLine = delta.target.position + delta.target.lines.size
                            }
                            
                            val end = minOf(oldLines.size, lastOldLine + 3)
                            var endOldLineNum = lastOldLine + 1
                            var endNewLineNum = lastNewLine + 1
                            for (i in lastOldLine until end) {
                                append(String.format("%4d | %4d |   %s\n", endOldLineNum++, endNewLineNum++, oldLines[i]))
                            }
                            if (end < oldLines.size) {
                                withStyle(SpanStyle(color = Color.Gray)) {
                                    append(" ... |  ... | \n")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        append("Error: ${e.message}")
                    }
                }
            }
        }
        
        ToolScaffold("Text Comparer", modifier = modifier, scrollable = false) {
            TabRow(selectedTabIndex = tabIndex) {
                Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Old Text") })
                Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("New Text") })
                Tab(selected = tabIndex == 2, onClick = { tabIndex = 2 }, text = { Text("Differences") })
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (tabIndex == 0) {
                    NumberedTextField(oldText, { oldText = it }, modifier = Modifier.fillMaxSize())
                } else if (tabIndex == 1) {
                    NumberedTextField(newText, { newText = it }, modifier = Modifier.fillMaxSize())
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Box(modifier = Modifier.padding(16.dp).horizontalScroll(rememberScrollState()).verticalScroll(rememberScrollState())) {
                                Text(output, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                            }
                        }
                        FloatingActionButton(
                            onClick = { clipboardManager.setText(output) },
                            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                        }
                    }
                }
            }
        }
    }
}