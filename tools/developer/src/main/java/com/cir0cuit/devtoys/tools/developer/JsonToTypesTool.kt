package com.cir0cuit.devtoys.tools.developer

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material3.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class JsonToTypesTool : Tool {
    override val descriptor = ToolDescriptor(
        id = "json-types",
        title = "JSON to Kotlin Types",
        description = "Generate Kotlin data classes from JSON",
        icon = Icons.Default.AccountTree,
        category = ToolCategory.CONVERTERS,
        keywords = listOf("json", "types", "kotlin", "typescript")
    )

    @Composable override fun Content(modifier: Modifier) {
        var input by rememberSaveable { mutableStateOf("{\n  \"id\": 1,\n  \"name\": \"Test\",\n  \"isActive\": true,\n  \"data\": {\"value\": 5}\n}") }
        var output by remember { mutableStateOf("") }
        LaunchedEffect(input) {
            if (output.isNotEmpty()) delay(250)
            output = withContext(Dispatchers.Default) { convert(input) }
        }

        ToolScaffold(descriptor.title, modifier = modifier) {
            MonoTextField(input, { input = it }, "JSON", minLines = 6)
            Spacer(modifier = Modifier.height(16.dp))
            OutputPanel(output, label = "Kotlin")
        }
    }

    private fun convert(input: String): String =
        if (input.isEmpty()) "" else try {
            val trimmed = input.trim()
            if (maxDepth(trimmed) > MAX_DEPTH) {
                "JSON is nested too deeply. Keep it under $MAX_DEPTH levels."
            } else if (trimmed.startsWith("[")) {
                val arr = JSONArray(trimmed)
                val first = if (arr.length() > 0) arr.optJSONObject(0) else null
                if (first != null) {
                    generateKotlinClass("RootItem", first, mutableSetOf("RootItem"))
                } else if (arr.length() == 0 || arr.isNull(0)) {
                    "// Empty array or null first element"
                } else {
                    "typealias Root = List<${elementType(arr)}>"
                }
            } else {
                val json = JSONObject(trimmed)
                generateKotlinClass("Root", json, mutableSetOf("Root"))
            }
        } catch (e: StackOverflowError) {
            "JSON is nested too deeply to convert."
        } catch (e: Exception) { "Not valid JSON: ${e.message ?: "check brackets, quotes and commas"}" }

    internal fun generateKotlinClass(className: String, json: JSONObject, usedClassNames: MutableSet<String>): String {
        val sb = StringBuilder()
        val nestedClasses = mutableListOf<String>()
        val usedPropNames = mutableSetOf<String>()
        // A data class needs at least one constructor parameter; an empty object has no shape to model.
        if (json.length() == 0) return "typealias $className = Map<String, Any?>"

        sb.append("data class $className(\n")
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = json.get(key)
            val typeName = toClassName(key)
            // Suffix names that would shadow kotlin builtins (a class String breaks every String property).
            fun className(base: String) = uniqueName(if (base in KOTLIN_BUILTIN_TYPES) base + "Type" else base, usedClassNames)
            
            val type = when {
                value is Int -> "Int"
                value is Long -> "Long"
                value is Boolean -> "Boolean"
                value is Double -> "Double"
                value is JSONObject -> {
                    val name = className(typeName)
                    nestedClasses.add(generateKotlinClass(name, value, usedClassNames))
                    name
                }
                value is JSONArray -> {
                    if (value.length() > 0 && value.optJSONObject(0) != null) {
                        val itemType = className(typeName + "Item")
                        nestedClasses.add(generateKotlinClass(itemType, value.getJSONObject(0), usedClassNames))
                        "List<$itemType>"
                    } else {
                        "List<${elementType(value)}>"
                    }
                }
                value == JSONObject.NULL -> "Any? /* null */"
                else -> "String"
            }
            val propName = uniqueName(toPropertyName(key), usedPropNames)
            if (propName != key) {
                sb.append("    @SerialName(\"${kotlinStringEscape(key)}\")\n")
            }
            val declared = if (propName in KOTLIN_HARD_KEYWORDS) "`$propName`" else propName
            sb.append("    val $declared: $type,\n")
        }
        sb.append(")\n\n")
        
        nestedClasses.forEach { sb.append(it) }
        return sb.toString().trim()
    }

    private fun kotlinStringEscape(s: String) = buildString {
        for (c in s) when (c) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '$' -> append("\\\$")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (c < ' ') append("\\u%04x".format(c.code)) else append(c)
        }
    }

    internal fun elementType(arr: JSONArray): String {
        if (arr.length() == 0 || arr.isNull(0)) return "Any"
        return when (arr.get(0)) {
            is Int -> "Int"
            is Long -> "Long"
            is Double -> "Double"
            is Boolean -> "Boolean"
            is String -> "String"
            is JSONArray -> "List<${elementType(arr.getJSONArray(0))}>"
            else -> "Any"
        }
    }

    private fun splitWords(key: String): List<String> =
        key.split(Regex("[^A-Za-z0-9]+")).filter { it.isNotEmpty() }

    internal fun toPropertyName(key: String): String {
        val words = splitWords(key)
        var name = if (words.isEmpty()) "field" else
            words.first().replaceFirstChar { it.lowercase() } +
                words.drop(1).joinToString("") { w -> w.replaceFirstChar { it.uppercase() } }
        if (name.first().isDigit()) name = "_$name"
        return name
    }

    internal fun toClassName(key: String): String {
        val words = splitWords(key)
        var name = if (words.isEmpty()) "Field" else
            words.joinToString("") { w -> w.replaceFirstChar { it.uppercase() } }
        if (name.first().isDigit()) name = "_$name"
        return name
    }

    internal fun uniqueName(base: String, used: MutableSet<String>): String {
        var name = base
        var i = 2
        while (!used.add(name)) name = base + i++
        return name
    }

    internal fun maxDepth(s: String): Int {
        var depth = 0
        var max = 0
        var inString = false
        var escaped = false
        for (c in s) {
            if (inString) {
                if (escaped) escaped = false
                else if (c == '\\') escaped = true
                else if (c == '"') inString = false
                continue
            }
            when (c) {
                '"' -> inString = true
                '{', '[' -> { depth++; if (depth > max) max = depth }
                '}', ']' -> depth--
            }
        }
        return max
    }

    private companion object {
        const val MAX_DEPTH = 512
        val KOTLIN_HARD_KEYWORDS = setOf(
            "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in",
            "interface", "is", "null", "object", "package", "return", "super", "this", "throw",
            "true", "try", "typealias", "typeof", "val", "var", "when", "while"
        )
        val KOTLIN_BUILTIN_TYPES = setOf(
            "Any", "Unit", "Nothing", "String", "Char", "Boolean", "Byte", "Short", "Int", "Long",
            "Float", "Double", "Number", "Array", "List", "Map", "Set", "Pair", "Triple", "Result",
            "SerialName", "Serializable"
        )
    }
}
