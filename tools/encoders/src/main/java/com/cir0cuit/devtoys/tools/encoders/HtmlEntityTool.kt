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
        var encodeInput by rememberSaveable { mutableStateOf("") }
        var decodeInput by rememberSaveable { mutableStateOf("") }
        var isEncodeMode by rememberSaveable { mutableStateOf(true) }

        val namedEntities = remember {
            val map = mutableMapOf<Int, String>()
            HtmlEntityData.categoriesMap.values.flatten().forEach { (charStr, entity) ->
                if (charStr.isNotEmpty() && charStr.length <= 2) map[charStr.codePointAt(0)] = entity
            }
            map
        }
        val encodeOutput = remember(encodeInput) {
            if (encodeInput.isEmpty()) "" else {
                val sb = java.lang.StringBuilder()
                var i = 0
                while (i < encodeInput.length) {
                    val cp = encodeInput.codePointAt(i)
                    val named = namedEntities[cp]
                    if (named != null) {
                        sb.append(named)
                    } else if (cp > 127 || cp == 34 || cp == 38 || cp == 39 || cp == 60 || cp == 62) {
                        sb.append("&#$cp;")
                    } else {
                        sb.append(cp.toChar())
                    }
                    i += java.lang.Character.charCount(cp)
                }
                sb.toString()
            }
        }
        val decodeOutput = remember(decodeInput) {
            if (decodeInput.isEmpty()) "" else android.text.Html.fromHtml(decodeInput, android.text.Html.FROM_HTML_MODE_LEGACY).toString()
        }
        
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