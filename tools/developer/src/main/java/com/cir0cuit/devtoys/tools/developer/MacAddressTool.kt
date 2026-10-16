package com.cir0cuit.devtoys.tools.developer

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material3.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class MacAddressTool : Tool {
    override val descriptor = ToolDescriptor(
        id = "mac",
        title = "MAC Address Lookup",
        description = "Format a MAC address and find its vendor",
        icon = Icons.Default.SettingsEthernet,
        category = ToolCategory.NETWORK_SYSTEM,
        keywords = listOf("mac", "address", "format")
    )

    @Composable
    override fun Content(modifier: Modifier) {
        var input by rememberSaveable { mutableStateOf("00:1A:2B:3C:4D:5E") }
        var vendor by rememberSaveable { mutableStateOf("Looking up...") }
        val context = LocalContext.current

        val cleanMac = input.replace(Regex("[^a-fA-F0-9]"), "").uppercase()
        val isValid = cleanMac.length == 12

        LaunchedEffect(cleanMac) {
            if (isValid) {
                val prefix = cleanMac.take(6)
                val hexPrefix = cleanMac.chunked(2).take(3).joinToString("-")
                vendor = "Looking up..."
                vendor = withContext(Dispatchers.IO) {
                    try {
                        var foundVendor = "Vendor not found in the offline list"
                        context.assets.open("oui.txt").bufferedReader().use { reader ->
                            for (line in reader.lineSequence()) {
                                ensureActive()
                                val marker = when {
                                    line.startsWith(prefix) && line.contains("(base 16)") -> "(base 16)"
                                    line.startsWith(hexPrefix) && line.contains("(hex)") -> "(hex)"
                                    else -> continue
                                }
                                val remainder = line.substringAfter(marker).trim()
                                if (remainder.isNotEmpty()) {
                                    foundVendor = remainder
                                    break
                                }
                            }
                        }
                        foundVendor
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        "Could not read the vendor list. Reinstall the app if this keeps happening."
                    }
                }
            }
        }

        ToolScaffold(descriptor.title, modifier = modifier) {
            MonoTextField(input, { input = it }, "MAC address", minLines = 1, maxLines = 1)
            Spacer(modifier = Modifier.height(16.dp))
            if (isValid) {
                val octets = cleanMac.chunked(2)
                LabeledValueSlab(
                    listOf(
                        "Vendor" to vendor,
                        "Colon separated" to octets.joinToString(":"),
                        "Hyphen separated" to octets.joinToString("-"),
                        "Cisco dot format" to cleanMac.chunked(4).joinToString(".").lowercase()
                    )
                )
            } else {
                Text(
                    "Enter 12 hex digits, like 00:1A:2B:3C:4D:5E.",
                    color = if (cleanMac.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
