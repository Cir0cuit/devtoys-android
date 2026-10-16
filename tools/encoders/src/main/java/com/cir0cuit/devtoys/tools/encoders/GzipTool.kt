package com.cir0cuit.devtoys.tools.encoders

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cir0cuit.devtoys.core.designsystem.CodeTextStyle
import com.cir0cuit.devtoys.core.ui.MonoTextField
import com.cir0cuit.devtoys.core.ui.OutputPanel
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.core.ui.rememberFileSaver
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolCategory
import com.cir0cuit.devtoys.tools.base.ToolDescriptor
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Largest input file and largest decompressed output, guarding against decompression bombs. */
internal const val MAX_ZIP_BYTES = 16 * 1024 * 1024

internal enum class ZipFormat(val label: String, val extension: String) {
    GZIP("GZip", "gz"), ZLIB("Zlib", "zz"), DEFLATE("Deflate", "deflate")
}

internal fun compress(data: ByteArray, format: ZipFormat): ByteArray {
    val out = ByteArrayOutputStream()
    when (format) {
        ZipFormat.GZIP -> GZIPOutputStream(out).use { it.write(data) }
        ZipFormat.ZLIB -> DeflaterOutputStream(out, Deflater(Deflater.DEFAULT_COMPRESSION, false)).use { it.write(data) }
        ZipFormat.DEFLATE -> DeflaterOutputStream(out, Deflater(Deflater.DEFAULT_COMPRESSION, true)).use { it.write(data) }
    }
    return out.toByteArray()
}

/** GZip by its 1F 8B magic, zlib by its header checksum, raw deflate otherwise. */
internal fun detectZipFormat(data: ByteArray): ZipFormat {
    if (data.size >= 2) {
        val b0 = data[0].toInt() and 0xFF
        val b1 = data[1].toInt() and 0xFF
        if (b0 == 0x1F && b1 == 0x8B) return ZipFormat.GZIP
        if ((b0 and 0x0F) == 8 && ((b0 shl 8) or b1) % 31 == 0) return ZipFormat.ZLIB
    }
    return ZipFormat.DEFLATE
}

/** Decompresses [data] in its detected format. Throws IllegalArgumentException with a user-facing message. */
internal fun decompress(data: ByteArray, maxBytes: Int = MAX_ZIP_BYTES): Pair<ZipFormat, ByteArray> {
    val format = detectZipFormat(data)
    return try {
        val stream: InputStream = when (format) {
            ZipFormat.GZIP -> GZIPInputStream(ByteArrayInputStream(data))
            ZipFormat.ZLIB -> InflaterInputStream(ByteArrayInputStream(data), Inflater(false))
            // nowrap inflation may need one extra input byte past the end.
            ZipFormat.DEFLATE -> InflaterInputStream(ByteArrayInputStream(data + byteArrayOf(0)), Inflater(true))
        }
        stream.use { input ->
            val out = ByteArrayOutputStream()
            val buf = ByteArray(8192)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                out.write(buf, 0, n)
                require(out.size() <= maxBytes) { "The decompressed data is larger than ${maxBytes / 1024 / 1024} MB." }
            }
            format to out.toByteArray()
        }
    } catch (e: IOException) {
        throw IllegalArgumentException("This is not valid ${format.label} data, or it is incomplete. Check that the whole input was copied.")
    }
}

/** Hex when the text is only hex digits (and separators), Base64 otherwise. */
internal fun parseBinaryText(text: String): ByteArray {
    val compact = text.filterNot { it.isWhitespace() }
    return if (compact.length % 2 == 0 && compact.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) hexDecodeBytes(compact)
    else base64DecodeBytes(text)
}

class GzipViewModel : ViewModel() {
    /** The opened file as (name, bytes); kept out of saved state because it can be large. */
    var file by mutableStateOf<Pair<String, ByteArray>?>(null)
}

private class ZipResult(val key: Any?, val error: String? = null, val inSize: Int = 0, val output: ByteArray? = null, val format: ZipFormat? = null, val text: String = "")

private const val MAX_SHOWN_CHARS = 200_000

class GzipTool : Tool {
    override val descriptor = ToolDescriptor(
        id = "gzip",
        title = "GZip / Deflate",
        description = "Compress and decompress GZip, Zlib, Deflate",
        icon = Icons.Default.Compress,
        category = ToolCategory.ENCODERS_DECODERS,
        keywords = listOf("gzip", "zlib", "deflate", "inflate", "compress", "decompress", "gz")
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val vm: GzipViewModel = viewModel()
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        var decompressMode by rememberSaveable { mutableStateOf(false) }
        var compressInput by rememberSaveableText()
        var decompressInput by rememberSaveableText()
        var format by rememberSaveable { mutableStateOf(ZipFormat.GZIP) }
        var asHex by rememberSaveable { mutableStateOf(false) }
        val file = vm.file
        val save = rememberFileSaver("application/octet-stream")

        DisposableEffect(vm) {
            onDispose { if (context.findActivity()?.isChangingConfigurations != true) vm.file = null }
        }

        val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                val loaded = withContext(Dispatchers.IO) {
                    runCatching {
                        val resolver = context.contentResolver
                        val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null } ?: "file"
                        val bytes = resolver.openInputStream(uri)?.use { s ->
                            val out = ByteArrayOutputStream()
                            val buf = ByteArray(8192)
                            while (true) {
                                val n = s.read(buf)
                                if (n < 0) break
                                out.write(buf, 0, n)
                                require(out.size() <= MAX_ZIP_BYTES) { "This file is larger than 16 MB." }
                            }
                            out.toByteArray()
                        } ?: error("The file could not be opened.")
                        name to bytes
                    }
                }
                loaded.onSuccess { vm.file = it }
                    .onFailure { Toast.makeText(context, it.message ?: "Could not open the file", Toast.LENGTH_LONG).show() }
            }
        }

        val input = if (decompressMode) decompressInput else compressInput
        val key = listOf(decompressMode, input, file, format, asHex)
        val result by produceState(ZipResult(null), key) {
            if (file == null && input.isEmpty()) {
                value = ZipResult(key)
                return@produceState
            }
            delay(250) // debounce typing; a newer key cancels this run
            value = withContext(Dispatchers.Default) {
                try {
                    if (decompressMode) {
                        val data = file?.second ?: parseBinaryText(input)
                        val (detected, out) = decompress(data)
                        ZipResult(key, inSize = data.size, output = out, format = detected)
                    } else {
                        val data = file?.second ?: input.toByteArray()
                        val out = compress(data, format)
                        val text = if (out.size * 2 > MAX_SHOWN_CHARS) "" else if (asHex) out.joinToString("") { "%02x".format(it) } else java.util.Base64.getEncoder().encodeToString(out)
                        ZipResult(key, inSize = data.size, output = out, format = format, text = text)
                    }
                } catch (e: IllegalArgumentException) {
                    ZipResult(key, error = e.message)
                }
            }
        }
        val current = result.takeIf { it.key == key }

        ToolScaffold(descriptor.title, modifier = modifier) {
            ModeSelector(listOf("Compress", "Decompress"), if (decompressMode) 1 else 0, { decompressMode = it == 1 })
            Spacer(Modifier.height(16.dp))
            if (file != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${file.first} · ${formatSize(file.second.size)}",
                        style = CodeTextStyle,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { vm.file = null }) { Icon(Icons.Default.Clear, contentDescription = "Close file") }
                }
            } else {
                MonoTextField(
                    input,
                    { if (decompressMode) decompressInput = it else compressInput = it },
                    if (decompressMode) "Base64 or hex to decompress" else "Text to compress",
                    minLines = 5
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { openFile.launch(arrayOf("*/*")) }) {
                    Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Open file")
                }
            }
            Spacer(Modifier.height(16.dp))
            if (!decompressMode) {
                ModeSelector(ZipFormat.entries.map { it.label }, format.ordinal, { format = ZipFormat.entries[it] })
                Spacer(Modifier.height(8.dp))
                ModeSelector(listOf("Base64", "Hex"), if (asHex) 1 else 0, { asHex = it == 1 })
                Spacer(Modifier.height(16.dp))
            }

            when {
                current == null -> Unit
                current.error != null -> ErrorText(current.error)
                current.output != null -> {
                    val output = current.output
                    val ratio = if (current.inSize == 0) "" else " (${output.size * 100L / current.inSize}%)"
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${current.format?.label} · ${formatSize(current.inSize)} → ${formatSize(output.size)}$ratio",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        if (!decompressMode) {
                            FilledTonalButton(onClick = { save("compressed.${format.extension}", output) }) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Save", maxLines = 1)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (decompressMode) {
                        BytesOutput(output, "Decompressed")
                    } else if (current.text.isNotEmpty()) {
                        OutputPanel(current.text, label = if (asHex) "Compressed (hex)" else "Compressed (Base64)")
                    } else {
                        Text("Too large to show here. Save it as a file instead.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
