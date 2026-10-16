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
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonSyntaxException
import com.google.gson.Strictness
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import java.io.StringReader

class JsonFormatterTool : Tool {
    override val descriptor = ToolDescriptor("json-formatter", android.R.string.untitled, android.R.string.untitled, Icons.Default.DataObject, ToolCategory.FORMATTERS, listOf("json", "format"))
    @Composable override fun Content(modifier: Modifier) {
        var input by rememberSaveable { mutableStateOf("") }
        val output = remember(input) {
            if (input.isBlank()) "" else try {
                val reader = JsonReader(StringReader(input))
                reader.setStrictness(Strictness.STRICT)
                val element = Gson().getAdapter(JsonElement::class.java).read(reader)
                if (reader.peek() != JsonToken.END_DOCUMENT) throw JsonSyntaxException("Unexpected trailing content at ${reader.path}")
                GsonBuilder().setPrettyPrinting().disableHtmlEscaping().serializeNulls().create().toJson(element)
            } catch (e: Exception) { "Error: ${e.message}" }
        }
        ToolScaffold("JSON Formatter", modifier = modifier) {
            MonoTextField(input, { input = it }, "JSON Input")
            Spacer(modifier = Modifier.height(16.dp))
            OutputPanel(output)
        }
    }
}