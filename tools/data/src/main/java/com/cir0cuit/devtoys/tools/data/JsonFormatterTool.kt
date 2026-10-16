package com.cir0cuit.devtoys.tools.data

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material3.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*
import com.google.gson.FormattingStyle
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.google.gson.Strictness
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import java.io.StringReader

private val indents = listOf("2 spaces", "4 spaces", "Minified")

class JsonFormatterTool : Tool {
    override val descriptor = ToolDescriptor("json-formatter", "JSON Formatter", "Format and validate JSON", Icons.Default.DataObject, ToolCategory.FORMATTERS, listOf("json", "format", "minify"))
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable override fun Content(modifier: Modifier) {
        var input by rememberSaveable { mutableStateOf("{\"name\":\"devtoys\",\"tags\":[\"json\",\"format\"],\"stars\":42}") }
        var indent by rememberSaveable { mutableStateOf(0) }
        val output = remember(input, indent) {
            if (input.isBlank()) "" else try {
                val style = when (indent) {
                    0 -> FormattingStyle.PRETTY
                    1 -> FormattingStyle.PRETTY.withIndent("    ")
                    else -> FormattingStyle.COMPACT
                }
                GsonBuilder().setFormattingStyle(style).disableHtmlEscaping().serializeNulls().create().toJson(parseStrictJson(input))
            } catch (e: Exception) { "Error: ${e.message}" }
        }
        ToolScaffold(descriptor.title, modifier = modifier) {
            PasteableInput(input, { input = it }, "JSON input")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                indents.forEachIndexed { i, label ->
                    SegmentedButton(
                        selected = indent == i,
                        onClick = { indent = i },
                        shape = SegmentedButtonDefaults.itemShape(i, indents.size),
                        icon = {}
                    ) { Text(label, maxLines = 1) }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            ResultPanel(output)
        }
    }
}

/** Parses one strict JSON document and rejects anything after it. */
internal fun parseStrictJson(input: String): JsonElement {
    val reader = JsonReader(StringReader(input)).apply { setStrictness(Strictness.STRICT) }
    val element = JsonParser.parseReader(reader)
    if (reader.peek() != JsonToken.END_DOCUMENT) throw JsonSyntaxException("Unexpected trailing content at ${reader.path}")
    return element
}
