package com.cir0cuit.devtoys.tools.developer

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material3.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*

class ChmodCalculatorTool : Tool {
    override val descriptor = ToolDescriptor(
        id = "chmod",
        title = "Chmod Calculator",
        description = "Turn file permissions into chmod values",
        icon = Icons.Default.AdminPanelSettings,
        category = ToolCategory.NETWORK_SYSTEM,
        keywords = listOf("chmod", "permissions", "linux", "octal")
    )

    @Composable
    override fun Content(modifier: Modifier) {
        // Nine permission bits, owner rwx in the high bits: 0b111_101_101 = 755.
        var mode by rememberSaveable { mutableIntStateOf(0b111_101_101) }
        var octalText by rememberSaveable { mutableStateOf("755") }
        val octal = "%03o".format(mode)
        val symbolic = (8 downTo 0).joinToString("") { bit -> if (mode shr bit and 1 == 1) "rwx"[(8 - bit) % 3].toString() else "-" }

        ToolScaffold(descriptor.title, modifier = modifier) {
            MonoTextField(
                value = octalText,
                onValueChange = { text ->
                    octalText = text
                    if (text.length == 3 && text.all { it in '0'..'7' }) mode = text.toInt(8)
                },
                label = "Octal",
                minLines = 1,
                maxLines = 1
            )
            if (octalText != octal) {
                Text(
                    "Enter three digits from 0 to 7, like 755.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(72.dp))
                listOf("Read", "Write", "Execute").forEach {
                    Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                }
            }
            listOf("Owner", "Group", "Public").forEachIndexed { row, who ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(who, style = MaterialTheme.typography.titleSmall, modifier = Modifier.width(72.dp))
                    for (col in 0..2) {
                        val bit = 1 shl (8 - row * 3 - col)
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Checkbox(checked = mode and bit != 0, onCheckedChange = {
                                mode = mode xor bit
                                octalText = "%03o".format(mode)
                            })
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            LabeledValueSlab(listOf("Symbolic" to symbolic, "Command" to "chmod $octal"))
        }
    }
}
