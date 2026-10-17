package com.cir0cuit.devtoys.tools.generators

import com.cir0cuit.devtoys.tools.base.OnSharedImage
import com.cir0cuit.devtoys.tools.base.OnSharedText
import com.cir0cuit.devtoys.tools.base.ShareKind
import android.content.ActivityNotFoundException
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.MonoTextField
import com.cir0cuit.devtoys.core.ui.OutputPanel
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.core.ui.rememberFileSaver
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolCategory
import com.cir0cuit.devtoys.tools.base.ToolDescriptor
import com.google.zxing.BarcodeFormat
import com.google.zxing.common.BitMatrix
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.Color as ComposeColor

/** Longest text the generator keeps; QR tops out at 4296 alphanumeric characters. */
private const val MAX_INPUT = 4296
private const val PREVIEW_PX = 720
private val EXPORT_SIZES = listOf(512, 1024, 2048)
private val BARCODE_COLORS = listOf("Black" to 0xFF000000.toInt(), "Navy" to 0xFF1A237E.toInt(), "Green" to 0xFF1B5E20.toInt(), "Maroon" to 0xFF7F1D1D.toInt())

private class Generated(val matrix: BitMatrix, val layout: BarcodeLayout, val text: String?, val preview: ImageBitmap)

private val ScanHitsSaver = listSaver<List<ScanHit>, String>(
    save = { hits -> hits.flatMap { listOf(it.format.name, it.text, it.ecLevel.orEmpty()) } },
    restore = { flat -> flat.chunked(3).map { (f, t, ec) -> ScanHit(BarcodeFormat.valueOf(f), t, ec.ifEmpty { null }) } },
)

class QrGeneratorTool : Tool {
    override val descriptor = ToolDescriptor(
        "qr-gen", "QR & Barcode Generator", "Make and scan QR codes and barcodes", Icons.Default.QrCode, ToolCategory.GENERATORS,
        listOf("qr", "barcode", "generator", "scanner", "scan", "ean", "upc", "datamatrix", "aztec", "pdf417", "code128", "wifi", "vcard"),
        accepts = setOf(ShareKind.TEXT, ShareKind.IMAGE)
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable override fun Content(modifier: Modifier) {
        var mode by rememberSaveable { mutableIntStateOf(0) }
        OnSharedText { mode = 0 }
        OnSharedImage { mode = 1 }
        // Keeps each pane's saved input while the other one is shown.
        val panes = rememberSaveableStateHolder()
        ToolScaffold(descriptor.title, modifier = modifier) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf("Generate", "Scan").forEachIndexed { i, label ->
                    SegmentedButton(
                        selected = mode == i, onClick = { mode = i },
                        shape = SegmentedButtonDefaults.itemShape(i, 2), icon = {}, label = { Text(label) }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            panes.SaveableStateProvider(mode) {
                if (mode == 0) GeneratePane() else ScanPane()
            }
        }
    }
}

@Composable
private fun OptionLabel(text: String) {
    Spacer(Modifier.height(12.dp))
    Text(text, style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(4.dp))
}

private fun fileStem(kind: BarcodeKind) = "barcode-" + kind.label.lowercase().replace(Regex("[^a-z0-9]+"), "-")

private fun keyboardFor(kind: FieldKind) = when (kind) {
    FieldKind.URL -> KeyboardType.Uri
    FieldKind.EMAIL -> KeyboardType.Email
    FieldKind.PHONE -> KeyboardType.Phone
    FieldKind.DECIMAL -> KeyboardType.Decimal
    FieldKind.PASSWORD -> KeyboardType.Password
    else -> KeyboardType.Text
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ColumnScope.GeneratePane() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var kind by rememberSaveable { mutableStateOf(BarcodeKind.QR) }
    var type by rememberSaveable { mutableStateOf(PayloadType.TEXT) }
    var text by rememberSaveable { mutableStateOf(BarcodeKind.QR.sample) }
    OnSharedText { text = it; type = PayloadType.TEXT }
    var fields by rememberSaveable { mutableStateOf<Map<String, String>>(emptyMap()) }
    var ec by rememberSaveable { mutableStateOf(EcLevel.M) }
    var dmShape by rememberSaveable { mutableStateOf(DmShape.AUTO) }
    var pdfCompact by rememberSaveable { mutableStateOf(false) }
    var gs1 by rememberSaveable { mutableStateOf(false) }
    var showText by rememberSaveable { mutableStateOf(true) }
    var transparent by rememberSaveable { mutableStateOf(false) }
    var inverted by rememberSaveable { mutableStateOf(false) }
    var quiet by rememberSaveable { mutableIntStateOf(BarcodeKind.QR.quiet) }
    var colorIndex by rememberSaveable { mutableIntStateOf(0) }
    var sizeIndex by rememberSaveable { mutableIntStateOf(1) }

    val usesTemplate = kind.is2D && type != PayloadType.TEXT
    val payload = remember(usesTemplate, type, text, fields) {
        if (usesTemplate) runCatching { buildPayload(type, fields) } else Result.success(text)
    }
    val content = payload.getOrNull()
    val opts = BarcodeOptions(ec, dmShape, pdfCompact, gs1 && kind.supportsGs1)
    val ink = BARCODE_COLORS[colorIndex].second
    val fg = if (inverted) Color.WHITE else ink
    val bg = when {
        transparent -> Color.TRANSPARENT
        inverted -> ink
        else -> Color.WHITE
    }
    val withText = showText && !kind.is2D

    var generated by remember { mutableStateOf<Generated?>(null) }
    var encodeError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(content, kind, opts, quiet, fg, bg, withText) {
        if (content.isNullOrEmpty()) {
            generated = null
            encodeError = null
            return@LaunchedEffect
        }
        delay(150)
        val result = withContext(Dispatchers.Default) {
            runCatching {
                val m = encodeBarcode(content, kind, opts)
                val l = barcodeLayout(m, quiet, withText)
                val human = if (withText) withCheckDigit(content, kind) else null
                Generated(m, l, human, renderBarcode(m, l, maxOf(1, PREVIEW_PX / l.cols), fg, bg, human).asImageBitmap())
            }
        }
        generated = result.getOrNull()
        encodeError = result.exceptionOrNull()?.let { it.message ?: "Could not create the code." }
    }

    val savePng = rememberFileSaver("image/png")
    val saveSvg = rememberFileSaver("image/svg+xml")
    val exportScale = generated?.let { maxOf(1, EXPORT_SIZES[sizeIndex] / it.layout.cols) } ?: 1
    fun exportPng(then: suspend (ByteArray) -> Unit) {
        val g = generated ?: return
        scope.launch {
            val bytes = withContext(Dispatchers.Default) {
                val bmp = renderBarcode(g.matrix, g.layout, exportScale, fg, bg, g.text)
                ByteArrayOutputStream().also { bmp.compress(Bitmap.CompressFormat.PNG, 100, it); bmp.recycle() }.toByteArray()
            }
            then(bytes)
        }
    }

    // Input
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SelectField("Format", kind, BarcodeKind.entries, { new ->
            if (text == kind.sample || text.isEmpty()) text = new.sample
            kind = new
            quiet = new.quiet
        }, Modifier.weight(1f)) { it.label }
        if (kind.is2D) SelectField("Content", type, PayloadType.entries, { type = it }, Modifier.weight(1f)) { it.label }
    }
    Spacer(Modifier.height(12.dp))
    if (usesTemplate) {
        if (type == PayloadType.WIFI) {
            ChoiceRow(WIFI_SECURITY.map { it.first }, fields["security"].takeUnless { it.isNullOrEmpty() } ?: "WPA", { fields = fields + ("security" to it) }) { key ->
                WIFI_SECURITY.first { it.first == key }.second
            }
            FilterChip(selected = fields["hidden"] == "true", onClick = { fields = fields + ("hidden" to (fields["hidden"] != "true").toString()) }, label = { Text("Hidden network") })
        }
        type.fields.forEach { f ->
            if (type == PayloadType.WIFI && f.key == "password" && fields["security"] == "nopass") return@forEach
            val multiline = f.kind == FieldKind.MULTILINE
            OutlinedTextField(
                value = fields[f.key].orEmpty(),
                onValueChange = { fields = fields + (f.key to it.take(1000)) },
                label = { Text(f.label) },
                placeholder = if (f.placeholder != null) { { Text(f.placeholder) } } else null,
                singleLine = !multiline,
                minLines = if (multiline) 2 else 1,
                maxLines = if (multiline) 5 else 1,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardFor(f.kind)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            )
        }
    } else {
        MonoTextField(text, { text = it.take(MAX_INPUT) }, if (kind.is2D) "Text or link" else "Content", minLines = if (kind.is2D) 2 else 1, maxLines = 6)
        val full = withCheckDigit(text, kind)
        if (full != text && validateBarcodeInput(text, kind) == null) {
            Text("Check digit ${full.last()} is added: $full", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }
    }

    // Result
    Spacer(Modifier.height(16.dp))
    val g = generated
    val error = payload.exceptionOrNull()?.message ?: encodeError
    val untouched = usesTemplate && type.fields.all { fields[it.key].isNullOrBlank() }
    when {
        error != null && untouched -> Text("Fill in the fields above to create the code.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        error != null -> Text(error, color = MaterialTheme.colorScheme.error)
        content.isNullOrEmpty() -> Text("Enter the content to encode.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        g == null -> Unit
        else -> {
            val l = g.layout
            val wide = !kind.is2D || kind == BarcodeKind.PDF_417
            Image(
                bitmap = g.preview,
                contentDescription = "${kind.label} preview",
                filterQuality = FilterQuality.None,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .then(if (wide) Modifier else Modifier.widthIn(max = 280.dp))
                    .fillMaxWidth()
                    .aspectRatio(l.cols.toFloat() / l.rows)
                    .clip(MaterialTheme.shapes.medium)
                    .background(if (inverted) ComposeColor.Black else ComposeColor.White)
            )
            val m = g.matrix
            val modules = if (m.height == 1) "${m.width} modules wide" else "${m.width} × ${m.height} modules"
            val version = if (kind == BarcodeKind.QR) " · version ${(m.width - 17) / 4}" else ""
            Text(
                "$modules$version · PNG ${l.cols * exportScale} × ${l.rows * exportScale} px",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)
            )
            Spacer(Modifier.height(12.dp))
            val padding = PaddingValues(horizontal = 8.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { exportPng { savePng("${fileStem(kind)}.png", it) } }, Modifier.weight(1f), contentPadding = padding) { Text("Save PNG", maxLines = 1) }
                FilledTonalButton(onClick = {
                    val hex = { c: Int -> "#" + (c and 0xFFFFFF).toString(16).padStart(6, '0').uppercase() }
                    val svg = barcodeSvg(m, l, hex(fg), if (transparent) null else hex(bg), g.text)
                    saveSvg("${fileStem(kind)}.svg", svg.toByteArray())
                }, Modifier.weight(1f), contentPadding = padding) { Text("Save SVG", maxLines = 1) }
                OutlinedButton(onClick = { exportPng { shareFile(context, "${fileStem(kind)}.png", "image/png", it) } }, Modifier.weight(1f), contentPadding = padding) {
                    Text("Share", maxLines = 1)
                }
            }
        }
    }
    if (usesTemplate && content != null) {
        Spacer(Modifier.height(16.dp))
        OutputPanel(content, label = "Encoded text")
    }
    // Options
    Spacer(Modifier.height(16.dp))
    Text("Options", style = MaterialTheme.typography.titleSmall)
    if (kind.hasErrorCorrection) {
        OptionLabel("Error correction")
        ChoiceRow(EcLevel.entries, ec, { ec = it }) { it.name }
        Text(ec.describe(kind), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
    }
    if (kind == BarcodeKind.DATA_MATRIX) {
        OptionLabel("Shape")
        ChoiceRow(DmShape.entries, dmShape, { dmShape = it }) { it.label }
    }
    OptionLabel("Color")
    ChoiceRow(BARCODE_COLORS.indices.toList(), colorIndex, { colorIndex = it }) { BARCODE_COLORS[it].first }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
        FilterChip(selected = inverted, onClick = { inverted = !inverted }, label = { Text("Inverted") })
        FilterChip(selected = transparent, onClick = { transparent = !transparent }, label = { Text("Transparent background") })
        if (!kind.is2D) FilterChip(selected = showText, onClick = { showText = !showText }, label = { Text("Show text") })
        if (kind.supportsGs1) FilterChip(selected = gs1, onClick = { gs1 = !gs1 }, label = { Text("GS1") })
        if (kind == BarcodeKind.PDF_417) FilterChip(selected = pdfCompact, onClick = { pdfCompact = !pdfCompact }, label = { Text("Compact") })
    }
    if (inverted) {
        Text("Light code on a dark background. Some older scanners can't read inverted codes.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (gs1 && kind.supportsGs1) {
        Text("Starts the code with FNC1. Enter element strings without parentheses, for example 0109501101530003.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Spacer(Modifier.height(4.dp))
    SliderSetting("Quiet zone (modules)", quiet, 0..12) { quiet = it }
    OptionLabel("PNG size")
    ChoiceRow(EXPORT_SIZES.indices.toList(), sizeIndex, { sizeIndex = it }) { "${EXPORT_SIZES[it]} px" }

}

@Composable
private fun ColumnScope.ScanPane() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var hits by rememberSaveable(stateSaver = ScanHitsSaver) { mutableStateOf(emptyList<ScanHit>()) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val job = remember { arrayOfNulls<Job>(1) }

    val scan: (Uri) -> Unit = { uri ->
        job[0]?.cancel()
        busy = true
        message = null
        job[0] = scope.launch {
            try {
                val (px, w, h) = withContext(Dispatchers.IO) { loadScanPixels(context, uri) }
                val found = withContext(Dispatchers.Default) { decodeBarcodes(px, w, h) }
                hits = found
                if (found.isEmpty()) message = "No code found. Use a sharp, well-lit photo where the code fills more of the frame, or crop the image to the code."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                hits = emptyList()
                message = "Could not read the image (${e.message ?: e.javaClass.simpleName}). Try another image."
            } finally {
                busy = false
            }
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(scan) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken -> if (taken) scan(scanPhotoUri(context)) }
    OnSharedImage(scan)

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilledTonalButton(
            onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            Modifier.weight(1f), contentPadding = ButtonDefaults.ButtonWithIconContentPadding
        ) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text("Pick image", maxLines = 1)
        }
        OutlinedButton(
            onClick = {
                try {
                    camera.launch(scanPhotoUri(context))
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(context, "No camera app found. Pick an image instead.", Toast.LENGTH_LONG).show()
                }
            },
            Modifier.weight(1f), contentPadding = ButtonDefaults.ButtonWithIconContentPadding
        ) {
            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text("Take photo", maxLines = 1)
        }
    }
    Text(
        "Reads QR, Data Matrix, Aztec, PDF417 and 1D barcodes. Images are decoded on this device.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp)
    )
    Spacer(Modifier.height(16.dp))
    if (busy) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(12.dp))
            Text("Looking for codes…")
        }
    }
    message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    if (hits.size > 1) {
        Text("${hits.size} codes found", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 8.dp))
    }
    hits.forEach { hit ->
        ScanResultCard(hit)
        Spacer(Modifier.height(12.dp))
    }
}
