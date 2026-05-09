package com.cir0cuit.devtoys.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.cir0cuit.devtoys.data.SettingsRepository
import com.cir0cuit.devtoys.tools.base.ToolRegistry

@Composable
fun MainScreen(toolRegistry: ToolRegistry, settingsRepository: SettingsRepository) {
    var selectedTab by remember { mutableStateOf(0) }
    var activeTool by remember { mutableStateOf<com.cir0cuit.devtoys.tools.base.Tool?>(null) }
    val listState = rememberLazyListState()
    val expandedCategories = remember { mutableStateMapOf<String, Boolean>() }
    val saveableStateHolder = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    
    Surface(color = MaterialTheme.colorScheme.background) {
        if (activeTool != null) {
            saveableStateHolder.SaveableStateProvider(activeTool!!.descriptor.id) {
                activeTool!!.Content(modifier = Modifier)
            }
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
                        expandedCategories = expandedCategories,
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
}