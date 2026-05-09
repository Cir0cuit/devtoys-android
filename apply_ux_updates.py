import os

project_dir = "C:/Users/barti/.gemini/antigravity/scratch/devtoys-android"

# Add datastore dependency
app_build_file = os.path.join(project_dir, "app/build.gradle.kts")
with open(app_build_file, "r", encoding="utf-8") as f:
    app_build_content = f.read()

if "androidx.datastore:datastore-preferences" not in app_build_content:
    app_build_content = app_build_content.replace(
        "dependencies {",
        "dependencies {\n    implementation(\"androidx.datastore:datastore-preferences:1.0.0\")"
    )
    with open(app_build_file, "w", encoding="utf-8") as f:
        f.write(app_build_content)

files = {
    "app/src/main/java/com/devtoys/android/data/SettingsRepository.kt": """package com.devtoys.android.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val THEME_MODE_KEY = intPreferencesKey("theme_mode")
    private val FAVORITES_KEY = stringSetPreferencesKey("favorites")

    // 0 = System, 1 = Light, 2 = Dark
    val themeMode: Flow<Int> = context.dataStore.data.map { it[THEME_MODE_KEY] ?: 0 }

    suspend fun setThemeMode(mode: Int) {
        context.dataStore.edit { it[THEME_MODE_KEY] = mode }
    }

    val favoriteTools: Flow<Set<String>> = context.dataStore.data.map { it[FAVORITES_KEY] ?: emptySet() }

    suspend fun toggleFavorite(toolId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[FAVORITES_KEY] ?: emptySet()
            val newSet = if (current.contains(toolId)) current - toolId else current + toolId
            prefs[FAVORITES_KEY] = newSet
        }
    }
}""",
    
    "app/src/main/java/com/devtoys/android/MainActivity.kt": """package com.devtoys.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
            val isDarkTheme = when (themeMode) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }
            MaterialTheme(
                colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()
            ) {
                MainScreen(toolRegistry = toolRegistry, settingsRepository = settingsRepository)
            }
        }
    }
}""",

    "app/src/main/java/com/devtoys/android/ui/MainScreen.kt": """package com.devtoys.android.ui

import androidx.compose.foundation.layout.padding
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
}""",

    "app/src/main/java/com/devtoys/android/ui/ToolsListScreen.kt": """package com.devtoys.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsListScreen(
    toolRegistry: ToolRegistry,
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier,
    onToolClick: (com.devtoys.android.tools.base.Tool) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val allTools = toolRegistry.getAllTools()
    val favorites by settingsRepository.favoriteTools.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()

    val filteredTools = allTools.filter { tool ->
        tool.descriptor.id.contains(searchQuery, ignoreCase = true) ||
        tool.descriptor.keywords.any { it.contains(searchQuery, ignoreCase = true) }
    }

    val groupedTools = filteredTools.groupBy { it.descriptor.category }
    val expandedCategories = remember { mutableStateMapOf<String, Boolean>() }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("Search tools...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            singleLine = true
        )
        
        LazyColumn(modifier = Modifier.fillMaxSize()) {
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
                    items(tools) { tool ->
                        val isFavorite = favorites.contains(tool.descriptor.id)
                        ListItem(
                            headlineContent = { Text(tool.descriptor.id.replace("-", " ").replaceFirstChar { it.uppercase() }) },
                            supportingContent = { Text(tool.descriptor.category.name) },
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
}""",

    "app/src/main/java/com/devtoys/android/ui/FavoritesScreen.kt": """package com.devtoys.android.ui

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
                    headlineContent = { Text(tool.descriptor.id.replace("-", " ").replaceFirstChar { it.uppercase() }) },
                    supportingContent = { Text(tool.descriptor.category.name) },
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
}""",

    "app/src/main/java/com/devtoys/android/ui/SettingsScreen.kt": """package com.devtoys.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Appearance", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        
        Text("Theme")
        val options = listOf("Follow system", "Light", "Dark")
        Column {
            options.forEachIndexed { index, title ->
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
        }
    }
}"""
}

for path, content in files.items():
    full_path = os.path.join(project_dir, path)
    os.makedirs(os.path.dirname(full_path), exist_ok=True)
    with open(full_path, "w", encoding="utf-8") as f:
        f.write(content)

print("Favorites, Settings, and Expandable Tool Categories applied successfully.")
