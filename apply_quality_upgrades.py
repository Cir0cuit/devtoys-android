import os

project_dir = "C:/Users/barti/.gemini/antigravity/scratch/devtoys-android"

def read_file(path):
    with open(os.path.join(project_dir, path), "r", encoding="utf-8") as f:
        return f.read()

def write_file(path, content):
    with open(os.path.join(project_dir, path), "w", encoding="utf-8") as f:
        f.write(content)

# 1. OutputPanel Margin
output_panel_path = "core/ui/src/main/java/com/devtoys/android/core/ui/OutputPanel.kt"
output_panel_content = read_file(output_panel_path)
if ".padding(horizontal = 16.dp)" not in output_panel_content:
    output_panel_content = output_panel_content.replace(
        ".verticalScroll(rememberScrollState())",
        ".verticalScroll(rememberScrollState())\n                    .padding(horizontal = 16.dp)"
    )
    write_file(output_panel_path, output_panel_content)

# Tool Name mapping helper
tool_name_mapping = """
fun getToolTitle(id: String): String {
    return when(id) {
        "base64" -> "Base64 Encoder / Decoder"
        "uuid" -> "UUID Generator"
        "url" -> "URL Encoder / Decoder"
        "hex" -> "Hex Encoder / Decoder"
        "json-formatter" -> "JSON Formatter"
        "regex" -> "Regex Tester"
        "hash" -> "Hash Calculator"
        "jwt" -> "JWT Decoder"
        "timestamp" -> "Timestamp Converter"
        "case" -> "Case Converter"
        "html-entity" -> "HTML Entity Encoder"
        "rot13" -> "ROT13 / Caesar Cipher"
        "qr-gen" -> "QR Code Generator"
        "text-diff" -> "Text Diff"
        "line-tools" -> "Line Tools"
        "yaml-json" -> "YAML ↔ JSON"
        "xml-formatter" -> "XML Formatter"
        "password-gen" -> "Password Generator"
        "hmac" -> "HMAC Calculator"
        "cron" -> "Cron Builder"
        "color-picker" -> "Color Picker"
        "markdown" -> "Markdown Previewer"
        "cheatsheet" -> "Cheatsheets"
        "http-status" -> "HTTP Status Codes"
        "sql-formatter" -> "SQL Formatter"
        "csv-viewer" -> "CSV Viewer"
        "json-types" -> "JSON to Types"
        "base-converter" -> "Base Converter"
        "lorem-ipsum" -> "Lorem Ipsum Generator"
        else -> id.replace("-", " ").replaceFirstChar { it.uppercase() }
    }
}
"""

# 2 & 3 & 5. Update ToolsListScreen
tools_list_path = "app/src/main/java/com/devtoys/android/ui/ToolsListScreen.kt"
tls_content = """package com.devtoys.android.ui

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
import com.devtoys.android.data.SettingsRepository
import com.devtoys.android.tools.base.ToolRegistry
import kotlinx.coroutines.launch

%s

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsListScreen(
    toolRegistry: ToolRegistry,
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    onToolClick: (com.devtoys.android.tools.base.Tool) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val allTools = toolRegistry.getAllTools()
    val favorites by settingsRepository.favoriteTools.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()

    val filteredTools = allTools.filter { tool ->
        getToolTitle(tool.descriptor.id).contains(searchQuery, ignoreCase = true) ||
        tool.descriptor.keywords.any { it.contains(searchQuery, ignoreCase = true) }
    }

    val groupedTools = filteredTools.groupBy { it.descriptor.category }
    val expandedCategories = remember { mutableStateMapOf<String, Boolean>() }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            placeholder = { Text("Search tools...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            singleLine = true
        )
        
        LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
            groupedTools.forEach { (category, tools) ->
                val categoryName = category.name.replace("_", " ")
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
}""" % tool_name_mapping
write_file(tools_list_path, tls_content)

# Update FavoritesScreen
favorites_path = "app/src/main/java/com/devtoys/android/ui/FavoritesScreen.kt"
fav_content = """package com.devtoys.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devtoys.android.data.SettingsRepository
import com.devtoys.android.tools.base.ToolRegistry
import kotlinx.coroutines.launch

@Composable
fun FavoritesScreen(
    toolRegistry: ToolRegistry,
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier,
    onToolClick: (com.devtoys.android.tools.base.Tool) -> Unit
) {
    val favorites by settingsRepository.favoriteTools.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()
    val allTools = toolRegistry.getAllTools()
    
    val favoriteToolsList = allTools.filter { favorites.contains(it.descriptor.id) }
    
    if (favoriteToolsList.isEmpty()) {
        Text("No favorites added yet. Click the heart icon on any tool to add it here.", modifier = modifier.padding(16.dp))
    } else {
        LazyColumn(modifier = modifier.fillMaxSize()) {
            items(favoriteToolsList) { tool ->
                ListItem(
                    headlineContent = { Text(getToolTitle(tool.descriptor.id)) },
                    leadingContent = { Icon(tool.descriptor.icon, contentDescription = null) },
                    trailingContent = {
                        IconButton(onClick = { scope.launch { settingsRepository.toggleFavorite(tool.descriptor.id) } }) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Remove Favorite",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    modifier = Modifier.clickable { onToolClick(tool) }
                )
            }
        }
    }
}"""
write_file(favorites_path, fav_content)

# Update MainScreen to hoist scroll state
main_screen_path = "app/src/main/java/com/devtoys/android/ui/MainScreen.kt"
main_content = """package com.devtoys.android.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.devtoys.android.data.SettingsRepository
import com.devtoys.android.tools.base.ToolRegistry

@Composable
fun MainScreen(toolRegistry: ToolRegistry, settingsRepository: SettingsRepository) {
    var selectedTab by remember { mutableStateOf(0) }
    var activeTool by remember { mutableStateOf<com.devtoys.android.tools.base.Tool?>(null) }
    val listState = rememberLazyListState()
    
    Surface(color = MaterialTheme.colorScheme.background) {
        if (activeTool != null) {
            activeTool?.Content(modifier = Modifier)
            androidx.activity.compose.BackHandler { activeTool = null }
        } else {
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = { Icon(Icons.Default.Build, contentDescription = "Tools") },
                            label = { Text("Tools") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = { Icon(Icons.Default.Favorite, contentDescription = "Favorites") },
                            label = { Text("Favorites") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                            label = { Text("Settings") }
                        )
                    }
                }
            ) { paddingValues ->
                val modifier = Modifier.padding(paddingValues)
                when (selectedTab) {
                    0 -> ToolsListScreen(
                        toolRegistry = toolRegistry,
                        settingsRepository = settingsRepository,
                        modifier = modifier,
                        listState = listState,
                        onToolClick = { tool -> activeTool = tool }
                    )
                    1 -> FavoritesScreen(
                        toolRegistry = toolRegistry,
                        settingsRepository = settingsRepository,
                        modifier = modifier,
                        onToolClick = { tool -> activeTool = tool }
                    )
                    2 -> SettingsScreen(
                        settingsRepository = settingsRepository,
                        modifier = modifier
                    )
                }
            }
        }
    }
}"""
write_file(main_screen_path, main_content)

# 4. SettingsRepository with Accent Color
settings_repo_path = "app/src/main/java/com/devtoys/android/data/SettingsRepository.kt"
repo_content = read_file(settings_repo_path)
if "ACCENT_COLOR_KEY" not in repo_content:
    repo_content = repo_content.replace(
        "val themeMode: Flow<Int> =",
        "private val ACCENT_COLOR_KEY = intPreferencesKey(\"accent_color\")\n    val themeMode: Flow<Int> ="
    )
    repo_content = repo_content.replace(
        "val favoriteTools",
        "val accentColor: Flow<Int> = context.dataStore.data.map { it[ACCENT_COLOR_KEY] ?: 0 }\n    suspend fun setAccentColor(color: Int) { context.dataStore.edit { it[ACCENT_COLOR_KEY] = color } }\n    val favoriteTools"
    )
    write_file(settings_repo_path, repo_content)

# Update SettingsScreen
settings_screen_path = "app/src/main/java/com/devtoys/android/ui/SettingsScreen.kt"
settings_screen_content = """package com.devtoys.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devtoys.android.data.SettingsRepository
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier
) {
    val themeMode by settingsRepository.themeMode.collectAsState(initial = 0)
    val accentColor by settingsRepository.accentColor.collectAsState(initial = 0)
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Theme", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        
        val themeOptions = listOf("Follow system", "Light", "Dark")
        themeOptions.forEachIndexed { index, title ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable {
                    scope.launch { settingsRepository.setThemeMode(index) }
                }.padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = themeMode == index,
                    onClick = { scope.launch { settingsRepository.setThemeMode(index) } }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(title)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Accent Color", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        
        val accentOptions = listOf("Dynamic (System Default)", "Blue", "Purple", "Green", "Orange")
        accentOptions.forEachIndexed { index, title ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable {
                    scope.launch { settingsRepository.setAccentColor(index) }
                }.padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = accentColor == index,
                    onClick = { scope.launch { settingsRepository.setAccentColor(index) } }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(title)
            }
        }
    }
}"""
write_file(settings_screen_path, settings_screen_content)

# Update MainActivity for dynamic accent color
main_activity_path = "app/src/main/java/com/devtoys/android/MainActivity.kt"
main_activity_content = """package com.devtoys.android

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.devtoys.android.data.SettingsRepository
import com.devtoys.android.tools.base.ToolRegistry
import com.devtoys.android.ui.MainScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var toolRegistry: ToolRegistry
    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by settingsRepository.themeMode.collectAsState(initial = 0)
            val accentColor by settingsRepository.accentColor.collectAsState(initial = 0)
            val isDarkTheme = when (themeMode) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }
            
            val context = LocalContext.current
            val colorScheme = when (accentColor) {
                0 -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                } else {
                    if (isDarkTheme) darkColorScheme() else lightColorScheme()
                }
                1 -> if (isDarkTheme) darkColorScheme(primary = Color(0xFF2196F3)) else lightColorScheme(primary = Color(0xFF2196F3))
                2 -> if (isDarkTheme) darkColorScheme(primary = Color(0xFF673AB7)) else lightColorScheme(primary = Color(0xFF673AB7))
                3 -> if (isDarkTheme) darkColorScheme(primary = Color(0xFF4CAF50)) else lightColorScheme(primary = Color(0xFF4CAF50))
                4 -> if (isDarkTheme) darkColorScheme(primary = Color(0xFFFF9800)) else lightColorScheme(primary = Color(0xFFFF9800))
                else -> if (isDarkTheme) darkColorScheme() else lightColorScheme()
            }

            MaterialTheme(colorScheme = colorScheme) {
                MainScreen(toolRegistry = toolRegistry, settingsRepository = settingsRepository)
            }
        }
    }
}"""
write_file(main_activity_path, main_activity_content)

# 6. HTML Entity Tool Expansion
html_tool_path = "tools/encoders/src/main/java/com/devtoys/android/tools/encoders/HtmlEntityTool.kt"
html_content = """package com.devtoys.android.tools.encoders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devtoys.android.core.ui.*
import com.devtoys.android.tools.base.*

class HtmlEntityTool : Tool {
    override val descriptor = ToolDescriptor("html-entity", android.R.string.untitled, android.R.string.untitled, Icons.Default.Build, ToolCategory.ENCODERS_DECODERS, listOf("html", "entity", "encode"))
    
    @Composable override fun Content(modifier: Modifier) {
        var tabIndex by remember { mutableStateOf(0) }
        
        ToolScaffold("HTML Entity", modifier = modifier) {
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
        var input by remember { mutableStateOf("") }
        var isEncodeMode by remember { mutableStateOf(true) }
        val output = remember(input, isEncodeMode) {
            if (input.isEmpty()) "" else if (isEncodeMode) {
                input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;")
            } else {
                input.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'").replace("&#x27;", "'")
            }
        }
        Text("Mode: ${if (isEncodeMode) "Encode" else "Decode"}")
        Switch(checked = isEncodeMode, onCheckedChange = { isEncodeMode = it })
        Spacer(modifier = Modifier.height(16.dp))
        MonoTextField(input, { input = it }, "Input")
        Spacer(modifier = Modifier.height(16.dp))
        OutputPanel(output)
    }

    @Composable fun EntityList() {
        val categories = mapOf(
            "Arrows" to listOf("←" to "&larr;", "→" to "&rarr;", "↑" to "&uarr;", "↓" to "&darr;", "↔" to "&harr;"),
            "Currency" to listOf("¢" to "&cent;", "£" to "&pound;", "¥" to "&yen;", "€" to "&euro;"),
            "Math" to listOf("±" to "&plusmn;", "×" to "&times;", "÷" to "&divide;", "∞" to "&infin;", "≈" to "&asymp;", "≠" to "&ne;"),
            "Punctuation" to listOf("©" to "&copy;", "®" to "&reg;", "™" to "&trade;", "•" to "&bull;", "…" to "&hellip;")
        )
        val expandedState = remember { mutableStateMapOf<String, Boolean>() }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            categories.forEach { (catName, entities) ->
                val isExpanded = expandedState[catName] ?: true
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
                            leadingContent = { Text(entity.first, style = MaterialTheme.typography.headlineSmall) }
                        )
                    }
                }
            }
        }
    }
}"""
write_file(html_tool_path, html_content)

print("Quality upgrades applied successfully.")
