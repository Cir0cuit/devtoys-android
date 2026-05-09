package com.cir0cuit.devtoys.tools.generators

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint

import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.OutputPanel
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolCategory
import com.cir0cuit.devtoys.tools.base.ToolDescriptor
import java.util.UUID

class UUIDTool : Tool {
    override val descriptor: ToolDescriptor = ToolDescriptor(
        id = "uuid",
        titleRes = android.R.string.untitled,
        descriptionRes = android.R.string.untitled,
        icon = Icons.Default.Fingerprint, // Placeholder icon
        category = ToolCategory.GENERATORS,
        keywords = listOf("uuid", "guid", "generate")
    )

    @Composable
    override fun Content(modifier: Modifier) {
        var output by rememberSaveable { mutableStateOf(UUID.randomUUID().toString()) }
        var count by rememberSaveable { mutableStateOf(1) }
        var isUppercase by rememberSaveable { mutableStateOf(false) }

        ToolScaffold(
            title = "UUID Generator",
            modifier = modifier
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Uppercase", modifier = Modifier.padding(end = 8.dp))
                Switch(
                    checked = isUppercase,
                    onCheckedChange = { isUppercase = it }
                )
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = {
                    val uuids = List(count) { UUID.randomUUID().toString() }
                    val result = uuids.joinToString("\n")
                    output = if (isUppercase) result.uppercase() else result
                }) {
                    Text("Generate")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            Text("Output")
            OutputPanel(content = output)
        }
    }
}
