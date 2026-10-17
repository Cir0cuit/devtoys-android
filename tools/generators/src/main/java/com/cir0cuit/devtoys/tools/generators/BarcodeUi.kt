package com.cir0cuit.devtoys.tools.generators

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.cir0cuit.devtoys.core.designsystem.CodeTextStyle
import com.cir0cuit.devtoys.core.designsystem.LocalSlabColor
import com.cir0cuit.devtoys.core.ui.shareText
import com.google.zxing.client.result.AddressBookParsedResult
import com.google.zxing.client.result.CalendarParsedResult
import com.google.zxing.client.result.EmailAddressParsedResult
import com.google.zxing.client.result.GeoParsedResult
import com.google.zxing.client.result.ParsedResult
import com.google.zxing.client.result.SMSParsedResult
import com.google.zxing.client.result.TelParsedResult
import com.google.zxing.client.result.URIParsedResult
import com.google.zxing.client.result.WifiParsedResult
import com.google.zxing.common.BitMatrix
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One scanned code: what it is, its fields (each copyable), the raw text and the actions that fit it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ScanResultCard(hit: ScanHit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val info = remember(hit) { describeScan(hit) }
    val actions = remember(hit) { scanActions(info.parsed) }
    val rows = info.rows + ((if (info.rows.isEmpty()) "Text" else "Raw text") to hit.text)
    Surface(modifier.fillMaxWidth(), color = LocalSlabColor.current, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(info.typeLabel, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                val format = formatLabel(hit.format) + (hit.ecLevel?.let { " · EC $it" } ?: "")
                Text(format, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            rows.forEach { (label, value) ->
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
                        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        SelectionContainer { Text(value, style = CodeTextStyle) }
                    }
                    IconButton(onClick = { clipboard.setText(AnnotatedString(value)) }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy $label", modifier = Modifier.size(20.dp))
                    }
                }
            }
            FlowRow(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                actions.forEachIndexed { i, (label, run) ->
                    if (i == 0) FilledTonalButton(onClick = { run(context) }) { Text(label) }
                    else OutlinedButton(onClick = { run(context) }) { Text(label) }
                }
                OutlinedButton(onClick = { shareText(context, hit.text) }) { Text("Share") }
            }
        }
    }
}

private fun startSafely(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No app on this device can open this.", Toast.LENGTH_SHORT).show()
    } catch (e: SecurityException) {
        Toast.makeText(context, "The app that handles this refused the request.", Toast.LENGTH_SHORT).show()
    }
}

/** Actions for a parsed scan; none of them needs a permission. */
private fun scanActions(p: ParsedResult): List<Pair<String, (Context) -> Unit>> = when (p) {
    is URIParsedResult -> listOf("Open link" to { c: Context -> startSafely(c, Intent(Intent.ACTION_VIEW, Uri.parse(p.getURI()))) })
    is WifiParsedResult -> buildList<Pair<String, (Context) -> Unit>> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !p.networkEncryption.equals("WEP", true)) {
            add("Join network" to { c: Context -> joinWifi(c, p) })
        }
        if (!p.password.isNullOrEmpty()) add("Copy password" to { c: Context ->
            copySensitive(c, "Wi-Fi password", p.password)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) Toast.makeText(c, "Copied", Toast.LENGTH_SHORT).show()
        })
    }
    is AddressBookParsedResult -> listOf("Add contact" to { c: Context ->
        startSafely(c, Intent(ContactsContract.Intents.Insert.ACTION).apply {
            type = ContactsContract.RawContacts.CONTENT_TYPE
            p.names?.firstOrNull()?.let { putExtra(ContactsContract.Intents.Insert.NAME, it) }
            p.phoneNumbers?.getOrNull(0)?.let { putExtra(ContactsContract.Intents.Insert.PHONE, it) }
            p.phoneNumbers?.getOrNull(1)?.let { putExtra(ContactsContract.Intents.Insert.SECONDARY_PHONE, it) }
            p.emails?.getOrNull(0)?.let { putExtra(ContactsContract.Intents.Insert.EMAIL, it) }
            p.emails?.getOrNull(1)?.let { putExtra(ContactsContract.Intents.Insert.SECONDARY_EMAIL, it) }
            p.org?.let { putExtra(ContactsContract.Intents.Insert.COMPANY, it) }
            p.title?.let { putExtra(ContactsContract.Intents.Insert.JOB_TITLE, it) }
            p.addresses?.firstOrNull()?.let { putExtra(ContactsContract.Intents.Insert.POSTAL, it) }
            p.note?.let { putExtra(ContactsContract.Intents.Insert.NOTES, it) }
        })
    })
    is EmailAddressParsedResult -> listOf("Write email" to { c: Context ->
        startSafely(c, Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
            putExtra(Intent.EXTRA_EMAIL, p.tos ?: emptyArray<String>())
            p.getCCs()?.let { putExtra(Intent.EXTRA_CC, it) }
            p.subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
            p.body?.let { putExtra(Intent.EXTRA_TEXT, it) }
        })
    })
    is SMSParsedResult -> listOf("Write SMS" to { c: Context ->
        val to = p.numbers?.joinToString(";").orEmpty()
        startSafely(c, Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(to))).apply { p.body?.let { putExtra("sms_body", it) } })
    })
    is TelParsedResult -> listOf("Dial" to { c: Context -> startSafely(c, Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(p.number)))) })
    is GeoParsedResult -> listOf("Open map" to { c: Context -> startSafely(c, Intent(Intent.ACTION_VIEW, Uri.parse(p.geoURI))) })
    is CalendarParsedResult -> listOf("Add to calendar" to { c: Context ->
        startSafely(c, Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI).apply {
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, p.startTimestamp)
            if (p.endTimestamp >= 0) putExtra(CalendarContract.EXTRA_EVENT_END_TIME, p.endTimestamp)
            putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, p.isStartAllDay)
            putExtra(CalendarContract.Events.TITLE, p.summary)
            p.location?.let { putExtra(CalendarContract.Events.EVENT_LOCATION, it) }
            p.description?.let { putExtra(CalendarContract.Events.DESCRIPTION, it) }
        })
    })
    else -> emptyList()
}

private fun joinWifi(context: Context, p: WifiParsedResult) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
    try {
        val suggestion = WifiNetworkSuggestion.Builder().setSsid(p.ssid).setIsHiddenSsid(p.isHidden).apply {
            val password = p.password.orEmpty()
            when {
                p.networkEncryption.equals("SAE", true) -> setWpa3Passphrase(password)
                p.networkEncryption.equals("nopass", true) || password.isEmpty() -> Unit
                else -> setWpa2Passphrase(password)
            }
        }.build()
        startSafely(context, Intent(Settings.ACTION_WIFI_ADD_NETWORKS).putParcelableArrayListExtra(Settings.EXTRA_WIFI_NETWORK_LIST, arrayListOf(suggestion)))
    } catch (e: IllegalArgumentException) {
        Toast.makeText(context, "Android rejected this network: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

/** Longest image edge the scanner works on; bigger photos are downscaled first. */
private const val MAX_SCAN_EDGE = 2000

/** Decodes [uri] into ARGB pixels at most [MAX_SCAN_EDGE] on the long edge. Call off the main thread. */
internal fun loadScanPixels(context: Context, uri: Uri): Triple<IntArray, Int, Int> {
    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val long = maxOf(info.size.width, info.size.height)
            if (long > MAX_SCAN_EDGE) {
                val f = MAX_SCAN_EDGE.toFloat() / long
                decoder.setTargetSize((info.size.width * f).toInt().coerceAtLeast(1), (info.size.height * f).toInt().coerceAtLeast(1))
            }
        }
    } else {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_SCAN_EDGE) sample *= 2
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
            ?: error("the image could not be read")
    }
    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    return Triple(pixels, bitmap.width, bitmap.height).also { bitmap.recycle() }
}

/** Renders [m] at [size], plus the human-readable line under 1D codes. */
internal fun renderBarcode(m: BitMatrix, l: BarcodeLayout, size: ExportSize, fg: Int, bg: Int, text: String?): Bitmap {
    val scale = size.scale
    val w = size.width
    val h = size.height
    val bitmap = Bitmap.createBitmap(rasterize(m, l, scale, fg, bg, size.padX, size.padY, w, h), w, h, Bitmap.Config.ARGB_8888)
    if (text != null && l.textRows > 0) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fg
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.CENTER
            textSize = 9f * scale
        }
        val maxWidth = m.width * scale.toFloat()
        val measured = paint.measureText(text)
        if (measured > maxWidth) paint.textSize *= maxWidth / measured
        // createBitmap(int[]) returns an immutable bitmap.
        val mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true).also { bitmap.recycle() }
        Canvas(mutable).drawText(text, w / 2f, (l.qy + l.barRows + 10f) * scale + size.padY, paint)
        return mutable
    }
    return bitmap
}

/** Writes [bytes] to the shared cache folder and opens the share sheet for it. */
internal suspend fun shareFile(context: Context, name: String, mime: String, bytes: ByteArray) {
    try {
        val uri = withContext(Dispatchers.IO) {
            val file = File(File(context.cacheDir, "images").apply { mkdirs() }, name)
            file.writeBytes(bytes)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, null))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Toast.makeText(context, "Could not share the image: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

/** Cache file the camera app writes the scan photo to; exposed through the app's FileProvider. */
internal fun scanPhotoUri(context: Context): Uri {
    val file = File(File(context.cacheDir, "images").apply { mkdirs() }, "scan.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
