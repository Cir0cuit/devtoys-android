package com.cir0cuit.devtoys.tools.encoders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Html
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.designsystem.CodeTextStyle
import com.cir0cuit.devtoys.core.ui.MonoTextField
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolCategory
import com.cir0cuit.devtoys.tools.base.ToolDescriptor

internal object HtmlEntityCodec {
    private val namedEntities: Map<Int, String> by lazy {
        HtmlEntityData.categoriesMap.values.flatten()
            .filter { (s, _) -> s.isNotEmpty() && s.codePointCount(0, s.length) == 1 }
            .associate { (s, entity) -> s.codePointAt(0) to entity }
    }
    private val reverseEntities: Map<String, String> by lazy {
        HtmlEntityData.categoriesMap.values.flatten().associate { (symbol, entity) -> entity to symbol }
    }
    private val entityRegex = Regex("&(#[0-9]+|#[xX][0-9a-fA-F]+|[A-Za-z][A-Za-z0-9]*);")
    private val windows1252 = charset("windows-1252")

    fun encode(input: String): String = buildString {
        var i = 0
        while (i < input.length) {
            val cp = input.codePointAt(i)
            when {
                cp == '\''.code -> append("&#39;")
                cp > 127 || cp == '"'.code || cp == '&'.code || cp == '<'.code || cp == '>'.code -> append(namedEntities[cp] ?: "&#$cp;")
                else -> append(cp.toChar())
            }
            i += Character.charCount(cp)
        }
    }

    fun decode(input: String): String =
        entityRegex.replace(input) { m ->
            val body = m.groupValues[1]
            if (!body.startsWith("#")) return@replace reverseEntities[m.value] ?: m.value
            // Regex guarantees valid digits, so null here means overflow -> out of range.
            val cp = (if (body[1] == 'x' || body[1] == 'X') body.substring(2).toIntOrNull(16) else body.substring(1).toIntOrNull()) ?: -1
            // WHATWG numeric character reference end state
            when {
                cp <= 0 || cp > 0x10FFFF || cp in 0xD800..0xDFFF -> "�"
                cp in 0x80..0x9F -> String(byteArrayOf(cp.toByte()), windows1252).takeIf { it != "�" } ?: cp.toChar().toString()
                else -> String(Character.toChars(cp))
            }
        }
}

class HtmlEntityTool : Tool {
    override val descriptor = ToolDescriptor(
        id = "html-entity",
        title = "HTML Entity Encoder",
        description = "Escape and unescape HTML entities",
        icon = Icons.Default.Html,
        category = ToolCategory.ENCODERS_DECODERS,
        keywords = listOf("html", "entity", "encode")
    )

    @Composable
    override fun Content(modifier: Modifier) {
        ToolScaffold(descriptor.title, modifier = modifier, scrollable = false) {
            EncodeDecodePane(HtmlEntityCodec::encode, HtmlEntityCodec::decode, extraMode = "Entities") { EntityList() }
        }
    }

    @Composable
    private fun EntityList() {
        val expandedState = remember { mutableStateMapOf<String, Boolean>() }
        var query by rememberSaveable { mutableStateOf("") }
        val clipboardManager = LocalClipboardManager.current
        val matches = remember(query) {
            val q = query.trim()
            if (q.isEmpty()) emptyList()
            else HtmlEntityData.categoriesMap.values.flatten().filter { (symbol, code) -> symbol == q || code.contains(q, ignoreCase = true) }
        }

        @Composable
        fun EntityRow(symbol: String, code: String) {
            ListItem(
                headlineContent = { Text(code, style = CodeTextStyle) },
                leadingContent = { Text(symbol, style = MaterialTheme.typography.titleLarge) },
                trailingContent = {
                    Row {
                        TextButton(onClick = { clipboardManager.setText(AnnotatedString(symbol)) }) { Text("Symbol") }
                        TextButton(onClick = { clipboardManager.setText(AnnotatedString(code)) }) { Text("Code") }
                    }
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        }

        MonoTextField(query, { query = it }, "Search by name or symbol", minLines = 1, maxLines = 1)
        Spacer(Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (query.isNotBlank()) {
                if (matches.isEmpty()) item { Text("No entity matches \"${query.trim()}\".", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(matches) { (symbol, code) -> EntityRow(symbol, code) }
            } else {
                HtmlEntityData.categoriesMap.forEach { (catName, entities) ->
                    val isExpanded = expandedState[catName] ?: false
                    item(key = catName) {
                        ListItem(
                            headlineContent = { Text(catName, style = MaterialTheme.typography.titleSmall) },
                            supportingContent = { Text("${entities.size} entities") },
                            trailingContent = { Icon(if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable { expandedState[catName] = !isExpanded }
                        )
                    }
                    if (isExpanded) items(entities) { (symbol, code) -> EntityRow(symbol, code) }
                }
            }
        }
    }
}
