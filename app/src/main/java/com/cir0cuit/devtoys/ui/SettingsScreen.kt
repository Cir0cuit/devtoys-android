package com.cir0cuit.devtoys.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cir0cuit.devtoys.core.designsystem.AccentPresets
import com.cir0cuit.devtoys.data.SettingsRepository
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import kotlinx.coroutines.launch

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, url, Toast.LENGTH_LONG).show()
    }
}

@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier
) {
    val themeMode by settingsRepository.themeMode.collectAsState()
    val accentColor by settingsRepository.accentColor.collectAsState()
    val amoledMode by settingsRepository.amoledMode.collectAsState()
    val highContrast by settingsRepository.highContrast.collectAsState()
    val hideStatusBar by settingsRepository.hideStatusBar.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val isDarkTheme = when (themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
    }

    var showLicenses by remember { mutableStateOf(false) }
    var displayedLegalText by remember { mutableStateOf<Pair<String, String>?>(null) }

    displayedLegalText?.let { (legalTitle, legalBody) ->
        FullScreenDialog(title = legalTitle, onClose = { displayedLegalText = null }) {
            SelectionContainer(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(text = legalBody, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    if (showLicenses) {
        FullScreenDialog(title = "Open source licenses", onClose = { showLicenses = false }) {
            LibrariesContainer(modifier = Modifier.fillMaxSize())
        }
    }

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenTitle("Settings")

        SectionHeader("Appearance")
        SettingLabel("Theme")
        ThemeSelector(themeMode) { scope.launch { settingsRepository.setThemeMode(it) } }
        SettingLabel("Accent")
        AccentSelector(accentColor) { scope.launch { settingsRepository.setAccentColor(it) } }
        SwitchRow(
            title = "AMOLED black",
            supporting = if (isDarkTheme) "Pure black background in dark theme" else "Available in dark theme",
            checked = amoledMode,
            enabled = isDarkTheme
        ) { scope.launch { settingsRepository.setAmoledMode(it) } }

        SectionHeader("Display")
        SwitchRow(title = "Hide status bar", checked = hideStatusBar) {
            scope.launch { settingsRepository.setHideStatusBar(it) }
        }

        SectionHeader("Accessibility")
        SwitchRow(
            title = "High contrast",
            supporting = "Stronger text and outline contrast",
            checked = highContrast
        ) { scope.launch { settingsRepository.setHighContrast(it) } }

        SectionHeader("About")
        LinkRow(Icons.Outlined.BugReport, "Report a bug or request a feature") {
            openUrl(context, LegalTexts.ISSUES_URL)
        }
        LinkRow(Icons.Outlined.Code, "Source code") { openUrl(context, LegalTexts.REPO_URL) }
        LinkRow(Icons.Outlined.Description, "Open source licenses") { showLicenses = true }
        LinkRow(Icons.Outlined.Gavel, "License (Apache 2.0)") {
            displayedLegalText = "License (Apache 2.0)" to LegalTexts.LICENSE
        }
        LinkRow(Icons.Outlined.Lock, "Privacy policy") {
            displayedLegalText = "Privacy policy" to LegalTexts.PRIVACY_POLICY
        }
        if (versionName != null) {
            Text(
                "Version $versionName",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun SettingLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSelector(themeMode: Int, onSelect: (Int) -> Unit) {
    val options = listOf("System", "Light", "Dark")
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
        options.forEachIndexed { index, label ->
            SegmentedButton(
                selected = themeMode == index,
                onClick = { onSelect(index) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = { Text(label) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccentSelector(selected: Int, onSelect: (Int) -> Unit) {
    val presetColors = AccentPresets.drop(1).map { it.second }
    val dynamicBrush = Brush.sweepGradient(presetColors + presetColors.first())
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        AccentPresets.forEachIndexed { index, (name, color) ->
            val isSelected = index == selected
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .then(
                        if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        else Modifier
                    )
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(index) })
                    .semantics { contentDescription = name },
                contentAlignment = Alignment.Center
            ) {
                val swatch = Modifier.size(36.dp).clip(CircleShape)
                Box(
                    modifier = if (color == Color.Unspecified) swatch.background(dynamicBrush) else swatch.background(color),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        // Contrast against the swatch itself, not the theme.
                        val checkTint = if (color != Color.Unspecified && color.luminance() > 0.5f) Color.Black else Color.White
                        Icon(Icons.Default.Check, contentDescription = null, tint = checkTint, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    supporting: String? = null,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = if (supporting != null) { { Text(supporting) } } else null,
        trailingContent = { Switch(checked = checked, onCheckedChange = null, enabled = enabled) },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.background,
            headlineColor = if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        ),
        modifier = Modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = onCheckedChange
        )
    )
}

@Composable
private fun LinkRow(icon: ImageVector, title: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = { Icon(icon, contentDescription = null) },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.background,
            leadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullScreenDialog(title: String, onClose: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
                content()
            }
        }
    }
}
