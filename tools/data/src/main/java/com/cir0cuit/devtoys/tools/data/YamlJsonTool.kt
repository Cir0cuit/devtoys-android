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
import org.json.JSONObject

class YamlJsonTool : Tool {
    override val descriptor = ToolDescriptor("yaml-json", android.R.string.untitled, android.R.string.untitled, Icons.Default.SyncAlt, ToolCategory.CONVERTERS, listOf("yaml", "json", "converter"))
    @Composable override fun Content(modifier: Modifier) {
        var yamlInput by rememberSaveable { mutableStateOf("") }
        var jsonInput by rememberSaveable { mutableStateOf("") }
        var toYaml by rememberSaveable { mutableStateOf(false) }
        
        val jsonOutput = remember(yamlInput) {
            if (yamlInput.isEmpty()) "" else try {
                val obj = Yaml().load<Any>(yamlInput)
                when (obj) {
                    is Map<*, *> -> org.json.JSONObject(obj).toString(2)
                    is List<*> -> org.json.JSONArray(obj).toString(2)
                    else -> obj.toString()
                }
            } catch (e: Exception) { "Error: ${e.message}" }
        }
        val yamlOutput = remember(jsonInput) {
            if (jsonInput.isEmpty()) "" else try {
                val obj = Yaml().load<Any>(jsonInput)
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