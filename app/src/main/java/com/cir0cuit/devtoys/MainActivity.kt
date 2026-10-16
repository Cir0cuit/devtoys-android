package com.cir0cuit.devtoys

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import com.cir0cuit.devtoys.core.designsystem.DevToysTheme
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
            
            DevToysTheme(darkTheme = isDarkTheme, accent = accentColor, amoled = amoledMode, highContrast = highContrast) {
                val barColor = MaterialTheme.colorScheme.background.toArgb()
                androidx.compose.runtime.LaunchedEffect(isDarkTheme, barColor) {
                    window.statusBarColor = barColor
                    window.navigationBarColor = barColor
                    androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = !isDarkTheme
                        isAppearanceLightNavigationBars = !isDarkTheme
                    }
                }
                MainScreen(toolRegistry = toolRegistry, settingsRepository = settingsRepository, initialToolId = intent.getStringExtra("tool"))
            }
        }
    }
}