package com.cir0cuit.devtoys.tools.developer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.cir0cuit.devtoys.core.designsystem.CodeTextStyle
import com.cir0cuit.devtoys.core.designsystem.LocalSlabColor
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.IsoFields
import java.util.Locale
import kotlin.math.abs

/** .NET ticks at the Unix epoch. */
private const val TICKS_EPOCH = 621355968000000000L

/** ponytail: one slab per line, capped; switch to a LazyColumn if people paste thousands. */
private const val MAX_BLOCKS = 50

private val ZONES: List<ZoneId> by lazy {
    (ZoneId.getAvailableZoneIds().filter { '/' in it && !it.startsWith("Etc/") && !it.startsWith("SystemV/") } + "UTC")
        .sorted().map(ZoneId::of)
}

internal class ParsedTime(val kind: String, val instant: Instant)

/**
 * Reads one value: an integer is Unix seconds, milliseconds, microseconds, .NET ticks or nanoseconds,
 * picked by magnitude; a decimal is Unix seconds; text is ISO 8601 or RFC 1123. Null when unreadable.
 */
internal fun parseTimestamp(text: String, zone: ZoneId): ParsedTime? = try {
    val s = text.trim()
    val v = s.toLongOrNull()
    when {
        v != null -> {
            val a = if (v == Long.MIN_VALUE) Long.MAX_VALUE else abs(v)
            when {
                a < 100_000_000_000L -> ParsedTime("Unix seconds", Instant.ofEpochSecond(v))
                a < 100_000_000_000_000L -> ParsedTime("Unix milliseconds", Instant.ofEpochMilli(v))
                a < 100_000_000_000_000_000L ->
                    ParsedTime("Unix microseconds", Instant.ofEpochSecond(Math.floorDiv(v, 1_000_000L), Math.floorMod(v, 1_000_000L) * 1000))
                v in 100_000_000_000_000_000L until 1_000_000_000_000_000_000L -> (v - TICKS_EPOCH).let {
                    ParsedTime(".NET ticks", Instant.ofEpochSecond(Math.floorDiv(it, 10_000_000L), Math.floorMod(it, 10_000_000L) * 100))
                }
                else -> ParsedTime("Unix nanoseconds", Instant.ofEpochSecond(Math.floorDiv(v, 1_000_000_000L), Math.floorMod(v, 1_000_000_000L)))
            }
        }
        s.matches(Regex("-?\\d+\\.\\d+")) -> {
            val d = BigDecimal(s)
            val sec = d.setScale(0, RoundingMode.FLOOR)
            ParsedTime("Unix seconds", Instant.ofEpochSecond(sec.longValueExact(), d.subtract(sec).movePointRight(9).toLong()))
        }
        else -> {
            val iso = s.replaceFirst(Regex("^(\\d{4}-\\d{2}-\\d{2}) "), "$1T")
            val city = cityName(zone)
            runCatching { ParsedTime("ISO 8601", ZonedDateTime.parse(iso, DateTimeFormatter.ISO_ZONED_DATE_TIME).toInstant()) }.getOrNull()
                ?: runCatching { ParsedTime("ISO 8601 without offset, read as $city time", LocalDateTime.parse(iso).atZone(zone).toInstant()) }.getOrNull()
                ?: runCatching { ParsedTime("Date, start of day in $city", LocalDate.parse(iso).atStartOfDay(zone).toInstant()) }.getOrNull()
                ?: ParsedTime("RFC 1123", ZonedDateTime.parse(s, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant())
        }
    }
} catch (e: Exception) { null }

/** "in 3 hours", "2 days ago", "now". */
internal fun relativeTime(then: Instant, now: Instant): String {
    val secs = Duration.between(now, then).seconds
    if (secs == 0L) return "now"
    val a = abs(secs)
    val (size, unit) = listOf(31_536_000L to "year", 2_592_000L to "month", 604_800L to "week", 86_400L to "day", 3_600L to "hour", 60L to "minute", 1L to "second")
        .first { a >= it.first }
    val n = a / size
    val text = "$n $unit" + if (n == 1L) "" else "s"
    return if (secs > 0) "in $text" else "$text ago"
}

internal fun cityName(zone: ZoneId) = zone.id.substringAfterLast('/').replace('_', ' ')

private fun offsetText(zone: ZoneId, at: Instant) = "UTC" + zone.rules.getOffset(at).id.let { if (it == "Z") "+00:00" else it }

/** Labeled results for one instant; the offset comes from the zone rules at that instant, so DST is right. */
internal fun timestampRows(t: Instant, zone: ZoneId, now: Instant, allEpochs: Boolean = false): List<Pair<String, String>> {
    val z = t.atZone(zone)
    val ms = if (t.nano == 0) "" else ".SSS"
    fun safe(f: () -> String) = runCatching(f).getOrDefault("Out of range")
    return listOf(
        "Local, ${cityName(zone)}" to safe { z.format(DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss$ms xxx")) },
        "UTC" to safe { t.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss$ms 'UTC'")) },
        "ISO 8601" to safe { z.toOffsetDateTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) },
        "RFC 1123" to safe { z.format(DateTimeFormatter.RFC_1123_DATE_TIME) },
        "Unix seconds" to t.epochSecond.toString(),
        "Unix milliseconds" to safe { t.toEpochMilli().toString() },
        "Relative" to relativeTime(t, now),
        "Day and ISO week" to safe {
            val day = z.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
            String.format(Locale.ROOT, "%s, %d-W%02d", day, z.get(IsoFields.WEEK_BASED_YEAR), z.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR))
        }
    ) + if (!allEpochs) emptyList() else Epoch.entries.filter { it != Epoch.UNIX_S && it != Epoch.UNIX_MS }.map { it.label to safe { toEpoch(it, t, zone) } }
}

/** Reads one line in the chosen epoch, or by auto-detection when [epoch] is null. */
internal fun parseAs(text: String, epoch: Epoch?, zone: ZoneId): ParsedTime? =
    if (epoch == null) parseTimestamp(text, zone) else fromEpoch(epoch, text, zone)?.let { ParsedTime(epoch.label, it) }

private val QUICK_DURATIONS = listOf("+1h" to "PT1H", "+1d" to "P1D", "+7d" to "P7D", "+30d" to "P30D", "-1d" to "-P1D")

class TimestampTool : Tool {
    override val descriptor = ToolDescriptor(
        id = "timestamp",
        title = "Timestamp Converter",
        description = "Convert timestamps and calculate dates",
        icon = Icons.Default.Schedule,
        category = ToolCategory.CONVERTERS,
        keywords = listOf("time", "stamp", "date", "unix", "epoch", "iso", "zone", "excel", "filetime", "ldap", "webkit", "cocoa", "gps", "duration", "difference", "calculator")
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable override fun Content(modifier: Modifier) {
        var mode by rememberSaveable { mutableStateOf(0) }
        var input by rememberSaveable { mutableStateOf((System.currentTimeMillis() / 1000).toString()) }
        var epoch by rememberSaveable { mutableStateOf<Epoch?>(null) }
        var dateA by rememberSaveable { mutableStateOf("") }
        var dateB by rememberSaveable { mutableStateOf("") }
        var subtract by rememberSaveable { mutableStateOf(true) }
        var durationText by rememberSaveable { mutableStateOf("P1D") }
        var zoneId by rememberSaveable { mutableStateOf(ZoneId.systemDefault().id) }
        var picking by rememberSaveable { mutableStateOf(false) }
        val zone = remember(zoneId) { runCatching { ZoneId.of(zoneId) }.getOrDefault(ZoneId.systemDefault()) }
        val now by produceState(Instant.now()) { while (true) { delay(1000); value = Instant.now() } }
        val lines = remember(input) { input.lines().map { it.trim() }.filter { it.isNotEmpty() } }
        val parsed = remember(lines, zone, epoch) { lines.take(MAX_BLOCKS).map { it to parseAs(it, epoch, zone) } }

        ToolScaffold(descriptor.title, modifier = modifier) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf("Convert", "Calculate").forEachIndexed { i, label ->
                    SegmentedButton(selected = mode == i, onClick = { mode = i }, shape = SegmentedButtonDefaults.itemShape(i, 2)) { Text(label) }
                }
            }
            Spacer(Modifier.height(16.dp))
            if (mode == 0) {
                MonoTextField(input, { input = it }, "Timestamps or dates, one per line", minLines = 2, maxLines = 6)
                Spacer(Modifier.height(8.dp))
                SelectField("Read input as", epoch, listOf<Epoch?>(null) + Epoch.entries, { epoch = it }, Modifier.fillMaxWidth()) { it?.label ?: "Auto-detect" }
            } else {
                MonoTextField(dateA, { dateA = it }, "Date A", minLines = 1, maxLines = 2)
                Spacer(Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf("A - B", "A + duration").forEachIndexed { i, label ->
                        SegmentedButton(selected = subtract == (i == 0), onClick = { subtract = i == 0 }, shape = SegmentedButtonDefaults.itemShape(i, 2)) { Text(label, maxLines = 1) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (subtract) {
                    MonoTextField(dateB, { dateB = it }, "Date B", minLines = 1, maxLines = 2)
                } else {
                    MonoTextField(durationText, { durationText = it }, "Duration, like PT90M or -P1DT2H", minLines = 1, maxLines = 1)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QUICK_DURATIONS.forEach { (label, iso) ->
                            FilterChip(selected = durationText == iso, onClick = { durationText = iso }, label = { Text(label) })
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { picking = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Public, contentDescription = "Time zone", modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(cityName(zone), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                FilledTonalButton(onClick = {
                    if (mode == 0) input = (System.currentTimeMillis() / 1000).toString()
                    else dateA = ZonedDateTime.now(zone).truncatedTo(ChronoUnit.SECONDS).toOffsetDateTime().toString()
                }) {
                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(if (mode == 0) "Now" else "A = now", maxLines = 1)
                }
            }
            Spacer(Modifier.height(16.dp))

            if (mode == 1) {
                CalculateResult(dateA, dateB, subtract, durationText, zone, now)
            } else {
                if (lines.isEmpty()) OutputPanel("", label = "Result", isCopyable = false)
                parsed.forEachIndexed { i, (line, p) ->
                    if (i > 0) Spacer(Modifier.height(8.dp))
                    when {
                        p == null -> ResultSlab(line, emptyList(), epoch?.let {
                            "Can't read this as ${it.label}. Enter a number" + (if (it == Epoch.DOS) " or a 0x hex value." else ".")
                        } ?: "Can't read this. Enter a Unix timestamp like 1792186953, or a date like 2026-10-16T21:42:33Z.")
                        lines.size == 1 -> ResultSlab("Read as ${p.kind}", timestampRows(p.instant, zone, now, allEpochs = true))
                        else -> ResultSlab("$line · ${p.kind}", timestampRows(p.instant, zone, now))
                    }
                }
                if (lines.size > MAX_BLOCKS) {
                    Text(
                        "Showing the first $MAX_BLOCKS of ${lines.size} values.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        if (picking) ZonePickerDialog(zone, onPick = { zoneId = it.id; picking = false }, onDismiss = { picking = false })
    }
}

@Composable
private fun CalculateResult(dateA: String, dateB: String, subtract: Boolean, durationText: String, zone: ZoneId, now: Instant) {
    val a = remember(dateA, zone) { parseTimestamp(dateA, zone) }
    val b = remember(dateB, zone) { parseTimestamp(dateB, zone) }
    val d = remember(durationText) { parseIsoDuration(durationText) }
    val dateHint = "Enter a Unix timestamp or a date like 2026-10-16T21:42:33Z."
    when {
        dateA.isBlank() -> OutputPanel("", label = "Result", isCopyable = false)
        a == null -> ResultSlab("Date A", emptyList(), "Can't read date A. $dateHint")
        subtract && dateB.isBlank() -> OutputPanel("", label = "A - B", isCopyable = false)
        subtract && b == null -> ResultSlab("Date B", emptyList(), "Can't read date B. $dateHint")
        subtract -> ResultSlab("A - B", dateDiffRows(a.instant, b!!.instant, zone))
        d == null -> ResultSlab("Duration", emptyList(), "Can't read this duration. Use ISO 8601, like PT90M, P1DT2H or -P1W.")
        else -> {
            val result = runCatching { addIsoDuration(a.instant, zone, d) }.getOrNull()
            if (result == null) ResultSlab("Result", emptyList(), "The result is out of range. Use a shorter duration.")
            else ResultSlab("A + ${durationText.trim()}", timestampRows(result, zone, now))
        }
    }
}

/** Recessed slab of tap-to-copy rows, label above value. */
@Composable
private fun ResultSlab(header: String, rows: List<Pair<String, String>>, error: String? = null) {
    val clipboard = LocalClipboardManager.current
    Surface(Modifier.fillMaxWidth(), color = LocalSlabColor.current, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Text(
                header,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            if (error != null) {
                Text(
                    error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            rows.forEach { (label, value) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClickLabel = "Copy $label") { clipboard.setText(AnnotatedString(value)) }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(value, style = CodeTextStyle, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
internal fun ZonePickerDialog(current: ZoneId, onPick: (ZoneId) -> Unit, onDismiss: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val now = remember { Instant.now() }
    val matches = remember(query) { ZONES.filter { it.id.replace('_', ' ').contains(query.trim(), ignoreCase = true) } }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
            Column(Modifier.padding(top = 24.dp, bottom = 8.dp)) {
                Text("Time zone", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 24.dp))
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    query, { query = it },
                    placeholder = { Text("Search city or region") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(8.dp))
                if (matches.isEmpty()) {
                    Text(
                        "No time zone matches \"${query.trim()}\". Try a city name like Tokyo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                    )
                }
                LazyColumn(Modifier.heightIn(max = 400.dp)) {
                    items(matches, key = { it.id }) { z ->
                        ListItem(
                            headlineContent = { Text(cityName(z)) },
                            supportingContent = { Text("${z.id} · ${offsetText(z, now)}") },
                            trailingContent = if (z == current) { { Icon(Icons.Default.Check, contentDescription = "Selected") } } else null,
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable { onPick(z) }
                        )
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End).padding(end = 16.dp)) { Text("Cancel") }
            }
        }
    }
}
