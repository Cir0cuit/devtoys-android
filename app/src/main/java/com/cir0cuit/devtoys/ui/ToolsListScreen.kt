package com.cir0cuit.devtoys.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.data.SettingsRepository
import com.cir0cuit.devtoys.tools.base.ToolRegistry
import kotlinx.coroutines.launch


fun getToolTitle(id: String): String {
    return when(id) {
        "base64" -> "Base64 Encoder / Decoder"
        "uuid" -> "UUID Generator"
        "url" -> "URL Encoder / Decoder"
        "hex" -> "Hex Encoder / Decoder"
        "json-formatter" -> "JSON Formatter"
        "regex" -> "Regex Tester"
        "hash" -> "Hash / Checksum"
        "jwt" -> "JWT Decoder"
        "timestamp" -> "Timestamp Converter"
        "case" -> "Case Converter"
        "html-entity" -> "HTML Entity Encoder"
        "rot13" -> "ROT13 / Caesar Cipher"
        "qr-gen" -> "QR Code Generator"
        "text-diff" -> "Text Comparer"
        "line-tools" -> "Line Tools"
        "yaml-json" -> "YAML ↔ JSON"
        "xml-formatter" -> "XML Formatter"
        "password-gen" -> "Password Generator"
        "hmac" -> "HMAC Calculator"
        "cron" -> "Cron Parser & Builder"
        "color-picker" -> "Color Converter"
        "markdown" -> "Markdown Previewer & Editor"
        "cheatsheet" -> "Cheatsheets"
        "http-status" -> "HTTP Status Codes"
        "sql-formatter" -> "SQL Formatter"
        "csv-viewer" -> "CSV Viewer & Converter"
        "json-types" -> "JSON to Kotlin Types"
        "base-converter" -> "Base Converter"
        "lorem-ipsum" -> "Lorem Ipsum Generator"
        "mac" -> "MAC Formatter & Vendor ID"
        "cidr" -> "CIDR Calculator"
        "chmod" -> "Chmod Calculator"
        "x509" -> "X.509 Certificate Decoder"
        "color-palette" -> "Color Palette Generator"
        "image-base64" -> "Image ↔ Base64"
        else -> id.replace("-", " ").replaceFirstChar { it.uppercase() }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsListScreen(
    toolRegistry: ToolRegistry,
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    expandedCategories: MutableMap<String, Boolean>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onToolClick: (com.cir0cuit.devtoys.tools.base.Tool) -> Unit
) {
    val allTools = toolRegistry.getAllTools()
    val favorites by settingsRepository.favoriteTools.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()

    val filteredTools = allTools.filter { tool ->
        getToolTitle(tool.descriptor.id).contains(searchQuery, ignoreCase = true) ||
        tool.descriptor.keywords.any { it.contains(searchQuery, ignoreCase = true) }
    }

    val groupedTools = filteredTools.groupBy { it.descriptor.category }.toSortedMap()
    

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            placeholder = { Text("Search tools...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            singleLine = true
        )
        
        LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
            groupedTools.forEach { (category, tools) ->
                val categoryName = category.name.replace("_", " & ")
                val isExpanded = expandedCategories[categoryName] ?: true

                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { expandedCategories[categoryName] = !isExpanded },
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(categoryName, fontWeight = FontWeight.Bold)
                            Icon(
                                if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null
                            )
                        }
                    }
                }
                
                if (isExpanded) {
                    items(tools, key = { it.descriptor.id }) { tool ->
                        val isFavorite = favorites.contains(tool.descriptor.id)
                        ListItem(
                            headlineContent = { Text(getToolTitle(tool.descriptor.id)) },
                            leadingContent = { Icon(tool.descriptor.icon, contentDescription = null) },
                            trailingContent = {
                                IconButton(onClick = { scope.launch { settingsRepository.toggleFavorite(tool.descriptor.id) } }) {
                                    Icon(
                                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Favorite",
                                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                    )
                                }
                            },
                            modifier = Modifier.clickable { onToolClick(tool) }
                        )
                    }
                }
            }
        }
    }
}