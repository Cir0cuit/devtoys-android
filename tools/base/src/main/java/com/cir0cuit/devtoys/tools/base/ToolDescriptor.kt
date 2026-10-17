package com.cir0cuit.devtoys.tools.base

import androidx.compose.ui.graphics.vector.ImageVector

data class ToolDescriptor(
    val id: String,                  // stable, e.g. "json-formatter"; used for favorites and recents
    val title: String,
    val description: String,        // one line, plain language
    val icon: ImageVector,
    val category: ToolCategory,
    val keywords: List<String>,      // for search
    val accepts: Set<ShareKind> = emptySet()  // content this tool can receive from the share sheet
)
