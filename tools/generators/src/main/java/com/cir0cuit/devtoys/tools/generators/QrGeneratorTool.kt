package com.cir0cuit.devtoys.tools.generators
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share

import androidx.compose.material3.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets

/** Encoder hints for [input]: ISO-8859-1 (the QR default) when it suffices, otherwise UTF-8. */
internal fun qrEncodeHints(input: String): Map<EncodeHintType, Any> =
    if (StandardCharsets.ISO_8859_1.newEncoder().canEncode(input)) emptyMap()
    else mapOf(EncodeHintType.CHARACTER_SET to "UTF-8")

/** Encodes [input] as a QR code; throws [WriterException] when it does not fit. */
internal fun encodeQrMatrix(input: String, size: Int = 512): BitMatrix =
    MultiFormatWriter().encode(input, BarcodeFormat.QR_CODE, size, size, qrEncodeHints(input))

class QrGeneratorTool : Tool {
    override val descriptor = ToolDescriptor("qr-gen", android.R.string.untitled, android.R.string.untitled, Icons.Default.QrCode, ToolCategory.GENERATORS, listOf("qr", "barcode", "generator"))
    @Composable override fun Content(modifier: Modifier) {
        var input by rememberSaveable { mutableStateOf("https://devtoys.app") }
        val context = LocalContext.current
        
        val qrResult: Pair<Bitmap?, String?> = remember(input) {
            if (input.isEmpty()) null to null else try {
                val matrix = encodeQrMatrix(input)
                val width = matrix.width
                val height = matrix.height
                val pixels = IntArray(width * height)
                for (y in 0 until height) {
                    val offset = y * width
                    for (x in 0 until width) {
                        pixels[offset + x] = if (matrix.get(x, y)) Color.BLACK else Color.WHITE
                    }
                }
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
                bmp.setPixels(pixels, 0, width, 0, 0, width, height)
                bmp to null
            } catch (e: WriterException) {
                null to "Input too long for a QR code."
            } catch (e: Exception) {
                null to (e.message ?: "Could not generate QR code.")
            }
        }
        val bitmap = qrResult.first
        
        val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri: android.net.Uri? ->
            uri?.let {
                try {
                    context.contentResolver.openOutputStream(it)?.use { out ->
                        bitmap?.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        val shareBitmap = {
            bitmap?.let { bmp ->
                try {
                    val cachePath = File(context.cacheDir, "images")
                    cachePath.mkdirs()
                    val file = File(cachePath, "qr_code.png")
                    val fileOutputStream = FileOutputStream(file)
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
                    fileOutputStream.close()

                    val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, contentUri)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share QR Code"))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        ToolScaffold("QR Code Generator", modifier = modifier) {
            MonoTextField(input, { input = it }, "Input")
            Spacer(modifier = Modifier.height(16.dp))
            if (bitmap != null) {
                Image(bitmap = bitmap.asImageBitmap(), contentDescription = "QR Code", modifier = Modifier.size(256.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { saveLauncher.launch("qrcode.png") }) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save as PNG")
                    }
                    Button(onClick = { shareBitmap() }) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share")
                    }
                }
            } else {
                val error = qrResult.second
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                } else {
                    Text("Enter text to generate QR code.")
                }
            }
        }
    }
}