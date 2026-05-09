package com.cir0cuit.devtoys.tools.encoders

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link

import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*
import java.net.URLDecoder
import java.net.URLEncoder

class UrlEncoderTool : Tool {
    override val descriptor = ToolDescriptor("url", android.R.string.untitled, android.R.string.untitled, Icons.Default.Link, ToolCategory.ENCODERS_DECODERS, listOf("url", "encode"))
    @Composable override fun Content(modifier: Modifier) {
        var encodeInput by rememberSaveable { mutableStateOf("") }
        var decodeInput by rememberSaveable { mutableStateOf("") }
        var isEncodeMode by rememberSaveable { mutableStateOf(true) }
        
        val encodeOutput = remember(encodeInput) {
            try {
                if (encodeInput.isEmpty()) "" else URLEncoder.encode(encodeInput, "UTF-8")
            } catch (e: Exception) { "Error: ${e.message}" }
        }
        val decodeOutput = remember(decodeInput) {
            try {
                if (decodeInput.isEmpty()) "" else URLDecoder.decode(decodeInput, "UTF-8")
            } catch (e: Exception) { "Error: ${e.message}" }
        }
        
        ToolScaffold("URL Encoder / Decoder", modifier = modifier) {
            TabRow(selectedTabIndex = if (isEncodeMode) 0 else 1) {
                Tab(selected = isEncodeMode, onClick = { isEncodeMode = true }, text = { Text("Encode") })
                Tab(selected = !isEncodeMode, onClick = { isEncodeMode = false }, text = { Text("Decode") })
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (isEncodeMode) {
                MonoTextField(encodeInput, { encodeInput = it }, "Input to Encode")
                Spacer(modifier = Modifier.height(16.dp))
                OutputPanel(encodeOutput)
            } else {
                MonoTextField(decodeInput, { decodeInput = it }, "Input to Decode")
                Spacer(modifier = Modifier.height(16.dp))
                OutputPanel(decodeOutput)
            }
        }
    }
}