package com.cir0cuit.devtoys.core.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Largest text file a tool loads into memory. */
const val MAX_TEXT_FILE_BYTES = 2 * 1024 * 1024

/**
 * Returns a function that opens the system file picker and delivers the chosen file's name and
 * UTF-8 text on the main thread. Files over [maxBytes] are rejected with a toast.
 */
@Composable
fun rememberTextFileOpener(
    mimeTypes: Array<String> = arrayOf("text/*", "application/json", "application/xml", "application/octet-stream"),
    maxBytes: Int = MAX_TEXT_FILE_BYTES,
    onLoaded: (name: String, text: String) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
                        val data = input.readNBytesCompat(maxBytes + 1)
                        require(data.size <= maxBytes) { "File is larger than ${maxBytes / 1024 / 1024} MB" }
                        data
                    } ?: error("Could not open the file")
                    displayName(context, uri) to bytes.toString(Charsets.UTF_8)
                }
            }
            result.onSuccess { (name, text) -> onLoaded(name, text) }
                .onFailure { Toast.makeText(context, it.message ?: "Could not open the file", Toast.LENGTH_LONG).show() }
        }
    }
    return { launcher.launch(mimeTypes) }
}

/**
 * Returns a function that asks where to save a file and writes the given bytes there.
 * Call it with the suggested file name and the content to write.
 */
@Composable
fun rememberFileSaver(mimeType: String): (fileName: String, content: ByteArray) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Kept across the picker round trip; the activity can be recreated while it is open.
    var pending by rememberSaveable { mutableStateOf<ByteArray?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(mimeType)) { uri ->
        val bytes = pending
        pending = null
        if (uri == null || bytes == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } != null }.getOrDefault(false)
            }
            Toast.makeText(context, if (ok) "Saved" else "Could not save the file", Toast.LENGTH_SHORT).show()
        }
    }
    return remember(launcher) {
        { fileName, content ->
            pending = content
            launcher.launch(fileName)
        }
    }
}

/** Opens the share sheet for plain text. */
fun shareText(context: Context, text: String, subject: String? = null) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        if (subject != null) putExtra(Intent.EXTRA_SUBJECT, subject)
    }
    try {
        context.startActivity(Intent.createChooser(send, null))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No app can receive shared text", Toast.LENGTH_SHORT).show()
    }
}

private fun displayName(context: Context, uri: Uri): String =
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) c.getString(0) else null
    } ?: uri.lastPathSegment ?: "file"

private fun java.io.InputStream.readNBytesCompat(limit: Int): ByteArray {
    val out = java.io.ByteArrayOutputStream()
    val buf = ByteArray(8192)
    while (out.size() < limit) {
        val n = read(buf, 0, minOf(buf.size, limit - out.size()))
        if (n < 0) break
        out.write(buf, 0, n)
    }
    return out.toByteArray()
}
