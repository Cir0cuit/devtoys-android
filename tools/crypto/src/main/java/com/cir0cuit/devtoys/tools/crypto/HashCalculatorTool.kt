package com.cir0cuit.devtoys.tools.crypto

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.MonoTextField
import com.cir0cuit.devtoys.core.ui.OutputPanel
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolCategory
import com.cir0cuit.devtoys.tools.base.ToolDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class HashCalculatorTool : Tool {
    override val descriptor = ToolDescriptor(
        "hash",
        android.R.string.untitled,
        android.R.string.untitled,
        Icons.Default.Lock,
        ToolCategory.CRYPTOGRAPHY,
        listOf("hash", "md5", "sha", "checksum", "file")
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val context = LocalContext.current
        
        var textInput by rememberSaveable { mutableStateOf("") }
        var fileUri by rememberSaveable { mutableStateOf<String?>(null) }
        var fileName by rememberSaveable { mutableStateOf<String?>(null) }
        var isFileMode by rememberSaveable { mutableStateOf(false) }
        var compareHash by rememberSaveable { mutableStateOf("") }

        var md5 by remember { mutableStateOf("") }
        var sha1 by remember { mutableStateOf("") }
        var sha256 by remember { mutableStateOf("") }
        var sha512 by remember { mutableStateOf("") }
        var isCalculating by remember { mutableStateOf(false) }

        val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                fileUri = uri.toString()
                
                // Extract filename
                var name = "Selected File"
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
            withContext(Dispatchers.IO) {
                try {
                    if (!isFileMode && textInput.isEmpty()) {
                        md5 = ""
                        sha1 = ""
                        sha256 = ""
                        sha512 = ""
                    } else if (isFileMode && fileUri == null) {
                        md5 = ""
                        sha1 = ""
                        sha256 = ""
                        sha512 = ""
                    } else {
                        val md5Digest = MessageDigest.getInstance("MD5")
                        val sha1Digest = MessageDigest.getInstance("SHA-1")
                        val sha256Digest = MessageDigest.getInstance("SHA-256")
                        val sha512Digest = MessageDigest.getInstance("SHA-512")
                        
                        if (!isFileMode) {
                            val bytes = textInput.toByteArray()
                            md5Digest.update(bytes)
                            sha1Digest.update(bytes)
                            sha256Digest.update(bytes)
                            sha512Digest.update(bytes)
                        } else if (fileUri != null) {
                            val uri = Uri.parse(fileUri)
                            context.contentResolver.openInputStream(uri)?.use { stream ->
                                val buffer = ByteArray(8192)
                                var read: Int
                                while (stream.read(buffer).also { read = it } != -1) {
                                    md5Digest.update(buffer, 0, read)
                                    sha1Digest.update(buffer, 0, read)
                                    sha256Digest.update(buffer, 0, read)
                                    sha512Digest.update(buffer, 0, read)
                                }
                            }
                        }
                        
                        md5 = md5Digest.digest().joinToString("") { "%02x".format(it) }
                        sha1 = sha1Digest.digest().joinToString("") { "%02x".format(it) }
                        sha256 = sha256Digest.digest().joinToString("") { "%02x".format(it) }
                        sha512 = sha512Digest.digest().joinToString("") { "%02x".format(it) }
                    }
                } catch (e: Exception) {
                    md5 = "Error: ${e.message}"
                    sha1 = ""
                    sha256 = ""
                    sha512 = ""
                }
            }
            isCalculating = false
        }

        ToolScaffold("Hash / Checksum", modifier = modifier) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Input Type:", modifier = Modifier.padding(end = 16.dp))
                FilterChip(selected = !isFileMode, onClick = { isFileMode = false }, label = { Text("Text") })
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(selected = isFileMode, onClick = { isFileMode = true }, label = { Text("File") })
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (isFileMode) {
                Button(onClick = { filePicker.launch("*/*") }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (fileName != null) "Selected: $fileName" else "Select File")
                }
            } else {
                MonoTextField(textInput, { textInput = it }, "Input Text", minLines = 4)
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = compareHash,
                onValueChange = { compareHash = it },
                label = { Text("Compare with Checksum (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isCalculating) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Calculating...")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            @Composable
            fun HashOutput(name: String, value: String) {
                val matches = compareHash.isNotBlank() && value.equals(compareHash.trim(), ignoreCase = true)
                val color = if (matches) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, style = MaterialTheme.typography.titleMedium, color = color)
                    if (matches) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.CheckCircle, contentDescription = "Match", tint = color, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutputPanel(value)
                Spacer(modifier = Modifier.height(16.dp))
            }

            HashOutput("MD5", md5)
            HashOutput("SHA-1", sha1)
            HashOutput("SHA-256", sha256)
            HashOutput("SHA-512", sha512)
        }
    }
}