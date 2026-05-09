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
import org.json.JSONArray
import org.json.JSONObject

class JsonFormatterTool : Tool {
    override val descriptor = ToolDescriptor("json-formatter", android.R.string.untitled, android.R.string.untitled, Icons.Default.DataObject, ToolCategory.FORMATTERS, listOf("json", "format"))
    @Composable override fun Content(modifier: Modifier) {
        var input by rememberSaveable { mutableStateOf("") }
        val output = remember(input) {
            if (input.isEmpty()) "" else try {
                val trimmed = input.trim()
                if (trimmed.startsWith("[")) JSONArray(trimmed).toString(2) else JSONObject(trimmed).toString(2)
            } catch (e: Exception) { "Error: ${e.message}" }
        }
        ToolScaffold("JSON Formatter", modifier = modifier) {
            MonoTextField(input, { input = it }, "JSON Input")
            Spacer(modifier = Modifier.height(16.dp))
            OutputPanel(output)
        }
    }
}