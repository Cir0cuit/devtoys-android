package com.cir0cuit.devtoys.tools.developer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class InputFormat(val label: String) {
    SECONDS("Seconds"),
    MILLISECONDS("Milliseconds"),
    TICKS("Ticks (100-ns)")
}

enum class OutputFormat(val label: String, val formatter: DateTimeFormatter) {
    ISO_LOCAL("ISO 8601 (Local)", DateTimeFormatter.ISO_LOCAL_DATE_TIME),
    ISO_OFFSET("ISO 8601 (Offset)", DateTimeFormatter.ISO_OFFSET_DATE_TIME),
    RFC_1123("RFC 1123", DateTimeFormatter.RFC_1123_DATE_TIME),
    CUSTOM_SIMPLE("Simple Date/Time", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"))
}

private const val SYSTEM_ZONE = "System"

class TimestampTool : Tool {
    override val descriptor = ToolDescriptor("timestamp", android.R.string.untitled, android.R.string.untitled, Icons.Default.Schedule, ToolCategory.CONVERTERS, listOf("time", "stamp", "date"))
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable override fun Content(modifier: Modifier) {
        var input by rememberSaveable { mutableStateOf((System.currentTimeMillis() / 1000).toString()) }
        var inputFormat by rememberSaveable { mutableStateOf(InputFormat.SECONDS) }
        var outputFormat by rememberSaveable { mutableStateOf(OutputFormat.ISO_LOCAL) }
        val systemZoneLabel = "System (${ZoneId.systemDefault().id})"

        var selectedUtc by rememberSaveable { mutableStateOf(SYSTEM_ZONE) }
        var isSummerTime by rememberSaveable { mutableStateOf(false) }
        
        val zoneId = remember(selectedUtc, isSummerTime) {
            if (selectedUtc == SYSTEM_ZONE) return@remember ZoneId.systemDefault()
            val baseStr = selectedUtc.replace("UTC", "").replace("Z", "+00:00")
            val baseOffset = try { java.time.ZoneOffset.of(if (baseStr.isEmpty()) "+00:00" else baseStr) } catch(e: Exception) { java.time.ZoneOffset.UTC }
            val finalOffset = if (isSummerTime) java.time.ZoneOffset.ofTotalSeconds(baseOffset.totalSeconds + 3600) else baseOffset
            ZoneId.ofOffset("UTC", finalOffset)
        }

        val output = remember(input, inputFormat, outputFormat, zoneId) {
            input.lines().map { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty()) "" else {
                    try {
                        val v = trimmed.toLongOrNull() ?: throw IllegalArgumentException("Invalid number")
                        val inst = when (inputFormat) {
                            InputFormat.SECONDS -> Instant.ofEpochSecond(v)
                            InputFormat.MILLISECONDS -> Instant.ofEpochMilli(v)
                            InputFormat.TICKS -> {
                                val TICKS_EPOCH = 621355968000000000L
                                val ticksSinceEpoch = v - TICKS_EPOCH
                                val seconds = Math.floorDiv(ticksSinceEpoch, 10000000L)
                                val nanos = Math.floorMod(ticksSinceEpoch, 10000000L) * 100L
                                Instant.ofEpochSecond(seconds, nanos)
                            }
                        }
                        inst.atZone(zoneId).format(outputFormat.formatter)
                    } catch (e: Exception) { "Error: ${e.message}" }
                }
            }.filter { it.isNotEmpty() }.joinToString("\n")
        }

        ToolScaffold("Timestamp Converter", modifier = modifier, scrollable = false) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Input Format Dropdown
                    var inExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = inExpanded, onExpandedChange = { inExpanded = !inExpanded }, modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = inputFormat.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Input Format") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = inExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = inExpanded, onDismissRequest = { inExpanded = false }) {
                            InputFormat.values().forEach { fmt ->
                                DropdownMenuItem(
                                    text = { Text(fmt.label) },
                                    onClick = { inputFormat = fmt; inExpanded = false }
                                )
                            }
                        }
                    }

                    // Output Format Dropdown
                    var outExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = outExpanded, onExpandedChange = { outExpanded = !outExpanded }, modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = outputFormat.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Output Format") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = outExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = outExpanded, onDismissRequest = { outExpanded = false }) {
                            OutputFormat.values().forEach { fmt ->
                                DropdownMenuItem(
                                    text = { Text(fmt.label) },
                                    onClick = { outputFormat = fmt; outExpanded = false }
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // UTC Offset & Summer Time
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    var tzExpanded by remember { mutableStateOf(false) }
                    
                    val utcOffsets = remember {
                        listOf(
                            "-12:00", "-11:00", "-10:00", "-09:30", "-09:00", "-08:00", "-07:00", "-06:00", "-05:00", "-04:00", "-03:30", "-03:00", "-02:00", "-01:00",
                            "+00:00", "+01:00", "+02:00", "+03:00", "+03:30", "+04:00", "+04:30", "+05:00", "+05:30", "+05:45", "+06:00", "+06:30", "+07:00", "+08:00", "+08:45", "+09:00", "+09:30", "+10:00", "+10:30", "+11:00", "+12:00", "+12:45", "+13:00", "+14:00"
                        ).map { "UTC$it" }.let { listOf(SYSTEM_ZONE) + it }
                    }

                    ExposedDropdownMenuBox(expanded = tzExpanded, onExpandedChange = { tzExpanded = !tzExpanded }, modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = if (selectedUtc == SYSTEM_ZONE) systemZoneLabel else selectedUtc,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Time Zone") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tzExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = tzExpanded, onDismissRequest = { tzExpanded = false }) {
                            utcOffsets.forEach { zone ->
                                DropdownMenuItem(
                                    text = { Text(if (zone == SYSTEM_ZONE) systemZoneLabel else zone) },
                                    onClick = { selectedUtc = zone; tzExpanded = false }
                                )
                            }
                        }
                    }
                    
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                        Text("Summer time")
                        Spacer(Modifier.width(8.dp))
                        Switch(checked = isSummerTime && selectedUtc != SYSTEM_ZONE, onCheckedChange = { isSummerTime = it }, enabled = selectedUtc != SYSTEM_ZONE)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                MonoTextField(input, { input = it }, "Timestamp Input (One per line)")
                Spacer(modifier = Modifier.height(16.dp))
                OutputPanel(output)
            }
        }
    }
}