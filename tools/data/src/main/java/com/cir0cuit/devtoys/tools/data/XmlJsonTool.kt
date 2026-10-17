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
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import org.w3c.dom.DOMException
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node

class XmlJsonTool : Tool {
    override val descriptor = ToolDescriptor("xml-json", "XML ↔ JSON", "Convert between XML and JSON", Icons.Default.Transform, ToolCategory.CONVERTERS, listOf("xml", "json", "converter", "soap", "rss"))
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable override fun Content(modifier: Modifier) {
        var xmlInput by rememberCappedText("<book id=\"1\"><title>Dune</title><tag>sf</tag><tag>classic</tag></book>")
        var jsonInput by rememberCappedText("")
        var rootName by rememberSaveable { mutableStateOf("") }
        var toXml by rememberSaveable { mutableStateOf(false) }
        var result by remember { mutableStateOf(XmlJsonResult(toXml, "")) }
        val debounce = rememberDebounce()
        LaunchedEffect(toXml, xmlInput, jsonInput, rootName) {
            val input = if (toXml) jsonInput else xmlInput
            if (input.isBlank()) {
                result = XmlJsonResult(toXml, "")
                return@LaunchedEffect
            }
            debounce.await(300)
            result = withContext(Dispatchers.Default) { convertXmlJson(input, toXml, rootName) }
        }
        // A result is never shown under the other direction while the next one is pending.
        val current = result.takeIf { it.toXml == toXml } ?: XmlJsonResult(toXml, "")
        val output = current.text
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
                FileInput(jsonInput, { jsonInput = it }, "JSON input")
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
                FileInput(xmlInput, { xmlInput = it }, "XML input", mimeTypes = XML_TYPES)
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (current.error != null) ErrorText(current.error)
            else ResultPanel(output, label = if (toXml) "XML" else "JSON")
            if (output.isNotEmpty()) {
                OutputActions(output, if (toXml) "converted.xml" else "converted.json", if (toXml) "application/xml" else "application/json")
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

/** Converted [text], or an [error] to show instead; [toXml] is the direction it was made for. */
internal class XmlJsonResult(val toXml: Boolean, val text: String, val error: String? = null)

internal fun convertXmlJson(input: String, toXml: Boolean, rootName: String): XmlJsonResult = try {
    XmlJsonResult(toXml, if (toXml) jsonToXml(input, rootName) else xmlToJson(input))
} catch (e: Exception) {
    val error = when {
        !toXml -> "Couldn't parse the input. ${xmlErrorMessage(e)}"
        // Gson reports trailing content as MalformedJsonException, an IOException.
        e is JsonParseException || e is IOException -> "Couldn't parse the input. ${describeJsonError(e, input)}"
        // The JSON parsed, but its keys or shape cannot become XML; the message says what to change.
        else -> e.message ?: "Couldn't convert the JSON."
    }
    XmlJsonResult(toXml, "", error)
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
    val json = parseJson(input)
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
