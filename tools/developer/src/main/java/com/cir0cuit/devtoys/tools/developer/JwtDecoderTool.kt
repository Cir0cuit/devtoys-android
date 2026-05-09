package com.cir0cuit.devtoys.tools.developer

import android.util.Base64
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VpnKey

import androidx.compose.material3.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*
import org.json.JSONObject

class JwtDecoderTool : Tool {
    override val descriptor = ToolDescriptor("jwt", android.R.string.untitled, android.R.string.untitled, Icons.Default.VpnKey, ToolCategory.ENCODERS_DECODERS, listOf("jwt", "decode"))
    @Composable override fun Content(modifier: Modifier) {
        var input by rememberSaveable { mutableStateOf("") }
        val output = remember(input) {
            if (input.isEmpty()) "" else try {
                val parts = input.split(".")
                if (parts.size >= 2) {
                    val header = String(Base64.decode(parts[0], Base64.URL_SAFE))
                    val payload = String(Base64.decode(parts[1], Base64.URL_SAFE))
                    """
                        Header:
                        ${JSONObject(header).toString(2)}
                        
                        Payload:
                        ${JSONObject(payload).toString(2)}
                    """.trimIndent()
                } else "Invalid JWT format"
            } catch (e: Exception) { "Error: ${e.message}" }
        }
        ToolScaffold("JWT Decoder", modifier = modifier) {
            MonoTextField(input, { input = it }, "JWT Input")
            Spacer(modifier = Modifier.height(16.dp))
            OutputPanel(output)
        }
    }
}