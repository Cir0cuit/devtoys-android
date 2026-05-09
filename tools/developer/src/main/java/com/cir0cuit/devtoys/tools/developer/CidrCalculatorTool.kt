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

class CidrCalculatorTool : Tool {
    override val descriptor = ToolDescriptor(
        "cidr",
        android.R.string.untitled,
        android.R.string.untitled,
        Icons.Default.Router,
        ToolCategory.NETWORK_SYSTEM,
        listOf("cidr", "ip", "subnet", "network")
    )

    @Composable
    override fun Content(modifier: Modifier) {
        var input by rememberSaveable { mutableStateOf("192.168.1.0/24") }
        
        val output = remember(input) {
            try {
                if (!input.contains("/")) throw Exception()
                val parts = input.split("/")
                val ipStr = parts[0]
                
                val isIpv4 = ipStr.contains(".") && !ipStr.contains(":")
                val isIpv6 = ipStr.contains(":")
                
                if (isIpv4) {
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
                    
                    """
                        Network: ${toIp(network)}
                        Netmask: ${toIp(mask)}
                        Broadcast: ${toIp(broadcast)}
                        First Host: ${toIp(minHost)}
                        Last Host: ${toIp(maxHost)}
                        Total Usable Hosts: $totalHosts
                    """.trimIndent()
                    
                } else if (isIpv6) {
                    val cidr = parts[1].toInt()
                    if (cidr < 0 || cidr > 128) throw Exception()
                    
                    val ipBytes = java.net.InetAddress.getByName(ipStr).address
                    if (ipBytes.size != 16) throw Exception()
                    
                    val allOnes = java.math.BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", 16)
                    val ipBigInt = java.math.BigInteger(1, ipBytes)
                    val maskBigInt = allOnes.shiftLeft(128 - cidr).and(allOnes)
                    val networkBigInt = ipBigInt.and(maskBigInt)
                    val broadcastBigInt = networkBigInt.or(maskBigInt.xor(allOnes))
                    
                    fun toIpv6(b: java.math.BigInteger): String {
                        val hex = String.format("%032x", b)
                        return java.net.InetAddress.getByName(hex.chunked(4).joinToString(":")).hostAddress
                    }
                    
                    val totalAddresses = if (cidr == 128) "1" else java.math.BigInteger.ONE.shiftLeft(128 - cidr).toString()
                    
                    """
                        Network: ${toIpv6(networkBigInt)}
                        Netmask: ${toIpv6(maskBigInt)}
                        Broadcast: ${toIpv6(broadcastBigInt)}
                        Total Addresses: $totalAddresses
                    """.trimIndent()
                } else {
                    throw Exception()
                }
            } catch (e: Exception) {
                "Invalid CIDR format. Try 192.168.1.0/24 or 2001:db8::/32"
            }
        }

        ToolScaffold("CIDR Calculator", modifier = modifier) {
            MonoTextField(
                value = input,
                onValueChange = { input = it },
                label = "IP/CIDR",
                minLines = 1,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutputPanel(output)
        }
    }
}
