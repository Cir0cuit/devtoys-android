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
import com.google.zxing.MultiFormatWriter
import java.io.File
import java.io.FileOutputStream

class QrGeneratorTool : Tool {
    override val descriptor = ToolDescriptor("qr-gen", android.R.string.untitled, android.R.string.untitled, Icons.Default.QrCode, ToolCategory.GENERATORS, listOf("qr", "barcode", "generator"))
    @Composable override fun Content(modifier: Modifier) {
        var input by rememberSaveable { mutableStateOf("https://devtoys.app") }
        val context = LocalContext.current
        
        val bitmap = remember(input) {
            if (input.isEmpty()) null else try {
                val matrix = MultiFormatWriter().encode(input, BarcodeFormat.QR_CODE, 512, 512)
                val width = matrix.width
                val height = matrix.height
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
                for (x in 0 until width) {
                    for (y in 0 until height) {
                        bmp.setPixel(x, y, if (matrix.get(x, y)) Color.BLACK else Color.WHITE)
                    }
                }
                bmp
            } catch (e: Exception) { null }
        }
        
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
                Text("Enter text to generate QR code.")
            }
        }
    }
}