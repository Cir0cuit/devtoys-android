package com.cir0cuit.devtoys.ui

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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Security
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import com.cir0cuit.devtoys.data.SettingsRepository
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier
) {
    val themeMode by settingsRepository.themeMode.collectAsState()
    val accentColor by settingsRepository.accentColor.collectAsState()
    val amoledMode by settingsRepository.amoledMode.collectAsState()
    val highContrast by settingsRepository.highContrast.collectAsState()
    val scope = rememberCoroutineScope()

    val isDarkTheme = when (themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    
    var showLicenses by remember { mutableStateOf(false) }
    var displayedLegalText by remember { mutableStateOf<Pair<String, String>?>(null) }
    
    if (displayedLegalText != null) {
        Dialog(
            onDismissRequest = { displayedLegalText = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    TopAppBar(
                        title = { Text(displayedLegalText!!.first) },
                        navigationIcon = {
                            IconButton(onClick = { displayedLegalText = null }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        if (displayedLegalText!!.first == "Support & Feedback") {
                            Text(
                                "Bug Reporting & Feature Requests\n\nReporting bugs and requesting features or changes is possible via email:",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            val clipboardManager = LocalClipboardManager.current
                            val context = LocalContext.current
                            val email = "cir0cuit.dev@gmail.com"
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                SelectionContainer {
                                    Text(
                                        text = email,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = {
                                    clipboardManager.setText(AnnotatedString(email))
                                    Toast.makeText(context, "Email copied to clipboard", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Email")
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Please note that while all feedback is appreciated, I do not promise to respond to every email or implement every requested feature.",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        } else {
                            SelectionContainer {
                                Text(
                                    text = displayedLegalText!!.second,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (showLicenses) {
        Dialog(
            onDismissRequest = { showLicenses = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    TopAppBar(
                        title = { Text("Open Source Licenses") },
                        navigationIcon = {
                            IconButton(onClick = { showLicenses = false }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                    LibrariesContainer(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Theme", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        
        val themeOptions = listOf("Follow system", "Light", "Dark")
        var themeExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = themeExpanded,
            onExpandedChange = { themeExpanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = themeOptions.getOrElse(themeMode) { "Follow system" },
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = themeExpanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = themeExpanded,
                onDismissRequest = { themeExpanded = false }
            ) {
                themeOptions.forEachIndexed { index, title ->
                    DropdownMenuItem(
                        text = { Text(title) },
                        onClick = {
                            scope.launch { settingsRepository.setThemeMode(index) }
                            themeExpanded = false
                        }
                    )
                }
            }
        }
        
        if (isDarkTheme) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth().clickable {
                    scope.launch { settingsRepository.setAmoledMode(!amoledMode) }
                }.padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(
                    checked = amoledMode,
                    onCheckedChange = { scope.launch { settingsRepository.setAmoledMode(it) } }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("AMOLED Background", color = LocalContentColor.current)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Accent Color", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        
        val accentOptions = listOf("Dynamic (System Default)", "Blue", "Purple", "Green", "Orange", "Red", "Yellow", "Teal", "Pink", "Cyan", "Indigo")
        
        var accentExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = accentExpanded,
            onExpandedChange = { accentExpanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = accentOptions.getOrElse(accentColor) { "Dynamic (System Default)" },
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accentExpanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = accentExpanded,
                onDismissRequest = { accentExpanded = false }
            ) {
                accentOptions.forEachIndexed { index, title ->
                    DropdownMenuItem(
                        text = { Text(title) },
                        onClick = {
                            scope.launch { settingsRepository.setAccentColor(index) }
                            accentExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Display", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        
        val hideStatusBar by settingsRepository.hideStatusBar.collectAsState()
        Row(
            modifier = Modifier.fillMaxWidth().clickable {
                scope.launch { settingsRepository.setHideStatusBar(!hideStatusBar) }
            }.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(
                checked = hideStatusBar,
                onCheckedChange = { scope.launch { settingsRepository.setHideStatusBar(it) } }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Hide Status Bar")
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Accessibility", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().clickable {
                scope.launch { settingsRepository.setHighContrast(!highContrast) }
            }.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(
                checked = highContrast,
                onCheckedChange = { scope.launch { settingsRepository.setHighContrast(it) } }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("High Contrast")
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Text("About", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().clickable { 
                displayedLegalText = Pair("Support & Feedback", LegalTexts.CONTACT_AND_SUPPORT) 
            }.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.BugReport, contentDescription = "Bug Reporting and Feature Requests")
            Spacer(modifier = Modifier.width(16.dp))
            Text("Bug Reporting & Feature Requests", style = MaterialTheme.typography.bodyLarge)
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().clickable { showLicenses = true }.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Code, contentDescription = "Licenses")
            Spacer(modifier = Modifier.width(16.dp))
            Text("Open Source Licenses", style = MaterialTheme.typography.bodyLarge)
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().clickable { 
                displayedLegalText = Pair("License (BSL)", LegalTexts.BSL) 
            }.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Gavel, contentDescription = "License (BSL)")
            Spacer(modifier = Modifier.width(16.dp))
            Text("License (BSL)", style = MaterialTheme.typography.bodyLarge)
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().clickable { 
                displayedLegalText = Pair("EULA", LegalTexts.EULA) 
            }.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Assignment, contentDescription = "EULA")
            Spacer(modifier = Modifier.width(16.dp))
            Text("EULA", style = MaterialTheme.typography.bodyLarge)
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().clickable { 
                displayedLegalText = Pair("Privacy Policy", LegalTexts.PRIVACY_POLICY) 
            }.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Security, contentDescription = "Privacy Policy")
            Spacer(modifier = Modifier.width(16.dp))
            Text("Privacy Policy", style = MaterialTheme.typography.bodyLarge)
        }
    }
}