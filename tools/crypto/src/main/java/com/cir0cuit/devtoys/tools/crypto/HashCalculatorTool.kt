package com.cir0cuit.devtoys.tools.crypto

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.designsystem.CodeTextStyle
import com.cir0cuit.devtoys.core.ui.MonoTextField
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolCategory
import com.cir0cuit.devtoys.tools.base.ToolDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException
import java.security.MessageDigest
import kotlin.coroutines.cancellation.CancellationException

internal val HASH_ALGORITHMS = listOf("MD5", "SHA-1", "SHA-256", "SHA-512")

internal fun newHashDigests(): List<MessageDigest> = HASH_ALGORITHMS.map { MessageDigest.getInstance(it) }

internal fun ByteArray.toLowerHex(): String = joinToString("") { "%02x".format(it) }

/** MD5, SHA-1, SHA-256, SHA-512 of [text] encoded as UTF-8, lowercase hex, in [HASH_ALGORITHMS] order. */
internal fun hashText(text: String): List<String> {
    val bytes = text.toByteArray()
    return newHashDigests().map { it.digest(bytes).toLowerHex() }
}

private val NO_HASHES = List(HASH_ALGORITHMS.size) { "" }

class HashCalculatorTool : Tool {
    override val descriptor = ToolDescriptor(
        id = "hash",
        title = "Hash / Checksum",
        description = "Hash text or files with MD5 and SHA",
        icon = Icons.Default.Lock,
        category = ToolCategory.CRYPTOGRAPHY,
        keywords = listOf("hash", "md5", "sha", "checksum", "file")
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(modifier: Modifier) {
        val context = LocalContext.current

        var textInput by rememberSaveable { mutableStateOf("") }
        var fileUri by rememberSaveable { mutableStateOf<String?>(null) }
        var fileName by rememberSaveable { mutableStateOf<String?>(null) }
        var isFileMode by rememberSaveable { mutableStateOf(false) }
        var compareHash by rememberSaveable { mutableStateOf("") }

        var hashes by remember { mutableStateOf(NO_HASHES) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var isCalculating by remember { mutableStateOf(false) }

        val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                fileUri = uri.toString()
                var name = "Selected file"
                if (uri.scheme == "content") {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (index != -1) name = cursor.getString(index)
                        }
                    }
                } else if (uri.scheme == "file") {
                    name = uri.lastPathSegment ?: name
                }
                fileName = name
            }
        }

        LaunchedEffect(textInput, fileUri, isFileMode) {
            isCalculating = true
            errorMessage = null
            hashes = withContext(Dispatchers.IO) {
                try {
                    when {
                        !isFileMode -> if (textInput.isEmpty()) NO_HASHES else hashText(textInput)
                        fileUri == null -> NO_HASHES
                        else -> {
                            val digests = newHashDigests()
                            val stream = context.contentResolver.openInputStream(Uri.parse(fileUri))
                                ?: throw IOException("the file could not be opened")
                            stream.use {
                                val buffer = ByteArray(8192)
                                while (true) {
                                    ensureActive()
                                    val read = it.read(buffer)
                                    if (read == -1) break
                                    digests.forEach { d -> d.update(buffer, 0, read) }
                                }
                            }
                            digests.map { it.digest().toLowerHex() }
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    null
                }
            } ?: NO_HASHES.also { errorMessage = "Could not read the file. Choose the file again." }
            isCalculating = false
        }

        val target = compareHash.trim()
        val match = if (target.isEmpty()) null else HASH_ALGORITHMS.zip(hashes).firstOrNull { (_, v) -> v.isNotEmpty() && v.equals(target, ignoreCase = true) }?.first

        ToolScaffold(descriptor.title, modifier = modifier) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf("Text", "File").forEachIndexed { i, label ->
                    SegmentedButton(
                        selected = isFileMode == (i == 1),
                        onClick = { isFileMode = i == 1 },
                        shape = SegmentedButtonDefaults.itemShape(i, 2),
                        label = { Text(label) }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            if (isFileMode) {
                OutlinedButton(onClick = { filePicker.launch("*/*") }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(fileName ?: "Choose a file", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            } else {
                MonoTextField(textInput, { textInput = it }, "Text", minLines = 3)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = compareHash,
                onValueChange = { compareHash = it },
                label = { Text("Compare with a checksum (optional)") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = CodeTextStyle,
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                supportingText = if (target.isEmpty() || hashes == NO_HASHES) null else {
                    {
                        if (match != null) Text("Matches $match", color = MaterialTheme.colorScheme.primary)
                        else Text("No match. Check that the checksum was copied in full.")
                    }
                }
            )
            Spacer(Modifier.height(16.dp))

            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(16.dp))
            }
            if (isCalculating) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            ResultSlab(HASH_ALGORITHMS.zip(hashes), marked = match)
        }
    }
}
