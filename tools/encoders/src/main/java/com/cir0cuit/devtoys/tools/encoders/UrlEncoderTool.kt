package com.cir0cuit.devtoys.tools.encoders

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.MonoTextField
import com.cir0cuit.devtoys.core.ui.OutputPanel
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.tools.base.ShareKind
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolCategory
import com.cir0cuit.devtoys.tools.base.ToolDescriptor
import java.net.URLDecoder
import java.net.URLEncoder

internal enum class UrlMode(val label: String) { COMPONENT("Component"), FULL("Full URL"), FORM("Form") }

/** Percent-encodes UTF-8 bytes, leaving ASCII letters, digits and [keep] as they are. */
private fun percentEncode(input: String, keep: String): String = buildString {
    for (b in input.toByteArray(Charsets.UTF_8)) {
        val c = b.toInt().toChar()
        if (b >= 0 && (c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' || c in keep)) append(c) else append("%%%02X".format(b))
    }
}

/**
 * Component: RFC 3986 unreserved characters only (stricter than encodeURIComponent).
 * Full URL: like JavaScript encodeURI, keeps the URL's structure characters.
 * Form: application/x-www-form-urlencoded, space as '+'.
 */
internal fun urlEncode(input: String, mode: UrlMode = UrlMode.COMPONENT): String = when (mode) {
    UrlMode.COMPONENT -> percentEncode(input, "-_.~")
    UrlMode.FULL -> percentEncode(input, "-_.~!*'();/?:@&=+$,#")
    UrlMode.FORM -> URLEncoder.encode(input, "UTF-8")
}

internal fun urlDecode(input: String): String =
    try {
        // Escape lone '%' so URLDecoder does not reject the whole string.
        URLDecoder.decode(input.replace(LONE_PERCENT, "%25"), "UTF-8")
    } catch (e: Exception) {
        "Error: could not decode this text (${e.message})."
    }

private val LONE_PERCENT = Regex("%(?![0-9A-Fa-f]{2})")

/** A note when [input] has a '%' without two hex digits or escapes that are not valid UTF-8, else null. */
internal fun urlDecodeWarning(input: String): String? {
    if (LONE_PERCENT.containsMatchIn(input)) return "Some '%' signs are not followed by two hex digits; they were kept as they are."
    val bytes = java.io.ByteArrayOutputStream()
    var i = 0
    while (i < input.length) {
        if (input[i] == '%') { bytes.write(input.substring(i + 1, i + 3).toInt(16)); i += 3 }
        else { bytes.write(input.substring(i, i + 1).toByteArray()); i++ }
    }
    val valid = runCatching { Charsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes.toByteArray())) }.isSuccess
    return if (valid) null else "The escapes are not valid UTF-8; invalid bytes are shown as \uFFFD."
}

/** Decodes until the text stops changing, for double-encoded input like `%2520`. */
internal fun urlDecodeRepeatedly(input: String): String {
    var current = input
    repeat(10) {
        val next = urlDecode(current)
        if (next == current || next.startsWith("Error: ")) return current
        current = next
    }
    return current
}

internal data class UrlParts(
    val scheme: String?,
    val userInfo: String?,
    val host: String?,
    val port: String?,
    val path: String,
    val query: String?,
    val fragment: String?
) {
    val params: List<Pair<String, String>>
        get() = query.orEmpty().split('&').filter { it.isNotEmpty() }.map {
            urlDecode(it.substringBefore('=')) to urlDecode(it.substringAfter('=', ""))
        }
}

// RFC 3986 appendix B.
private val URL_REGEX = Regex("""^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\?([^#]*))?(#(.*))?""")

internal fun parseUrl(url: String): UrlParts? {
    val m = URL_REGEX.find(url.trim()) ?: return null
    val g = m.groupValues
    val authority = m.groups[4]?.value
    val hostPort = authority?.substringAfterLast('@')
    // A bracketed IPv6 host keeps its colons; the port follows the closing bracket.
    val portSep = hostPort?.let { if (it.startsWith("[")) it.indexOf("]:").let { i -> if (i < 0) -1 else i + 1 } else it.lastIndexOf(':') } ?: -1
    return UrlParts(
        scheme = m.groups[2]?.value,
        userInfo = authority?.takeIf { '@' in it }?.substringBeforeLast('@'),
        host = if (portSep >= 0) hostPort!!.substring(0, portSep) else hostPort,
        port = if (portSep >= 0) hostPort!!.substring(portSep + 1) else null,
        path = g[5],
        query = m.groups[7]?.value,
        fragment = m.groups[9]?.value
    )
}

private val TRACKING_PARAMS = setOf("fbclid", "gclid", "dclid", "gbraid", "wbraid", "msclkid", "yclid", "igshid", "mc_cid", "mc_eid", "_ga", "_gl")

internal fun isTrackingParam(name: String) = name.startsWith("utm_") || name in TRACKING_PARAMS

/** The URL with tracking parameters (utm_*, fbclid, gclid, ...) removed; other parameters keep their encoding. */
internal fun stripTracking(url: String): String {
    val trimmed = url.trim()
    val beforeFragment = trimmed.substringBefore('#')
    val fragment = trimmed.substringAfter('#', "").let { if ('#' in trimmed) "#$it" else "" }
    if ('?' !in beforeFragment) return trimmed
    val kept = beforeFragment.substringAfter('?').split('&').filter { it.isNotEmpty() && !isTrackingParam(urlDecode(it.substringBefore('='))) }
    return beforeFragment.substringBefore('?') + (if (kept.isEmpty()) "" else "?" + kept.joinToString("&")) + fragment
}

@Composable
private fun UrlParser() {
    var input by rememberSaveableText()
    val parts = remember(input) { if (input.isBlank()) null else parseUrl(input) }
    Column(Modifier.verticalScroll(rememberScrollState())) {
        MonoTextField(input, { input = it }, "URL to parse", minLines = 3)
        Spacer(Modifier.height(16.dp))
        if (parts != null) {
            ValueSlab(
                listOfNotNull(
                    "Scheme" to parts.scheme.orEmpty(),
                    parts.userInfo?.let { "User info" to it },
                    "Host" to parts.host.orEmpty(),
                    parts.port?.let { "Port" to it },
                    "Path" to urlDecode(parts.path.replace("+", "%2B")),
                    "Fragment" to parts.fragment?.let(::urlDecode).orEmpty()
                )
            )
            val params = parts.params
            if (params.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("Query parameters", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                ValueSlab(params)
                if (params.any { isTrackingParam(it.first) }) {
                    Spacer(Modifier.height(16.dp))
                    OutputPanel(remember(input) { stripTracking(input) }, label = "Without tracking parameters")
                }
            }
        }
    }
}

class UrlEncoderTool : Tool {
    override val descriptor = ToolDescriptor(
        id = "url",
        title = "URL Encoder / Decoder",
        description = "Percent-encode, decode and parse URLs",
        icon = Icons.Default.Link,
        category = ToolCategory.ENCODERS_DECODERS,
        keywords = listOf("url", "encode", "parse", "query", "utm", "percent"),
        accepts = setOf(ShareKind.TEXT)
    )

    @OptIn(ExperimentalMaterial3Api::class) // FilterChip
    @Composable
    override fun Content(modifier: Modifier) {
        var mode by rememberSaveable { mutableStateOf(UrlMode.COMPONENT) }
        var repeatDecode by rememberSaveable { mutableStateOf(false) }
        ToolScaffold(descriptor.title, modifier = modifier, scrollable = false) {
            EncodeDecodePane(
                encode = { urlEncode(it, mode) },
                decode = { if (repeatDecode) urlDecodeRepeatedly(it) else urlDecode(it) },
                optionsKey = mode to repeatDecode,
                decodeWarning = ::urlDecodeWarning,
                options = { isEncode ->
                    if (isEncode) {
                        ModeSelector(UrlMode.entries.map { it.label }, mode.ordinal, { mode = UrlMode.entries[it] })
                    } else {
                        FilterChip(selected = repeatDecode, onClick = { repeatDecode = !repeatDecode }, label = { Text("Decode repeatedly") })
                    }
                    Spacer(Modifier.height(8.dp))
                },
                extraMode = "Parse",
                extraContent = { UrlParser() }
            )
        }
    }
}
