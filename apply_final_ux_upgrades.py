import os

project_dir = "C:/Users/barti/.gemini/antigravity/scratch/devtoys-android"

def read_file(path):
    with open(os.path.join(project_dir, path), "r", encoding="utf-8") as f:
        return f.read()

def write_file(path, content):
    with open(os.path.join(project_dir, path), "w", encoding="utf-8") as f:
        f.write(content)

# 1. SettingsRepository (Add AMOLED Mode)
settings_repo_path = "app/src/main/java/com/devtoys/android/data/SettingsRepository.kt"
repo_content = read_file(settings_repo_path)
if "AMOLED_MODE_KEY" not in repo_content:
    repo_content = repo_content.replace(
        "import androidx.datastore.preferences.core.intPreferencesKey",
        "import androidx.datastore.preferences.core.booleanPreferencesKey\nimport androidx.datastore.preferences.core.intPreferencesKey"
    )
    repo_content = repo_content.replace(
        "val accentColor: Flow<Int>",
        "private val AMOLED_MODE_KEY = booleanPreferencesKey(\"amoled_mode\")\n    val amoledMode: Flow<Boolean> = context.dataStore.data.map { it[AMOLED_MODE_KEY] ?: false }\n    suspend fun setAmoledMode(enabled: Boolean) { context.dataStore.edit { it[AMOLED_MODE_KEY] = enabled } }\n    val accentColor: Flow<Int>"
    )
    write_file(settings_repo_path, repo_content)

# 2. SettingsScreen (Add AMOLED Toggle)
settings_screen_path = "app/src/main/java/com/devtoys/android/ui/SettingsScreen.kt"
settings_screen_content = """package com.devtoys.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    val amoledMode by settingsRepository.amoledMode.collectAsState(initial = false)
    val scope = rememberCoroutineScope()

    val isDarkTheme = when (themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }

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
        
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth().clickable(enabled = isDarkTheme) {
                if (isDarkTheme) scope.launch { settingsRepository.setAmoledMode(!amoledMode) }
            }.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(
                checked = amoledMode,
                onCheckedChange = { scope.launch { settingsRepository.setAmoledMode(it) } },
                enabled = isDarkTheme
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("AMOLED Background", color = if (isDarkTheme) LocalContentColor.current else Color.Gray)
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

# 3. MainActivity Theme Override (Neutral Surfaces & AMOLED)
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
            val amoledMode by settingsRepository.amoledMode.collectAsState(initial = false)
            
            val isDarkTheme = when (themeMode) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }
            
            val context = LocalContext.current
            
            val neutralSurfaceDark = if (amoledMode) Color.Black else Color(0xFF1E1E1E)
            val neutralBackgroundDark = if (amoledMode) Color.Black else Color(0xFF121212)
            val neutralSurfaceVariantDark = if (amoledMode) Color(0xFF111111) else Color(0xFF2C2C2C)

            val neutralSurfaceLight = Color(0xFFFFFFFF)
            val neutralBackgroundLight = Color(0xFFFAFAFA)
            val neutralSurfaceVariantLight = Color(0xFFE0E0E0)

            val baseDark = darkColorScheme(
                background = neutralBackgroundDark,
                surface = neutralSurfaceDark,
                surfaceVariant = neutralSurfaceVariantDark
            )
            val baseLight = lightColorScheme(
                background = neutralBackgroundLight,
                surface = neutralSurfaceLight,
                surfaceVariant = neutralSurfaceVariantLight
            )

            val colorScheme = when (accentColor) {
                0 -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val dynamic = if (isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                    if (amoledMode && isDarkTheme) dynamic.copy(background = Color.Black, surface = Color.Black, surfaceVariant = Color(0xFF111111)) else dynamic
                } else {
                    if (isDarkTheme) baseDark else baseLight
                }
                1 -> if (isDarkTheme) baseDark.copy(primary = Color(0xFF2196F3)) else baseLight.copy(primary = Color(0xFF2196F3))
                2 -> if (isDarkTheme) baseDark.copy(primary = Color(0xFF673AB7)) else baseLight.copy(primary = Color(0xFF673AB7))
                3 -> if (isDarkTheme) baseDark.copy(primary = Color(0xFF4CAF50)) else baseLight.copy(primary = Color(0xFF4CAF50))
                4 -> if (isDarkTheme) baseDark.copy(primary = Color(0xFFFF9800)) else baseLight.copy(primary = Color(0xFFFF9800))
                else -> if (isDarkTheme) baseDark else baseLight
            }

            MaterialTheme(colorScheme = colorScheme) {
                MainScreen(toolRegistry = toolRegistry, settingsRepository = settingsRepository)
            }
        }
    }
}"""
write_file(main_activity_path, main_activity_content)

# 4. CheatsheetTool Refactor (Expand and Organize)
cheatsheet_path = "tools/reference/src/main/java/com/devtoys/android/tools/reference/CheatsheetTool.kt"
cheatsheet_content = """package com.devtoys.android.tools.reference

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devtoys.android.core.ui.*
import com.devtoys.android.tools.base.*

class CheatsheetTool : Tool {
    override val descriptor = ToolDescriptor("cheatsheet", android.R.string.untitled, android.R.string.untitled, Icons.Default.Build, ToolCategory.REFERENCE, listOf("cheat", "sheet", "commands"))
    
    @Composable override fun Content(modifier: Modifier) {
        var selected by remember { mutableStateOf("Git") }
        val clipboardManager = LocalClipboardManager.current
        val context = LocalContext.current
        
        val cheatsheets = mapOf(
            "Git" to mapOf(
                "Basics" to listOf("Clone" to "git clone <url>", "Init" to "git init", "Add all" to "git add .", "Commit" to "git commit -m \\"Msg\\"", "Status" to "git status", "Log" to "git log"),
                "Branching" to listOf("Create branch" to "git checkout -b <name>", "Switch" to "git checkout <name>", "List" to "git branch", "Merge" to "git merge <branch>"),
                "Remote" to listOf("Push" to "git push origin <branch>", "Pull" to "git pull", "Add remote" to "git remote add origin <url>")
            ),
            "Linux / Bash" to mapOf(
                "File System" to listOf("List" to "ls -la", "Change dir" to "cd <path>", "Show dir" to "pwd", "Make dir" to "mkdir <dir>", "Remove dir" to "rm -r <dir>", "Copy" to "cp <src> <dest>", "Move" to "mv <src> <dest>", "Find" to "find . -name <file>"),
                "Permissions" to listOf("Make executable" to "chmod +x <file>", "Change owner" to "chown user:group <file>"),
                "Archives" to listOf("Create Tar" to "tar -czvf archive.tar.gz /dir", "Extract Tar" to "tar -xzvf archive.tar.gz", "Unzip" to "unzip archive.zip")
            ),
            "Docker" to mapOf(
                "Containers" to listOf("Run image" to "docker run <image>", "Run detached" to "docker run -d <image>", "List running" to "docker ps", "List all" to "docker ps -a", "Stop" to "docker stop <id>", "Remove" to "docker rm <id>"),
                "Images" to listOf("List images" to "docker images", "Remove image" to "docker rmi <id>", "Build" to "docker build -t <name> .", "Pull" to "docker pull <image>")
            ),
            "Kubernetes" to mapOf(
                "Pods" to listOf("Get pods" to "kubectl get pods", "Describe" to "kubectl describe pod <name>", "Delete" to "kubectl delete pod <name>", "Logs" to "kubectl logs <name>"),
                "Deployments" to listOf("Get deployments" to "kubectl get deployments", "Scale" to "kubectl scale deployment <name> --replicas=3", "Rollout status" to "kubectl rollout status deployment <name>")
            ),
            "Regex" to mapOf(
                "Characters" to listOf("Any character" to ".", "Digit" to "\\\\d", "Word character" to "\\\\w", "Whitespace" to "\\\\s", "Non-digit" to "\\\\D"),
                "Quantifiers" to listOf("Zero or more" to "*", "One or more" to "+", "Zero or one" to "?", "Exactly N times" to "{N}"),
                "Anchors" to listOf("Start of string" to "^", "End of string" to "$")
            ),
            "Vim" to mapOf(
                "Basics" to listOf("Insert Mode" to "i", "Normal Mode" to "Esc", "Save" to ":w", "Quit" to ":q", "Save & Quit" to ":wq", "Force Quit" to ":q!"),
                "Editing" to listOf("Delete line" to "dd", "Undo" to "u", "Redo" to "Ctrl+r", "Copy line" to "yy", "Paste" to "p")
            )
        )
        
        val expandedState = remember { mutableStateMapOf<String, Boolean>() }
        
        ToolScaffold("Cheatsheets", modifier = modifier) {
            LazyRow(modifier = Modifier.fillMaxWidth()) {
                items(cheatsheets.keys.size) { idx ->
                    val key = cheatsheets.keys.toList()[idx]
                    FilterChip(
                        selected = selected == key,
                        onClick = { selected = key },
                        label = { Text(key) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            val currentToolCats = cheatsheets[selected] ?: emptyMap()
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                currentToolCats.forEach { (catName, commands) ->
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
                        items(commands) { cmd ->
                            ListItem(
                                headlineContent = { Text(cmd.second, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.primary) },
                                supportingContent = { Text(cmd.first) },
                                trailingContent = {
                                    IconButton(onClick = {
                                        clipboardManager.setText(AnnotatedString(cmd.second))
                                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(Icons.Default.Share, contentDescription = "Copy")
                                    }
                                }
                            )
                            Divider()
                        }
                    }
                }
            }
        }
    }
}"""
write_file(cheatsheet_path, cheatsheet_content)

print("Color schemes patched and Cheatsheets expanded.")
