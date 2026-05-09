package com.cir0cuit.devtoys.ui

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
import com.cir0cuit.devtoys.data.SettingsRepository
import com.cir0cuit.devtoys.tools.base.ToolRegistry
import kotlinx.coroutines.launch

@Composable
fun FavoritesScreen(
    toolRegistry: ToolRegistry,
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier,
    onToolClick: (com.cir0cuit.devtoys.tools.base.Tool) -> Unit
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
}