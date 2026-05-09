package com.cir0cuit.devtoys.tools.developer

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material3.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*

class ChmodCalculatorTool : Tool {
    override val descriptor = ToolDescriptor(
        "chmod",
        android.R.string.untitled,
        android.R.string.untitled,
        Icons.Default.AdminPanelSettings,
        ToolCategory.NETWORK_SYSTEM,
        listOf("chmod", "permissions", "linux", "octal")
    )

    @Composable
    override fun Content(modifier: Modifier) {
        var ownerR by rememberSaveable { mutableStateOf(true) }
        var ownerW by rememberSaveable { mutableStateOf(true) }
        var ownerX by rememberSaveable { mutableStateOf(true) }

        var groupR by rememberSaveable { mutableStateOf(true) }
        var groupW by rememberSaveable { mutableStateOf(false) }
        var groupX by rememberSaveable { mutableStateOf(true) }

        var publicR by rememberSaveable { mutableStateOf(true) }
        var publicW by rememberSaveable { mutableStateOf(false) }
        var publicX by rememberSaveable { mutableStateOf(true) }

        val octal = remember(ownerR, ownerW, ownerX, groupR, groupW, groupX, publicR, publicW, publicX) {
            val o = (if (ownerR) 4 else 0) + (if (ownerW) 2 else 0) + (if (ownerX) 1 else 0)
            val g = (if (groupR) 4 else 0) + (if (groupW) 2 else 0) + (if (groupX) 1 else 0)
            val p = (if (publicR) 4 else 0) + (if (publicW) 2 else 0) + (if (publicX) 1 else 0)
            "$o$g$p"
        }

        val symbolic = remember(ownerR, ownerW, ownerX, groupR, groupW, groupX, publicR, publicW, publicX) {
            val o = "${if(ownerR) "r" else "-"}${if(ownerW) "w" else "-"}${if(ownerX) "x" else "-"}"
            val g = "${if(groupR) "r" else "-"}${if(groupW) "w" else "-"}${if(groupX) "x" else "-"}"
            val p = "${if(publicR) "r" else "-"}${if(publicW) "w" else "-"}${if(publicX) "x" else "-"}"
            "$o$g$p"
        }

        ToolScaffold("Chmod Calculator", modifier = modifier) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                PermissionColumn("Owner", ownerR, ownerW, ownerX, { ownerR = it }, { ownerW = it }, { ownerX = it })
                PermissionColumn("Group", groupR, groupW, groupX, { groupR = it }, { groupW = it }, { groupX = it })
                PermissionColumn("Public", publicR, publicW, publicX, { publicR = it }, { publicW = it }, { publicX = it })
            }
            Spacer(modifier = Modifier.height(32.dp))
            
            Text("Octal", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            OutputPanel(octal)
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Symbolic", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            OutputPanel(symbolic)
        }
    }

    @Composable
    private fun PermissionColumn(
        title: String,
        r: Boolean, w: Boolean, x: Boolean,
        onR: (Boolean) -> Unit, onW: (Boolean) -> Unit, onX: (Boolean) -> Unit
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(title, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp, start = 12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = r, onCheckedChange = onR)
                Text("Read")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = w, onCheckedChange = onW)
                Text("Write")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = x, onCheckedChange = onX)
                Text("Execute")
            }
        }
    }
}
