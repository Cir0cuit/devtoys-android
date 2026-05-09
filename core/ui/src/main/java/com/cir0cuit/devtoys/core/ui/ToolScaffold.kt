package com.cir0cuit.devtoys.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.layout.imePadding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolScaffold(
    title: String,
    onInfoClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                actions = {
                    if (onInfoClick != null) {
                        IconButton(onClick = onInfoClick) {
                            Icon(Icons.Default.Info, contentDescription = "Info")
                        }
                    }
                }
            )
        }
    ) { padding ->
        val colModifier = Modifier
            .padding(padding)
            .padding(horizontal = 16.dp)
            .fillMaxSize()
            .imePadding()
            
        Column(
            modifier = if (scrollable) colModifier.verticalScroll(rememberScrollState()) else colModifier,
            content = content
        )
    }
}
