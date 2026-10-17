package com.cir0cuit.devtoys.ui

import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.cir0cuit.devtoys.core.ui.readTextFile
import com.cir0cuit.devtoys.tools.base.ShareKind
import com.cir0cuit.devtoys.tools.base.extension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.LocalToolChrome
import com.cir0cuit.devtoys.core.ui.ToolChrome
import com.cir0cuit.devtoys.data.SettingsRepository
import com.cir0cuit.devtoys.tools.base.LocalSharedContent
import com.cir0cuit.devtoys.tools.base.SharedContent
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.kind
import com.cir0cuit.devtoys.tools.base.ToolRegistry
import kotlinx.coroutines.launch

private data class Tab(val label: String, val selectedIcon: ImageVector, val icon: ImageVector)

private val tabs = listOf(
    Tab("Tools", Icons.Filled.Build, Icons.Outlined.Build),
    Tab("Favorites", Icons.Filled.Star, Icons.Filled.StarBorder),
    Tab("Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
)

@Composable
fun MainScreen(
    toolRegistry: ToolRegistry,
    settingsRepository: SettingsRepository,
    initialToolId: String? = null,
    shared: SharedContent? = null,
    onShareDone: () -> Unit = {},
) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var activeToolId by rememberSaveable { mutableStateOf(initialToolId?.takeIf { toolRegistry.getToolById(it) != null }) }
    val activeTool = activeToolId?.let { toolRegistry.getToolById(it) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val saveableStateHolder = rememberSaveableStateHolder()
    val favorites by settingsRepository.favoriteTools.collectAsState()
    val scope = rememberCoroutineScope()

    // Tool the current share was handed to; it keeps seeing the content until it is closed.
    var shareTarget by remember { mutableStateOf<String?>(null) }
    // What that tool receives: the share itself, or a shared file read as text for tools that only take text.
    var delivered by remember { mutableStateOf<SharedContent?>(null) }
    val context = LocalContext.current

    val openTool: (Tool) -> Unit = { tool ->
        activeToolId = tool.descriptor.id
        scope.launch { settingsRepository.addRecent(tool.descriptor.id) }
    }

    Surface(color = MaterialTheme.colorScheme.background) {
        if (activeTool != null) {
            val id = activeTool.descriptor.id
            val close = {
                activeToolId = null
                if (shareTarget != null) {
                    shareTarget = null
                    delivered = null
                    onShareDone()
                }
            }
            val chrome = ToolChrome(
                onBack = close,
                isFavorite = id in favorites,
                onToggleFavorite = { scope.launch { settingsRepository.toggleFavorite(id) } }
            )
            CompositionLocalProvider(
                LocalToolChrome provides chrome,
                LocalSharedContent provides delivered?.takeIf { shareTarget == id }
            ) {
                saveableStateHolder.SaveableStateProvider(id) {
                    activeTool.Content(modifier = Modifier)
                }
            }
            BackHandler(onBack = close)
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
        if (shared != null && shareTarget == null) {
            val deliver: (Tool) -> Unit = { tool ->
                val hand = { content: SharedContent ->
                    delivered = content
                    shareTarget = tool.descriptor.id
                    openTool(tool)
                }
                if (shared is SharedContent.File && ShareKind.FILE !in tool.descriptor.accepts) {
                    scope.launch {
                        withContext(Dispatchers.IO) { readTextFile(context, shared.uri, MAX_SHARED_FILE_BYTES) }
                            .onSuccess { hand(SharedContent.Text(it)) }
                            .onFailure {
                                Toast.makeText(context, "Could not open ${shared.name}: ${it.message}", Toast.LENGTH_LONG).show()
                                onShareDone()
                            }
                    }
                } else {
                    hand(shared)
                }
            }
            val candidates = remember(shared) { shareCandidates(toolRegistry.getAllTools(), shared) }
            when {
                candidates.isEmpty() -> LaunchedEffect(shared) {
                    Toast.makeText(context, "No tool can open this", Toast.LENGTH_SHORT).show()
                    onShareDone()
                }
                // A file whose type only one tool handles (for example .md) opens straight in that tool.
                shared is SharedContent.File && candidates.size == 1 -> LaunchedEffect(shared) { deliver(candidates.single()) }
                else -> ShareSheet(candidates, shared, favorites, onPick = deliver, onDismiss = onShareDone)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareSheet(tools: List<Tool>, shared: SharedContent, favorites: Set<String>, onPick: (Tool) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Text(
            when (shared) {
                is SharedContent.Image -> "Open image with"
                is SharedContent.File -> "Open ${shared.name} with"
                is SharedContent.Text -> "Open text with"
            },
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
        )
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            items(tools, key = { it.descriptor.id }) { tool ->
                ToolRow(tool.descriptor, tool.descriptor.id in favorites, onClick = { onPick(tool) }, onToggleFavorite = {})
            }
        }
    }
}

/** Largest file read as text for a text tool; keeps the tool's saved state within the Binder limit. */
private const val MAX_SHARED_FILE_BYTES = 256 * 1024

/** Tools offered for [shared]: files by extension (falling back to tools that take any file), other content by kind. */
internal fun shareCandidates(tools: List<Tool>, shared: SharedContent): List<Tool> = when (shared) {
    is SharedContent.File -> tools.filter { shared.extension in it.descriptor.fileTypes }
        .ifEmpty { tools.filter { "*" in it.descriptor.fileTypes } }
    else -> tools.filter { shared.kind in it.descriptor.accepts }
}
