package com.cir0cuit.devtoys.tools.data

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.designsystem.CodeTextStyle
import com.cir0cuit.devtoys.core.designsystem.LocalSlabColor
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import java.math.BigDecimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Delimiters the viewer can read; index 0 of the picker is "Auto". */
internal val CSV_DELIMITERS = listOf(',', ';', '\t', '|')
private val DELIMITER_LABELS = listOf("Auto", "Comma", "Semicolon", "Tab", "Pipe")

private class CsvExport(val label: String, val mimeType: String, val fileName: String)
private val CSV_EXPORTS = listOf(
    CsvExport("CSV", "text/csv", "table.csv"),
    CsvExport("Markdown", "text/markdown", "table.md"),
    CsvExport("HTML", "text/html", "table.html"),
    CsvExport("JSON", "application/json", "table.json"),
    CsvExport("SQL INSERT", "application/sql", "table.sql"),
)

/** Rows shown in the on-screen table; exports always use every row. */
private const val DISPLAY_ROWS = 1000
/** Larger input is shown as a summary instead of an editable field. */
private const val EDITABLE_CHARS = 100_000
private const val CSV_MAX_SAVED_CHARS = 20_000
private const val RETAINED_MARKER = "\u0000retained"

// ponytail: one in-process slot, so large input survives rotation but not process death.
private object RetainedCsvInput { @Volatile var text = "" }

private val csvInputSaver = Saver<MutableState<String>, String>(
    save = {
        if (it.value.length <= CSV_MAX_SAVED_CHARS) it.value
        else { RetainedCsvInput.text = it.value; RETAINED_MARKER }
    },
    restore = { mutableStateOf(if (it == RETAINED_MARKER) RetainedCsvInput.text else it) }
)

class CsvViewerTool : Tool {
    override val descriptor = ToolDescriptor("csv-viewer", "CSV Viewer & Converter", "View CSV or JSON as a table and convert it", Icons.Default.GridOn, ToolCategory.CONVERTERS, listOf("csv", "tsv", "table", "viewer", "html", "markdown", "json", "sql"))
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable override fun Content(modifier: Modifier) {
        var input by rememberSaveable(saver = csvInputSaver) { mutableStateOf("id,name,age\n1,Alice,30\n2,Bob,25") }
        var fileName by rememberSaveable { mutableStateOf<String?>(null) }
        var delimiterChoice by rememberSaveable { mutableStateOf(0) }
        var hasHeader by rememberSaveable { mutableStateOf(true) }
        var exportIndex by rememberSaveable { mutableStateOf(1) }
        var inferTypes by rememberSaveable { mutableStateOf(true) }
        var tableName by rememberSaveable { mutableStateOf("my_table") }
        var emptyAsNull by rememberSaveable { mutableStateOf(false) }
        var table by remember { mutableStateOf<CsvTable?>(null) }
        LaunchedEffect(input, delimiterChoice, hasHeader) {
            delay(200)
            table = withContext(Dispatchers.Default) { readCsvTable(input, CSV_DELIMITERS.getOrNull(delimiterChoice - 1), hasHeader) }
        }

        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val clipboard = LocalClipboardManager.current
        // Rebuilt from the current input when needed, so it also works after the activity was recreated.
        fun exportText(format: Int): String {
            val rows = readCsvTable(input, CSV_DELIMITERS.getOrNull(delimiterChoice - 1), hasHeader).rows
            if (rows.isEmpty()) return ""
            return when (format) {
                0 -> toCsv(rows)
                1 -> toMarkdown(rows)
                2 -> toHtmlDocument(toHtmlTable(rows))
                3 -> csvToJson(rows, inferTypes)
                else -> toSqlInsert(rows, tableName, emptyAsNull)
            }
        }
        val saveLaunchers = CSV_EXPORTS.mapIndexed { index, export ->
            rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(export.mimeType)) { uri ->
                if (uri == null) return@rememberLauncherForActivityResult
                scope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        runCatching {
                            val bytes = exportText(index).toByteArray(Charsets.UTF_8)
                            context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } != null
                        }.getOrDefault(false)
                    }
                    Toast.makeText(context, if (ok) "Saved" else "Couldn't save the file. Try another location.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        val openFile = rememberTextFileOpener(arrayOf("text/*", "application/vnd.ms-excel", "application/json", "application/octet-stream")) { name, text ->
            fileName = name
            input = text
        }

        ToolScaffold(descriptor.title, modifier = modifier, scrollable = false) {
            if (input.length > EDITABLE_CHARS) {
                Surface(color = LocalSlabColor.current, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${fileName ?: "Pasted text"}, ${input.length / 1024} KB. Too large to edit here.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { input = ""; fileName = null }) { Text("Clear") }
                    }
                }
            } else {
                MonoTextField(input, { input = it }, "CSV, TSV or JSON input", minLines = 4, maxLines = 6)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = openFile) {
                    Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Open file")
                }
                TextButton(onClick = { clipboard.getText()?.text?.let { input = it; fileName = null } }) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Paste")
                }
            }

            val current = table
            val fromJson = current?.fromJson == true
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                CsvSelect(
                    label = "Delimiter",
                    options = DELIMITER_LABELS.indices.toList(),
                    selected = delimiterChoice,
                    optionLabel = { i ->
                        val detected = current?.delimiter
                        if (i == 0 && detected != null && !fromJson) "Auto: ${DELIMITER_LABELS[CSV_DELIMITERS.indexOf(detected) + 1].lowercase()}"
                        else DELIMITER_LABELS[i]
                    },
                    onSelect = { delimiterChoice = it },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                Text("Header", style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                Spacer(Modifier.width(8.dp))
                Switch(checked = hasHeader || fromJson, onCheckedChange = { hasHeader = it }, enabled = !fromJson)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                CsvSelect(
                    label = "Export as",
                    options = CSV_EXPORTS.indices.toList(),
                    selected = exportIndex,
                    optionLabel = { CSV_EXPORTS[it].label },
                    onSelect = { exportIndex = it },
                    modifier = Modifier.weight(1f)
                )
                val export = CSV_EXPORTS[exportIndex]
                val enabled = !current?.rows.isNullOrEmpty()
                IconButton(enabled = enabled, onClick = {
                    scope.launch {
                        val text = withContext(Dispatchers.Default) { exportText(exportIndex) }
                        clipboard.setText(AnnotatedString(text))
                    }
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy ${export.label}")
                }
                IconButton(enabled = enabled, onClick = { saveLaunchers[exportIndex].launch(export.fileName) }) {
                    Icon(Icons.Default.Save, contentDescription = "Save ${export.label} file")
                }
            }
            when (exportIndex) {
                3 -> Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Detect numbers, booleans and null", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Switch(checked = inferTypes, onCheckedChange = { inferTypes = it })
                }
                4 -> Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = tableName,
                        onValueChange = { tableName = it },
                        label = { Text("Table name") },
                        singleLine = true,
                        textStyle = CodeTextStyle,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Empty is NULL", style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                    Spacer(Modifier.width(8.dp))
                    Switch(checked = emptyAsNull, onCheckedChange = { emptyAsNull = it })
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (current == null) return@ToolScaffold
            if (current.error != null) {
                Text(current.error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                return@ToolScaffold
            }
            if (current.rows.isEmpty()) {
                Text("Enter CSV or a JSON array of objects to see it as a table.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@ToolScaffold
            }
            val rows = current.rows
            val shown = remember(rows) { rows.take(DISPLAY_ROWS + 1) }
            val columns = remember(shown) { shown.maxOf { it.size } }
            val dataRows = rows.size - 1
            Text(
                "%,d data rows, %d columns".format(dataRows, columns) +
                    (if (dataRows > DISPLAY_ROWS) ", showing the first %,d".format(DISPLAY_ROWS) else "") +
                    (if (fromJson) ", read as JSON" else ""),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Fixed per-column widths from the longest cell, so every row lines up under one shared horizontal scroll.
            val widths = remember(shown) {
                (0 until columns).map { c -> (shown.maxOf { it.getOrNull(c)?.length ?: 0 }.coerceIn(3, 24) * 9 + 24).dp }
            }
            val hScroll = rememberScrollState()
            Surface(color = LocalSlabColor.current, shape = MaterialTheme.shapes.medium, modifier = Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                    itemsIndexed(shown) { index, row ->
                        Row(Modifier.horizontalScroll(hScroll).padding(horizontal = 4.dp)) {
                            widths.forEachIndexed { c, w ->
                                Text(
                                    row.getOrNull(c) ?: "",
                                    style = if (index == 0) CodeTextStyle.copy(fontWeight = FontWeight.SemiBold) else CodeTextStyle,
                                    color = if (index == 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.width(w).padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                        if (index == 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CsvSelect(label: String, options: List<Int>, selected: Int, optionLabel: (Int) -> String, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = modifier) {
        OutlinedTextField(
            value = optionLabel(selected),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(optionLabel(option)) }, onClick = { onSelect(option); expanded = false })
            }
        }
    }
}

/** Parsed input: [rows] with the header first, the [delimiter] used, or an [error] to show. */
internal class CsvTable(val rows: List<List<String>>, val delimiter: Char?, val fromJson: Boolean, val error: String? = null)

/**
 * Reads [input] as JSON when it starts with '[' or '{', otherwise as delimited text using [delimiter]
 * (auto-detected when null). Without a header row, "Column 1".."Column N" is added as the header.
 */
internal fun readCsvTable(input: String, delimiter: Char?, hasHeader: Boolean): CsvTable {
    val start = input.trimStart()
    if (start.startsWith("[") || start.startsWith("{")) {
        return try {
            CsvTable(jsonToCsvRows(input), null, true)
        } catch (e: Exception) {
            val reason = (e.cause?.message ?: e.message ?: "invalid JSON").substringBefore("\nSee ").trimEnd('.')
            CsvTable(emptyList(), null, true, "Couldn't read the input as JSON: $reason. Use an array of objects, or CSV text.")
        }
    }
    val d = delimiter ?: detectCsvDelimiter(input)
    val rows = parseCsv(input, d)
    if (hasHeader || rows.isEmpty()) return CsvTable(rows, d, false)
    val columns = rows.maxOf { it.size }
    return CsvTable(listOf((1..columns).map { "Column $it" }) + rows, d, false)
}

/** Picks the delimiter that occurs most often outside quotes in the first record; comma on a tie or none. */
internal fun detectCsvDelimiter(text: String): Char {
    val counts = IntArray(CSV_DELIMITERS.size)
    var inQuotes = false
    for (c in text) {
        if (c == '"') inQuotes = !inQuotes
        else if (!inQuotes) {
            if (c == '\n') break
            val k = CSV_DELIMITERS.indexOf(c)
            if (k >= 0) counts[k]++
        }
    }
    val best = counts.indices.maxBy { counts[it] }
    return if (counts[best] == 0) ',' else CSV_DELIMITERS[best]
}

internal fun parseCsv(text: String, delimiter: Char = ','): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    var row = mutableListOf<String>()
    val field = StringBuilder()
    var inQuotes = false
    var fieldStarted = false
    var i = 0
    while (i < text.length) {
        val c = text[i]
        if (inQuotes) {
            if (c == '"') {
                if (i + 1 < text.length && text[i + 1] == '"') {
                    field.append('"')
                    i++
                } else {
                    inQuotes = false
                }
            } else {
                field.append(c)
            }
        } else {
            when (c) {
                '"' -> if (fieldStarted) field.append(c) else { inQuotes = true; fieldStarted = true }
                delimiter -> { row.add(field.toString()); field.clear(); fieldStarted = false }
                '\r' -> {}
                '\n' -> { row.add(field.toString()); field.clear(); fieldStarted = false; rows.add(row); row = mutableListOf() }
                else -> { field.append(c); fieldStarted = true }
            }
        }
        i++
    }
    if (field.isNotEmpty() || row.isNotEmpty()) {
        row.add(field.toString())
        rows.add(row)
    }
    return rows.filterNot { it.size == 1 && it[0].isEmpty() }
}

/** RFC 4180 output: fields with the delimiter, a quote or a line break are quoted, quotes doubled. */
internal fun toCsv(rows: List<List<String>>, delimiter: Char = ','): String = rows.joinToString("\n") { row ->
    row.joinToString(delimiter.toString()) { f ->
        if (f.any { it == delimiter || it == '"' || it == '\n' || it == '\r' }) "\"" + f.replace("\"", "\"\"") + "\"" else f
    }
}

private fun csvColumnName(header: List<String>, index: Int) = header.getOrNull(index)?.takeIf { it.isNotEmpty() } ?: "column${index + 1}"

private val CSV_JSON_NUMBER = Regex("-?(0|[1-9]\\d*)(\\.\\d+)?([eE][+-]?\\d+)?")

/** Array of objects keyed by the header row; [inferTypes] turns numbers, true/false and empty or "null" cells into JSON types. */
internal fun csvToJson(rows: List<List<String>>, inferTypes: Boolean): String {
    val header = rows.first()
    val columns = rows.maxOf { it.size }
    val array = JsonArray()
    for (row in rows.drop(1)) {
        val obj = JsonObject()
        for (c in 0 until columns) {
            val v = row.getOrElse(c) { "" }
            obj.add(csvColumnName(header, c), if (inferTypes) csvInferJsonValue(v) else JsonPrimitive(v))
        }
        array.add(obj)
    }
    return GsonBuilder().setPrettyPrinting().disableHtmlEscaping().serializeNulls().create().toJson(array)
}

private fun csvInferJsonValue(v: String): JsonElement = when {
    v.isEmpty() || v == "null" -> JsonNull.INSTANCE
    v == "true" || v == "false" -> JsonPrimitive(v.toBoolean())
    CSV_JSON_NUMBER.matches(v) -> JsonPrimitive(BigDecimal(v))
    else -> JsonPrimitive(v)
}

/**
 * Rows from a JSON array of objects (or one object): nested keys become dot paths, the header is
 * the union of keys in first-seen order. An array of arrays is taken as rows as-is.
 */
internal fun jsonToCsvRows(json: String): List<List<String>> {
    val root = JsonParser.parseString(json)
    val items = if (root.isJsonArray) root.asJsonArray.toList() else listOf(root)
    if (items.isEmpty()) return emptyList()
    if (items.all { it.isJsonArray }) return items.map { row -> row.asJsonArray.map(::csvJsonCell) }
    val flat = items.map { LinkedHashMap<String, String>().also { map -> csvFlattenJson(it, "", map) } }
    val header = LinkedHashSet<String>().apply { flat.forEach { addAll(it.keys) } }.toList()
    return listOf(header) + flat.map { m -> header.map { m[it] ?: "" } }
}

private fun csvJsonCell(e: JsonElement): String = when {
    e.isJsonNull -> ""
    e.isJsonPrimitive -> e.asString
    else -> e.toString()
}

private fun csvFlattenJson(e: JsonElement, path: String, out: MutableMap<String, String>) {
    if (e.isJsonObject && e.asJsonObject.size() > 0) {
        for ((k, v) in e.asJsonObject.entrySet()) csvFlattenJson(v, if (path.isEmpty()) k else "$path.$k", out)
    } else {
        out[path.ifEmpty { "value" }] = csvJsonCell(e)
    }
}

private val CSV_SIMPLE_SQL_NAME = Regex("[A-Za-z_][A-Za-z0-9_]*")

private fun csvSqlName(name: String) = if (CSV_SIMPLE_SQL_NAME.matches(name)) name else "\"" + name.replace("\"", "\"\"") + "\""

/** One multi-row INSERT; every value is a quoted string literal, empty cells become NULL when [emptyAsNull]. */
internal fun toSqlInsert(rows: List<List<String>>, table: String, emptyAsNull: Boolean): String {
    if (rows.size < 2) return "-- No data rows to insert"
    val header = rows.first()
    val columns = rows.maxOf { it.size }
    val tableSql = table.trim().ifEmpty { "my_table" }.split('.').joinToString(".") { csvSqlName(it) }
    val columnSql = (0 until columns).joinToString(", ") { csvSqlName(csvColumnName(header, it)) }
    val values = rows.drop(1).joinToString(",\n") { row ->
        "  (" + (0 until columns).joinToString(", ") { c ->
            val v = row.getOrElse(c) { "" }
            if (emptyAsNull && v.isEmpty()) "NULL" else "'" + v.replace("'", "''") + "'"
        } + ")"
    }
    return "INSERT INTO $tableSql ($columnSql) VALUES\n$values;"
}

private val LINE_BREAK = Regex("\r\n|\r|\n")

internal fun mdEscape(s: String) = s.replace("\\", "\\\\").replace("|", "\\|").replace(LINE_BREAK, "<br>")

internal fun htmlEscape(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;")

internal fun toMarkdown(rows: List<List<String>>): String = buildString {
    rows.forEachIndexed { index, cols ->
        append("| " + cols.joinToString(" | ") { mdEscape(it) } + " |\n")
        if (index == 0) {
            append("|" + cols.joinToString("|") { "---" } + "|\n")
        }
    }
}

internal fun toHtmlTable(rows: List<List<String>>): String = buildString {
    append("<table>\n")
    rows.forEachIndexed { index, cols ->
        val tag = if (index == 0) "th" else "td"
        append("  <tr>")
        cols.forEach { append("<$tag>${htmlEscape(it)}</$tag>") }
        append("</tr>\n")
    }
    append("</table>")
}

private fun toHtmlDocument(table: String): String =
    "<!DOCTYPE html>\n<html><head><meta charset=\"utf-8\"><title>table</title></head><body>\n" + table + "\n</body></html>\n"
