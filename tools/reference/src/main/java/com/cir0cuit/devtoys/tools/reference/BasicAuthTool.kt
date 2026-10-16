package com.cir0cuit.devtoys.tools.reference

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolCategory
import com.cir0cuit.devtoys.tools.base.ToolDescriptor
import java.util.Base64

/** RFC 7617 token: base64(UTF-8 "user:password"). */
internal fun basicAuthToken(user: String, password: String): String {
    require(':' !in user) { "The username cannot contain a colon (:). Remove it; only the password may contain one." }
    return Base64.getEncoder().encodeToString("$user:$password".toByteArray(Charsets.UTF_8))
}

/** Accepts "Authorization: Basic x", "Basic x" or the bare token; returns user to password. */
internal fun parseBasicAuth(input: String): Pair<String, String> {
    val token = input.trim()
        .replaceFirst(Regex("^authorization\\s*:\\s*", RegexOption.IGNORE_CASE), "")
        .replaceFirst(Regex("^basic\\s+", RegexOption.IGNORE_CASE), "")
        .trim()
    val bytes = try {
        Base64.getDecoder().decode(token)
    } catch (e: IllegalArgumentException) {
        throw IllegalArgumentException("This is not valid Base64. Paste the header or the value after \"Basic\".")
    }
    val text = String(bytes, Charsets.UTF_8)
    val colon = text.indexOf(':')
    require(colon >= 0) { "The decoded text has no colon, so it is not user:password. Check that you pasted a Basic auth value." }
    return text.substring(0, colon) to text.substring(colon + 1)
}

class BasicAuthTool : Tool {
    override val descriptor = ToolDescriptor(
        "basic-auth",
        "Basic Auth Header",
        "Build and decode HTTP Basic auth headers",
        Icons.Default.Badge,
        ToolCategory.ENCODERS_DECODERS,
        listOf("basic", "auth", "authorization", "header", "http", "base64", "credentials", "curl")
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(modifier: Modifier) {
        var decodeMode by rememberSaveable { mutableStateOf(false) }
        var user by rememberSaveable { mutableStateOf("") }
        // Credentials stay out of saved instance state.
        var password by remember { mutableStateOf("") }
        var header by remember { mutableStateOf("") }
        var showPassword by remember { mutableStateOf(false) }

        ToolScaffold(descriptor.title, modifier = modifier) {
            if (decodeMode) {
                MonoTextField(header, { header = it.take(8192) }, "Authorization header or token", minLines = 3)
            } else {
                OutlinedTextField(
                    value = user,
                    onValueChange = { user = it.take(1024) },
                    label = { Text("Username") },
                    singleLine = true,
                    textStyle = CodeTextStyle,
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(autoCorrect = false),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it.take(1024) },
                    label = { Text("Password") },
                    singleLine = true,
                    textStyle = CodeTextStyle,
                    shape = MaterialTheme.shapes.medium,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrect = false),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showPassword) "Hide password" else "Show password"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(16.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf("Encode", "Decode").forEachIndexed { i, label ->
                    SegmentedButton(
                        selected = decodeMode == (i == 1),
                        onClick = { decodeMode = i == 1 },
                        shape = SegmentedButtonDefaults.itemShape(i, 2)
                    ) { Text(label, maxLines = 1) }
                }
            }
            Spacer(Modifier.height(16.dp))
            if (decodeMode) {
                if (header.isNotBlank()) {
                    runCatching { parseBasicAuth(header) }
                        .onSuccess { (u, p) -> LabeledValueSlab(listOf("Username" to u, "Password" to p)) }
                        .onFailure { ErrorText(it.message.orEmpty()) }
                }
            } else if (user.isNotEmpty() || password.isNotEmpty()) {
                runCatching { basicAuthToken(user, password) }
                    .onSuccess { token ->
                        LabeledValueSlab(
                            listOf(
                                "Header" to "Authorization: Basic $token",
                                "Value" to "Basic $token",
                                "curl" to "curl -H \"Authorization: Basic $token\""
                            )
                        )
                    }
                    .onFailure { ErrorText(it.message.orEmpty()) }
            }
        }
    }
}
