package com.cir0cuit.devtoys.tools.encoders

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImageBase64Tool : Tool {
    override val descriptor = ToolDescriptor("image-base64", android.R.string.untitled, android.R.string.untitled, Icons.Default.Image, ToolCategory.GRAPHICS, listOf("image", "base64", "encode", "decode", "picture"))

    @Composable
    override fun Content(modifier: Modifier) {
        var isEncodeMode by rememberSaveable { mutableStateOf(true) }
        var encodeBitmap by remember { mutableStateOf<Bitmap?>(null) }
        var encodeOutputBase64 by remember { mutableStateOf("") }
        var encodeErrorMessage by remember { mutableStateOf("") }

        var decodeBase64String by remember { mutableStateOf("") }
        var decodeBitmap by remember { mutableStateOf<Bitmap?>(null) }
        var decodeErrorMessage by remember { mutableStateOf("") }

        var addPrefix by rememberSaveable { mutableStateOf(true) }
        val context = LocalContext.current

        val scope = rememberCoroutineScope()
        val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                scope.launch(Dispatchers.IO) {
                    try {
                        val inputStream = context.contentResolver.openInputStream(it)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        if (bitmap != null) {
                            val outputStream = ByteArrayOutputStream()
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                            val bytes = outputStream.toByteArray()
                            val prefix = if (addPrefix) "data:image/png;base64," else ""
                            val encoded = prefix + Base64.encodeToString(bytes, Base64.NO_WRAP)
                            withContext(Dispatchers.Main) {
                                encodeBitmap = bitmap
                                encodeOutputBase64 = encoded
                                encodeErrorMessage = ""
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                encodeErrorMessage = "Failed to decode image"
                            }
                        }
                    } catch (e: Throwable) {
                        withContext(Dispatchers.Main) {
                            encodeErrorMessage = "Error loading image: ${e.message}"
                        }
                    }
                }
            }
        }

        // Re-encode if addPrefix changes
        LaunchedEffect(addPrefix) {
            if (encodeBitmap != null) {
                withContext(Dispatchers.IO) {
                    try {
                        val outputStream = ByteArrayOutputStream()
                        encodeBitmap!!.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                        val bytes = outputStream.toByteArray()
                        val prefix = if (addPrefix) "data:image/png;base64," else ""
                        val encoded = prefix + Base64.encodeToString(bytes, Base64.NO_WRAP)
                        withContext(Dispatchers.Main) {
                            encodeOutputBase64 = encoded
                        }
                    } catch (e: Throwable) {
                        // ignore
                    }
                }
            }
        }

        val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri: Uri? ->
            uri?.let {
                scope.launch(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(it)?.use { out ->
                            decodeBitmap?.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }
            }
        }

        val saveTextLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri: Uri? ->
            uri?.let {
                scope.launch(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(it)?.use { out ->
                            out.bufferedWriter().use { writer ->
                                writer.write(encodeOutputBase64)
                            }
                        }
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }
            }
        }

        val loadTextLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                scope.launch(Dispatchers.IO) {
                    try {
                        val inputStream = context.contentResolver.openInputStream(it)
                        val text = inputStream?.bufferedReader()?.use { reader -> reader.readText() } ?: ""
                        withContext(Dispatchers.Main) {
                            decodeBase64String = text
                        }
                    } catch (e: Throwable) {
                        withContext(Dispatchers.Main) {
                            decodeErrorMessage = "Error loading text file: ${e.message}"
                        }
                    }
                }
            }
        }

        val shareBitmap = {
            decodeBitmap?.let { bmp ->
                scope.launch(Dispatchers.IO) {
                    try {
                        val cachePath = File(context.cacheDir, "images")
                        cachePath.mkdirs()
                        val file = File(cachePath, "shared_image.png")
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
                        withContext(Dispatchers.Main) {
                            context.startActivity(Intent.createChooser(shareIntent, "Share Image"))
                        }
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }
            }
        }

        LaunchedEffect(decodeBase64String) {
            if (decodeBase64String.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    try {
                        val cleanString = if (decodeBase64String.contains(",")) decodeBase64String.substringAfter(",") else decodeBase64String
                        val decodedBytes = Base64.decode(cleanString, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                        withContext(Dispatchers.Main) {
                            if (bitmap != null) {
                                decodeBitmap = bitmap
                                decodeErrorMessage = ""
                            } else {
                                decodeBitmap = null
                                decodeErrorMessage = "Invalid Base64 image string"
                            }
                        }
                    } catch (e: Throwable) {
                        withContext(Dispatchers.Main) {
                            decodeBitmap = null
                            decodeErrorMessage = "Invalid Base64 format or image too large"
                        }
                    }
                }
            } else {
                decodeBitmap = null
                decodeErrorMessage = ""
            }
        }

        ToolScaffold("Image ↔ Base64", modifier = modifier, scrollable = false) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                TabRow(selectedTabIndex = if (isEncodeMode) 0 else 1) {
                    Tab(selected = isEncodeMode, onClick = { isEncodeMode = true }, text = { Text("Image to Base64") })
                    Tab(selected = !isEncodeMode, onClick = { isEncodeMode = false }, text = { Text("Base64 to Image") })
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (isEncodeMode) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Add Data URI prefix")
                        Spacer(Modifier.width(8.dp))
                        Switch(checked = addPrefix, onCheckedChange = { addPrefix = it })
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { imagePicker.launch("image/*") }) {
                        Icon(Icons.Default.Upload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Image")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    if (encodeErrorMessage.isNotEmpty()) {
                        Text(encodeErrorMessage, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    if (encodeBitmap != null) {
                        Image(bitmap = encodeBitmap!!.asImageBitmap(), contentDescription = "Preview", modifier = Modifier.fillMaxWidth().height(200.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val localClipboard = androidx.compose.ui.platform.LocalClipboardManager.current
                        Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Generated Base64 String:", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                val previewText = if (encodeOutputBase64.length > 500) encodeOutputBase64.take(500) + "...\n(Truncated for preview)" else encodeOutputBase64
                                Text(previewText, style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.align(Alignment.End)) {
                                    Button(onClick = { saveTextLauncher.launch("image_base64.txt") }) {
                                        Icon(Icons.Default.Save, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Save as .txt")
                                    }
                                    Button(onClick = { 
                                        if (encodeOutputBase64.length > 500_000) {
                                            android.widget.Toast.makeText(context, "String too large for clipboard. Please save as .txt instead.", android.widget.Toast.LENGTH_LONG).show()
                                        } else {
                                            try {
                                                localClipboard.setText(androidx.compose.ui.text.AnnotatedString(encodeOutputBase64))
                                                android.widget.Toast.makeText(context, "Copied full Base64 to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                                            } catch(e: Exception) {
                                                android.widget.Toast.makeText(context, "Failed to copy to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Copy Full Base64")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val localClipboard = androidx.compose.ui.platform.LocalClipboardManager.current
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = {
                            try {
                                val clipText = localClipboard.getText()?.text
                                if (!clipText.isNullOrEmpty()) {
                                    decodeBase64String = clipText
                                } else {
                                    decodeErrorMessage = "Clipboard is empty or does not contain text"
                                }
                            } catch (e: Exception) {
                                decodeErrorMessage = "Failed to access clipboard"
                            }
                        }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Paste Base64")
                        }
                        
                        Button(onClick = { loadTextLauncher.launch("text/plain") }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Upload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Load .txt")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    if (decodeBase64String.isNotEmpty()) {
                        Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Input Base64 String Preview:", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                val previewText = if (decodeBase64String.length > 500) decodeBase64String.take(500) + "...\n(Truncated for preview)" else decodeBase64String
                                Text(previewText, style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(onClick = { decodeBase64String = ""; decodeBitmap = null; decodeErrorMessage = "" }, modifier = Modifier.align(Alignment.End)) {
                                    Text("Clear")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (decodeErrorMessage.isNotEmpty()) {
                        Text(decodeErrorMessage, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    if (decodeBitmap != null) {
                        Text("Preview:", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Image(bitmap = decodeBitmap!!.asImageBitmap(), contentDescription = "Preview", modifier = Modifier.fillMaxWidth().height(200.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { saveLauncher.launch("decoded_image.png") }) {
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
                    }
                }
            }
        }
    }
}
