package com.cir0cuit.devtoys.tools.developer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.designsystem.CodeTextStyle
import com.cir0cuit.devtoys.core.designsystem.LocalSlabColor
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*
import com.cronutils.model.time.ExecutionTime
import kotlinx.coroutines.delay
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

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
        keywords = listOf("cron", "schedule", "quartz", "spring", "crontab", "kubernetes", "systemd", "eventbridge")
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable override fun Content(modifier: Modifier) {
        var tabIndex by rememberSaveable { mutableStateOf(0) }
        var input by rememberSaveable { mutableStateOf("*/5 * * * *") }
        var dialect by rememberSaveable { mutableStateOf(CronDialect.UNIX) }
        var outputFormat by rememberSaveable { mutableStateOf(CronOutputFormat.STANDARD) }
        var nextExecutionsCount by rememberSaveable { mutableStateOf(5) }
        var zoneId by rememberSaveable { mutableStateOf(ZoneId.systemDefault().id) }
        var picking by rememberSaveable { mutableStateOf(false) }
        val zone = remember(zoneId) { runCatching { ZoneId.of(zoneId) }.getOrDefault(ZoneId.systemDefault()) }

        var bSec by rememberSaveable { mutableStateOf("0") }
        var bMin by rememberSaveable { mutableStateOf("*") }
        var bHour by rememberSaveable { mutableStateOf("*") }
        var bDay by rememberSaveable { mutableStateOf("*") }
        var bMonth by rememberSaveable { mutableStateOf("*") }
        var bDayWeek by rememberSaveable { mutableStateOf("*") }

        val activeCron = if (tabIndex == 0) input.trim() else buildCron(dialect, bSec, bMin, bHour, bDay, bMonth, bDayWeek)
        val seconds = dialect.hasSeconds

        var now by remember { mutableStateOf(ZonedDateTime.now(zone)) }
        LaunchedEffect(seconds, zone) {
            while (true) {
                val current = ZonedDateTime.now(zone)
                now = current.truncatedTo(if (seconds) ChronoUnit.SECONDS else ChronoUnit.MINUTES)
                val elapsedMs = current.nano / 1_000_000L + if (seconds) 0L else current.second * 1000L
                val periodMs = if (seconds) 1000L else 60_000L
                delay((periodMs - elapsedMs).coerceAtLeast(1L))
            }
        }

        val parsed = remember(activeCron, dialect) { runCatching { parseCron(dialect, activeCron) }.getOrNull() }
        val rangeError = remember(parsed, activeCron, dialect) { if (parsed == null) cronRangeError(dialect, activeCron) else null }
        val runs = remember(parsed, nextExecutionsCount, now) {
            parsed?.let { (cron, _) ->
                runCatching {
                    val executionTime = ExecutionTime.forCron(cron)
                    val next = generateSequence(executionTime.nextExecution(now).orElse(null)) { executionTime.nextExecution(it).orElse(null) }
                        .take(nextExecutionsCount).toList()
                    val previous = generateSequence(executionTime.lastExecution(now).orElse(null)) { executionTime.lastExecution(it).orElse(null) }
                        .take(nextExecutionsCount).toList()
                    next to previous
                }.getOrNull()
            } ?: (emptyList<ZonedDateTime>() to emptyList())
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
                        .takeIf { seconds },
                    CronPart("Minute", bMin, { bMin = it }, listOf("Every minute" to "*", "At minute 0" to "0", "Every 5 minutes" to "*/5", "Every 15 minutes" to "*/15", "Every 30 minutes" to "*/30", "Minute 15" to "15", "Minute 30" to "30", "Minute 45" to "45")),
                    CronPart("Hour", bHour, { bHour = it }, listOf("Every hour" to "*", "Midnight" to "0", "Noon" to "12", "Every 2 hours" to "*/2", "Every 6 hours" to "*/6", "Every 12 hours" to "*/12", "Working hours" to "9-17")),
                    CronPart("Day of month", bDay, { bDay = it }, listOf("Every day" to "*", "1st of month" to "1", "15th of month" to "15", "Every 2 days" to "*/2")),
                    CronPart("Month", bMonth, { bMonth = it }, listOf("Every month" to "*", "January" to "1", "June" to "6", "December" to "12", "Every 3 months" to "*/3", "Every 6 months" to "*/6")),
                    // Names, not numbers: Quartz counts weekdays from 1 = Sunday, Unix from 0.
                    CronPart("Day of week", bDayWeek, { bDayWeek = it }, listOf("Every day" to "*", "Monday" to "MON", "Friday" to "FRI", "Monday to Friday" to "MON-FRI", "Weekend" to "SAT,SUN"))
                )
                parts.chunked(2).forEachIndexed { i, pair ->
                    if (i > 0) Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { CronPartField(it, rangeError?.field == it.label, Modifier.weight(1f)) }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // Full width: "AWS EventBridge" does not fit a half-width field.
            SelectField("Dialect", dialect, CronDialect.entries, { dialect = it }, Modifier.fillMaxWidth()) { it.label }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { picking = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Public, contentDescription = "Time zone", modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(cityName(zone), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
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
                activeCron.equals("@reboot", ignoreCase = true) -> OutputPanel("Once, when the machine starts. There are no scheduled runs.", label = "Meaning")
                parsed == null -> Text(
                    rangeError?.message ?: "Not a valid ${dialect.label} cron expression. Use ${dialect.hint}, or pick another dialect.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                else -> {
                    OutputPanel(parsed.second, label = "Meaning")
                    Spacer(Modifier.height(8.dp))
                    CopyAsButton(dialect, activeCron, zone)
                    Spacer(Modifier.height(8.dp))
                    val (next, previous) = runs
                    val first = next.firstOrNull()?.let { " · first " + relativeTime(it.toInstant(), now.toInstant()) }.orEmpty()
                    RunsPanel("Next runs$first", next.map { it.format(outputFormat.formatter) }, "No upcoming runs", nextExecutionsCount) { nextExecutionsCount = it }
                    Spacer(Modifier.height(8.dp))
                    RunsPanel("Previous runs", previous.map { it.format(outputFormat.formatter) }, "No earlier runs", null, null)
                }
            }
        }

        if (picking) ZonePickerDialog(zone, onPick = { zoneId = it.id; picking = false }, onDismiss = { picking = false })
    }
}

/** "Copy as" menu; targets the schedule can't be converted to stay disabled with the reason. */
@Composable
private fun CopyAsButton(dialect: CronDialect, expr: String, zone: ZoneId) {
    val clipboard = LocalClipboardManager.current
    var menu by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { menu = true }) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text("Copy as")
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            CronTarget.entries.forEach { target ->
                val snippet = remember(target, dialect, expr, zone) { runCatching { cronSnippet(target, dialect, expr, zone) } }
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(target.label)
                            snippet.exceptionOrNull()?.let {
                                Text(it.message ?: "Not convertible.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    enabled = snippet.isSuccess,
                    onClick = { clipboard.setText(AnnotatedString(snippet.getOrThrow())); menu = false }
                )
            }
        }
    }
}

/** Compact preset-or-type field for one cron part. */
@Composable
private fun CronPartField(part: CronPart, isError: Boolean, modifier: Modifier) {
    var menu by remember { mutableStateOf(false) }
    var fieldWidth by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    Box(modifier.onSizeChanged { fieldWidth = it.width }) {
        OutlinedTextField(
            value = part.value,
            onValueChange = part.onChange,
            label = { Text(part.label, maxLines = 1) },
            singleLine = true,
            isError = isError,
            textStyle = CodeTextStyle,
            shape = MaterialTheme.shapes.medium,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, autoCorrect = false),
            trailingIcon = {
                IconButton(onClick = { menu = true }) { Icon(Icons.Default.ArrowDropDown, contentDescription = "${part.label} presets") }
            },
            modifier = Modifier.fillMaxWidth()
        )
        // Same rules as SelectField: as wide as the field, capped height (the menu scrolls), current value in primary.
        DropdownMenu(
            expanded = menu,
            onDismissRequest = { menu = false },
            modifier = Modifier.width(with(density) { fieldWidth.toDp() }).heightIn(max = 280.dp)
        ) {
            part.presets.forEach { (name, code) ->
                val current = code == part.value.trim()
                DropdownMenuItem(
                    text = { Text(name, color = if (current) MaterialTheme.colorScheme.primary else Color.Unspecified) },
                    trailingIcon = { Text(code, style = CodeTextStyle, color = if (current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) },
                    onClick = { part.onChange(code); menu = false }
                )
            }
        }
    }
}

/** Slab of runs; the run count picker sits in the header when [onCount] is set. */
@Composable
private fun RunsPanel(title: String, runs: List<String>, empty: String, count: Int?, onCount: ((Int) -> Unit)?) {
    val clipboard = LocalClipboardManager.current
    var menu by remember { mutableStateOf(false) }
    val text = runs.joinToString("\n")
    Surface(Modifier.fillMaxWidth(), color = LocalSlabColor.current, shape = MaterialTheme.shapes.medium) {
        Column {
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (count != null && onCount != null) Box {
                    TextButton(onClick = { menu = true }) {
                        Text("Show $count")
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        NEXT_RUN_COUNTS.forEach { n ->
                            DropdownMenuItem(
                                text = { Text("$n", color = if (n == count) MaterialTheme.colorScheme.primary else Color.Unspecified) },
                                onClick = { onCount(n); menu = false }
                            )
                        }
                    }
                }
                IconButton(onClick = { clipboard.setText(AnnotatedString(text)) }, enabled = runs.isNotEmpty()) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy ${title.substringBefore(" ·").lowercase()}", modifier = Modifier.size(20.dp))
                }
            }
            SelectionContainer {
                Text(
                    text.ifEmpty { empty },
                    style = CodeTextStyle,
                    color = if (runs.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                )
            }
        }
    }
}
