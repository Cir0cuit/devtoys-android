package com.cir0cuit.devtoys.data

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(
    name = "settings",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val THEME_MODE_KEY = intPreferencesKey("theme_mode")
    private val FAVORITES_KEY = stringSetPreferencesKey("favorites")
    private val ACCENT_COLOR_KEY = intPreferencesKey("accent_color")
    private val AMOLED_MODE_KEY = booleanPreferencesKey("amoled_mode")
    private val HIGH_CONTRAST_KEY = booleanPreferencesKey("high_contrast")
    private val HIDE_STATUS_BAR_KEY = booleanPreferencesKey("hide_status_bar")

    private val prefs: Flow<Preferences> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }

    val isLoaded: StateFlow<Boolean> = prefs.map { true }
        .stateIn(scope, SharingStarted.Eagerly, false)

    val themeMode: StateFlow<Int> = prefs.map { it[THEME_MODE_KEY] ?: 0 }
        .stateIn(scope, SharingStarted.Eagerly, 0)

    suspend fun setThemeMode(mode: Int) {
        context.dataStore.edit { it[THEME_MODE_KEY] = mode }
    }

    val amoledMode: StateFlow<Boolean> = prefs.map { it[AMOLED_MODE_KEY] ?: false }
        .stateIn(scope, SharingStarted.Eagerly, false)
        
    suspend fun setAmoledMode(enabled: Boolean) { context.dataStore.edit { it[AMOLED_MODE_KEY] = enabled } }
    
    val highContrast: StateFlow<Boolean> = prefs.map { it[HIGH_CONTRAST_KEY] ?: false }
        .stateIn(scope, SharingStarted.Eagerly, false)
        
    suspend fun setHighContrast(enabled: Boolean) { context.dataStore.edit { it[HIGH_CONTRAST_KEY] = enabled } }

    val hideStatusBar: StateFlow<Boolean> = prefs.map { it[HIDE_STATUS_BAR_KEY] ?: false }
        .stateIn(scope, SharingStarted.Eagerly, false)

    suspend fun setHideStatusBar(enabled: Boolean) { context.dataStore.edit { it[HIDE_STATUS_BAR_KEY] = enabled } }
    
    val accentColor: StateFlow<Int> = prefs.map { it[ACCENT_COLOR_KEY] ?: 0 }
        .stateIn(scope, SharingStarted.Eagerly, 0)
        
    suspend fun setAccentColor(color: Int) { context.dataStore.edit { it[ACCENT_COLOR_KEY] = color } }

    val favoriteTools: StateFlow<Set<String>> = prefs.map { it[FAVORITES_KEY] ?: emptySet() }
        .stateIn(scope, SharingStarted.Eagerly, emptySet())

    suspend fun toggleFavorite(toolId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[FAVORITES_KEY] ?: emptySet()
            val newSet = if (current.contains(toolId)) current - toolId else current + toolId
            prefs[FAVORITES_KEY] = newSet
        }
    }

    /** Most recently opened tool ids, newest first. */
    val recentTools: StateFlow<List<String>> = prefs.map { p -> p[RECENTS_KEY]?.split(',')?.filter { it.isNotEmpty() } ?: emptyList() }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    suspend fun addRecent(toolId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[RECENTS_KEY]?.split(',')?.filter { it.isNotEmpty() } ?: emptyList()
            prefs[RECENTS_KEY] = (listOf(toolId) + (current - toolId)).take(MAX_RECENTS).joinToString(",")
        }
    }

    private companion object {
        val RECENTS_KEY = stringPreferencesKey("recent_tools")
        const val MAX_RECENTS = 6
    }
}