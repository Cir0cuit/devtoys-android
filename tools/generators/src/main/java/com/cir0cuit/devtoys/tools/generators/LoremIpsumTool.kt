package com.cir0cuit.devtoys.tools.generators
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Abc

import androidx.compose.material3.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*

class LoremIpsumTool : Tool {
    override val descriptor = ToolDescriptor("lorem-ipsum", android.R.string.untitled, android.R.string.untitled, Icons.Default.Abc, ToolCategory.GENERATORS, listOf("lorem", "ipsum", "text"))
    @Composable override fun Content(modifier: Modifier) {
        var count by rememberSaveable { mutableStateOf(5f) }
        val output = remember(count) {
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. ".repeat(count.toInt())
        }
        ToolScaffold("Lorem Ipsum Generator", modifier = modifier) {
            Text("Sentences: ${count.toInt()}")
            Slider(value = count, onValueChange = { count = it }, valueRange = 1f..50f)
            Spacer(modifier = Modifier.height(16.dp))
            OutputPanel(output)
        }
    }
}