package com.cir0cuit.devtoys.tools.developer

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*
import java.security.SecureRandom

class IpConverterTool : Tool {
    override val descriptor = ToolDescriptor(
        id = "ip-converter",
        title = "IP Address Converter",
        description = "Convert and inspect IPv4 and IPv6 addresses",
        icon = Icons.Default.Lan,
        category = ToolCategory.NETWORK_SYSTEM,
        keywords = listOf("ip", "ipv4", "ipv6", "address", "decimal", "hex", "binary", "ptr", "reverse dns", "ula", "6to4", "mapped")
    )

    @Composable
    override fun Content(modifier: Modifier) {
        var input by rememberSaveable { mutableStateOf("192.168.1.1") }
        var ula by rememberSaveable { mutableStateOf("") }
        val addr = remember(input) { parseIpFlexible(input) }

        ToolScaffold(descriptor.title, modifier = modifier) {
            MonoInputField(input, { input = it }, "IP address, integer or 0x hex", isError = addr == null)
            Spacer(Modifier.height(16.dp))
            if (addr == null) {
                NetError("Enter an IPv4 address like 192.168.1.1, an IPv6 address like 2001:db8::1, or a number like 3232235777 or 0xC0A80101.")
            } else {
                LabeledValueSlab(ipRows(addr))
            }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = { ula = ulaPrefix(SecureRandom().nextLong() and 0xFF_FFFF_FFFFL) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Generate ULA /48") }
            if (ula.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                LabeledValueSlab(listOf("Random unique local prefix (RFC 4193)" to ula))
            }
        }
    }
}

internal fun ipRows(a: IpAddr): List<Pair<String, String>> {
    val v = a.value
    return if (!a.v6) {
        val n = v.toLong()
        listOf(
            "Dotted decimal" to formatIp(a),
            "Decimal" to n.toString(),
            "Hex" to "0x%08X".format(n),
            "Octal" to "0" + java.lang.Long.toOctalString(n),
            "Binary" to java.lang.Long.toBinaryString(n or (1L shl 32)).drop(1).chunked(8).joinToString("."),
            "IPv4-mapped IPv6" to "::ffff:${formatIp(a)}",
            "6to4 prefix" to "2002:%x:%x::/48".format(n shr 16, n and 0xFFFF),
            "Reverse DNS (PTR)" to ptrName(a),
            "Class" to ipv4Class(n),
            "Address type" to classifyIp(a)
        )
    } else {
        val type = classifyIp(a)
        val embedded = when (type) {
            "IPv4-mapped", "NAT64 (RFC 6052)" -> formatIpv4(v.toLong() and 0xFFFFFFFFL)
            "6to4" -> formatIpv4(v.shiftRight(80).toLong() and 0xFFFFFFFFL)
            else -> null
        }
        val compressed = formatIp(a)
        listOfNotNull(
            "Compressed" to compressed,
            "Expanded" to expandIpv6(v),
            "Decimal" to v.toString(),
            "Hex" to "0x" + v.toString(16).padStart(32, '0').uppercase(),
            "Binary" to v.toString(2).padStart(128, '0').chunked(16).joinToString(":"),
            embedded?.let { "Embedded IPv4" to it },
            "Reverse DNS (PTR)" to ptrName(a),
            "URL literal" to "http://[$compressed]:8080/",
            "Address type" to type
        )
    }
}
