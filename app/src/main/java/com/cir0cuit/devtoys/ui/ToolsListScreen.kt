package com.cir0cuit.devtoys.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.designsystem.LocalSlabColor
import com.cir0cuit.devtoys.data.SettingsRepository
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolRegistry
import kotlinx.coroutines.launch

internal fun Tool.matches(query: String): Boolean = with(descriptor) {
    title.contains(query, ignoreCase = true) ||
        description.contains(query, ignoreCase = true) ||
        keywords.any { it.contains(query, ignoreCase = true) }
}

@Composable
fun ToolsListScreen(
    toolRegistry: ToolRegistry,
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onToolClick: (Tool) -> Unit
) {
    val allTools = toolRegistry.getAllTools()
    val favorites by settingsRepository.favoriteTools.collectAsState()
    val recentIds by settingsRepository.recentTools.collectAsState()
    val scope = rememberCoroutineScope()
    val query = searchQuery.trim()

    @Composable
    fun ToolItem(tool: Tool) = ToolRow(
        descriptor = tool.descriptor,
        isFavorite = tool.descriptor.id in favorites,
        onClick = { onToolClick(tool) },
        onToggleFavorite = { scope.launch { settingsRepository.toggleFavorite(tool.descriptor.id) } }
    )

    LazyColumn(modifier = modifier.fillMaxSize(), state = listState) {
        item(key = "title") { ScreenTitle("DevToys") }
        item(key = "search") {
            SearchField(searchQuery, onSearchQueryChange, toolCount = allTools.size)
        }

        if (query.isNotEmpty()) {
            val results = allTools.filter { it.matches(query) }
            if (results.isEmpty()) {
                item(key = "empty") {
                    Text(
                        "No tools match “$query”. Try a shorter word.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
                    )
                }
            }
            items(results, key = { it.descriptor.id }) { ToolItem(it) }
        } else {
            val recents = recentIds.mapNotNull { toolRegistry.getToolById(it) }
            if (recents.isNotEmpty()) {
                item(key = "recent-header") { SectionHeader("Recent") }
                item(key = "recent-row") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(recents, key = { it.descriptor.id }) { tool ->
                            RecentTile(tool) { onToolClick(tool) }
                        }
                    }
                }
            }
            allTools.groupBy { it.descriptor.category }.toSortedMap().forEach { (category, tools) ->
                item(key = "header-${category.name}") {
                    SectionHeader(category.label, count = tools.size)
                }
                items(tools, key = { it.descriptor.id }) { ToolItem(it) }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, toolCount: Int) {
    val slab = LocalSlabColor.current
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text("Search $toolCount tools") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                }
            }
        } else null,
        singleLine = true,
        shape = MaterialTheme.shapes.extraLarge,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = slab,
            unfocusedContainerColor = slab,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        )
    )
}

@Composable
private fun RecentTile(tool: Tool, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(112.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Icon(
            tool.descriptor.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Text(
            tool.descriptor.title,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
internal fun ScreenTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.displaySmall,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
internal fun SectionHeader(text: String, count: Int? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        if (count != null) {
            Text(
                count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
