package com.cir0cuit.devtoys.tools.crypto

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.designsystem.CodeTextStyle
import com.cir0cuit.devtoys.core.ui.MonoTextField
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolCategory
import com.cir0cuit.devtoys.tools.base.ToolDescriptor
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private val HMAC_ALGORITHMS = listOf("HmacMD5", "HmacSHA1", "HmacSHA256", "HmacSHA512")

/** HMAC of [data] keyed with [key]; throws when the algorithm is unavailable or [key] is empty. */
internal fun computeHmac(algo: String, key: ByteArray, data: ByteArray): ByteArray =
    Mac.getInstance(algo).run {
        init(SecretKeySpec(key, algo))
        doFinal(data)
    }

/** Hex, Base64 and Base64url forms of a MAC, as labeled rows. */
internal fun hmacRows(mac: ByteArray): List<Pair<String, String>> = listOf(
    "Hex" to mac.toLowerHex(),
    "Base64" to Base64.getEncoder().encodeToString(mac),
    "Base64url" to Base64.getUrlEncoder().withoutPadding().encodeToString(mac),
)

private val SIGNATURE_PREFIX = Regex("^(sha(1|256|384|512)|md5|v0|v1)=", RegexOption.IGNORE_CASE)

/**
 * Constant-time check of a webhook-style signature against [mac]. Accepts hex or Base64(url),
 * with a "sha256=" (GitHub), "v1=" (Stripe) or "v0=" (Slack) prefix.
 */
internal fun signatureMatches(expected: String, mac: ByteArray): Boolean {
    val s = expected.trim().replace(SIGNATURE_PREFIX, "")
    if (s.isEmpty()) return false
    return listOfNotNull(runCatching { parseHex(s) }.getOrNull(), runCatching { parseBase64(s) }.getOrNull())
        .any { MessageDigest.isEqual(it, mac) }
}

class HmacCalculatorTool : Tool {
    override val descriptor = ToolDescriptor(
        id = "hmac",
        title = "HMAC Calculator",
        description = "Sign or verify text with an HMAC key",
        icon = Icons.Default.EnhancedEncryption,
        category = ToolCategory.CRYPTOGRAPHY,
        keywords = listOf("hmac", "hash", "signature", "webhook", "verify")
    )

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    override fun Content(modifier: Modifier) {
        val clipboard = LocalClipboardManager.current
        var input by rememberSaveableText()
        var inputEncoding by rememberSaveable { mutableStateOf(0) }
        // Not saveable on purpose: the secret is not written to saved instance state.
        var secret by remember { mutableStateOf("") }
        var keyEncoding by rememberSaveable { mutableStateOf(0) }
        var algo by rememberSaveable { mutableStateOf("HmacSHA256") }
        var expected by rememberSaveable { mutableStateOf("") }
        var showSecret by remember { mutableStateOf(false) }

        // null: no key yet. Failure: message for the user.
        val result: Result<ByteArray>? = remember(input, inputEncoding, secret, keyEncoding, algo) {
            if (secret.isEmpty()) null else try {
                val key = decodeInput(secret, keyEncoding, "Key")
                require(key.isNotEmpty()) { "Key is empty after decoding. Enter at least one byte." }
                Result.success(computeHmac(algo, key, decodeInput(input, inputEncoding, "Message")))
            } catch (e: IllegalArgumentException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(Exception("Could not compute $algo. Pick another algorithm."))
            }
        }
        val mac = result?.getOrNull()

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
            ChoiceRow("Message", INPUT_ENCODINGS, inputEncoding, { inputEncoding = it })
            Spacer(Modifier.height(8.dp))
            ChoiceRow("Key", INPUT_ENCODINGS, keyEncoding, { keyEncoding = it })
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HMAC_ALGORITHMS.forEach {
                    FilterChip(selected = algo == it, onClick = { algo = it }, label = { Text(it.removePrefix("Hmac").replace("SHA", "SHA-")) })
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = expected,
                onValueChange = { expected = it },
                label = { Text("Expected signature (optional)") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = CodeTextStyle,
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                trailingIcon = {
                    IconButton(onClick = { clipboard.getText()?.text?.let { expected = it.trim() } }) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste signature")
                    }
                },
                supportingText = if (expected.isBlank() || mac == null) null else {
                    {
                        if (signatureMatches(expected, mac)) Text("Valid signature", color = MaterialTheme.colorScheme.primary)
                        else Text("Invalid. Check the key, its encoding and the algorithm.", color = MaterialTheme.colorScheme.error)
                    }
                }
            )
            Spacer(Modifier.height(16.dp))
            val error = result?.exceptionOrNull()
            if (error != null) Text(error.message.orEmpty(), color = MaterialTheme.colorScheme.error)
            else ResultSlab(if (mac == null) listOf("Hex" to "", "Base64" to "", "Base64url" to "") else hmacRows(mac))
        }
    }
}
