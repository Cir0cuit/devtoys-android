package com.cir0cuit.devtoys.tools.encoders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Html
import androidx.compose.material.icons.filled.ContentCopy

import androidx.compose.material3.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*

internal object HtmlEntityCodec {
    private val namedEntities: Map<Int, String> by lazy {
        val map = mutableMapOf<Int, String>()
        HtmlEntityData.categoriesMap.values.flatten().forEach { (charStr, entity) ->
            if (charStr.isNotEmpty() && charStr.codePointCount(0, charStr.length) == 1) map[charStr.codePointAt(0)] = entity
        }
        map
    }
    private val reverseEntities: Map<String, String> by lazy {
        HtmlEntityData.categoriesMap.values.flatten().associate { (symbol, entity) -> entity to symbol }
    }
    private val entityRegex = Regex("&(#[0-9]+|#[xX][0-9a-fA-F]+|[A-Za-z][A-Za-z0-9]*);")

    fun encode(input: String): String {
        if (input.isEmpty()) return ""
        val sb = java.lang.StringBuilder()
        var i = 0
        while (i < input.length) {
            val cp = input.codePointAt(i)
            if (cp == 39) {
                sb.append("&#39;")
            } else if (cp > 127 || cp == 34 || cp == 38 || cp == 60 || cp == 62) {
                sb.append(namedEntities[cp] ?: "&#$cp;")
            } else {
                sb.append(cp.toChar())
            }
            i += java.lang.Character.charCount(cp)
        }
        return sb.toString()
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

    private val windows1252 = charset("windows-1252")
}

class HtmlEntityTool : Tool {
    override val descriptor = ToolDescriptor("html-entity", android.R.string.untitled, android.R.string.untitled, Icons.Default.Html, ToolCategory.ENCODERS_DECODERS, listOf("html", "entity", "encode"))
    
    // Using the massive exhaustive entity map from HtmlEntityData


    @Composable override fun Content(modifier: Modifier) {
        var tabIndex by rememberSaveable { mutableStateOf(0) }
        
        ToolScaffold("HTML Entity", modifier = modifier, scrollable = false) {
            TabRow(selectedTabIndex = tabIndex) {
                Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Encoder/Decoder") })
                Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("Entity List") })
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (tabIndex == 0) {
                EncoderDecoder()
            } else {
                EntityList()
            }
        }
    }

    @Composable fun EncoderDecoder() {
        var encodeInput by rememberSaveableText()
        var decodeInput by rememberSaveableText()
        var isEncodeMode by rememberSaveable { mutableStateOf(true) }

        val encodeOutput = remember(encodeInput) { HtmlEntityCodec.encode(encodeInput) }
        val decodeOutput = remember(decodeInput) { HtmlEntityCodec.decode(decodeInput) }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            TabRow(selectedTabIndex = if (isEncodeMode) 0 else 1) {
                Tab(selected = isEncodeMode, onClick = { isEncodeMode = true }, text = { Text("Encode") })
                Tab(selected = !isEncodeMode, onClick = { isEncodeMode = false }, text = { Text("Decode") })
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (isEncodeMode) {
                MonoTextField(encodeInput, { encodeInput = it }, "Input to Encode")
                Spacer(modifier = Modifier.height(16.dp))
                OutputPanel(encodeOutput)
            } else {
                MonoTextField(decodeInput, { decodeInput = it }, "Input to Decode")
                Spacer(modifier = Modifier.height(16.dp))
                OutputPanel(decodeOutput)
            }
        }
    }

    @Composable fun EntityList() {
        val expandedState = remember { mutableStateMapOf<String, Boolean>() }
        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            HtmlEntityData.categoriesMap.forEach { (catName, entities) ->
                val isExpanded = expandedState[catName] ?: false
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { expandedState[catName] = !isExpanded },
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(catName, fontWeight = FontWeight.Bold)
                            Icon(if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null)
                        }
                    }
                }
                if (isExpanded) {
                    items(entities) { entity ->
                        ListItem(
                            headlineContent = { Text(entity.second) },
                            leadingContent = { Text(entity.first, style = MaterialTheme.typography.headlineSmall) },
                            trailingContent = {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AssistChip(
                                        onClick = { clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(entity.first)) },
                                        label = { Text("Symbol") },
                                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = "Copy Symbol", modifier = Modifier.size(16.dp)) }
                                    )
                                    AssistChip(
                                        onClick = { clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(entity.second)) },
                                        label = { Text("Code") },
                                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = "Copy Code", modifier = Modifier.size(16.dp)) }
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}