package com.cir0cuit.devtoys

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.cir0cuit.devtoys.data.SettingsRepository
import com.cir0cuit.devtoys.tools.base.ToolRegistry
import com.cir0cuit.devtoys.ui.MainScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var toolRegistry: ToolRegistry
    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && settingsRepository.hideStatusBar.value) {
            androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                .hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        splashScreen.setKeepOnScreenCondition {
            !settingsRepository.isLoaded.value
        }
        
        setContent {
            val themeMode by settingsRepository.themeMode.collectAsState()
            val accentColor by settingsRepository.accentColor.collectAsState()
            val amoledMode by settingsRepository.amoledMode.collectAsState()
            val highContrast by settingsRepository.highContrast.collectAsState()
            val hideStatusBar by settingsRepository.hideStatusBar.collectAsState()
            
            androidx.compose.runtime.LaunchedEffect(hideStatusBar) {
                val windowInsetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                if (hideStatusBar) {
                    windowInsetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    windowInsetsController.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
                } else {
                    windowInsetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
                    windowInsetsController.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
                }
            }
            
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

            fun customDark(primary: Color, container: Color): ColorScheme = baseDark.copy(
                primary = primary, onPrimary = Color(0xFF111111),
                primaryContainer = container, onPrimaryContainer = Color.White,
                secondary = primary, onSecondary = Color(0xFF111111),
                secondaryContainer = container, onSecondaryContainer = Color.White,
                tertiary = primary, onTertiary = Color(0xFF111111),
                tertiaryContainer = container, onTertiaryContainer = Color.White,
                outline = Color(0xFF888888), outlineVariant = Color(0xFF444444)
            )
            fun customLight(primary: Color, container: Color): ColorScheme = baseLight.copy(
                primary = primary, onPrimary = Color.White,
                primaryContainer = container, onPrimaryContainer = Color(0xFF111111),
                secondary = primary, onSecondary = Color.White,
                secondaryContainer = container, onSecondaryContainer = Color(0xFF111111),
                tertiary = primary, onTertiary = Color.White,
                tertiaryContainer = container, onTertiaryContainer = Color(0xFF111111),
                outline = Color(0xFF777777), outlineVariant = Color(0xFFCCCCCC)
            )

            val colorScheme = when (accentColor) {
                0 -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val dynamic = if (isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                    if (amoledMode && isDarkTheme) dynamic.copy(background = Color.Black, surface = Color.Black, surfaceVariant = Color(0xFF111111)) else dynamic
                } else {
                    if (isDarkTheme) baseDark else baseLight
                }
                1 -> if (isDarkTheme) customDark(Color(0xFF2196F3), Color(0xFF004B71)) else customLight(Color(0xFF2196F3), Color(0xFFCBE6FF))
                2 -> if (isDarkTheme) customDark(Color(0xFF673AB7), Color(0xFF4A148C)) else customLight(Color(0xFF673AB7), Color(0xFFEADDFF))
                3 -> if (isDarkTheme) customDark(Color(0xFF4CAF50), Color(0xFF005313)) else customLight(Color(0xFF4CAF50), Color(0xFFC8E6C9))
                4 -> if (isDarkTheme) customDark(Color(0xFFFF9800), Color(0xFF7A2E00)) else customLight(Color(0xFFFF9800), Color(0xFFFFDCC1))
                5 -> if (isDarkTheme) customDark(Color(0xFFF44336), Color(0xFF8B0000)) else customLight(Color(0xFFF44336), Color(0xFFFFCDD2))
                6 -> if (isDarkTheme) customDark(Color(0xFFFFEB3B), Color(0xFFF57F17)) else customLight(Color(0xFFFFC107), Color(0xFFFFF9C4))
                7 -> if (isDarkTheme) customDark(Color(0xFF009688), Color(0xFF004D40)) else customLight(Color(0xFF009688), Color(0xFFB2DFDB))
                8 -> if (isDarkTheme) customDark(Color(0xFFE91E63), Color(0xFF880E4F)) else customLight(Color(0xFFE91E63), Color(0xFFF8BBD0))
                9 -> if (isDarkTheme) customDark(Color(0xFF00BCD4), Color(0xFF006064)) else customLight(Color(0xFF00BCD4), Color(0xFFB2EBF2))
                10 -> if (isDarkTheme) customDark(Color(0xFF3F51B5), Color(0xFF1A237E)) else customLight(Color(0xFF3F51B5), Color(0xFFC5CAE9))
                else -> if (isDarkTheme) baseDark else baseLight
            }

            val finalColorScheme = if (highContrast) {
                colorScheme.copy(
                    outline = if (isDarkTheme) Color.White else Color.Black,
                    outlineVariant = if (isDarkTheme) Color.LightGray else Color.DarkGray,
                    surfaceVariant = if (isDarkTheme) Color.Black else Color.White,
                    onSurfaceVariant = if (isDarkTheme) Color.White else Color.Black,
                    onSurface = if (isDarkTheme) Color.White else Color.Black,
                    onBackground = if (isDarkTheme) Color.White else Color.Black
                )
            } else colorScheme

            val barColor = finalColorScheme.background.toArgb()
            androidx.compose.runtime.LaunchedEffect(isDarkTheme, barColor) {
                window.statusBarColor = barColor
                window.navigationBarColor = barColor
                androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !isDarkTheme
                    isAppearanceLightNavigationBars = !isDarkTheme
                }
            }

            MaterialTheme(colorScheme = finalColorScheme) {
                MainScreen(toolRegistry = toolRegistry, settingsRepository = settingsRepository)
            }
        }
    }
}