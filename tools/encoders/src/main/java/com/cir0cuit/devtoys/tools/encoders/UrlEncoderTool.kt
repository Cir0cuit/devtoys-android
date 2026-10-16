package com.cir0cuit.devtoys.tools.encoders

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolCategory
import com.cir0cuit.devtoys.tools.base.ToolDescriptor
import java.net.URLDecoder
import java.net.URLEncoder

internal fun urlEncode(input: String): String =
    URLEncoder.encode(input, "UTF-8").replace("+", "%20").replace("*", "%2A").replace("%7E", "~")

internal fun urlDecode(input: String): String =
    try {
        // Escape lone '%' so URLDecoder does not reject the whole string.
        URLDecoder.decode(input.replace(Regex("%(?![0-9A-Fa-f]{2})"), "%25"), "UTF-8")
    } catch (e: Exception) {
        "Error: could not decode this text (${e.message})."
    }

class UrlEncoderTool : Tool {
    override val descriptor = ToolDescriptor(
        id = "url",
        title = "URL Encoder / Decoder",
        description = "Percent-encode and decode text for URLs",
        icon = Icons.Default.Link,
        category = ToolCategory.ENCODERS_DECODERS,
        keywords = listOf("url", "encode")
    )

    @Composable
    override fun Content(modifier: Modifier) {
        ToolScaffold(descriptor.title, modifier = modifier) {
            EncodeDecodePane(::urlEncode, ::urlDecode)
        }
    }
}
