package com.cir0cuit.devtoys.tools.data

import com.google.gson.FormattingStyle
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSyntaxException
import com.google.gson.Strictness
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import org.yaml.snakeyaml.error.MarkedYAMLException
import java.io.StringReader
import java.math.BigDecimal
import java.time.format.DateTimeFormatter
import java.util.Date

internal enum class JsonIndent(val label: String) { TWO("2"), FOUR("4"), TAB("Tab"), MINIFY("Minify") }

/** Parses one JSON document and rejects anything after it. Lenient accepts comments, single quotes and unquoted keys. */
internal fun parseJson(input: String, lenient: Boolean = false): JsonElement {
    val reader = JsonReader(StringReader(input)).apply { setStrictness(if (lenient) Strictness.LENIENT else Strictness.STRICT) }
    val element = JsonParser.parseReader(reader)
    // JsonReader.toString() is "JsonReader at line X column Y path $"
    if (reader.peek() != JsonToken.END_DOCUMENT) throw JsonSyntaxException("Unexpected content after the JSON value ${reader.toString().removePrefix("JsonReader ")}")
    return element
}

/** Turns a Gson parse failure into "Line X, column Y: reason." without the troubleshooting URL. */
internal fun describeJsonError(e: Throwable): String {
    var c = e
    while (c.cause != null && c.cause !== c) c = c.cause!!
    val msg = (c.message ?: "Invalid JSON").substringBefore("\nSee ").trim()
    val m = Regex("""^(.*?)\s*at line (\d+) column (\d+)""", RegexOption.DOT_MATCHES_ALL).find(msg)
        ?: return "$msg."
    val raw = m.groupValues[1].trim()
    val reason = when {
        raw.contains("lenient", ignoreCase = true) -> "malformed JSON (turn on Lenient to accept comments, single quotes and unquoted keys)"
        raw.isEmpty() -> "invalid JSON"
        else -> raw
    }
    return "Line ${m.groupValues[2]}, column ${m.groupValues[3]}: $reason."
}

internal fun describeYamlError(e: Throwable): String {
    if (e is MarkedYAMLException) {
        val mark = e.problemMark ?: e.contextMark
        val problem = e.problem ?: e.context ?: "invalid YAML"
        if (mark != null) return "Line ${mark.line + 1}, column ${mark.column + 1}: $problem."
    }
    return e.message?.lineSequence()?.firstOrNull() ?: "Invalid YAML"
}

/** Recursively rebuilds objects with their members in alphabetical key order. */
internal fun sortKeys(e: JsonElement): JsonElement = when {
    e.isJsonObject -> JsonObject().also { o -> e.asJsonObject.entrySet().sortedBy { it.key }.forEach { (k, v) -> o.add(k, sortKeys(v)) } }
    e.isJsonArray -> JsonArray().also { a -> e.asJsonArray.forEach { a.add(sortKeys(it)) } }
    else -> e
}

internal fun formatJson(e: JsonElement, indent: JsonIndent = JsonIndent.TWO, sort: Boolean = false): String {
    val style = when (indent) {
        JsonIndent.TWO -> FormattingStyle.PRETTY
        JsonIndent.FOUR -> FormattingStyle.PRETTY.withIndent("    ")
        JsonIndent.TAB -> FormattingStyle.PRETTY.withIndent("\t")
        JsonIndent.MINIFY -> FormattingStyle.COMPACT
    }
    return GsonBuilder().setFormattingStyle(style).disableHtmlEscaping().serializeNulls().create().toJson(if (sort) sortKeys(e) else e)
}

/** Text as a JSON string literal, ready to embed in another document. */
internal fun escapeJsonString(text: String): String = formatJson(JsonPrimitive(text), JsonIndent.MINIFY)

/** Decodes a JSON string literal; the surrounding quotes are optional. */
internal fun unescapeJsonString(input: String): String {
    val t = input.trim()
    return parseJson(if (t.startsWith("\"")) t else "\"$t\"").asString
}

private val identifier = Regex("[A-Za-z_$][A-Za-z0-9_$]*")

internal fun childPath(parent: String, key: String): String =
    if (identifier.matches(key)) "$parent.$key" else "$parent['${key.replace("\\", "\\\\").replace("'", "\\'")}']"

internal fun childPath(parent: String, index: Int): String = "$parent[$index]"

// ---- Tree view ----

internal class JsonRow(val path: String, val depth: Int, val label: String, val value: JsonElement, val expanded: Boolean)

/** Visible rows of the tree, depth first, skipping children of collapsed containers; stops after [limit] rows. */
internal fun jsonRows(root: JsonElement, isExpanded: (path: String, depth: Int) -> Boolean, limit: Int): List<JsonRow> {
    val rows = ArrayList<JsonRow>()
    fun walk(e: JsonElement, path: String, depth: Int, label: String) {
        if (rows.size >= limit) return
        val container = e.isJsonObject || e.isJsonArray
        val open = container && isExpanded(path, depth)
        rows += JsonRow(path, depth, label, e, open)
        if (!open) return
        if (e.isJsonObject) e.asJsonObject.entrySet().forEach { (k, v) -> walk(v, childPath(path, k), depth + 1, k) }
        else e.asJsonArray.forEachIndexed { i, v -> walk(v, childPath(path, i), depth + 1, "[$i]") }
    }
    walk(root, "$", 0, "$")
    return rows
}

internal fun summarize(e: JsonElement, max: Int = 200): String = when {
    e.isJsonObject -> "{${e.asJsonObject.size()}}"
    e.isJsonArray -> "[${e.asJsonArray.size()}]"
    else -> formatJson(e, JsonIndent.MINIFY).let { if (it.length > max) it.take(max) + "…" else it }
}

// ---- YAML ----

/** Converts SnakeYAML's loaded objects to a Gson tree (keeps BigInteger and decimals exact, no "\/" escaping). */
internal fun yamlToJsonElement(v: Any?): JsonElement = when (v) {
    null -> JsonNull.INSTANCE
    is Map<*, *> -> JsonObject().also { o -> v.forEach { (k, x) -> o.add(k?.toString() ?: "null", yamlToJsonElement(x)) } }
    is Collection<*> -> JsonArray().also { a -> v.forEach { a.add(yamlToJsonElement(it)) } }
    is Array<*> -> yamlToJsonElement(v.asList())
    is ByteArray -> JsonPrimitive(java.util.Base64.getEncoder().encodeToString(v))
    is Date -> JsonPrimitive(DateTimeFormatter.ISO_INSTANT.format(v.toInstant()))
    is Double -> if (v.isNaN() || v.isInfinite()) JsonPrimitive(v.toString()) else JsonPrimitive(v)
    is Float -> if (v.isNaN() || v.isInfinite()) JsonPrimitive(v.toString()) else JsonPrimitive(v)
    is Number -> JsonPrimitive(v)
    is Boolean -> JsonPrimitive(v)
    is String -> JsonPrimitive(v)
    else -> JsonPrimitive(v.toString())
}

// ---- Structural diff ----

internal class JsonChange(val kind: Char, val path: String, val old: JsonElement?, val new: JsonElement?)

/** Key-order-insensitive canonical form; numbers compare by value (1 == 1.0). */
internal fun canonical(e: JsonElement, sortArrays: Boolean): String = when {
    e.isJsonObject -> e.asJsonObject.entrySet().sortedBy { it.key }
        .joinToString(",", "{", "}") { (k, v) -> escapeJsonString(k) + ":" + canonical(v, sortArrays) }
    e.isJsonArray -> e.asJsonArray.map { canonical(it, sortArrays) }.let { if (sortArrays) it.sorted() else it }.joinToString(",", "[", "]")
    e.isJsonPrimitive && e.asJsonPrimitive.isNumber -> runCatching { BigDecimal(e.asString).stripTrailingZeros().toString() }.getOrDefault(e.asString)
    else -> formatJson(e, JsonIndent.MINIFY)
}

internal fun diffJson(a: JsonElement, b: JsonElement, ignoreArrayOrder: Boolean): List<JsonChange> {
    val out = ArrayList<JsonChange>()
    fun walk(a: JsonElement, b: JsonElement, path: String) {
        when {
            a.isJsonObject && b.isJsonObject -> {
                val ao = a.asJsonObject
                val bo = b.asJsonObject
                for ((k, v) in ao.entrySet()) {
                    val p = childPath(path, k)
                    if (bo.has(k)) walk(v, bo[k], p) else out += JsonChange('-', p, v, null)
                }
                for ((k, v) in bo.entrySet()) if (!ao.has(k)) out += JsonChange('+', childPath(path, k), null, v)
            }
            a.isJsonArray && b.isJsonArray && ignoreArrayOrder -> {
                // ponytail: unmatched items are reported as removed + added, not paired into "changed"
                val unmatched = HashMap<String, ArrayDeque<Int>>()
                b.asJsonArray.forEachIndexed { j, v -> unmatched.getOrPut(canonical(v, true)) { ArrayDeque() }.addLast(j) }
                a.asJsonArray.forEachIndexed { i, v ->
                    if (unmatched[canonical(v, true)]?.removeFirstOrNull() == null) out += JsonChange('-', childPath(path, i), v, null)
                }
                unmatched.values.flatten().sorted().forEach { j -> out += JsonChange('+', childPath(path, j), null, b.asJsonArray[j]) }
            }
            a.isJsonArray && b.isJsonArray -> {
                val aa = a.asJsonArray
                val ba = b.asJsonArray
                for (i in 0 until minOf(aa.size(), ba.size())) walk(aa[i], ba[i], childPath(path, i))
                for (i in ba.size() until aa.size()) out += JsonChange('-', childPath(path, i), aa[i], null)
                for (i in aa.size() until ba.size()) out += JsonChange('+', childPath(path, i), null, ba[i])
            }
            canonical(a, ignoreArrayOrder) != canonical(b, ignoreArrayOrder) -> out += JsonChange('~', path, a, b)
        }
    }
    walk(a, b, "$")
    return out
}

internal fun describeChanges(changes: List<JsonChange>): String = changes.joinToString("\n") { c ->
    when (c.kind) {
        '+' -> "+ ${c.path}: ${summarize(c.new!!, 80)}"
        '-' -> "- ${c.path}: ${summarize(c.old!!, 80)}"
        else -> "~ ${c.path}: ${summarize(c.old!!, 80)} -> ${summarize(c.new!!, 80)}"
    }
}

internal fun changeSummary(changes: List<JsonChange>): String {
    if (changes.isEmpty()) return "No differences"
    val counts = changes.groupingBy { it.kind }.eachCount()
    return listOfNotNull(
        counts['+']?.let { "$it added" },
        counts['-']?.let { "$it removed" },
        counts['~']?.let { "$it changed" },
    ).joinToString(", ")
}
