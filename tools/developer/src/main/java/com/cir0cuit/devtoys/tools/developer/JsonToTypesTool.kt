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

class JsonToTypesTool : Tool {
    override val descriptor = ToolDescriptor("json-types", com.cir0cuit.devtoys.tools.base.R.string.tool_json_to_kotlin_title, com.cir0cuit.devtoys.tools.base.R.string.tool_json_to_kotlin_desc, Icons.Default.AccountTree, ToolCategory.CONVERTERS, listOf("json", "types", "kotlin", "typescript"))
    
    @Composable override fun Content(modifier: Modifier) {
        var input by rememberSaveable { mutableStateOf("{\n  \"id\": 1,\n  \"name\": \"Test\",\n  \"isActive\": true,\n  \"data\": {\"value\": 5}\n}") }
        
        val output = remember(input) {
            if (input.isEmpty()) "" else try {
                val trimmed = input.trim()
                if (trimmed.startsWith("[")) {
                    val arr = JSONArray(trimmed)
                    if (arr.length() > 0 && !arr.isNull(0)) {
                        generateKotlinClass("RootItem", arr.getJSONObject(0))
                    } else {
                        "// Empty or primitive array"
                    }
                } else {
                    val json = JSONObject(trimmed)
                    generateKotlinClass("Root", json)
                }
            } catch (e: Exception) { "Error: ${e.message ?: "Invalid JSON"}" }
        }
        
        ToolScaffold("JSON to Kotlin Types", modifier = modifier) {
            MonoTextField(input, { input = it }, "JSON Input", minLines=6)
            Spacer(modifier = Modifier.height(16.dp))
            OutputPanel(output)
        }
    }

    private fun generateKotlinClass(className: String, json: JSONObject): String {
        val sb = StringBuilder()
        val nestedClasses = mutableListOf<String>()
        
        sb.append("data class $className(\n")
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = json.get(key)
            val typeName = key.replaceFirstChar { it.uppercase() }
            
            val type = when {
                value is Int -> "Int"
                value is Long -> "Long"
                value is Boolean -> "Boolean"
                value is Double -> "Double"
                value is JSONObject -> {
                    nestedClasses.add(generateKotlinClass(typeName, value))
                    typeName
                }
                value is JSONArray -> {
                    if (value.length() > 0 && value.optJSONObject(0) != null) {
                        val itemType = typeName + "Item"
                        nestedClasses.add(generateKotlinClass(itemType, value.getJSONObject(0)))
                        "List<$itemType>"
                    } else {
                        "List<Any>"
                    }
                }
                value == JSONObject.NULL -> "Any? /* null */"
                else -> "String"
            }
            sb.append("    val $key: $type,\n")
        }
        sb.append(")\n\n")
        
        nestedClasses.forEach { sb.append(it) }
        return sb.toString().trim()
    }
}