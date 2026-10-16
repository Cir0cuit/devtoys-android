package com.cir0cuit.devtoys.tools.data
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.IntegrationInstructions
import androidx.compose.material3.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.designsystem.CodeTextStyle
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*
import javax.xml.XMLConstants
import javax.xml.namespace.NamespaceContext
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathExpressionException
import javax.xml.xpath.XPathFactory
import java.io.StringReader
import java.io.StringWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.w3c.dom.Document
import org.w3c.dom.DocumentType
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import org.xml.sax.SAXParseException

private val XML_LAYOUTS = listOf("2 spaces", "4 spaces", "Minify")
private val XML_INDENT_UNITS = listOf("  ", "    ", null)
private const val MAX_XPATH_NODES = 200

class XmlFormatterTool : Tool {
    override val descriptor = ToolDescriptor("xml-formatter", "XML Formatter", "Format, minify and query XML with XPath", Icons.Default.IntegrationInstructions, ToolCategory.FORMATTERS, listOf("xml", "format", "minify", "xpath"))
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable override fun Content(modifier: Modifier) {
        var input by rememberSaveable { mutableStateOf("<note><to>Tove</to><from>Jani</from><body>Don't forget me this weekend!</body></note>") }
        var layout by rememberSaveable { mutableStateOf(0) }
        var xpath by rememberSaveable { mutableStateOf("") }
        var output by remember { mutableStateOf("") }
        var xpathResult by remember { mutableStateOf<XPathResult?>(null) }
        LaunchedEffect(input, layout) {
            if (input.isEmpty()) {
                output = ""
                return@LaunchedEffect
            }
            delay(300)
            output = withContext(Dispatchers.Default) { formatXml(input, XML_INDENT_UNITS[layout]) }
        }
        LaunchedEffect(input, xpath) {
            if (input.isEmpty() || xpath.isBlank()) {
                xpathResult = null
                return@LaunchedEffect
            }
            delay(300)
            xpathResult = withContext(Dispatchers.Default) { evaluateXPath(input, xpath) }
        }
        ToolScaffold(descriptor.title, modifier = modifier) {
            PasteableInput(input, { input = it }, "XML input")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                XML_LAYOUTS.forEachIndexed { i, label ->
                    SegmentedButton(
                        selected = layout == i,
                        onClick = { layout = i },
                        shape = SegmentedButtonDefaults.itemShape(i, XML_LAYOUTS.size),
                        icon = {}
                    ) { Text(label, maxLines = 1) }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = xpath,
                onValueChange = { xpath = it },
                label = { Text("XPath query (optional)") },
                placeholder = { Text("//item/@id", style = CodeTextStyle) },
                supportingText = { Text("Use d: for a default namespace") },
                singleLine = true,
                textStyle = CodeTextStyle,
                modifier = Modifier.fillMaxWidth()
            )
            xpathResult?.let { r ->
                Spacer(modifier = Modifier.height(8.dp))
                if (r.error != null) {
                    Text(r.error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                } else {
                    OutputPanel(r.text, label = r.label)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            ResultPanel(output, label = if (layout == 2) "Minified XML" else "Formatted XML")
        }
    }
}

/** Formats [input] with [indentUnit] per level, or minifies it when [indentUnit] is null. Errors start with "Error: ". */
internal fun formatXml(input: String, indentUnit: String? = "  "): String = try {
    serializeXml(parseXmlDocument(input), indentUnit)
} catch (e: Exception) { "Error: ${xmlErrorMessage(e)}" }

/** Document builder hardened against XXE and entity expansion. */
internal fun secureDocumentBuilder(): DocumentBuilder = DocumentBuilderFactory.newInstance().apply {
    isNamespaceAware = true
    isExpandEntityReferences = false
    runCatching { setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true) }
    runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
    runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
    runCatching { setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false) }
}.newDocumentBuilder()

/** Parses untrusted XML; documents with an internal DTD subset are rejected. */
internal fun parseXmlDocument(input: String): Document {
    require(!hasInternalDtdSubset(input)) { "DTD internal subsets are not supported" }
    return secureDocumentBuilder().parse(InputSource(StringReader(input)))
}

/** Parser message with the line and column when the parser reports them. */
internal fun xmlErrorMessage(e: Exception): String {
    val message = e.message ?: e.javaClass.simpleName
    return if (e is SAXParseException && e.lineNumber > 0) "Line ${e.lineNumber}, column ${e.columnNumber}: $message" else message
}

private fun newXmlTransformer(): Transformer = TransformerFactory.newInstance().apply {
    runCatching { setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true) }
}.newTransformer().apply {
    setOutputProperty(OutputKeys.METHOD, "xml") // JDK would otherwise switch to HTML output for a root named <html>
    setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
}

private fun nodeToString(node: Node, transformer: Transformer): String =
    StringWriter().also { transformer.transform(DOMSource(node), StreamResult(it)) }.toString()

/** Serializes [doc] (mutating it) indented by [indentUnit], or on one line without comments when it is null. */
internal fun serializeXml(doc: Document, indentUnit: String?): String {
    // Indentation is done in the DOM (Android's and the JDK's indent=yes differ, and both
    // mangle mixed content); top-level nodes are serialized one per line.
    if (indentUnit == null) minifyNode(doc) else doc.documentElement?.let { indentElement(it, 0, indentUnit) }
    val transformer = newXmlTransformer()
    val parts = mutableListOf("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
    var node = doc.firstChild
    while (node != null) {
        parts += if (node is DocumentType) doctypeDeclaration(node) else nodeToString(node, transformer)
        node = node.nextSibling
    }
    return parts.joinToString(if (indentUnit == null) "" else "\n")
}

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

private fun children(node: Node): List<Node> = generateSequence(node.firstChild) { it.nextSibling }.toList()

private fun hasText(children: List<Node>) =
    children.any { it.nodeType == Node.CDATA_SECTION_NODE || (it.nodeType == Node.TEXT_NODE && it.nodeValue.isNotBlank()) }

/** Re-indents element-only content; mixed content (XML 1.0 sec 2.10) is left untouched. */
private fun indentElement(element: Node, depth: Int, unit: String) {
    val children = children(element)
    if (hasText(children)) return
    children.filter { it.nodeType == Node.TEXT_NODE }.forEach { element.removeChild(it) }
    val kept = children.filter { it.nodeType != Node.TEXT_NODE }
    if (kept.isEmpty()) return
    val doc = element.ownerDocument
    for (child in kept) {
        element.insertBefore(doc.createTextNode("\n" + unit.repeat(depth + 1)), child)
        if (child.nodeType == Node.ELEMENT_NODE) indentElement(child, depth + 1, unit)
    }
    element.appendChild(doc.createTextNode("\n" + unit.repeat(depth)))
}

/** Drops comments everywhere and whitespace in element-only content; mixed content keeps its text. */
private fun minifyNode(node: Node) {
    val children = children(node)
    val elementOnly = !hasText(children)
    for (child in children) when {
        child.nodeType == Node.COMMENT_NODE -> node.removeChild(child)
        child.nodeType == Node.TEXT_NODE && elementOnly -> node.removeChild(child)
        child.nodeType == Node.ELEMENT_NODE -> minifyNode(child)
    }
}

/** XPath outcome: [label] and [text] for the result slab, or an [error] for an invalid expression. */
internal data class XPathResult(val label: String, val text: String, val error: String? = null)

/** Evaluates [expression] against [input]; returns null when the XML itself does not parse. */
internal fun evaluateXPath(input: String, expression: String): XPathResult? {
    val doc = try { parseXmlDocument(input) } catch (e: Exception) { return null }
    val xpath = XPathFactory.newInstance().newXPath().apply { namespaceContext = namespacesOf(doc) }
    return try {
        val nodes = xpath.evaluate(expression, doc, XPathConstants.NODESET) as NodeList
        val transformer = newXmlTransformer()
        val total = nodes.length
        val text = (0 until minOf(total, MAX_XPATH_NODES)).joinToString("\n") { i ->
            val node = nodes.item(i)
            when (node.nodeType) {
                Node.ATTRIBUTE_NODE -> "${node.nodeName}=\"${node.nodeValue}\""
                Node.TEXT_NODE, Node.CDATA_SECTION_NODE, Node.COMMENT_NODE -> node.nodeValue
                Node.DOCUMENT_NODE -> nodeToString((node as Document).documentElement, transformer)
                else -> nodeToString(node, transformer)
            }
        }
        val label = when (total) {
            0 -> "No matches"
            1 -> "1 match"
            else -> "$total matches" + if (total > MAX_XPATH_NODES) ", first $MAX_XPATH_NODES shown" else ""
        }
        XPathResult(label, text)
    } catch (e: XPathExpressionException) {
        // Not a node set: count(), string(), boolean() and friends.
        try {
            XPathResult("Result", xpath.evaluate(expression, doc))
        } catch (e2: XPathExpressionException) {
            val reason = (e2.cause?.message ?: e2.message)?.let { "$it. " } ?: ""
            XPathResult("", "", "Invalid XPath expression. ${reason}Check the syntax, for example //item[@id='1']/name.")
        }
    }
}

/** Prefixes used in the document, plus "d" for the default namespace (XPath 1.0 cannot address it unprefixed). */
private fun namespacesOf(doc: Document): NamespaceContext {
    val map = HashMap<String, String>()
    fun add(node: Node) {
        val uri = node.namespaceURI ?: return
        if (uri == XMLConstants.XMLNS_ATTRIBUTE_NS_URI) return
        map.putIfAbsent(node.prefix ?: "d", uri)
    }
    val elements = doc.getElementsByTagName("*")
    for (i in 0 until elements.length) {
        val el = elements.item(i) as Element
        add(el)
        val attrs = el.attributes
        for (j in 0 until attrs.length) attrs.item(j).takeIf { it.prefix != null }?.let(::add)
    }
    return object : NamespaceContext {
        override fun getNamespaceURI(prefix: String?): String = prefix?.let { map[it] } ?: XMLConstants.NULL_NS_URI
        override fun getPrefix(namespaceURI: String?): String? = null
        override fun getPrefixes(namespaceURI: String?): Iterator<String> = emptyList<String>().iterator()
    }
}
