import os

repo_path = r'C:\Users\barti\.gemini\antigravity\scratch\devtoys-android\app\src\main\java\com\devtoys\android\data\SettingsRepository.kt'
screen_path = r'C:\Users\barti\.gemini\antigravity\scratch\devtoys-android\app\src\main\java\com\devtoys\android\ui\SettingsScreen.kt'

with open(repo_path, 'r', encoding='utf-8') as f:
    repo_content = f.read()

# Inject Settings
repo_additions = """
    private val AUTO_PASTE_KEY = booleanPreferencesKey("auto_paste")
    val autoPaste: Flow<Boolean> = context.dataStore.data.map { it[AUTO_PASTE_KEY] ?: false }
    suspend fun setAutoPaste(enabled: Boolean) { context.dataStore.edit { it[AUTO_PASTE_KEY] = enabled } }

    private val AUTO_COPY_KEY = booleanPreferencesKey("auto_copy")
    val autoCopy: Flow<Boolean> = context.dataStore.data.map { it[AUTO_COPY_KEY] ?: false }
    suspend fun setAutoCopy(enabled: Boolean) { context.dataStore.edit { it[AUTO_COPY_KEY] = enabled } }

    private val COMPACT_MODE_KEY = booleanPreferencesKey("compact_mode")
    val compactMode: Flow<Boolean> = context.dataStore.data.map { it[COMPACT_MODE_KEY] ?: false }
    suspend fun setCompactMode(enabled: Boolean) { context.dataStore.edit { it[COMPACT_MODE_KEY] = enabled } }

    private val HAPTIC_FEEDBACK_KEY = booleanPreferencesKey("haptic_feedback")
    val hapticFeedback: Flow<Boolean> = context.dataStore.data.map { it[HAPTIC_FEEDBACK_KEY] ?: true }
    suspend fun setHapticFeedback(enabled: Boolean) { context.dataStore.edit { it[HAPTIC_FEEDBACK_KEY] = enabled } }
"""

repo_content = repo_content.replace('suspend fun setAccentColor(color: Int) { context.dataStore.edit { it[ACCENT_COLOR_KEY] = color } }', 'suspend fun setAccentColor(color: Int) { context.dataStore.edit { it[ACCENT_COLOR_KEY] = color } }\n' + repo_additions)

with open(repo_path, 'w', encoding='utf-8') as f:
    f.write(repo_content)

with open(screen_path, 'r', encoding='utf-8') as f:
    screen_content = f.read()

screen_additions = """
        val autoPaste by viewModel.autoPaste.collectAsState()
        val autoCopy by viewModel.autoCopy.collectAsState()
        val compactMode by viewModel.compactMode.collectAsState()
        val hapticFeedback by viewModel.hapticFeedback.collectAsState()
        
        Text("Behaviors", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))
        ListItem(
            headlineContent = { Text("Auto Paste") },
            supportingContent = { Text("Automatically paste from clipboard when opening a tool") },
            trailingContent = { Switch(checked = autoPaste, onCheckedChange = { viewModel.setAutoPaste(it) }) }
        )
        ListItem(
            headlineContent = { Text("Auto Copy") },
            supportingContent = { Text("Automatically copy generated results to clipboard") },
            trailingContent = { Switch(checked = autoCopy, onCheckedChange = { viewModel.setAutoCopy(it) }) }
        )
        Divider(modifier = Modifier.padding(vertical = 16.dp))
        
        Text("Customizations", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))
        ListItem(
            headlineContent = { Text("Compact Mode") },
            supportingContent = { Text("Reduce paddings to fit more content on screen") },
            trailingContent = { Switch(checked = compactMode, onCheckedChange = { viewModel.setCompactMode(it) }) }
        )
        ListItem(
            headlineContent = { Text("Haptic Feedback") },
            supportingContent = { Text("Vibrate on interactions and tool outputs") },
            trailingContent = { Switch(checked = hapticFeedback, onCheckedChange = { viewModel.setHapticFeedback(it) }) }
        )
        Divider(modifier = Modifier.padding(vertical = 16.dp))
"""

if 'val themeMode by viewModel.themeMode' in screen_content:
    screen_content = screen_content.replace('val themeMode by viewModel.themeMode.collectAsState()', 'val themeMode by viewModel.themeMode.collectAsState()\n        val autoPaste by viewModel.autoPaste.collectAsState()\n        val autoCopy by viewModel.autoCopy.collectAsState()\n        val compactMode by viewModel.compactMode.collectAsState()\n        val hapticFeedback by viewModel.hapticFeedback.collectAsState()')

    screen_content = screen_content.replace('Text("Appearance", style = MaterialTheme.typography.titleMedium', 'Text("Behaviors", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))\n        ListItem(headlineContent = { Text("Auto Paste") }, supportingContent = { Text("Automatically paste from clipboard when opening a tool") }, trailingContent = { Switch(checked = autoPaste, onCheckedChange = { viewModel.setAutoPaste(it) }) })\n        ListItem(headlineContent = { Text("Auto Copy") }, supportingContent = { Text("Automatically copy generated results to clipboard") }, trailingContent = { Switch(checked = autoCopy, onCheckedChange = { viewModel.setAutoCopy(it) }) })\n        Divider(modifier = Modifier.padding(vertical = 16.dp))\n        \n        Text("Customizations", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))\n        ListItem(headlineContent = { Text("Compact Mode") }, supportingContent = { Text("Reduce paddings to fit more content on screen") }, trailingContent = { Switch(checked = compactMode, onCheckedChange = { viewModel.setCompactMode(it) }) })\n        ListItem(headlineContent = { Text("Haptic Feedback") }, supportingContent = { Text("Vibrate on interactions and tool outputs") }, trailingContent = { Switch(checked = hapticFeedback, onCheckedChange = { viewModel.setHapticFeedback(it) }) })\n        Divider(modifier = Modifier.padding(vertical = 16.dp))\n        \n        Text("Appearance", style = MaterialTheme.typography.titleMedium')

with open(screen_path, 'w', encoding='utf-8') as f:
    f.write(screen_content)

vm_path = r'C:\Users\barti\.gemini\antigravity\scratch\devtoys-android\app\src\main\java\com\devtoys\android\ui\SettingsViewModel.kt'
with open(vm_path, 'r', encoding='utf-8') as f:
    vm_content = f.read()

vm_additions = """
    val autoPaste = repository.autoPaste.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val autoCopy = repository.autoCopy.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val compactMode = repository.compactMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val hapticFeedback = repository.hapticFeedback.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setAutoPaste(enabled: Boolean) { viewModelScope.launch { repository.setAutoPaste(enabled) } }
    fun setAutoCopy(enabled: Boolean) { viewModelScope.launch { repository.setAutoCopy(enabled) } }
    fun setCompactMode(enabled: Boolean) { viewModelScope.launch { repository.setCompactMode(enabled) } }
    fun setHapticFeedback(enabled: Boolean) { viewModelScope.launch { repository.setHapticFeedback(enabled) } }
"""

if 'fun setAccentColor' in vm_content:
    vm_content = vm_content.replace('fun setAccentColor(color: Int) {', vm_additions + '\n    fun setAccentColor(color: Int) {')
    
with open(vm_path, 'w', encoding='utf-8') as f:
    f.write(vm_content)
    
print("Updated settings successfully")
