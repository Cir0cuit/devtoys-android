package com.cir0cuit.devtoys.tools.base

import androidx.compose.ui.graphics.vector.ImageVector

data class ToolDescriptor(
    val id: String,                  // stable, e.g. "json-formatter"
    val titleRes: Int,               // string resource
    val descriptionRes: Int,
    val icon: ImageVector,
    val category: ToolCategory,
    val keywords: List<String>       // for search
)
