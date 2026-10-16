package com.cir0cuit.devtoys.tools.encoders

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cir0cuit.devtoys.core.designsystem.CodeTextStyle
import com.cir0cuit.devtoys.core.designsystem.LocalSlabColor
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolCategory
import com.cir0cuit.devtoys.tools.base.ToolDescriptor
import java.io.File
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

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private const val PREVIEW_MAX_SIDE = 2048
private const val PREVIEW_MAX_CHARS = 500

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

@Composable
private fun IconTextButton(icon: ImageVector, text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilledTonalButton(onClick = onClick, modifier = modifier) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, maxLines = 1)
    }
}

/** Recessed slab showing at most [PREVIEW_MAX_CHARS] of a Base64 string, with icon actions in its header. */
@Composable
private fun Base64Preview(label: String, base64: String, actions: @Composable () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = LocalSlabColor.current, shape = MaterialTheme.shapes.medium) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp).padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                actions()
            }
            val isShortened = base64.length > PREVIEW_MAX_CHARS
            Text(
                if (isShortened) base64.take(PREVIEW_MAX_CHARS) + "…" else base64,
                style = CodeTextStyle,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = if (isShortened) 4.dp else 16.dp)
            )
            if (isShortened) {
                Text(
                    "Showing the first $PREVIEW_MAX_CHARS of ${"%,d".format(base64.length)} characters.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                )
            }
        }
    }
}

class ImageBase64Tool : Tool {
    override val descriptor = ToolDescriptor(
        id = "image-base64",
        title = "Image ↔ Base64",
        description = "Turn images into Base64 and back",
        icon = Icons.Default.Image,
        category = ToolCategory.GRAPHICS,
        keywords = listOf("image", "base64", "encode", "decode", "picture")
    )

    @Composable
    override fun Content(modifier: Modifier) {
        var isEncodeMode by rememberSaveable { mutableStateOf(true) }
        val vm: ImageBase64ViewModel = viewModel()

        var addPrefix by rememberSaveable { mutableStateOf(true) }
        val context = LocalContext.current
        val clipboard = LocalClipboardManager.current

        DisposableEffect(vm) {
            onDispose {
                if (context.findActivity()?.isChangingConfigurations != true) vm.clear()
            }
        }

        val scope = rememberCoroutineScope()
        val loadImage: (Uri) -> Unit = { uri ->
            val resolver = context.applicationContext.contentResolver
            vm.viewModelScope.launch(Dispatchers.IO) {
                try {
                    val bytes = resolver.openInputStream(uri)?.use { s -> s.readBytes() } ?: error("the file could not be opened")
                    val bounds = decodeBounds(bytes)
                    val mime = resolver.getType(uri)?.takeIf { t -> t.startsWith("image/") } ?: bounds.outMimeType ?: "image/png"
                    val preview = decodePreviewBitmap(bytes, bounds)
                    val encoded = encodeBase64(bytes, mime, addPrefix)
                    withContext(Dispatchers.Main) {
                        vm.encodeBytes = bytes
                        vm.encodeMime = mime
                        vm.encodeBitmap = preview
                        vm.encodeOutputBase64 = encoded
                        vm.encodeErrorMessage = if (preview == null) "This file could not be read as an image, so the output encodes its raw bytes." else ""
                    }
                } catch (e: Throwable) {
                    withContext(Dispatchers.Main) {
                        vm.encodeErrorMessage = "Could not load the image: ${e.message}. Try another file."
                    }
                }
            }
        }
        // System photo picker; falls back to the document picker where it is unavailable.
        val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? -> uri?.let(loadImage) }
        // The camera app writes into our FileProvider cache path, so no CAMERA permission is needed.
        val cameraUri = remember(context) {
            val file = File(File(context.cacheDir, "images").apply { mkdirs() }, "camera_photo.jpg")
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }
        val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved -> if (saved) loadImage(cameraUri) }

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
                        context.contentResolver.openOutputStream(it)?.bufferedWriter()?.use { writer -> writer.write(vm.encodeOutputBase64) }
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
                        val text = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader -> reader.readText() }.orEmpty()
                        withContext(Dispatchers.Main) {
                            vm.decodeBase64String = text
                        }
                    } catch (e: Throwable) {
                        withContext(Dispatchers.Main) {
                            vm.decodeErrorMessage = "Could not read the text file: ${e.message}"
                        }
                    }
                }
            }
        }

        val shareBitmap = {
            vm.decodeBitmap?.let { bmp ->
                scope.launch(Dispatchers.IO) {
                    try {
                        val file = File(File(context.cacheDir, "images").apply { mkdirs() }, "shared_image.png")
                        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                        val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, contentUri)
                        }
                        withContext(Dispatchers.Main) {
                            context.startActivity(Intent.createChooser(shareIntent, "Share image"))
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
            val result: Triple<Bitmap?, Bitmap?, String> = if (decodeBase64String.isEmpty()) {
                Triple(null, null, "")
            } else {
                withContext(Dispatchers.IO) {
                    try {
                        // substringAfter keeps the whole string when there is no data URI prefix.
                        val decodedBytes = Base64.decode(decodeBase64String.substringAfter(","), Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                        if (bitmap != null) Triple(bitmap, previewOf(bitmap), "")
                        else Triple(null, null, "This Base64 is not an image this device can read. Check that it is complete.")
                    } catch (e: Throwable) {
                        Triple(null, null, "Could not decode this Base64. Check that it is complete, or try a smaller image.")
                    }
                }
            }
            vm.decodeBitmap = result.first
            vm.decodePreview = result.second
            vm.decodeErrorMessage = result.third
            vm.decodedBase64String = decodeBase64String
        }

        ToolScaffold(descriptor.title, modifier = modifier) {
            ModeSelector(listOf("To Base64", "To image"), if (isEncodeMode) 0 else 1, { isEncodeMode = it == 0 })
            Spacer(Modifier.height(16.dp))

            if (isEncodeMode) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    IconTextButton(Icons.Default.Upload, "Pick image", {
                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }, Modifier.weight(1f))
                    IconTextButton(Icons.Default.PhotoCamera, "Take photo", {
                        try {
                            takePicture.launch(cameraUri)
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(context, "No camera app is available", Toast.LENGTH_SHORT).show()
                        }
                    }, Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Add data URI prefix", style = MaterialTheme.typography.bodyLarge)
                        Text("data:image/…;base64,", style = CodeTextStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = addPrefix, onCheckedChange = { addPrefix = it })
                }
                Spacer(Modifier.height(16.dp))
                if (vm.encodeErrorMessage.isNotEmpty()) {
                    ErrorText(vm.encodeErrorMessage)
                    Spacer(Modifier.height(16.dp))
                }
                val encodeBytes = vm.encodeBytes
                if (encodeBytes != null) {
                    vm.encodeBitmap?.let {
                        Image(bitmap = it.asImageBitmap(), contentDescription = "Selected image", modifier = Modifier.fillMaxWidth().height(200.dp))
                        Spacer(Modifier.height(8.dp))
                    }
                    Text(
                        "${vm.encodeMime} · ${formatSize(encodeBytes.size)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    val encodeOutputBase64 = vm.encodeOutputBase64
                    Base64Preview("Base64", encodeOutputBase64) {
                        IconButton(onClick = { saveTextLauncher.launch("image_base64.txt") }) {
                            Icon(Icons.Default.Save, contentDescription = "Save as .txt", modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = {
                            if (encodeOutputBase64.length > 500_000) {
                                Toast.makeText(context, "Too large for the clipboard. Save it as a .txt file instead.", Toast.LENGTH_LONG).show()
                            } else {
                                try {
                                    clipboard.setText(AnnotatedString(encodeOutputBase64))
                                    Toast.makeText(context, "Copied the full Base64", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not copy to the clipboard. Save it as a .txt file instead.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy all", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    IconTextButton(Icons.Default.ContentPaste, "Paste", {
                        try {
                            val clipText = clipboard.getText()?.text
                            if (!clipText.isNullOrEmpty()) {
                                vm.decodeBase64String = clipText
                            } else {
                                vm.decodeErrorMessage = "The clipboard has no text. Copy the Base64 first."
                            }
                        } catch (e: Exception) {
                            vm.decodeErrorMessage = "Could not read the clipboard. Try loading a .txt file instead."
                        }
                    }, Modifier.weight(1f))
                    IconTextButton(Icons.Default.Upload, "Load .txt", { loadTextLauncher.launch("text/plain") }, Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))

                if (decodeBase64String.isNotEmpty()) {
                    Base64Preview("Base64 input", decodeBase64String) {
                        IconButton(onClick = { vm.decodeBase64String = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear input", modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                } else if (vm.decodeErrorMessage.isEmpty()) {
                    Text(
                        "Paste Base64 or a data URI, or load it from a .txt file.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (vm.decodeErrorMessage.isNotEmpty()) {
                    ErrorText(vm.decodeErrorMessage)
                    Spacer(Modifier.height(16.dp))
                }
                val decodeBitmap = vm.decodeBitmap
                val decodePreview = vm.decodePreview
                if (decodeBitmap != null && decodePreview != null) {
                    Text("Preview · ${decodeBitmap.width} × ${decodeBitmap.height}", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    Image(bitmap = decodePreview.asImageBitmap(), contentDescription = "Decoded image", modifier = Modifier.fillMaxWidth().height(200.dp))
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        IconTextButton(Icons.Default.Save, "Save PNG", { saveLauncher.launch("decoded_image.png") }, Modifier.weight(1f))
                        IconTextButton(Icons.Default.Share, "Share", { shareBitmap() }, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
