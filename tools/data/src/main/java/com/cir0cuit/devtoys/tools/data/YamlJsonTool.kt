package com.cir0cuit.devtoys.tools.data
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
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
import android.util.Base64
import java.math.BigDecimal
import java.math.BigInteger
import java.time.format.DateTimeFormatter
import java.util.Date

class YamlJsonTool : Tool {
    override val descriptor = ToolDescriptor("yaml-json", "YAML ↔ JSON", "Convert between YAML and JSON", Icons.Default.SyncAlt, ToolCategory.CONVERTERS, listOf("yaml", "json", "converter"))
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable override fun Content(modifier: Modifier) {
        var yamlInput by rememberSaveable { mutableStateOf("name: devtoys\ntags:\n  - yaml\n  - json\nstars: 42\n") }
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
                val obj = jsonToPlain(parseStrictJson(jsonInput))
                val options = DumperOptions().apply {
                    defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
                    indent = 2
                    isPrettyFlow = true
                }
                Yaml(options).dump(obj)
            } catch (e: Exception) { "Error: ${e.message}" }
        }
        ToolScaffold(descriptor.title, modifier = modifier) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf("YAML to JSON", "JSON to YAML").forEachIndexed { i, label ->
                    SegmentedButton(
                        selected = toYaml == (i == 1),
                        onClick = { toYaml = i == 1 },
                        shape = SegmentedButtonDefaults.itemShape(i, 2)
                    ) { Text(label, maxLines = 1) }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            val output = if (toYaml) yamlOutput else jsonOutput
            if (toYaml) PasteableInput(jsonInput, { jsonInput = it }, "JSON input")
            else PasteableInput(yamlInput, { yamlInput = it }, "YAML input")
            Spacer(modifier = Modifier.height(8.dp))
            ResultPanel(output, label = if (toYaml) "YAML" else "JSON")
            if (output.isNotEmpty() && !output.startsWith("Error: ")) {
                TextButton(onClick = {
                    if (toYaml) yamlInput = output else jsonInput = output
                    toYaml = !toYaml
                }) {
                    Icon(Icons.Default.SwapVert, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Use result as input")
                }
            }
        }
    }
}

private fun yamlToJson(v: Any?): Any = when (v) {
    null -> JSONObject.NULL
    is Map<*, *> -> JSONObject().also { o -> v.forEach { (k, x) -> o.put(k?.toString() ?: "null", yamlToJson(x)) } }
    is Collection<*> -> JSONArray().also { a -> v.forEach { a.put(yamlToJson(it)) } }
    is Array<*> -> yamlToJson(v.asList())
    is ByteArray -> Base64.encodeToString(v, Base64.NO_WRAP)
    is Date -> DateTimeFormatter.ISO_INSTANT.format(v.toInstant())
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
                if (n.any { it == '.' || it == 'e' || it == 'E' }) BigDecimal(n)
                else BigInteger(n).let { if (it.bitLength() < 64) it.toLong() else it }
            }
            else -> p.asString
        }
    }
}
