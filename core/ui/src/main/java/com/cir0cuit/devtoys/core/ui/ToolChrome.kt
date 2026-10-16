package com.cir0cuit.devtoys.core.ui

import androidx.compose.runtime.staticCompositionLocalOf

/** Navigation and favorite state the host supplies to the tool currently on screen. */
data class ToolChrome(
    val onBack: () -> Unit,
    val isFavorite: Boolean,
    val onToggleFavorite: () -> Unit,
)

val LocalToolChrome = staticCompositionLocalOf<ToolChrome?> { null }
