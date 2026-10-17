package com.cir0cuit.devtoys.tools.developer

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*
import java.math.BigInteger

private const val MAX_SUBNETS = 1024
private const val MAX_LIST_CHARS = 20_000

class CidrCalculatorTool : Tool {
    override val descriptor = ToolDescriptor(
        id = "cidr",
        title = "CIDR Calculator",
        description = "Work out the address range of a subnet",
        icon = Icons.Default.Router,
        category = ToolCategory.NETWORK_SYSTEM,
        keywords = listOf("cidr", "ip", "subnet", "network", "netmask", "vlsm", "split", "range", "aggregate", "merge", "wildcard")
    )

    @Composable
    override fun Content(modifier: Modifier) {
        // All inputs live here so switching modes keeps them.
        var mode by rememberSaveable { mutableIntStateOf(0) }
        var input by rememberSaveable { mutableStateOf("192.168.1.0/24") }
        var tests by rememberSaveable { mutableStateOf("") }
        var byHosts by rememberSaveable { mutableStateOf(false) }
        var newPrefix by rememberSaveable { mutableStateOf("26") }
        var hosts by rememberSaveable { mutableStateOf("50, 20, 10") }
        var rangeStart by rememberSaveable { mutableStateOf("192.168.1.10") }
        var rangeEnd by rememberSaveable { mutableStateOf("192.168.1.100") }
        var list by rememberSaveable { mutableStateOf("10.0.0.0/24\n10.0.1.0/24\n10.0.2.0/23") }
        var listTests by rememberSaveable { mutableStateOf("") }

        ToolScaffold(descriptor.title, modifier = modifier) {
            NetChoiceRow(listOf("Subnet", "Split", "Range", "Merge"), mode, { mode = it })
            Spacer(Modifier.height(16.dp))
            when (mode) {
                0 -> SubnetMode(input, { input = it }, tests, { tests = it.take(MAX_LIST_CHARS) })
                1 -> SplitMode(input, { input = it }, byHosts, { byHosts = it }, newPrefix, { newPrefix = it }, hosts, { hosts = it })
                2 -> RangeMode(rangeStart, { rangeStart = it }, rangeEnd, { rangeEnd = it })
                else -> MergeMode(list, { list = it.take(MAX_LIST_CHARS) }, listTests, { listTests = it.take(MAX_LIST_CHARS) })
            }
        }
    }

    // Kept as members for existing callers and tests.
    internal fun parseIpv6(s: String): ByteArray? = parseIpv6Bytes(s)
    internal fun formatIpv6(hextets: List<Int>): String = formatIpv6Hextets(hextets)
}

@Composable
private fun SubnetMode(input: String, onInput: (String) -> Unit, tests: String, onTests: (String) -> Unit) {
    val parsed = remember(input) { runCatching { parseCidr(input) } }
    val c = parsed.getOrNull()
    MonoInputField(input, onInput, "Address and prefix or netmask", isError = c == null)
    Spacer(Modifier.height(16.dp))
    if (c == null) {
        NetError(parsed.exceptionOrNull()?.message ?: "Not a valid CIDR range.")
        return
    }
    LabeledValueSlab(subnetRows(c))
    Spacer(Modifier.height(16.dp))
    MonoTextField(tests, onTests, "Test addresses, one per line", minLines = 2, maxLines = 8)
    if (tests.isNotBlank()) {
        Spacer(Modifier.height(8.dp))
        val result = rememberNetResult(c, tests) { membership(tests, listOf(c)) }
        OutputPanel(result?.getOrNull().orEmpty(), label = "Membership")
    }
}

internal fun subnetRows(c: Cidr): List<Pair<String, String>> {
    val mask = IpAddr(prefixMask(c.ip.bits, c.prefix), c.ip.v6)
    return if (!c.ip.v6) {
        val m = mask.value.toLong()
        listOf(
            "Network" to c.toString(),
            "Netmask" to formatIp(mask),
            "Wildcard mask" to formatIpv4(m xor 0xFFFFFFFFL),
            "Netmask hex" to "0x%08X".format(m),
            "Netmask binary" to java.lang.Long.toBinaryString(m or (1L shl 32)).drop(1).chunked(8).joinToString("."),
            "Broadcast" to formatIp(c.last),
            "First host" to formatIp(firstHost(c)),
            "Last host" to formatIp(lastHost(c)),
            "Usable hosts" to usableHosts(c).toString(),
            "Class" to ipv4Class(c.ip.value.toLong()),
            "Address type" to classifyIp(c.ip),
            "Reverse DNS zone" to reverseZone(c)
        )
    } else {
        listOfNotNull(
            "Network" to c.toString(),
            "Netmask" to formatIp(mask),
            "First address" to formatIp(c.first),
            "Last address" to formatIp(c.last),
            "Total addresses" to c.size.toString(),
            if (c.prefix <= 64) "/64 subnets" to BigInteger.ONE.shiftLeft(64 - c.prefix).toString() else null,
            "Address type" to classifyIp(c.ip),
            "Reverse DNS zone" to reverseZone(c)
        )
    }
}

@Composable
private fun SplitMode(
    input: String, onInput: (String) -> Unit,
    byHosts: Boolean, onByHosts: (Boolean) -> Unit,
    newPrefix: String, onNewPrefix: (String) -> Unit,
    hosts: String, onHosts: (String) -> Unit
) {
    val context = LocalContext.current
    val network = remember(input) { runCatching { parseCidr(input) } }
    // Computed in composition like Subnet, so the first frame already shows the subnets; capped at MAX_SUBNETS.
    val result = remember(network, byHosts, newPrefix, hosts) {
        network.mapCatching { c ->
            if (byHosts) {
                val needs = hosts.split(',', ' ', ';', '\n').filter { it.isNotBlank() }.map {
                    it.trim().toIntOrNull()?.takeIf { n -> n > 0 } ?: throw IllegalArgumentException("\"${it.trim()}\" is not a host count. Enter whole numbers above 0.")
                }
                require(needs.size <= MAX_SUBNETS) { "Enter at most $MAX_SUBNETS host counts." }
                splitByHosts(c, needs).let { it to BigInteger.valueOf(it.size.toLong()) }
            } else {
                val p = newPrefix.trim().removePrefix("/").toIntOrNull() ?: throw IllegalArgumentException("Enter the new prefix length as a number, like 26.")
                splitByPrefix(c, p, MAX_SUBNETS)
            }
        }
    }
    MonoInputField(input, onInput, "Network to split", isError = network.isFailure)
    Spacer(Modifier.height(16.dp))
    NetChoiceRow(listOf("By prefix", "By host count"), if (byHosts) 1 else 0, { onByHosts(it == 1) })
    Spacer(Modifier.height(8.dp))
    val splitError = network.isSuccess && result.isFailure
    if (byHosts) MonoInputField(hosts, onHosts, "Hosts per subnet, like 50, 20, 10", isError = splitError)
    else MonoInputField(newPrefix, onNewPrefix, "New prefix length", isError = splitError)
    Spacer(Modifier.height(16.dp))

    val (subnets, total) = result.getOrElse { NetError(it.message ?: "Could not split this network."); return }

    Text(
        if (total > BigInteger.valueOf(subnets.size.toLong())) "Showing the first ${subnets.size} of $total subnets." else "${subnets.size} subnets",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(8.dp))
    val text = remember(subnets) {
        subnets.joinToString("\n") { s ->
            val need = s.needed?.let { "needs $it, " }.orEmpty()
            "${s.cidr}  ($need${usableHosts(s.cidr)} usable)\n  ${firstHost(s.cidr)} - ${lastHost(s.cidr)}"
        }
    }
    OutputPanel(text, label = "Subnets")
    Spacer(Modifier.height(8.dp))
    OutlinedButton(onClick = { shareText(context, subnetsCsv(subnets), "Subnets") }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text("Share as CSV")
    }
}

internal fun subnetsCsv(subnets: List<Subnet>): String =
    "network,prefix,first_host,last_host,broadcast,usable_hosts,needed\n" + subnets.joinToString("\n") { s ->
        val c = s.cidr
        listOf(c.first, c.prefix, firstHost(c), lastHost(c), if (c.ip.v6) "" else c.last, usableHosts(c), s.needed ?: "").joinToString(",")
    }

@Composable
private fun RangeMode(start: String, onStart: (String) -> Unit, end: String, onEnd: (String) -> Unit) {
    val result = rememberNetResult(start, end) {
        val a = parseIp(start) ?: throw IllegalArgumentException("The start is not an IPv4 or IPv6 address.")
        val b = parseIp(end) ?: throw IllegalArgumentException("The end is not an IPv4 or IPv6 address.")
        rangeToCidrs(a, b).joinToString("\n")
    }
    // A bad address marks its own field; a family mismatch or reversed range marks both.
    val failed = result?.isFailure == true
    val startBad = failed && parseIp(start) == null
    val endBad = failed && parseIp(end) == null
    MonoInputField(start, onStart, "Start address", isError = startBad || (failed && !endBad))
    Spacer(Modifier.height(8.dp))
    MonoInputField(end, onEnd, "End address", isError = endBad || (failed && !startBad))
    Spacer(Modifier.height(16.dp))
    if (result == null) return
    result.fold({ OutputPanel(it, label = "CIDR blocks") }, { NetError(it.message ?: "Could not convert this range.") })
}

@Composable
private fun MergeMode(list: String, onList: (String) -> Unit, tests: String, onTests: (String) -> Unit) {
    val parsed = rememberNetResult(list) { parseCidrList(list).let { it to mergeCidrs(it) } }
    MonoInputField(list, onList, "CIDR list, one per line", isError = parsed?.isFailure == true, minLines = 4, maxLines = 12)
    Spacer(Modifier.height(16.dp))
    if (parsed == null) return
    val (blocks, merged) = parsed.getOrElse { NetError(it.message ?: "Could not read the list."); return }
    OutputPanel(merged.joinToString("\n"), label = "Merged (${blocks.size} in, ${merged.size} out)")
    Spacer(Modifier.height(16.dp))
    MonoTextField(tests, onTests, "Test addresses, one per line", minLines = 2, maxLines = 8)
    if (tests.isNotBlank()) {
        Spacer(Modifier.height(8.dp))
        val result = rememberNetResult(blocks, tests) { membership(tests, blocks) }
        OutputPanel(result?.getOrNull().orEmpty(), label = "Membership")
    }
}
