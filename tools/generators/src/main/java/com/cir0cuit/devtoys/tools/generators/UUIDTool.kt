package com.cir0cuit.devtoys.tools.generators

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.OutputPanel
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolCategory
import com.cir0cuit.devtoys.tools.base.ToolDescriptor
import java.util.UUID

private val UUID_COUNTS = listOf(1, 5, 10, 25)

private fun randomUuids(count: Int) = List(count) { UUID.randomUUID().toString() }.joinToString("\n")

class UUIDTool : Tool {
    override val descriptor: ToolDescriptor = ToolDescriptor(
        id = "uuid",
        title = "UUID Generator",
        description = "Generate random UUIDs",
        icon = Icons.Default.Fingerprint,
        category = ToolCategory.GENERATORS,
        keywords = listOf("uuid", "guid", "generate")
    )

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @Composable
    override fun Content(modifier: Modifier) {
        var count by rememberSaveable { mutableStateOf(1) }
        var raw by rememberSaveable { mutableStateOf(randomUuids(1)) }
        var isUppercase by rememberSaveable { mutableStateOf(false) }
        var hyphens by rememberSaveable { mutableStateOf(true) }
        val output = raw.let { if (hyphens) it else it.replace("-", "") }.let { if (isUppercase) it.uppercase() else it }

        ToolScaffold(title = descriptor.title, modifier = modifier) {
            Text("How many", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                UUID_COUNTS.forEachIndexed { i, n ->
                    SegmentedButton(
                        selected = count == n,
                        onClick = { count = n; raw = randomUuids(n) },
                        shape = SegmentedButtonDefaults.itemShape(i, UUID_COUNTS.size),
                        icon = {},
                        label = { Text(n.toString()) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = isUppercase, onClick = { isUppercase = !isUppercase }, label = { Text("Uppercase") })
                FilterChip(selected = hyphens, onClick = { hyphens = !hyphens }, label = { Text("Hyphens") })
            }
            Spacer(Modifier.height(16.dp))
            RegenerateButton("Generate") { raw = randomUuids(count) }
            Spacer(Modifier.height(16.dp))
            OutputPanel(content = output, label = if (count == 1) "UUID" else "$count UUIDs")
        }
    }
}
