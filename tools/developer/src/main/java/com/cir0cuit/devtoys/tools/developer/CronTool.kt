package com.cir0cuit.devtoys.tools.developer
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ContentCopy

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.model.CronType
import com.cronutils.parser.CronParser
import com.cronutils.descriptor.CronDescriptor
import java.util.Locale

enum class CronOutputFormat(val label: String, val formatter: java.time.format.DateTimeFormatter) {
    STANDARD("Standard", java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")),
    ISO_8601("ISO 8601", java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME),
    RFC_1123("RFC 1123", java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME)
}

class CronTool : Tool {
    override val descriptor = ToolDescriptor("cron", android.R.string.untitled, android.R.string.untitled, Icons.Default.Event, ToolCategory.CONVERTERS, listOf("cron", "schedule"))
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable override fun Content(modifier: Modifier) {
        var tabIndex by rememberSaveable { mutableStateOf(0) }
        var input by rememberSaveable { mutableStateOf("* * * * * *") }
        var includeSeconds by rememberSaveable { mutableStateOf(true) }
        var outputFormat by rememberSaveable { mutableStateOf(CronOutputFormat.STANDARD) }
        var nextExecutionsCount by rememberSaveable { mutableStateOf(5) }
        
        var bSec by rememberSaveable { mutableStateOf("*") }
        var bMin by rememberSaveable { mutableStateOf("*") }
        var bHour by rememberSaveable { mutableStateOf("*") }
        var bDay by rememberSaveable { mutableStateOf("*") }
        var bMonth by rememberSaveable { mutableStateOf("*") }
        var bDayWeek by rememberSaveable { mutableStateOf("*") }

        val activeCron = if (tabIndex == 0) input else {
            if (includeSeconds) "$bSec $bMin $bHour $bDay $bMonth $bDayWeek" else "$bMin $bHour $bDay $bMonth $bDayWeek"
        }

        val output = remember(activeCron, includeSeconds, outputFormat, nextExecutionsCount) {
            if (activeCron.isEmpty()) "" else try {
                val definition = if (includeSeconds) {
                    CronDefinitionBuilder.instanceDefinitionFor(CronType.SPRING)
                } else {
                    CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX)
                }
                val parser = CronParser(definition)
                val cron = parser.parse(activeCron)
                val descriptor = CronDescriptor.instance(Locale.ENGLISH)
                val description = descriptor.describe(cron)
                
                val executionTime = com.cronutils.model.time.ExecutionTime.forCron(cron)
                var next = executionTime.nextExecution(java.time.ZonedDateTime.now())
                val dates = mutableListOf<String>()
                
                for (i in 1..nextExecutionsCount) {
                    if (next.isPresent) {
                        dates.add(next.get().format(outputFormat.formatter))
                        next = executionTime.nextExecution(next.get())
                    }
                }
                "$description\n\nNext $nextExecutionsCount Executions:\n" + dates.joinToString("\n")
            } catch (e: Exception) { "Error: Invalid Cron Expression" }
        }
        
        ToolScaffold("Cron Parser & Builder", modifier = modifier, scrollable = false) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                TabRow(selectedTabIndex = tabIndex) {
                    Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Parser") })
                    Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("Builder") })
                }
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            CronOutputFormat.values().forEach { fmt ->
                                DropdownMenuItem(
                                    text = { Text(fmt.label) },
                                    onClick = { outputFormat = fmt; outExpanded = false }
                                )
                            }
                        }
                    }

                    var nextExpanded by remember { mutableStateOf(false) }
                    val nextOptions = listOf(1, 5, 10, 25, 50, 100)
                    ExposedDropdownMenuBox(expanded = nextExpanded, onExpandedChange = { nextExpanded = !nextExpanded }, modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = nextExecutionsCount.toString(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Next Executions") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = nextExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = nextExpanded, onDismissRequest = { nextExpanded = false }) {
                            nextOptions.forEach { count ->
                                DropdownMenuItem(
                                    text = { Text(count.toString()) },
                                    onClick = { nextExecutionsCount = count; nextExpanded = false }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("Include seconds in definition")
                    Spacer(Modifier.width(8.dp))
                    Switch(checked = includeSeconds, onCheckedChange = { includeSeconds = it })
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                if (tabIndex == 0) {
                    MonoTextField(input, { input = it }, "Cron Expression", minLines=1)
                } else {
                    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                    val context = androidx.compose.ui.platform.LocalContext.current
                    OutlinedTextField(
                        value = activeCron,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Generated Cron Expression") },
                        trailingIcon = {
                            TextButton(onClick = {
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(activeCron))
                                android.widget.Toast.makeText(context, "Copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                            }) {
                                Text("Copy")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (includeSeconds) {
                        CronPartDropdown("Second", bSec, listOf("Every second" to "*", "At second 0" to "0", "Every 5 seconds" to "*/5", "Every 15 seconds" to "*/15", "Every 30 seconds" to "*/30")) { bSec = it }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    CronPartDropdown("Minute", bMin, listOf("Every minute" to "*", "At minute 0" to "0", "Every 5 minutes" to "*/5", "Every 15 minutes" to "*/15", "Every 30 minutes" to "*/30", "Minute 15" to "15", "Minute 30" to "30", "Minute 45" to "45")) { bMin = it }
                    Spacer(modifier = Modifier.height(8.dp))
                    CronPartDropdown("Hour", bHour, listOf("Every hour" to "*", "Midnight (0)" to "0", "Noon (12)" to "12", "Every 2 hours" to "*/2", "Every 6 hours" to "*/6", "Every 12 hours" to "*/12", "Working hours (9-17)" to "9-17")) { bHour = it }
                    Spacer(modifier = Modifier.height(8.dp))
                    CronPartDropdown("Day of Month", bDay, listOf("Every day" to "*", "1st of month" to "1", "15th of month" to "15", "Every 2 days" to "*/2")) { bDay = it }
                    Spacer(modifier = Modifier.height(8.dp))
                    CronPartDropdown("Month", bMonth, listOf("Every month" to "*", "January" to "1", "June" to "6", "December" to "12", "Every 3 months" to "*/3", "Every 6 months" to "*/6")) { bMonth = it }
                    Spacer(modifier = Modifier.height(8.dp))
                    CronPartDropdown("Day of Week", bDayWeek, listOf("Every day" to "*", "Monday" to "1", "Friday" to "5", "Monday to Friday" to "1-5", "Saturday and Sunday" to "6,0")) { bDayWeek = it }
                }

                Spacer(modifier = Modifier.height(16.dp))
                OutputPanel(output)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun CronPartDropdown(label: String, selected: String, options: List<Pair<String, String>>, onSelect: (String) -> Unit) {
        var expanded by remember { mutableStateOf(false) }
        
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selected,
                onValueChange = { onSelect(it); expanded = true },
                readOnly = false,
                label = { Text("$label (Type custom or select)") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text("${opt.first}  [ ${opt.second} ]") },
                        onClick = { onSelect(opt.second); expanded = false }
                    )
                }
            }
        }
    }
}