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
import java.io.BufferedReader
import java.io.InputStreamReader

class MacAddressTool : Tool {
    override val descriptor = ToolDescriptor(
        "mac",
        com.cir0cuit.devtoys.tools.base.R.string.tool_mac_address_title,
        com.cir0cuit.devtoys.tools.base.R.string.tool_mac_address_desc,
        Icons.Default.SettingsEthernet,
        ToolCategory.NETWORK_SYSTEM,
        listOf("mac", "address", "format")
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
                val prefix = cleanMac.take(6).uppercase()
                val hexPrefix = cleanMac.chunked(2).take(3).joinToString("-")
                vendor = "Looking up..."
                vendor = withContext(Dispatchers.IO) {
                    try {
                        var foundVendor = "Unknown Vendor"
                        context.assets.open("oui.txt").use { inputStream ->
                            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                                var line: String?
                                while (reader.readLine().also { line = it } != null) {
                                    ensureActive()
                                    val currentLine = line ?: continue
                                    if ((currentLine.startsWith(prefix) && currentLine.contains("(base 16)")) ||
                                        (currentLine.startsWith(hexPrefix) && currentLine.contains("(hex)"))) {
                                        
                                        val marker = if (currentLine.contains("(base 16)")) "(base 16)" else "(hex)"
                                        val remainder = currentLine.substringAfter(marker).trim()
                                        if (remainder.isNotEmpty()) {
                                            foundVendor = remainder
                                            break
                                        }
                                    }
                                }
                            }
                        }
                        foundVendor
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        "Database missing or error"
                    }
                }
            } else {
                vendor = "Invalid MAC Address"
            }
        }
        
        val upperColon = if (isValid) cleanMac.chunked(2).joinToString(":") else "Invalid"
        val lowerColon = upperColon.lowercase()
        val upperHyphen = if (isValid) cleanMac.chunked(2).joinToString("-") else "Invalid"
        val lowerHyphen = upperHyphen.lowercase()
        val dotFormat = if (isValid) cleanMac.chunked(4).joinToString(".").lowercase() else "Invalid"
        
        ToolScaffold("MAC Formatter & Vendor ID", modifier = modifier) {
            MonoTextField(input, { input = it }, "MAC Address")
            Spacer(modifier = Modifier.height(16.dp))
            Text("Vendor", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            OutputPanel(vendor)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Colon (Upper)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            OutputPanel(upperColon)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Hyphen (Upper)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            OutputPanel(upperHyphen)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Cisco Dot Format", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            OutputPanel(dotFormat)
        }
    }
}
