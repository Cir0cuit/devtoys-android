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
            val token = input.trim().replace(Regex("^Bearer\\s+", RegexOption.IGNORE_CASE), "").trim()
            if (token.isEmpty()) "" else {
                val parts = token.split(".")
                fun section(label: String, part: String) = try {
                    val json = String(Base64.decode(part, Base64.URL_SAFE or Base64.NO_WRAP), Charsets.UTF_8)
                    "$label:\n" + JSONObject(json).toString(2).replace("\\/", "/")
                } catch (e: Exception) { "$label: Error: ${e.message}" }
                when {
                    parts.size < 2 -> "Invalid JWT format"
                    parts.size == 5 -> section("Header", parts[0]) + "\n\n(JWE: payload is encrypted)"
                    else -> section("Header", parts[0]) + "\n\n" + section("Payload", parts[1])
                }
            }
        }
        ToolScaffold("JWT Decoder", modifier = modifier) {
            MonoTextField(input, { input = it }, "JWT Input")
            Spacer(modifier = Modifier.height(16.dp))
            OutputPanel(output)
        }
    }
}