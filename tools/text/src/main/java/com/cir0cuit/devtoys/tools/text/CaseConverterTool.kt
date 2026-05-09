package com.cir0cuit.devtoys.tools.text

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatSize

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*

class CaseConverterTool : Tool {
    override val descriptor = ToolDescriptor("case", android.R.string.untitled, android.R.string.untitled, Icons.Default.FormatSize, ToolCategory.TEXT, listOf("case", "upper", "lower"))
    @Composable override fun Content(modifier: Modifier) {
        var input by rememberSaveable { mutableStateOf("") }
        val upperOutput = remember(input) { input.uppercase() }
        val lowerOutput = remember(input) { input.lowercase() }
        
        ToolScaffold("Case Converter", modifier = modifier) {
            MonoTextField(input, { input = it }, "Input")
            Spacer(modifier = Modifier.height(16.dp))
            androidx.compose.material3.Text("Uppercase", style = androidx.compose.material3.MaterialTheme.typography.titleMedium, color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            OutputPanel(upperOutput)
            Spacer(modifier = Modifier.height(16.dp))
            androidx.compose.material3.Text("Lowercase", style = androidx.compose.material3.MaterialTheme.typography.titleMedium, color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            OutputPanel(lowerOutput)
        }
    }
}