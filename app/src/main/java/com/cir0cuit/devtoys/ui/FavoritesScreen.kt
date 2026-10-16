package com.cir0cuit.devtoys.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.data.SettingsRepository
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolRegistry
import kotlinx.coroutines.launch

@Composable
fun FavoritesScreen(
    toolRegistry: ToolRegistry,
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier,
    onToolClick: (Tool) -> Unit
) {
    val favorites by settingsRepository.favoriteTools.collectAsState()
    val scope = rememberCoroutineScope()
    val favoriteTools = toolRegistry.getAllTools().filter { it.descriptor.id in favorites }

    if (favoriteTools.isEmpty()) {
        Column(modifier = modifier.fillMaxSize()) {
            ScreenTitle("Favorites")
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.StarBorder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    "No favorites yet",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Text(
                    "Open a tool and tap the star, or long-press it in the list.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    } else {
        LazyColumn(modifier = modifier.fillMaxSize()) {
            item(key = "title") { ScreenTitle("Favorites") }
            items(favoriteTools, key = { it.descriptor.id }) { tool ->
                ToolRow(
                    descriptor = tool.descriptor,
                    isFavorite = true,
                    onClick = { onToolClick(tool) },
                    onToggleFavorite = { scope.launch { settingsRepository.toggleFavorite(tool.descriptor.id) } }
                )
            }
        }
    }
}
