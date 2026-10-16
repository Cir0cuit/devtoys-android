package com.cir0cuit.devtoys.tools.data

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.designsystem.CodeTextStyle
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.w3c.dom.DOMException
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node

class XmlJsonTool : Tool {
    override val descriptor = ToolDescriptor("xml-json", "XML ↔ JSON", "Convert between XML and JSON", Icons.Default.Transform, ToolCategory.CONVERTERS, listOf("xml", "json", "converter", "soap", "rss"))
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable override fun Content(modifier: Modifier) {
        var xmlInput by rememberSaveable { mutableStateOf("<book id=\"1\"><title>Dune</title><tag>sf</tag><tag>classic</tag></book>") }
        var jsonInput by rememberSaveable { mutableStateOf("") }
        var rootName by rememberSaveable { mutableStateOf("") }
        var toXml by rememberSaveable { mutableStateOf(false) }
        var output by remember { mutableStateOf("") }
        LaunchedEffect(toXml, xmlInput, jsonInput, rootName) {
            val input = if (toXml) jsonInput else xmlInput
            if (input.isBlank()) {
                output = ""
                return@LaunchedEffect
            }
            delay(300)
            output = withContext(Dispatchers.Default) {
                try {
                    if (toXml) jsonToXml(input, rootName) else xmlToJson(input)
                } catch (e: Exception) {
                    "Error: " + if (toXml) (e.cause?.message ?: e.message ?: "invalid JSON").substringBefore("\nSee ") else xmlErrorMessage(e)
                }
            }
        }
        ToolScaffold(descriptor.title, modifier = modifier) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf("XML to JSON", "JSON to XML").forEachIndexed { i, label ->
                    SegmentedButton(
                        selected = toXml == (i == 1),
                        onClick = { toXml = i == 1 },
                        shape = SegmentedButtonDefaults.itemShape(i, 2)
                    ) { Text(label, maxLines = 1) }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (toXml) {
                PasteableInput(jsonInput, { jsonInput = it }, "JSON input")
                OutlinedTextField(
                    value = rootName,
                    onValueChange = { rootName = it },
                    label = { Text("Root element (optional)") },
                    supportingText = { Text("Empty uses the single top-level key") },
                    singleLine = true,
                    textStyle = CodeTextStyle,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                PasteableInput(xmlInput, { xmlInput = it }, "XML input")
            }
            Spacer(modifier = Modifier.height(8.dp))
            ResultPanel(output, label = if (toXml) "XML" else "JSON")
            if (output.isNotEmpty() && !output.startsWith("Error: ")) {
                TextButton(onClick = {
                    if (toXml) xmlInput = output else jsonInput = output
                    toXml = !toXml
                }) {
                    Icon(Icons.Default.SwapVert, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Use result as input")
                }
            }
        }
    }
}

/**
 * Elements become objects keyed by the root name, attributes "@name", text "#text", repeated
 * siblings arrays. An element with only text (or nothing) becomes a string. Values stay strings.
 */
internal fun xmlToJson(input: String): String {
    val root = parseXmlDocument(input).documentElement
    val result = JsonObject().apply { add(root.nodeName, xmlElementToJson(root)) }
    return GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(result)
}

private fun xmlElementToJson(element: Element): JsonElement {
    val obj = JsonObject()
    val attrs = element.attributes
    for (i in 0 until attrs.length) attrs.item(i).let { obj.addProperty("@" + it.nodeName, it.nodeValue) }
    val text = StringBuilder()
    var child = element.firstChild
    while (child != null) {
        when (child.nodeType) {
            Node.ELEMENT_NODE -> {
                val name = child.nodeName
                val value = xmlElementToJson(child as Element)
                when (val existing = obj.get(name)) {
                    null -> obj.add(name, value)
                    is JsonArray -> existing.add(value)
                    else -> obj.add(name, JsonArray().apply { add(existing); add(value) })
                }
            }
            Node.TEXT_NODE, Node.CDATA_SECTION_NODE -> text.append(child.nodeValue)
        }
        child = child.nextSibling
    }
    if (obj.size() == 0) return JsonPrimitive(text.toString())
    if (text.isNotBlank()) obj.addProperty("#text", text.toString().trim())
    return obj
}

/**
 * Reverse of [xmlToJson]. With a blank [rootName] the JSON must be an object with one key, which
 * names the root element; otherwise the whole JSON goes inside <rootName>. Array items under the
 * root element are named "item".
 */
internal fun jsonToXml(input: String, rootName: String): String {
    val json = JsonParser.parseString(input)
    val doc = secureDocumentBuilder().newDocument()
    val name = rootName.trim()
    if (name.isEmpty()) {
        val obj = json.takeIf { it.isJsonObject }?.asJsonObject
        require(obj != null && obj.size() == 1 && !obj.keySet().first().startsWith("@")) {
            "The JSON needs exactly one top-level key to name the root element. Enter a root element name to wrap it."
        }
        val (key, value) = obj.entrySet().first()
        require(!value.isJsonArray) {
            "\"$key\" holds an array, which would give several root elements. Enter a root element name to wrap it."
        }
        doc.appendChild(xmlElement(doc, key).also { fillXml(doc, it, value) })
    } else {
        doc.appendChild(xmlElement(doc, name).also { fillXml(doc, it, json) })
    }
    return serializeXml(doc, "  ")
}

private fun fillXml(doc: Document, element: Element, value: JsonElement) {
    when {
        value.isJsonNull -> {}
        value.isJsonPrimitive -> element.appendChild(doc.createTextNode(value.asString))
        value.isJsonArray -> value.asJsonArray.forEach { item -> element.appendChild(xmlElement(doc, "item").also { fillXml(doc, it, item) }) }
        else -> for ((key, v) in value.asJsonObject.entrySet()) when {
            key.startsWith("@") -> try {
                element.setAttribute(key.substring(1), if (v.isJsonPrimitive) v.asString else if (v.isJsonNull) "" else v.toString())
            } catch (e: DOMException) {
                throw IllegalArgumentException("\"${key.substring(1)}\" is not a valid XML attribute name. Rename the key.")
            }
            key == "#text" -> element.appendChild(doc.createTextNode(if (v.isJsonPrimitive) v.asString else if (v.isJsonNull) "" else v.toString()))
            v.isJsonArray -> v.asJsonArray.forEach { item -> element.appendChild(xmlElement(doc, key).also { fillXml(doc, it, item) }) }
            else -> element.appendChild(xmlElement(doc, key).also { fillXml(doc, it, v) })
        }
    }
}

private fun xmlElement(doc: Document, name: String): Element = try {
    doc.createElement(name)
} catch (e: DOMException) {
    throw IllegalArgumentException("\"$name\" is not a valid XML element name. Rename the key so it starts with a letter or underscore and has no spaces.")
}
