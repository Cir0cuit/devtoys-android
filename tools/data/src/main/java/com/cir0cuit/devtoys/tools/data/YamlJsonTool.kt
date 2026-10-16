package com.cir0cuit.devtoys.tools.data
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SyncAlt

import androidx.compose.material3.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.DumperOptions
import org.json.JSONArray
import org.json.JSONObject
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.Strictness
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken

class YamlJsonTool : Tool {
    override val descriptor = ToolDescriptor("yaml-json", android.R.string.untitled, android.R.string.untitled, Icons.Default.SyncAlt, ToolCategory.CONVERTERS, listOf("yaml", "json", "converter"))
    @Composable override fun Content(modifier: Modifier) {
        var yamlInput by rememberSaveable { mutableStateOf("") }
        var jsonInput by rememberSaveable { mutableStateOf("") }
        var toYaml by rememberSaveable { mutableStateOf(false) }
        
        val jsonOutput = remember(yamlInput) {
            if (yamlInput.isEmpty()) "" else try {
                val obj = Yaml().load<Any?>(yamlInput)
                when (val j = yamlToJson(obj)) {
                    is JSONObject -> j.toString(2)
                    is JSONArray -> j.toString(2)
                    is String -> JSONObject.quote(j)
                    else -> j.toString()
                }
            } catch (e: Exception) { "Error: ${e.message}" }
        }
        val yamlOutput = remember(jsonInput) {
            if (jsonInput.isEmpty()) "" else try {
                val reader = JsonReader(java.io.StringReader(jsonInput)).apply { setStrictness(Strictness.STRICT) }
                val parsed = JsonParser.parseReader(reader)
                if (reader.peek() != JsonToken.END_DOCUMENT) throw IllegalArgumentException("Unexpected trailing content")
                val obj = jsonToPlain(parsed)
                val options = DumperOptions().apply {
                    defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
                    indent = 2
                    isPrettyFlow = true
                }
                Yaml(options).dump(obj)
            } catch (e: Exception) { "Error: ${e.message}" }
        }
        
        ToolScaffold("YAML ↔ JSON Converter", modifier = modifier) {
            TabRow(selectedTabIndex = if (toYaml) 1 else 0) {
                Tab(selected = !toYaml, onClick = { toYaml = false }, text = { Text("YAML to JSON") })
                Tab(selected = toYaml, onClick = { toYaml = true }, text = { Text("JSON to YAML") })
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (toYaml) {
                MonoTextField(jsonInput, { jsonInput = it }, "JSON Input")
                Spacer(modifier = Modifier.height(16.dp))
                OutputPanel(yamlOutput)
            } else {
                MonoTextField(yamlInput, { yamlInput = it }, "YAML Input")
                Spacer(modifier = Modifier.height(16.dp))
                OutputPanel(jsonOutput)
            }
        }
    }
}

private fun yamlToJson(v: Any?): Any = when (v) {
    null -> JSONObject.NULL
    is Map<*, *> -> JSONObject().also { o -> v.forEach { (k, x) -> o.put(k?.toString() ?: "null", yamlToJson(x)) } }
    is Collection<*> -> JSONArray().also { a -> v.forEach { a.put(yamlToJson(it)) } }
    is Array<*> -> JSONArray().also { a -> v.forEach { a.put(yamlToJson(it)) } }
    is ByteArray -> android.util.Base64.encodeToString(v, android.util.Base64.NO_WRAP)
    is java.util.Date -> java.time.format.DateTimeFormatter.ISO_INSTANT.format(v.toInstant())
    is Number -> if (v.toDouble().let { it.isNaN() || it.isInfinite() }) v.toString() else v
    is Boolean, is String -> v
    else -> v.toString()
}

internal fun jsonToPlain(v: JsonElement): Any? = when {
    v.isJsonNull -> null
    v.isJsonObject -> LinkedHashMap<String, Any?>().also { m -> v.asJsonObject.entrySet().forEach { (k, x) -> m[k] = jsonToPlain(x) } }
    v.isJsonArray -> v.asJsonArray.map { jsonToPlain(it) }
    else -> v.asJsonPrimitive.let { p ->
        when {
            p.isBoolean -> p.asBoolean
            p.isNumber -> p.asString.let { n ->
                if (n.any { it == '.' || it == 'e' || it == 'E' }) java.math.BigDecimal(n)
                else java.math.BigInteger(n).let { if (it.bitLength() < 64) it.toLong() else it }
            }
            else -> p.asString
        }
    }
}
