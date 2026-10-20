package com.cir0cuit.devtoys.tools.developer

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.designsystem.LocalSlabColor
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val MAX_MAC_INPUT = 20_000
private const val MAX_MAC_ROWS = 200
private const val OUI_URL = "https://standards-oui.ieee.org/oui/oui.txt"
private const val MIN_OUI_PREFIXES = 1000
// The IEEE file is about 6.4 MB; anything far larger is not it.
private const val MAX_OUI_BYTES = 32L * 1024 * 1024

class MacAddressTool : Tool {
    override val descriptor = ToolDescriptor(
        id = "mac",
        title = "MAC Address Lookup",
        description = "Format a MAC address and find its vendor",
        icon = Icons.Default.SettingsEthernet,
        category = ToolCategory.NETWORK_SYSTEM,
        keywords = listOf("mac", "address", "format", "oui", "vendor", "eui-64", "arp")
    )

    @Composable
    override fun Content(modifier: Modifier) {
        var input by rememberSaveable { mutableStateOf("00:1A:2B:3C:4D:5E") }
        val context = LocalContext.current
        // Null while the vendor list loads; empty map if none is imported or it could not be read.
        var vendors by remember { mutableStateOf<Map<String, String>?>(null) }
        var importedAt by remember { mutableLongStateOf(0L) }
        // Bumped after an import or removal to reload the list.
        var listVersion by remember { mutableIntStateOf(0) }
        LaunchedEffect(listVersion) {
            val (map, time) = withContext(Dispatchers.IO) {
                runCatching { OuiIndex.get(context) }.getOrDefault(emptyMap()) to OuiIndex.file(context).lastModified()
            }
            vendors = map
            importedAt = time
        }

        val parsed = remember(input) { macsFromInput(input) }
        fun vendorOf(mac: String): String = when {
            isLocalMac(mac) -> "None: locally administered"
            vendors == null -> "Looking up..."
            vendors!!.isEmpty() -> "No vendor list imported"
            else -> vendors!![mac.take(6)] ?: "Not in the imported list"
        }

        ToolScaffold(descriptor.title, modifier = modifier) {
            MonoInputField(input, { input = it.take(MAX_MAC_INPUT) }, "MAC address, EUI-64 IPv6 or pasted text", isError = input.isNotBlank() && parsed.macs.isEmpty(), maxLines = 8)
            Spacer(modifier = Modifier.height(16.dp))
            when {
                parsed.macs.isEmpty() -> Text(
                    "Enter 12 hex digits like 00:1A:2B:3C:4D:5E, an EUI-64 IPv6 address, or paste arp -a or ip neigh output.",
                    color = if (input.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                parsed.macs.size == 1 -> {
                    val mac = parsed.macs[0]
                    LabeledValueSlab(macRows(mac, vendorOf(mac), parsed.fromIpv6))
                    if (isLocalMac(mac)) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "The locally administered bit is set. This is usually a randomized privacy MAC (Android, iOS and Windows Wi-Fi), so no vendor applies.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    val macs = parsed.macs
                    Text(
                        if (macs.size > MAX_MAC_ROWS) "Found ${macs.size} addresses, showing the first $MAX_MAC_ROWS." else "Found ${macs.size} addresses",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    LabeledValueSlab(macs.take(MAX_MAC_ROWS).map { "${vendorOf(it)} · ${macKind(it)}" to formatMac(it, ":") })
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            val csv = "mac,vendor,type,administration\n" + macs.joinToString("\n") { m ->
                                listOf(formatMac(m, ":"), vendorOf(m), macKind(m), if (isLocalMac(m)) "local" else "universal")
                                    .joinToString(",") { v -> if (v.contains(',') || v.contains('"')) "\"${v.replace("\"", "\"\"")}\"" else v }
                            }
                            shareText(context, csv, "MAC addresses")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                        Text("Share as CSV")
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            VendorListCard(vendors, importedAt, onChanged = { listVersion++ })
        }
    }
}

/** Shows whether a vendor list is imported and lets the user download, import, replace or remove it. */
@Composable
private fun VendorListCard(vendors: Map<String, String>?, importedAt: Long, onChanged: () -> Unit) {
    val context = LocalContext.current
    // The import can outlive this composition (rotation); don't hold the Activity meanwhile.
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true
        message = null
        scope.launch {
            val accepted = withContext(Dispatchers.IO) { runCatching { OuiIndex.import(appContext, uri) }.getOrNull() }
            busy = false
            message = when (accepted) {
                true -> null
                false -> "This doesn't look like the IEEE oui.txt. Download oui.txt from the IEEE and import that file."
                null -> "Could not read the file."
            }
            if (accepted == true) onChanged()
        }
    }
    val pick = { launcher.launch(arrayOf("text/plain", "text/*", "application/octet-stream")) }
    val imported = !vendors.isNullOrEmpty()

    Surface(Modifier.fillMaxWidth(), color = LocalSlabColor.current, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(16.dp)) {
            Text("Vendor list", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                if (imported) {
                    val date = DateTimeFormatter.ofPattern("d MMM yyyy").format(Instant.ofEpochMilli(importedAt).atZone(ZoneId.systemDefault()))
                    "%,d vendor prefixes, imported %s.".format(vendors!!.size, date)
                } else {
                    "Vendor names come from the IEEE public OUI listing, which DevToys doesn't include. Download oui.txt from the IEEE in your browser, then import it here. The file stays on this device."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (imported) {
                    OutlinedButton(onClick = pick, enabled = !busy) { Text("Replace") }
                    OutlinedButton(
                        onClick = {
                            message = null
                            scope.launch {
                                withContext(Dispatchers.IO) { OuiIndex.remove(appContext) }
                                onChanged()
                            }
                        },
                        enabled = !busy
                    ) { Text("Remove") }
                } else {
                    OutlinedButton(onClick = { openUrl(context, OUI_URL) }) { Text("Download from IEEE") }
                    FilledTonalButton(onClick = pick, enabled = !busy && vendors != null) { Text("Import oui.txt") }
                }
            }
            if (busy) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            message?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, url, Toast.LENGTH_LONG).show()
    }
}

/**
 * The oui.txt the user imported into app-private storage, indexed once per process:
 * 6-hex-digit prefix to organization name. Empty when nothing is imported.
 */
private object OuiIndex {
    @Volatile private var map: Map<String, String>? = null

    fun file(context: Context) = File(context.filesDir, "oui.txt")

    fun get(context: Context): Map<String, String> = map ?: synchronized(this) {
        map ?: file(context).let { f -> if (f.exists()) f.bufferedReader().useLines { parseOui(it) } else emptyMap() }.also { map = it }
    }

    /**
     * Streams [uri] into a temporary file and moves it over oui.txt only if it parses as an IEEE
     * listing. Returns false when the file is rejected; throws when it cannot be read.
     */
    fun import(context: Context, uri: Uri): Boolean = synchronized(this) { importLocked(context, uri) }

    private fun importLocked(context: Context, uri: Uri): Boolean {
        val tmp = File(context.filesDir, "oui.txt.tmp")
        try {
            val withinLimit = context.contentResolver.openInputStream(uri)?.use { input ->
                tmp.outputStream().use { out -> copyAtMost(input, out, MAX_OUI_BYTES) }
            } ?: throw IOException("Could not open the file")
            if (!withinLimit) return false
            val parsed = tmp.bufferedReader().useLines { parseOui(it) }
            if (!isOuiListing(parsed)) return false
            if (!tmp.renameTo(file(context))) throw IOException("Could not save the file")
            map = parsed
            return true
        } finally {
            tmp.delete()
        }
    }

    fun remove(context: Context) = synchronized(this) {
        file(context).delete()
        map = emptyMap()
    }
}

/** Copies at most [limit] bytes; false if the input is longer. */
private fun copyAtMost(input: InputStream, out: OutputStream, limit: Long): Boolean {
    val buf = ByteArray(64 * 1024)
    var total = 0L
    while (true) {
        val n = input.read(buf)
        if (n < 0) return true
        total += n
        if (total > limit) return false
        out.write(buf, 0, n)
    }
}

/** True when a parsed file is plausibly the IEEE oui.txt rather than some other text file. */
internal fun isOuiListing(map: Map<String, String>) = map.size >= MIN_OUI_PREFIXES

/** Reads the "XXXXXX     (base 16)\t\tVendor" lines of the IEEE oui.txt. */
internal fun parseOui(lines: Sequence<String>): Map<String, String> {
    val map = HashMap<String, String>(65536)
    for (line in lines) {
        val i = line.indexOf("(base 16)")
        if (i < 6) continue
        val prefix = line.substring(0, 6)
        if (!prefix.all { it in '0'..'9' || it in 'A'..'F' }) continue
        val vendor = line.substring(i + 9).trim()
        if (vendor.isNotEmpty()) map.putIfAbsent(prefix, vendor)
    }
    return map
}

private val MAC_REGEX = Regex(
    "(?i)(?<![0-9a-f:])(?:[0-9a-f]{2}([:-])(?:[0-9a-f]{2}\\1){4}[0-9a-f]{2}|[0-9a-f]{4}\\.[0-9a-f]{4}\\.[0-9a-f]{4}|[0-9a-f]{12})(?![0-9a-f:])"
)

internal data class MacInput(val macs: List<String>, val fromIpv6: Boolean = false)

/** Every MAC in the text as 12 uppercase hex digits, or the MAC inside an EUI-64 IPv6 address. */
internal fun macsFromInput(text: String): MacInput {
    val t = text.trim()
    if (t.contains("::") || t.count { it == ':' } == 7 && t.length > 17) {
        parseIpv6Bytes(t)?.let { b -> eui64ToMac(b)?.let { return MacInput(listOf(it), fromIpv6 = true) } }
    }
    val found = MAC_REGEX.findAll(t).map { m -> m.value.filter { it.isLetterOrDigit() }.uppercase() }.distinct().toList()
    if (found.isNotEmpty()) return MacInput(found)
    val hex = t.filter { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }.uppercase()
    return MacInput(if (hex.length == 12) listOf(hex) else emptyList())
}

internal fun formatMac(mac: String, sep: String) = mac.chunked(2).joinToString(sep)

private fun firstOctet(mac: String) = mac.take(2).toInt(16)

internal fun isLocalMac(mac: String) = firstOctet(mac) and 2 != 0

internal fun macKind(mac: String) = when {
    mac == "FFFFFFFFFFFF" -> "broadcast"
    firstOctet(mac) and 1 != 0 -> "multicast"
    else -> "unicast"
}

/** Modified EUI-64 interface ID (RFC 4291 appendix A) as four hextets. */
internal fun macToEui64(mac: String): List<Int> {
    val b = mac.chunked(2).map { it.toInt(16) }.toMutableList()
    b[0] = b[0] xor 2
    val e = b.take(3) + listOf(0xFF, 0xFE) + b.drop(3)
    return e.chunked(2).map { (hi, lo) -> (hi shl 8) or lo }
}

/** MAC inside an EUI-64 interface ID (ff:fe in the middle), or null. */
internal fun eui64ToMac(addr: ByteArray): String? {
    if (addr[11] != 0xFF.toByte() || addr[12] != 0xFE.toByte()) return null
    val b = listOf(8, 9, 10, 13, 14, 15).map { addr[it].toInt() and 0xFF }.toMutableList()
    b[0] = b[0] xor 2
    return b.joinToString("") { "%02X".format(it) }
}

private fun macRows(mac: String, vendor: String, fromIpv6: Boolean): List<Pair<String, String>> {
    val eui = macToEui64(mac)
    return listOfNotNull(
        if (fromIpv6) "MAC address" to formatMac(mac, ":") else null,
        "Vendor" to vendor,
        "Type" to macKind(mac).replaceFirstChar { it.uppercase() },
        "Administration" to if (isLocalMac(mac)) "Local (U/L bit set)" else "Universal (vendor assigned)",
        "Colon separated" to formatMac(mac, ":"),
        "Hyphen separated" to formatMac(mac, "-"),
        "Cisco dot format" to mac.chunked(4).joinToString(".").lowercase(),
        "EUI-64 interface ID" to eui.joinToString(":") { "%x".format(it) },
        "IPv6 link-local (SLAAC)" to formatIpv6Hextets(listOf(0xFE80, 0, 0, 0) + eui)
    )
}
