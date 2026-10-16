package com.cir0cuit.devtoys.tools.crypto

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.designsystem.CodeTextStyle
import com.cir0cuit.devtoys.core.ui.MonoTextField
import com.cir0cuit.devtoys.core.ui.OutputPanel
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolCategory
import com.cir0cuit.devtoys.tools.base.ToolDescriptor
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private val HMAC_ALGORITHMS = listOf("HmacMD5", "HmacSHA1", "HmacSHA256", "HmacSHA512")

/** Lowercase-hex HMAC of [input] (UTF-8) keyed with [secret] (UTF-8); "" when [secret] is empty, "Error: ..." on failure. */
internal fun computeHmac(algo: String, secret: String, input: String): String =
    if (secret.isEmpty()) "" else try {
        val mac = Mac.getInstance(algo)
        mac.init(SecretKeySpec(secret.toByteArray(), algo))
        mac.doFinal(input.toByteArray()).toLowerHex()
    } catch (e: Exception) {
        "Error: could not compute $algo (${e.message}). Pick another algorithm."
    }

class HmacCalculatorTool : Tool {
    override val descriptor = ToolDescriptor(
        id = "hmac",
        title = "HMAC Calculator",
        description = "Sign text with a secret key using HMAC",
        icon = Icons.Default.EnhancedEncryption,
        category = ToolCategory.CRYPTOGRAPHY,
        keywords = listOf("hmac", "hash")
    )

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    override fun Content(modifier: Modifier) {
        var input by rememberSaveable { mutableStateOf("") }
        // Not saveable on purpose: the secret is not written to saved instance state.
        var secret by remember { mutableStateOf("") }
        var algo by rememberSaveable { mutableStateOf("HmacSHA256") }
        val output = remember(input, secret, algo) { computeHmac(algo, secret, input) }

        var showSecret by remember { mutableStateOf(false) }

        ToolScaffold(descriptor.title, modifier = modifier) {
            MonoTextField(input, { input = it }, "Message", minLines = 4)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = secret,
                onValueChange = { secret = it },
                label = { Text("Secret key") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = CodeTextStyle,
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                visualTransformation = if (showSecret) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrect = false),
                trailingIcon = {
                    IconButton(onClick = { showSecret = !showSecret }) {
                        Icon(
                            if (showSecret) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showSecret) "Hide key" else "Show key"
                        )
                    }
                }
            )
            Spacer(Modifier.height(16.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HMAC_ALGORITHMS.forEach {
                    FilterChip(selected = algo == it, onClick = { algo = it }, label = { Text(it.removePrefix("Hmac").replace("SHA", "SHA-")) })
                }
            }
            Spacer(Modifier.height(16.dp))
            if (output.startsWith("Error:")) Text(output.removePrefix("Error: ").replaceFirstChar { it.uppercase() }, color = MaterialTheme.colorScheme.error)
            else OutputPanel(output, label = "HMAC")
        }
    }
}
