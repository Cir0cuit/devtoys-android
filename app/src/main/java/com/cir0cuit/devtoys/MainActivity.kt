package com.cir0cuit.devtoys

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.content.IntentCompat
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.cir0cuit.devtoys.tools.base.SharedContent
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

    // ponytail: not saved across recreation; a pending share picker closes on rotation, a tool that received it keeps its input.
    private var shared by mutableStateOf<SharedContent?>(null)

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && settingsRepository.hideStatusBar.value) {
            androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                .hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.sharedContent()?.let { shared = it }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) shared = intent.sharedContent()
        
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
                MainScreen(
                    toolRegistry = toolRegistry,
                    settingsRepository = settingsRepository,
                    initialToolId = intent.getStringExtra("tool"),
                    shared = shared,
                    onShareDone = { shared = null }
                )
            }
        }
    }
}

/** Largest shared text accepted; longer text is cut so tool state stays within the saved-state limit. */
private const val MAX_SHARED_CHARS = 200_000

private fun Intent.sharedContent(): SharedContent? {
    if (action != Intent.ACTION_SEND) return null
    if (type?.startsWith("image/") == true) {
        return IntentCompat.getParcelableExtra(this, Intent.EXTRA_STREAM, Uri::class.java)?.let { SharedContent.Image(it) }
    }
    return getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()?.take(MAX_SHARED_CHARS)?.let { SharedContent.Text(it) }
}
