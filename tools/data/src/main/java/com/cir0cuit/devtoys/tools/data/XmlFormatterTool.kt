package com.cir0cuit.devtoys.tools.data
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.IntegrationInstructions

import androidx.compose.material3.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import java.io.StringReader
import java.io.StringWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.w3c.dom.DocumentType
import org.w3c.dom.Node
import org.xml.sax.InputSource

class XmlFormatterTool : Tool {
    override val descriptor = ToolDescriptor("xml-formatter", android.R.string.untitled, android.R.string.untitled, Icons.Default.IntegrationInstructions, ToolCategory.FORMATTERS, listOf("xml", "format"))
    @Composable override fun Content(modifier: Modifier) {
        var input by rememberSaveable { mutableStateOf("") }
        var output by remember { mutableStateOf("") }
        LaunchedEffect(input) {
            if (input.isEmpty()) {
                output = ""
                return@LaunchedEffect
            }
            delay(300)
            output = withContext(Dispatchers.Default) { formatXml(input) }
        }
        ToolScaffold("XML Formatter", modifier = modifier) {
            MonoTextField(input, { input = it }, "XML Input")
            Spacer(modifier = Modifier.height(16.dp))
            OutputPanel(output)
        }
    }
}

internal fun formatXml(input: String): String = try {
    if (hasInternalDtdSubset(input)) {
        "Error: DTD internal subsets are not supported"
    } else {
        val dbf = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            isExpandEntityReferences = false
            runCatching { setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true) }
            runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
            runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
            runCatching { setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false) }
        }
        val doc = dbf.newDocumentBuilder().parse(InputSource(StringReader(input)))
        indentElement(doc.documentElement, 0)
        // Indentation is done in the DOM (Android's and the JDK's indent=yes differ, and both
        // mangle mixed content); top-level nodes are serialized one per line.
        val transformer = TransformerFactory.newInstance().apply {
            runCatching { setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true) }
        }.newTransformer().apply {
            setOutputProperty(OutputKeys.METHOD, "xml") // JDK would otherwise switch to HTML output for a root named <html>
            setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
        }
        val lines = mutableListOf("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        var node = doc.firstChild
        while (node != null) {
            lines += if (node is DocumentType) doctypeDeclaration(node)
            else StringWriter().also { transformer.transform(DOMSource(node), StreamResult(it)) }.toString()
            node = node.nextSibling
        }
        lines.joinToString("\n")
    }
} catch (e: Exception) { "Error: ${e.message}" }

internal fun hasInternalDtdSubset(input: String): Boolean {
    var i = if (input.startsWith(Char(0xFEFF))) 1 else 0
    while (true) {
        while (i < input.length && input[i].isWhitespace()) i++
        when {
            input.startsWith("<?", i) -> {
                val end = input.indexOf("?>", i + 2)
                if (end < 0) return false
                i = end + 2
            }
            input.startsWith("<!--", i) -> {
                val end = input.indexOf("-->", i + 4)
                if (end < 0) return false
                i = end + 3
            }
            input.startsWith("<!DOCTYPE", i) -> {
                var quote: Char? = null
                for (j in i + 9 until input.length) {
                    val c = input[j]
                    when {
                        quote != null -> if (c == quote) quote = null
                        c == '"' || c == '\'' -> quote = c
                        c == '[' -> return true
                        c == '>' -> return false
                    }
                }
                return false
            }
            else -> return false
        }
    }
}

private fun doctypeDeclaration(dt: DocumentType): String {
    fun quote(s: String) = if ('"' in s) "'$s'" else "\"$s\""
    val id = when {
        dt.publicId != null -> " PUBLIC ${quote(dt.publicId)}" + (dt.systemId?.let { " " + quote(it) } ?: "")
        dt.systemId != null -> " SYSTEM ${quote(dt.systemId)}"
        else -> ""
    }
    return "<!DOCTYPE ${dt.name}$id>"
}

/** Re-indents element-only content; mixed content (XML 1.0 sec 2.10) is left untouched. */
private fun indentElement(element: Node, depth: Int) {
    val children = generateSequence(element.firstChild) { it.nextSibling }.toList()
    if (children.any { it.nodeType == Node.CDATA_SECTION_NODE || (it.nodeType == Node.TEXT_NODE && it.nodeValue.isNotBlank()) }) return
    children.filter { it.nodeType == Node.TEXT_NODE }.forEach { element.removeChild(it) }
    val kept = children.filter { it.nodeType != Node.TEXT_NODE }
    if (kept.isEmpty()) return
    val doc = element.ownerDocument
    for (child in kept) {
        element.insertBefore(doc.createTextNode("\n" + "  ".repeat(depth + 1)), child)
        if (child.nodeType == Node.ELEMENT_NODE) indentElement(child, depth + 1)
    }
    element.appendChild(doc.createTextNode("\n" + "  ".repeat(depth)))
}
