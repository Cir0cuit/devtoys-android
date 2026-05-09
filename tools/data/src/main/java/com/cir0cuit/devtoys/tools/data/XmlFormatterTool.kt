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
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource
import java.io.StringReader
import java.io.StringWriter

class XmlFormatterTool : Tool {
    override val descriptor = ToolDescriptor("xml-formatter", android.R.string.untitled, android.R.string.untitled, Icons.Default.IntegrationInstructions, ToolCategory.FORMATTERS, listOf("xml", "format"))
    @Composable override fun Content(modifier: Modifier) {
        var input by rememberSaveable { mutableStateOf("") }
        val output = remember(input) {
            if (input.isEmpty()) "" else try {
                val transformer = TransformerFactory.newInstance().newTransformer()
                transformer.setOutputProperty(OutputKeys.INDENT, "yes")
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
                val result = StringWriter()
                transformer.transform(StreamSource(StringReader(input)), StreamResult(result))
                result.toString()
            } catch (e: Exception) { "Error: ${e.message}" }
        }
        ToolScaffold("XML Formatter", modifier = modifier) {
            MonoTextField(input, { input = it }, "XML Input")
            Spacer(modifier = Modifier.height(16.dp))
            OutputPanel(output)
        }
    }
}