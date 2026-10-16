package com.cir0cuit.devtoys.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.cir0cuit.devtoys.core.ui.LocalToolChrome
import com.cir0cuit.devtoys.core.ui.ToolChrome
import com.cir0cuit.devtoys.data.SettingsRepository
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolRegistry
import kotlinx.coroutines.launch

private data class Tab(val label: String, val selectedIcon: ImageVector, val icon: ImageVector)

private val tabs = listOf(
    Tab("Tools", Icons.Filled.Build, Icons.Outlined.Build),
    Tab("Favorites", Icons.Filled.Star, Icons.Filled.StarBorder),
    Tab("Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
)

@Composable
fun MainScreen(toolRegistry: ToolRegistry, settingsRepository: SettingsRepository, initialToolId: String? = null) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var activeToolId by rememberSaveable { mutableStateOf(initialToolId?.takeIf { toolRegistry.getToolById(it) != null }) }
    val activeTool = activeToolId?.let { toolRegistry.getToolById(it) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val saveableStateHolder = rememberSaveableStateHolder()
    val favorites by settingsRepository.favoriteTools.collectAsState()
    val scope = rememberCoroutineScope()

    val openTool: (Tool) -> Unit = { tool ->
        activeToolId = tool.descriptor.id
        scope.launch { settingsRepository.addRecent(tool.descriptor.id) }
    }

    Surface(color = MaterialTheme.colorScheme.background) {
        if (activeTool != null) {
            val id = activeTool.descriptor.id
            val chrome = ToolChrome(
                onBack = { activeToolId = null },
                isFavorite = id in favorites,
                onToggleFavorite = { scope.launch { settingsRepository.toggleFavorite(id) } }
            )
            CompositionLocalProvider(LocalToolChrome provides chrome) {
                saveableStateHolder.SaveableStateProvider(id) {
                    activeTool.Content(modifier = Modifier)
                }
            }
            BackHandler { activeToolId = null }
        } else {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.background) {
                        tabs.forEachIndexed { index, tab ->
                            val selected = selectedTab == index
                            NavigationBarItem(
                                selected = selected,
                                onClick = { selectedTab = index },
                                icon = { Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null) },
                                label = { Text(tab.label) }
                            )
                        }
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
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onToolClick = openTool
                    )
                    1 -> FavoritesScreen(
                        toolRegistry = toolRegistry,
                        settingsRepository = settingsRepository,
                        modifier = modifier,
                        onToolClick = openTool
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
