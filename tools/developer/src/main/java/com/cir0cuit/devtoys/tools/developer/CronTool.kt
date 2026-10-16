package com.cir0cuit.devtoys.tools.developer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.designsystem.CodeTextStyle
import com.cir0cuit.devtoys.core.designsystem.LocalSlabColor
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*
import com.cronutils.descriptor.CronDescriptor
import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.model.time.ExecutionTime
import com.cronutils.parser.CronParser
import kotlinx.coroutines.delay
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

enum class CronOutputFormat(val label: String, val formatter: DateTimeFormatter) {
    STANDARD("Standard", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")),
    ISO_8601("ISO 8601", DateTimeFormatter.ISO_OFFSET_DATE_TIME),
    RFC_1123("RFC 1123", DateTimeFormatter.RFC_1123_DATE_TIME)
}

private val NEXT_RUN_COUNTS = listOf(1, 5, 10, 25, 50, 100)

private class CronPart(val label: String, val value: String, val onChange: (String) -> Unit, val presets: List<Pair<String, String>>)

class CronTool : Tool {
    override val descriptor = ToolDescriptor(
        id = "cron",
        title = "Cron Parser & Builder",
        description = "Read, build and preview cron schedules",
        icon = Icons.Default.Event,
        category = ToolCategory.CONVERTERS,
        keywords = listOf("cron", "schedule")
    )

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

        val activeCron = if (tabIndex == 0) input.trim() else {
            if (includeSeconds) "$bSec $bMin $bHour $bDay $bMonth $bDayWeek" else "$bMin $bHour $bDay $bMonth $bDayWeek"
        }

        var now by remember { mutableStateOf(ZonedDateTime.now()) }
        LaunchedEffect(includeSeconds) {
            while (true) {
                val current = ZonedDateTime.now()
                now = current.truncatedTo(if (includeSeconds) ChronoUnit.SECONDS else ChronoUnit.MINUTES)
                val elapsedMs = current.nano / 1_000_000L + if (includeSeconds) 0L else current.second * 1000L
                val periodMs = if (includeSeconds) 1000L else 60_000L
                delay((periodMs - elapsedMs).coerceAtLeast(1L))
            }
        }

        val parsed = remember(activeCron, includeSeconds) {
            runCatching {
                val cron = CronParser(CronDefinitionBuilder.instanceDefinitionFor(if (includeSeconds) CronType.SPRING else CronType.UNIX)).parse(activeCron)
                cron to CronDescriptor.instance(Locale.ENGLISH).describe(cron)
            }.getOrNull()
        }
        val runs = remember(parsed, outputFormat, nextExecutionsCount, now) {
            parsed?.let { (cron, _) ->
                runCatching {
                    val executionTime = ExecutionTime.forCron(cron)
                    generateSequence(executionTime.nextExecution(now).orElse(null)) { executionTime.nextExecution(it).orElse(null) }
                        .take(nextExecutionsCount)
                        .map { it.format(outputFormat.formatter) }
                        .toList()
                }.getOrNull()
            }.orEmpty()
        }

        ToolScaffold(descriptor.title, modifier = modifier) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf("Parser", "Builder").forEachIndexed { i, label ->
                    SegmentedButton(selected = tabIndex == i, onClick = { tabIndex = i }, shape = SegmentedButtonDefaults.itemShape(i, 2)) { Text(label) }
                }
            }
            Spacer(Modifier.height(16.dp))

            if (tabIndex == 0) {
                MonoTextField(input, { input = it }, "Cron expression", minLines = 1)
            } else {
                val parts = listOfNotNull(
                    CronPart("Second", bSec, { bSec = it }, listOf("Every second" to "*", "At second 0" to "0", "Every 5 seconds" to "*/5", "Every 15 seconds" to "*/15", "Every 30 seconds" to "*/30"))
                        .takeIf { includeSeconds },
                    CronPart("Minute", bMin, { bMin = it }, listOf("Every minute" to "*", "At minute 0" to "0", "Every 5 minutes" to "*/5", "Every 15 minutes" to "*/15", "Every 30 minutes" to "*/30", "Minute 15" to "15", "Minute 30" to "30", "Minute 45" to "45")),
                    CronPart("Hour", bHour, { bHour = it }, listOf("Every hour" to "*", "Midnight" to "0", "Noon" to "12", "Every 2 hours" to "*/2", "Every 6 hours" to "*/6", "Every 12 hours" to "*/12", "Working hours" to "9-17")),
                    CronPart("Day of month", bDay, { bDay = it }, listOf("Every day" to "*", "1st of month" to "1", "15th of month" to "15", "Every 2 days" to "*/2")),
                    CronPart("Month", bMonth, { bMonth = it }, listOf("Every month" to "*", "January" to "1", "June" to "6", "December" to "12", "Every 3 months" to "*/3", "Every 6 months" to "*/6")),
                    CronPart("Day of week", bDayWeek, { bDayWeek = it }, listOf("Every day" to "*", "Monday" to "1", "Friday" to "5", "Monday to Friday" to "1-5", "Weekend" to "6,0"))
                )
                parts.chunked(2).forEachIndexed { i, pair ->
                    if (i > 0) Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { CronPartField(it, Modifier.weight(1f)) }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            FilterChip(
                selected = includeSeconds,
                onClick = { includeSeconds = !includeSeconds },
                label = { Text("Seconds field") },
                leadingIcon = if (includeSeconds) { { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) } } else null
            )
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                CronOutputFormat.entries.forEachIndexed { i, f ->
                    SegmentedButton(
                        selected = outputFormat == f,
                        onClick = { outputFormat = f },
                        shape = SegmentedButtonDefaults.itemShape(i, CronOutputFormat.entries.size),
                        icon = {}
                    ) { Text(f.label, maxLines = 1) }
                }
            }
            Spacer(Modifier.height(16.dp))

            if (tabIndex == 1) {
                OutputPanel(activeCron, label = "Expression")
                Spacer(Modifier.height(8.dp))
            }
            when {
                activeCron.isEmpty() -> OutputPanel("", label = "Meaning", isCopyable = false)
                parsed == null -> Text(
                    if (includeSeconds) "Not a valid cron expression. Use 6 fields (second minute hour day month weekday), like 0 */5 * * * *, or turn off Seconds field."
                    else "Not a valid cron expression. Use 5 fields (minute hour day month weekday), like */5 * * * *, or turn on Seconds field.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                else -> {
                    OutputPanel(parsed.second, label = "Meaning")
                    Spacer(Modifier.height(8.dp))
                    RunsPanel(runs, nextExecutionsCount) { nextExecutionsCount = it }
                }
            }
        }
    }
}

/** Compact preset-or-type field for one cron part. */
@Composable
private fun CronPartField(part: CronPart, modifier: Modifier) {
    var menu by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(
            value = part.value,
            onValueChange = part.onChange,
            label = { Text(part.label, maxLines = 1) },
            singleLine = true,
            textStyle = CodeTextStyle,
            shape = MaterialTheme.shapes.medium,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, autoCorrect = false),
            trailingIcon = {
                IconButton(onClick = { menu = true }) { Icon(Icons.Default.ArrowDropDown, contentDescription = "${part.label} presets") }
            },
            modifier = Modifier.fillMaxWidth()
        )
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            part.presets.forEach { (name, code) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    trailingIcon = { Text(code, style = CodeTextStyle, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    onClick = { part.onChange(code); menu = false }
                )
            }
        }
    }
}

/** Slab of upcoming runs with the run count picker in its header. */
@Composable
private fun RunsPanel(runs: List<String>, count: Int, onCount: (Int) -> Unit) {
    val clipboard = LocalClipboardManager.current
    var menu by remember { mutableStateOf(false) }
    val text = runs.joinToString("\n")
    Surface(Modifier.fillMaxWidth(), color = LocalSlabColor.current, shape = MaterialTheme.shapes.medium) {
        Column {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Next runs", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Box {
                    TextButton(onClick = { menu = true }) {
                        Text("Show $count")
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        NEXT_RUN_COUNTS.forEach { n ->
                            DropdownMenuItem(text = { Text("$n") }, onClick = { onCount(n); menu = false })
                        }
                    }
                }
                IconButton(onClick = { clipboard.setText(AnnotatedString(text)) }, enabled = runs.isNotEmpty()) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy next runs", modifier = Modifier.size(20.dp))
                }
            }
            SelectionContainer {
                Text(
                    text.ifEmpty { "No upcoming runs" },
                    style = CodeTextStyle,
                    color = if (runs.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                )
            }
        }
    }
}
