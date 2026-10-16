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
import com.google.gson.JsonElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.BigInteger

private val YAML_TYPES = arrayOf("application/yaml", "application/x-yaml", "text/*", "application/octet-stream")

/** All "---" documents; several become a JSON array. */
internal fun yamlToJson(input: String, indent: JsonIndent, sort: Boolean): String {
    val docs = Yaml().loadAll(input).toList()
    val tree = when (docs.size) {
        0 -> return ""
        1 -> yamlToJsonElement(docs[0])
        else -> yamlToJsonElement(docs)
    }
    return formatJson(tree, indent, sort)
}

/** With [split], a top-level array becomes one YAML document per item. */
internal fun jsonToYaml(input: String, indent: Int, sort: Boolean, split: Boolean): String {
    val tree = parseJson(input).let { if (sort) sortKeys(it) else it }
    val yaml = Yaml(DumperOptions().apply {
        defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        this.indent = indent
        isPrettyFlow = true
    })
    return if (split && tree.isJsonArray) yaml.dumpAll(tree.asJsonArray.map { jsonToPlain(it) }.iterator())
    else yaml.dump(jsonToPlain(tree))
}

class YamlJsonTool : Tool {
    override val descriptor = ToolDescriptor("yaml-json", "YAML ↔ JSON", "Convert between YAML and JSON", Icons.Default.SyncAlt, ToolCategory.CONVERTERS, listOf("yaml", "json", "converter"))

    @Composable override fun Content(modifier: Modifier) {
        var yamlInput by rememberCappedText("name: devtoys\ntags:\n  - yaml\n  - json\nstars: 42\n")
        var jsonInput by rememberCappedText("")
        var toYaml by rememberSaveable { mutableStateOf(false) }
        var jsonIndent by rememberSaveable { mutableStateOf(JsonIndent.TWO) }
        var yamlIndent by rememberSaveable { mutableStateOf(2) }
        var sort by rememberSaveable { mutableStateOf(false) }
        var split by rememberSaveable { mutableStateOf(false) }
        var output by remember { mutableStateOf("") }
        val input = if (toYaml) jsonInput else yamlInput
        LaunchedEffect(toYaml, input, jsonIndent, yamlIndent, sort, split) {
            if (input.isBlank()) {
                output = ""
                return@LaunchedEffect
            }
            delay(300)
            output = withContext(Dispatchers.Default) {
                try {
                    if (toYaml) jsonToYaml(input, yamlIndent, sort, split) else yamlToJson(input, jsonIndent, sort)
                } catch (e: Exception) {
                    "Error: " + if (toYaml) describeJsonError(e) else describeYamlError(e)
                } catch (e: StackOverflowError) {
                    "Error: The document nests too deeply or refers to itself through an anchor."
                }
            }
        }
        ToolScaffold(descriptor.title, modifier = modifier) {
            Segmented(listOf(false, true), toYaml, { if (it) "JSON to YAML" else "YAML to JSON" }, { toYaml = it })
            Spacer(modifier = Modifier.height(16.dp))
            if (toYaml) FileInput(jsonInput, { jsonInput = it }, "JSON input")
            else FileInput(yamlInput, { yamlInput = it }, "YAML input", mimeTypes = YAML_TYPES)
            Text("Indentation", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            if (toYaml) Segmented(listOf(2, 4), yamlIndent, { "$it spaces" }, { yamlIndent = it })
            else Segmented(JsonIndent.entries, jsonIndent, { it.label }, { jsonIndent = it })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = sort, onClick = { sort = !sort }, label = { Text("Sort keys", maxLines = 1) })
                if (toYaml) FilterChip(selected = split, onClick = { split = !split }, label = { Text("Array as documents", maxLines = 1) })
            }
            Spacer(modifier = Modifier.height(8.dp))
            ResultPanel(output, label = if (toYaml) "YAML" else "JSON")
            if (output.isNotEmpty() && !output.startsWith("Error: ")) {
                OutputActions(output, if (toYaml) "converted.yaml" else "converted.json", if (toYaml) "application/yaml" else "application/json")
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
