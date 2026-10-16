package com.cir0cuit.devtoys.tools.encoders

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImageBase64ViewModel : ViewModel() {
    var encodeBytes by mutableStateOf<ByteArray?>(null)
    var encodeMime by mutableStateOf("image/png")
    var encodeBitmap by mutableStateOf<Bitmap?>(null)
    var encodeOutputBase64 by mutableStateOf("")
    var encodeErrorMessage by mutableStateOf("")

    var decodeBase64String by mutableStateOf("")
    var decodedBase64String: String? = null
    var decodeBitmap by mutableStateOf<Bitmap?>(null)
    var decodePreview by mutableStateOf<Bitmap?>(null)
    var decodeErrorMessage by mutableStateOf("")

    fun clear() {
        viewModelScope.coroutineContext.cancelChildren()
        encodeBytes = null
        encodeMime = "image/png"
        encodeBitmap = null
        encodeOutputBase64 = ""
        encodeErrorMessage = ""
        decodeBase64String = ""
        decodedBase64String = null
        decodeBitmap = null
        decodePreview = null
        decodeErrorMessage = ""
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private const val PREVIEW_MAX_SIDE = 2048

private fun encodeBase64(bytes: ByteArray, mime: String, addPrefix: Boolean): String =
    (if (addPrefix) "data:$mime;base64," else "") + Base64.encodeToString(bytes, Base64.NO_WRAP)

private fun decodeBounds(bytes: ByteArray): BitmapFactory.Options =
    BitmapFactory.Options().apply { inJustDecodeBounds = true }.also { BitmapFactory.decodeByteArray(bytes, 0, bytes.size, it) }

private fun decodePreviewBitmap(bytes: ByteArray, bounds: BitmapFactory.Options): Bitmap? {
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > PREVIEW_MAX_SIDE) sample *= 2
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
}

private fun previewOf(src: Bitmap): Bitmap {
    val longest = maxOf(src.width, src.height)
    if (longest <= PREVIEW_MAX_SIDE) return src
    val scale = PREVIEW_MAX_SIDE.toFloat() / longest
    return Bitmap.createScaledBitmap(src, (src.width * scale).toInt().coerceAtLeast(1), (src.height * scale).toInt().coerceAtLeast(1), true)
}

class ImageBase64Tool : Tool {
    override val descriptor = ToolDescriptor("image-base64", android.R.string.untitled, android.R.string.untitled, Icons.Default.Image, ToolCategory.GRAPHICS, listOf("image", "base64", "encode", "decode", "picture"))

    @Composable
    override fun Content(modifier: Modifier) {
        var isEncodeMode by rememberSaveable { mutableStateOf(true) }
        val vm: ImageBase64ViewModel = viewModel()

        var addPrefix by rememberSaveable { mutableStateOf(true) }
        val context = LocalContext.current

        DisposableEffect(vm) {
            onDispose {
                if (context.findActivity()?.isChangingConfigurations != true) vm.clear()
            }
        }

        val scope = rememberCoroutineScope()
        val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                val resolver = context.applicationContext.contentResolver
                vm.viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val bytes = resolver.openInputStream(it)?.use { s -> s.readBytes() } ?: error("Cannot open file")
                        val bounds = decodeBounds(bytes)
                        val mime = resolver.getType(it)?.takeIf { t -> t.startsWith("image/") } ?: bounds.outMimeType ?: "image/png"
                        val preview = decodePreviewBitmap(bytes, bounds)
                        val encoded = encodeBase64(bytes, mime, addPrefix)
                        withContext(Dispatchers.Main) {
                            vm.encodeBytes = bytes
                            vm.encodeMime = mime
                            vm.encodeBitmap = preview
                            vm.encodeOutputBase64 = encoded
                            vm.encodeErrorMessage = if (preview == null) "Failed to decode image; output contains the raw file bytes" else ""
                        }
                    } catch (e: Throwable) {
                        withContext(Dispatchers.Main) {
                            vm.encodeErrorMessage = "Error loading image: ${e.message}"
                        }
                    }
                }
            }
        }

        // Re-encode if addPrefix changes
        LaunchedEffect(addPrefix) {
            val bytes = vm.encodeBytes ?: return@LaunchedEffect
            if (vm.encodeOutputBase64.startsWith("data:") == addPrefix) return@LaunchedEffect
            val mime = vm.encodeMime
            vm.encodeOutputBase64 = withContext(Dispatchers.Default) { encodeBase64(bytes, mime, addPrefix) }
        }

        val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri: Uri? ->
            uri?.let {
                scope.launch(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(it)?.use { out ->
                            vm.decodeBitmap?.compress(Bitmap.CompressFormat.PNG, 100, out)
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
                                writer.write(vm.encodeOutputBase64)
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
                            vm.decodeBase64String = text
                        }
                    } catch (e: Throwable) {
                        withContext(Dispatchers.Main) {
                            vm.decodeErrorMessage = "Error loading text file: ${e.message}"
                        }
                    }
                }
            }
        }

        val shareBitmap = {
            vm.decodeBitmap?.let { bmp ->
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

        val decodeBase64String = vm.decodeBase64String
        LaunchedEffect(decodeBase64String) {
            if (vm.decodedBase64String == decodeBase64String) return@LaunchedEffect
            if (decodeBase64String.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    try {
                        val cleanString = if (decodeBase64String.contains(",")) decodeBase64String.substringAfter(",") else decodeBase64String
                        val decodedBytes = Base64.decode(cleanString, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                        val preview = bitmap?.let { previewOf(it) }
                        withContext(Dispatchers.Main) {
                            if (bitmap != null) {
                                vm.decodeBitmap = bitmap
                                vm.decodePreview = preview
                                vm.decodeErrorMessage = ""
                            } else {
                                vm.decodeBitmap = null
                                vm.decodePreview = null
                                vm.decodeErrorMessage = "Invalid Base64 image string"
                            }
                        }
                    } catch (e: Throwable) {
                        withContext(Dispatchers.Main) {
                            vm.decodeBitmap = null
                            vm.decodePreview = null
                            vm.decodeErrorMessage = "Invalid Base64 format or image too large"
                        }
                    }
                }
            } else {
                vm.decodeBitmap = null
                vm.decodePreview = null
                vm.decodeErrorMessage = ""
            }
            vm.decodedBase64String = decodeBase64String
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
                    if (vm.encodeErrorMessage.isNotEmpty()) {
                        Text(vm.encodeErrorMessage, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    if (vm.encodeBytes != null) {
                        val encodeBitmap = vm.encodeBitmap
                        if (encodeBitmap != null) {
                            Image(bitmap = encodeBitmap.asImageBitmap(), contentDescription = "Preview", modifier = Modifier.fillMaxWidth().height(200.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        val encodeOutputBase64 = vm.encodeOutputBase64
                        
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
                                    vm.decodeBase64String = clipText
                                } else {
                                    vm.decodeErrorMessage = "Clipboard is empty or does not contain text"
                                }
                            } catch (e: Exception) {
                                vm.decodeErrorMessage = "Failed to access clipboard"
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
                                TextButton(onClick = { vm.decodeBase64String = ""; vm.decodeBitmap = null; vm.decodePreview = null; vm.decodeErrorMessage = "" }, modifier = Modifier.align(Alignment.End)) {
                                    Text("Clear")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (vm.decodeErrorMessage.isNotEmpty()) {
                        Text(vm.decodeErrorMessage, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    val decodePreview = vm.decodePreview
                    if (vm.decodeBitmap != null && decodePreview != null) {
                        Text("Preview:", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Image(bitmap = decodePreview.asImageBitmap(), contentDescription = "Preview", modifier = Modifier.fillMaxWidth().height(200.dp))
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
