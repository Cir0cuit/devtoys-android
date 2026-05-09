package com.cir0cuit.devtoys.tools.base

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface Tool {
    val descriptor: ToolDescriptor
    @Composable
    fun Content(modifier: Modifier)
}
