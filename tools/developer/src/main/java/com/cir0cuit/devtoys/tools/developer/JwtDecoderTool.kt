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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class JwtDecoderTool : Tool {
    override val descriptor = ToolDescriptor(
        id = "jwt",
        title = "JWT Decoder",
        description = "Read the header and payload of a JWT",
        icon = Icons.Default.VpnKey,
        category = ToolCategory.ENCODERS_DECODERS,
        keywords = listOf("jwt", "decode")
    )

    @Composable override fun Content(modifier: Modifier) {
        var input by rememberSaveable { mutableStateOf(SAMPLE_TOKEN) }
        val decoded = remember(input) { decode(input) }
        ToolScaffold(descriptor.title, modifier = modifier) {
            MonoTextField(input, { input = it }, "Token", minLines = 4)
            Spacer(modifier = Modifier.height(16.dp))
            when (decoded) {
                null -> OutputPanel("", label = "Payload")
                is Decoded.Error -> Text(decoded.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                is Decoded.Token -> {
                    if (decoded.times.isNotEmpty()) {
                        LabeledValueSlab(decoded.times)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    OutputPanel(decoded.payload, label = "Payload")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutputPanel(decoded.header, label = "Header")
                }
            }
        }
    }

    private sealed interface Decoded {
        data class Error(val message: String) : Decoded
        data class Token(val header: String, val payload: String, val times: List<Pair<String, String>>) : Decoded
    }

    private fun decode(input: String): Decoded? {
        val token = input.trim().replace(Regex("^Bearer\\s+", RegexOption.IGNORE_CASE), "").trim()
        if (token.isEmpty()) return null
        val parts = token.split(".")
        if (parts.size < 2) return Decoded.Error("Not a JWT. A token has dot-separated parts: header.payload.signature.")
        fun json(part: String): JSONObject? = try {
            JSONObject(String(Base64.decode(part, Base64.URL_SAFE or Base64.NO_WRAP), Charsets.UTF_8))
        } catch (e: Exception) { null }
        fun pretty(o: JSONObject) = o.toString(2).replace("\\/", "/")
        val header = json(parts[0])
            ?: return Decoded.Error("Could not decode the header. Check that the token is complete.")
        if (parts.size == 5) return Decoded.Token(pretty(header), "This is an encrypted token (JWE), so the payload cannot be read.", emptyList())
        val payload = json(parts[1])
            ?: return Decoded.Error("Could not decode the payload. Check that the token is complete.")
        val now = Instant.now().epochSecond
        val times = listOf("exp" to "Expires", "iat" to "Issued", "nbf" to "Not before").mapNotNull { (claim, label) ->
            val seconds = payload.optLong(claim, Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE } ?: return@mapNotNull null
            val date = runCatching { DATE_FORMAT.format(Instant.ofEpochSecond(seconds)) }.getOrNull() ?: return@mapNotNull null
            val note = when {
                claim == "exp" && seconds < now -> " (expired)"
                claim == "nbf" && seconds > now -> " (not yet valid)"
                else -> ""
            }
            label to date + note
        }
        return Decoded.Token(pretty(header), pretty(payload), times)
    }

    private companion object {
        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(ZoneId.systemDefault())
        const val SAMPLE_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
    }
}
