package com.cir0cuit.devtoys.tools.developer

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*
import java.math.BigInteger

class CidrCalculatorTool : Tool {
    override val descriptor = ToolDescriptor(
        id = "cidr",
        title = "CIDR Calculator",
        description = "Work out the address range of a subnet",
        icon = Icons.Default.Router,
        category = ToolCategory.NETWORK_SYSTEM,
        keywords = listOf("cidr", "ip", "subnet", "network")
    )

    @Composable
    override fun Content(modifier: Modifier) {
        var input by rememberSaveable { mutableStateOf("192.168.1.0/24") }

        val rows = remember(input) {
            try {
                val trimmed = input.trim()
                if (!trimmed.contains("/")) throw Exception()
                val parts = trimmed.split("/").map { it.trim() }
                if (parts.size != 2) throw Exception()
                val ipStr = parts[0]

                if (ipStr.contains(".") && !ipStr.contains(":")) {
                    val octets = ipStr.split(".")
                    if (octets.size != 4) throw Exception()
                    if (octets.any { it.toIntOrNull() == null || it.toInt() !in 0..255 }) throw Exception()
                    
                    val cidr = parts[1].toInt()
                    if (cidr < 0 || cidr > 32) throw Exception()
                    
                    val ipLong = (octets[0].toLong() shl 24) or 
                                 (octets[1].toLong() shl 16) or 
                                 (octets[2].toLong() shl 8) or 
                                 (octets[3].toLong())
                                 
                    val mask = (-1L shl (32 - cidr)) and 0xFFFFFFFFL
                    val network = ipLong and mask
                    val broadcast = network or mask.inv() and 0xFFFFFFFFL
                    val minHost = if (cidr < 31) network + 1 else network
                    val maxHost = if (cidr < 31) broadcast - 1 else broadcast
                    val totalHosts = if (cidr == 32) 1 else if (cidr == 31) 2 else (maxHost - minHost + 1)
                    
                    fun toIp(v: Long) = "${(v shr 24) and 0xFF}.${(v shr 16) and 0xFF}.${(v shr 8) and 0xFF}.${v and 0xFF}"
                    
                    listOf(
                        "Network" to toIp(network),
                        "Netmask" to toIp(mask),
                        "Broadcast" to toIp(broadcast),
                        "First host" to toIp(minHost),
                        "Last host" to toIp(maxHost),
                        "Usable hosts" to totalHosts.toString()
                    )
                    
                } else if (ipStr.contains(":")) {
                    val cidr = parts[1].toInt()
                    if (cidr < 0 || cidr > 128) throw Exception()
                    
                    val ipBytes = parseIpv6(ipStr) ?: throw Exception()
                    
                    val allOnes = BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", 16)
                    val ipBigInt = BigInteger(1, ipBytes)
                    val maskBigInt = allOnes.shiftLeft(128 - cidr).and(allOnes)
                    val networkBigInt = ipBigInt.and(maskBigInt)
                    val broadcastBigInt = networkBigInt.or(maskBigInt.xor(allOnes))
                    
                    fun toIpv6(b: BigInteger) = formatIpv6(b.toString(16).padStart(32, '0').chunked(4).map { it.toInt(16) })
                    
                    val totalAddresses = if (cidr == 128) "1" else BigInteger.ONE.shiftLeft(128 - cidr).toString()
                    
                    listOf(
                        "Network" to toIpv6(networkBigInt),
                        "Netmask" to toIpv6(maskBigInt),
                        "Broadcast" to toIpv6(broadcastBigInt),
                        "Total addresses" to totalAddresses
                    )
                } else {
                    throw Exception()
                }
            } catch (e: Exception) {
                null
            }
        }

        ToolScaffold(descriptor.title, modifier = modifier) {
            MonoTextField(
                value = input,
                onValueChange = { input = it },
                label = "Address and prefix",
                minLines = 1,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (rows != null) {
                LabeledValueSlab(rows)
            } else {
                Text(
                    "Not a valid CIDR range. Enter an address and prefix length, like 192.168.1.0/24 or 2001:db8::/32.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    internal fun parseIpv6(s: String): ByteArray? {
        if (s.endsWith('%')) return null
        var str = s.substringBefore('%')
        val lastColon = str.lastIndexOf(':')
        if (lastColon < 0) return null
        val tail = str.substring(lastColon + 1)
        if (tail.contains('.')) {
            val octets = tail.split(".")
            if (octets.size != 4) return null
            val values = octets.map { o -> o.takeIf { it.length in 1..3 && it.all(Char::isDigit) }?.toInt()?.takeIf { it in 0..255 } ?: return null }
            str = str.substring(0, lastColon + 1) +
                Integer.toHexString((values[0] shl 8) or values[1]) + ":" +
                Integer.toHexString((values[2] shl 8) or values[3])
        }

        fun groups(part: String): List<Int>? {
            if (part.isEmpty()) return emptyList()
            return part.split(":").map { g ->
                if (g.length !in 1..4 || !g.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) return null
                g.toInt(16)
            }
        }

        val dc = str.indexOf("::")
        val hextets: List<Int> = if (dc >= 0) {
            if (str.lastIndexOf("::") != dc) return null
            val head = groups(str.substring(0, dc)) ?: return null
            val rest = groups(str.substring(dc + 2)) ?: return null
            if (head.size + rest.size > 7) return null
            head + List(8 - head.size - rest.size) { 0 } + rest
        } else {
            groups(str)?.takeIf { it.size == 8 } ?: return null
        }

        val bytes = ByteArray(16)
        hextets.forEachIndexed { i, h ->
            bytes[i * 2] = (h shr 8).toByte()
            bytes[i * 2 + 1] = h.toByte()
        }
        return bytes
    }

    internal fun formatIpv6(hextets: List<Int>): String {
        var bestStart = -1
        var bestLen = 0
        var i = 0
        while (i < hextets.size) {
            if (hextets[i] == 0) {
                var j = i
                while (j < hextets.size && hextets[j] == 0) j++
                if (j - i > bestLen) { bestStart = i; bestLen = j - i }
                i = j
            } else {
                i++
            }
        }
        val hex = hextets.map { Integer.toHexString(it) }
        if (bestLen < 2) return hex.joinToString(":")
        val head = hex.subList(0, bestStart).joinToString(":")
        val tail = hex.subList(bestStart + bestLen, hex.size).joinToString(":")
        return "$head::$tail"
    }
}
