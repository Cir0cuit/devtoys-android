package com.cir0cuit.devtoys.tools.developer

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId

class JwtDecoderTool : Tool {
    override val descriptor = ToolDescriptor(
        id = "jwt",
        title = "JWT Decoder",
        description = "Decode, verify and sign JSON Web Tokens",
        icon = Icons.Default.VpnKey,
        category = ToolCategory.ENCODERS_DECODERS,
        keywords = listOf("jwt", "decode", "encode", "verify", "sign", "jws", "token", "bearer"),
        accepts = setOf(ShareKind.TEXT),
        fileTypes = setOf("jwt")
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable override fun Content(modifier: Modifier) {
        var encodeMode by rememberSaveable { mutableStateOf(false) }
        ToolScaffold(descriptor.title, modifier = modifier) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf("Decode", "Encode").forEachIndexed { i, label ->
                    SegmentedButton(
                        selected = encodeMode == (i == 1),
                        onClick = { encodeMode = i == 1 },
                        shape = SegmentedButtonDefaults.itemShape(i, 2),
                        label = { Text(label) }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            if (encodeMode) EncodePane() else DecodePane()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable private fun ColumnScope.DecodePane() {
        var input by rememberSaveable { mutableStateOf(SAMPLE_TOKEN) }
        OnSharedText { input = it.trim() }
        var key by rememberSaveable { mutableStateOf("") }
        var base64Secret by rememberSaveable { mutableStateOf(false) }
        val clipboard = LocalClipboardManager.current
        val tokens = remember(input) { extractTokens(input) }
        val decoded = remember(tokens) { tokens.map { runCatching { decodeJwt(it) } } }
        var now by remember { mutableStateOf(Instant.now()) }
        LaunchedEffect(Unit) {
            while (true) { delay(1000); now = Instant.now() }
        }
        var verdicts by remember { mutableStateOf<List<JwtVerdict>>(emptyList()) }
        LaunchedEffect(tokens, key, base64Secret) {
            if (key.isBlank()) { verdicts = emptyList(); return@LaunchedEffect }
            delay(250)
            verdicts = withContext(Dispatchers.Default) { tokens.map { verifyJwt(it, key, base64Secret) } }
        }

        MonoTextField(input, { input = it }, "Token, header, URL or JSON", minLines = 4)
        TextButton(onClick = { clipboard.getText()?.text?.let { input = it } }, modifier = Modifier.align(Alignment.End)) {
            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text("Paste")
        }
        MonoTextField(key, { key = it }, "Secret or public key (optional)", minLines = 1, maxLines = 6)
        Spacer(Modifier.height(8.dp))
        FilterChip(base64Secret, { base64Secret = !base64Secret }, label = { Text("Base64 secret") })
        Spacer(Modifier.height(16.dp))

        if (tokens.isEmpty()) OutputPanel("", label = "Payload")
        val zone = ZoneId.systemDefault()
        decoded.forEachIndexed { i, result ->
            if (tokens.size > 1) {
                Text("Token ${i + 1} of ${tokens.size}", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
            }
            result.onFailure {
                Text(it.message ?: "Could not decode the token.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }.onSuccess { jwt ->
                verdicts.getOrNull(i)?.let {
                    Text(it.message, color = if (it.valid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                }
                jwtWarnings(jwt).forEach {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(4.dp))
                }
                Spacer(Modifier.height(4.dp))
                LabeledValueSlab(jwtClaimRows(jwt, now, zone))
                Spacer(Modifier.height(16.dp))
                OutputPanel(jwt.payload?.let(::jwtPrettyJson) ?: "This is an encrypted token (JWE), so the payload cannot be read.", label = "Payload")
                Spacer(Modifier.height(16.dp))
                OutputPanel(jwtPrettyJson(jwt.header), label = "Header")
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @Composable private fun ColumnScope.EncodePane() {
        var header by rememberSaveable { mutableStateOf("{\n  \"typ\": \"JWT\"\n}") }
        var payload by rememberSaveable { mutableStateOf(SAMPLE_PAYLOAD) }
        var alg by rememberSaveable { mutableStateOf("HS256") }
        var key by rememberSaveable { mutableStateOf("your-256-bit-secret") }
        var base64Secret by rememberSaveable { mutableStateOf(false) }
        var claimError by remember { mutableStateOf<String?>(null) }
        var result by remember { mutableStateOf<Result<String>?>(null) }
        val context = LocalContext.current
        LaunchedEffect(header, payload, alg, key, base64Secret) {
            delay(250)
            result = withContext(Dispatchers.Default) { runCatching { signJwt(header, payload, alg, key, base64Secret) } }
        }
        fun claim(name: String, offsetSeconds: Long) {
            runCatching { setClaim(payload, name, Instant.now().epochSecond + offsetSeconds) }
                .onSuccess { payload = it; claimError = null }
                .onFailure { claimError = it.message }
        }

        MonoTextField(header, { header = it }, "Header JSON (alg is set below)", minLines = 3)
        Spacer(Modifier.height(8.dp))
        MonoTextField(payload, { payload = it }, "Payload JSON", minLines = 5)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip({ claim("iat", 0) }, label = { Text("iat now") })
            AssistChip({ claim("exp", 3600) }, label = { Text("exp 1 h") })
            AssistChip({ claim("exp", 86400) }, label = { Text("exp 24 h") })
            AssistChip({ claim("nbf", 0) }, label = { Text("nbf now") })
        }
        claimError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        Spacer(Modifier.height(16.dp))
        SelectField("Algorithm", alg, JWT_SIGN_ALGS, { alg = it }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        val hmac = alg.startsWith("HS")
        MonoTextField(key, { key = it }, if (hmac) "Secret" else "Private key (PEM, PKCS#8)", minLines = if (hmac) 1 else 4, maxLines = 8)
        if (hmac) {
            Spacer(Modifier.height(8.dp))
            FilterChip(base64Secret, { base64Secret = !base64Secret }, label = { Text("Base64 secret") })
        }
        Spacer(Modifier.height(16.dp))
        val token = result?.getOrNull().orEmpty()
        result?.exceptionOrNull()?.let {
            Text(it.message ?: "Could not sign the token.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
        }
        OutputPanel(token, label = "Token")
        if (token.isNotEmpty()) {
            TextButton(onClick = { shareText(context, token) }, modifier = Modifier.align(Alignment.End)) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text("Share")
            }
        }
    }

    private companion object {
        const val SAMPLE_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
        const val SAMPLE_PAYLOAD = "{\n  \"sub\": \"1234567890\",\n  \"name\": \"John Doe\",\n  \"iat\": 1516239022\n}"
    }
}
