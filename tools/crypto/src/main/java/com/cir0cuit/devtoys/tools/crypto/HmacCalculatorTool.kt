package com.cir0cuit.devtoys.tools.crypto
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EnhancedEncryption

import androidx.compose.material3.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** Lowercase-hex HMAC of [input] (UTF-8) keyed with [secret] (UTF-8); "" when [secret] is empty, "Error: ..." on failure. */
internal fun computeHmac(algo: String, secret: String, input: String): String =
    if (secret.isEmpty()) "" else try {
        val mac = Mac.getInstance(algo)
        mac.init(SecretKeySpec(secret.toByteArray(), algo))
        mac.doFinal(input.toByteArray()).joinToString("") { "%02x".format(it) }
    } catch (e: Exception) { "Error: ${e.message}" }

class HmacCalculatorTool : Tool {
    override val descriptor = ToolDescriptor("hmac", android.R.string.untitled, android.R.string.untitled, Icons.Default.EnhancedEncryption, ToolCategory.CRYPTOGRAPHY, listOf("hmac", "hash"))
    @Composable override fun Content(modifier: Modifier) {
        var input by rememberSaveable { mutableStateOf("") }
        var secret by remember { mutableStateOf("") }
        var algo by rememberSaveable { mutableStateOf("HmacSHA256") }
        val output = remember(input, secret, algo) {
            computeHmac(algo, secret, input)
        }
        ToolScaffold("HMAC Calculator", modifier = modifier) {
            Row {
                listOf("HmacMD5", "HmacSHA1", "HmacSHA256", "HmacSHA512").forEach {
                    FilterChip(selected = algo == it, onClick = { algo = it }, label = { Text(it.replace("Hmac", "")) }, modifier=Modifier.padding(end=4.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            MonoTextField(secret, { secret = it }, "Secret Key", minLines=1)
            Spacer(modifier = Modifier.height(8.dp))
            MonoTextField(input, { input = it }, "Input", minLines=4)
            Spacer(modifier = Modifier.height(16.dp))
            OutputPanel(output)
        }
    }
}