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
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.constructor.AbstractConstruct
import org.yaml.snakeyaml.constructor.SafeConstructor
import org.yaml.snakeyaml.error.YAMLException
import org.yaml.snakeyaml.nodes.Node
import org.yaml.snakeyaml.nodes.ScalarNode
import org.yaml.snakeyaml.nodes.Tag
import org.yaml.snakeyaml.representer.Representer
import org.yaml.snakeyaml.resolver.Resolver
import com.google.gson.JsonElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.BigInteger
import java.util.regex.Pattern

private val YAML_TYPES = arrayOf("application/yaml", "application/x-yaml", "text/*", "application/octet-stream")

// YAML 1.2 core schema (https://yaml.org/spec/1.2.2/#1032-tag-resolution).
private val CORE_BOOL = Pattern.compile("true|True|TRUE|false|False|FALSE")
private val CORE_INT = Pattern.compile("[-+]?[0-9]+|0o[0-7]+|0x[0-9a-fA-F]+")
private val CORE_FLOAT = Pattern.compile("[-+]?(?:\\.[0-9]+|[0-9]+(?:\\.[0-9]*)?)(?:[eE][-+]?[0-9]+)?|[-+]?\\.(?:inf|Inf|INF)|\\.(?:nan|NaN|NAN)")
private val CORE_NULL = Pattern.compile("~|null|Null|NULL|")

/** INT before FLOAT: "12" matches both. */
private fun Resolver.addCoreSchemaResolvers() {
    addImplicitResolver(Tag.BOOL, CORE_BOOL, "tTfF")
    addImplicitResolver(Tag.INT, CORE_INT, "-+0123456789")
    addImplicitResolver(Tag.FLOAT, CORE_FLOAT, "-+0123456789.")
    addImplicitResolver(Tag.NULL, CORE_NULL, "~nN\u0000")
}

/**
 * Plain scalars typed by the YAML 1.2 core schema instead of SnakeYAML's YAML 1.1 rules: yes/no/on/off,
 * dates and 1:30 stay strings, 012 is decimal 12. "<<" merge keys still work.
 */
private class CoreSchemaResolver : Resolver() {
    override fun addImplicitResolvers() {
        addCoreSchemaResolvers()
        addImplicitResolver(Tag.MERGE, Resolver.MERGE, "<", 10)
    }
}

/** YAML 1.1 plus 1.2 core rules, so the dumper quotes every string that either version would read as another type. */
private class QuotingResolver : Resolver() {
    override fun addImplicitResolvers() {
        super.addImplicitResolvers()
        addCoreSchemaResolvers()
    }
}

/** SnakeYAML's int constructor reads 012 as octal and rejects 0o17. */
internal fun parseCoreInt(s: String): Number {
    val n = try {
        when {
            s.startsWith("0o") -> BigInteger(s.substring(2), 8)
            s.startsWith("0x") -> BigInteger(s.substring(2), 16)
            else -> BigInteger(s)
        }
    } catch (e: NumberFormatException) {
        throw YAMLException("\"$s\" is not an integer")
    }
    return if (n.bitLength() < 64) n.toLong() else n
}

private class CoreSchemaConstructor(options: LoaderOptions) : SafeConstructor(options) {
    init {
        yamlConstructors[Tag.INT] = object : AbstractConstruct() {
            override fun construct(node: Node): Any = parseCoreInt((node as ScalarNode).value)
        }
    }
}

private fun coreSchemaYaml(): Yaml {
    val loaderOptions = LoaderOptions()
    val dumperOptions = DumperOptions()
    return Yaml(CoreSchemaConstructor(loaderOptions), Representer(dumperOptions), dumperOptions, loaderOptions, CoreSchemaResolver())
}

/** All "---" documents; several become a JSON array. */
internal fun yamlToJson(input: String, indent: JsonIndent, sort: Boolean): String {
    val docs = coreSchemaYaml().loadAll(input).toList()
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
    val options = DumperOptions().apply {
        defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        this.indent = indent
        isPrettyFlow = true
    }
    val yaml = Yaml(SafeConstructor(LoaderOptions()), Representer(options), options, QuotingResolver())
    return if (split && tree.isJsonArray) yaml.dumpAll(tree.asJsonArray.map { jsonToPlain(it) }.iterator())
    else yaml.dump(jsonToPlain(tree))
}

class YamlJsonTool : Tool {
    override val descriptor = ToolDescriptor("yaml-json", "YAML ↔ JSON", "Convert between YAML and JSON", Icons.Default.SyncAlt, ToolCategory.CONVERTERS, listOf("yaml", "json", "converter"), accepts = setOf(ShareKind.TEXT), fileTypes = setOf("yaml", "yml"))

    @Composable override fun Content(modifier: Modifier) {
        var yamlInput by rememberCappedText("name: devtoys\ntags:\n  - yaml\n  - json\nstars: 42\n")
        var jsonInput by rememberCappedText("")
        var toYaml by rememberSaveable { mutableStateOf(false) }
        OnSharedText { yamlInput = it; toYaml = false }
        var jsonIndent by rememberSaveable { mutableStateOf(JsonIndent.TWO) }
        var yamlIndent by rememberSaveable { mutableStateOf(2) }
        var sort by rememberSaveable { mutableStateOf(false) }
        var split by rememberSaveable { mutableStateOf(false) }
        // Tagged with its direction, so a result is never shown under the other direction while the next one is pending.
        var result by remember { mutableStateOf(toYaml to "") }
        val output = if (result.first == toYaml) result.second else ""
        val input = if (toYaml) jsonInput else yamlInput
        val debounce = rememberDebounce()
        LaunchedEffect(toYaml, input, jsonIndent, yamlIndent, sort, split) {
            if (input.isBlank()) {
                result = toYaml to ""
                return@LaunchedEffect
            }
            debounce.await(300)
            result = toYaml to withContext(Dispatchers.Default) {
                try {
                    if (toYaml) jsonToYaml(input, yamlIndent, sort, split) else yamlToJson(input, jsonIndent, sort)
                } catch (e: Exception) {
                    "Error: " + if (toYaml) describeJsonError(e, input) else describeYamlError(e)
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
