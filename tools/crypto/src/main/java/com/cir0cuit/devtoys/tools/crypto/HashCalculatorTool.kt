package com.cir0cuit.devtoys.tools.crypto

import com.cir0cuit.devtoys.tools.base.OnSharedText
import com.cir0cuit.devtoys.tools.base.ShareKind
import android.os.Build
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.designsystem.CodeTextStyle
import com.cir0cuit.devtoys.core.designsystem.LocalSlabColor
import com.cir0cuit.devtoys.core.ui.MonoTextField
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.core.ui.rememberTextFileOpener
import com.cir0cuit.devtoys.core.ui.shareText
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolCategory
import com.cir0cuit.devtoys.tools.base.ToolDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException
import java.security.MessageDigest
import java.util.zip.Adler32
import java.util.zip.CRC32
import java.util.zip.CRC32C
import java.util.zip.Checksum
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext

internal val HASH_ALGORITHMS = listOfNotNull(
    "MD5", "SHA-1", "SHA-224", "SHA-256", "SHA-384", "SHA-512", "CRC32",
    "CRC32C".takeIf { Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE }, "Adler-32"
)
internal val DEFAULT_HASH_ALGORITHMS = listOf("MD5", "SHA-1", "SHA-256", "SHA-512")

/** Incremental hash over a MessageDigest or a java.util.zip checksum (result: 4 big-endian bytes). */
internal class Hasher(algorithm: String) {
    private val checksum: Checksum? = when (algorithm) {
        "CRC32" -> CRC32()
        "CRC32C" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) CRC32C() else error("CRC32C needs Android 14 or later")
        "Adler-32" -> Adler32()
        else -> null
    }
    private val digest = if (checksum == null) MessageDigest.getInstance(algorithm) else null

    fun update(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size) {
        if (checksum != null) checksum.update(bytes, offset, length) else digest!!.update(bytes, offset, length)
    }

    fun result(): ByteArray =
        if (checksum != null) checksum.value.let { v -> ByteArray(4) { (v shr (24 - 8 * it)).toByte() } } else digest!!.digest()
}

internal fun hashBytes(bytes: ByteArray, algorithms: List<String>): Map<String, ByteArray> =
    algorithms.associateWith { Hasher(it).apply { update(bytes) }.result() }

internal data class FileHashes(val name: String, val digests: Map<String, ByteArray>)

private val CHECKSUM_PREFIX = Regex("^(md5|sha-?\\d+|crc32c?|adler-?32)[:=]", RegexOption.IGNORE_CASE)

/** True when a pasted checksum (hex with optional colons/spaces, Base64, or an "sha256:" prefix) equals [digest]. */
internal fun checksumMatches(pasted: String, digest: ByteArray): Boolean {
    val clean = pasted.trim().replace(CHECKSUM_PREFIX, "").filterNot { it.isWhitespace() || it == ':' }
    if (clean.isEmpty()) return false
    return clean.equals(digest.toLowerHex(), ignoreCase = true) ||
        runCatching { parseBase64(clean) }.getOrNull()?.contentEquals(digest) == true
}

internal data class SumEntry(val algorithm: String, val name: String, val hex: String)

internal enum class VerifyStatus(val label: String) { OK("OK"), FAILED("Failed"), MISSING("Missing") }

private val GNU_LINE = Regex("^([0-9a-fA-F]+) [ *](.+)$")
private val BSD_LINE = Regex("^([A-Za-z0-9-]+) ?\\((.+)\\) ?= ?([0-9a-fA-F]+)$")
private val ALGORITHM_BY_HEX_LENGTH = mapOf(32 to "MD5", 40 to "SHA-1", 56 to "SHA-224", 64 to "SHA-256", 96 to "SHA-384", 128 to "SHA-512")

private fun bsdTag(algorithm: String) = algorithm.replace("-", "").uppercase()

/** Entries of a GNU ("<hex>  name", "<hex> *name") or BSD ("SHA256 (name) = <hex>") checksum file; other lines are skipped. */
internal fun parseChecksumFile(text: String): List<SumEntry> = text.lineSequence().mapNotNull { raw ->
    val line = raw.trim()
    BSD_LINE.matchEntire(line)?.let { m ->
        HASH_ALGORITHMS.firstOrNull { bsdTag(it) == bsdTag(m.groupValues[1]) }
            ?.let { SumEntry(it, m.groupValues[2], m.groupValues[3].lowercase()) }
    } ?: GNU_LINE.matchEntire(line)?.let { m ->
        ALGORITHM_BY_HEX_LENGTH[m.groupValues[1].length]?.let { SumEntry(it, m.groupValues[2], m.groupValues[1].lowercase()) }
    }
}.toList()

/** Each entry's status; files are matched by display name against the entry's last path segment. */
internal fun verifyChecksums(entries: List<SumEntry>, files: List<FileHashes>): List<Pair<String, VerifyStatus>> = entries.map { e ->
    val file = files.firstOrNull { it.name == e.name.substringAfterLast('/') }
    e.name to when {
        file == null -> VerifyStatus.MISSING
        file.digests[e.algorithm]?.toLowerHex() == e.hex -> VerifyStatus.OK
        else -> VerifyStatus.FAILED
    }
}

/** sha256sum-style list for one algorithm, BSD tagged lines ("SHA256 (name) = hex") for several. */
internal fun checksumList(files: List<FileHashes>, algorithms: List<String>): String = files.flatMap { f ->
    algorithms.mapNotNull { a ->
        val hex = f.digests[a]?.toLowerHex() ?: return@mapNotNull null
        if (algorithms.size == 1) "$hex  ${f.name}" else "${bsdTag(a)} (${f.name}) = $hex"
    }
}.joinToString("\n", postfix = "\n")

private suspend fun hashFiles(context: Context, uris: List<Uri>, algorithms: List<String>, onProgress: (Float) -> Unit): List<FileHashes> {
    val resolver = context.contentResolver
    val meta = uris.map { uri ->
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst()) (c.getString(0) ?: "file") to (if (c.isNull(1)) 0L else c.getLong(1)) else null
        } ?: ((uri.lastPathSegment ?: "file") to 0L)
    }
    val total = meta.sumOf { it.second }
    var done = 0L
    val buffer = ByteArray(64 * 1024)
    return uris.mapIndexed { index, uri ->
        val hashers = algorithms.map { Hasher(it) }
        (resolver.openInputStream(uri) ?: throw IOException("could not open ${meta[index].first}")).use { input ->
            while (true) {
                coroutineContext.ensureActive()
                val read = input.read(buffer)
                if (read == -1) break
                hashers.forEach { it.update(buffer, 0, read) }
                done += read
                if (total > 0) onProgress((done.toFloat() / total).coerceAtMost(1f))
            }
        }
        if (total == 0L) onProgress((index + 1f) / uris.size)
        FileHashes(meta[index].first, algorithms.zip(hashers).associate { (a, h) -> a to h.result() })
    }
}

class HashCalculatorTool : Tool {
    override val descriptor = ToolDescriptor(
        id = "hash",
        title = "Hash / Checksum",
        description = "Hash text or files with MD5, SHA and CRC",
        icon = Icons.Default.Lock,
        category = ToolCategory.CRYPTOGRAPHY,
        keywords = listOf("hash", "md5", "sha", "checksum", "file", "crc32", "sha256sum", "verify"),
        accepts = setOf(ShareKind.TEXT)
    )

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @Composable
    override fun Content(modifier: Modifier) {
        val context = LocalContext.current
        val clipboard = LocalClipboardManager.current

        var textInput by rememberSaveableText()
        var inputEncoding by rememberSaveable { mutableStateOf(0) }
        var outputFormat by rememberSaveable { mutableStateOf(0) }
        var selected by rememberSaveable { mutableStateOf(DEFAULT_HASH_ALGORITHMS.joinToString(",")) }
        // Newline-separated content URIs.
        var fileUris by rememberSaveable { mutableStateOf("") }
        var isFileMode by rememberSaveable { mutableStateOf(false) }
        OnSharedText { textInput = it; isFileMode = false }
        var compareHash by rememberSaveable { mutableStateOf("") }
        var sumsName by rememberSaveable { mutableStateOf<String?>(null) }
        var sumsText by rememberSaveableText()

        var results by remember { mutableStateOf<List<FileHashes>>(emptyList()) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var progress by remember { mutableStateOf<Float?>(null) }

        val selectedSet = selected.split(',').toSet()
        val algorithms = HASH_ALGORITHMS.filter { it in selectedSet }
        val sumEntries = remember(sumsText) { parseChecksumFile(sumsText) }
        val needed = (algorithms + if (isFileMode) sumEntries.map { it.algorithm } else emptyList()).distinct()

        val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (uris.isNotEmpty()) fileUris = uris.joinToString("\n")
        }
        val openSums = rememberTextFileOpener(mimeTypes = arrayOf("*/*"), maxBytes = 256 * 1024) { name, text ->
            sumsName = name
            sumsText = text
        }

        LaunchedEffect(isFileMode, textInput, inputEncoding, fileUris, needed) {
            errorMessage = null
            progress = null
            if (!isFileMode) {
                if (textInput.isEmpty()) {
                    results = emptyList()
                    return@LaunchedEffect
                }
                delay(200)
                results = try {
                    withContext(Dispatchers.Default) { listOf(FileHashes("", hashBytes(decodeInput(textInput, inputEncoding), needed))) }
                } catch (e: IllegalArgumentException) {
                    errorMessage = e.message
                    emptyList()
                }
                return@LaunchedEffect
            }
            val uris = fileUris.lines().filter { it.isNotEmpty() }.map(Uri::parse)
            if (uris.isEmpty()) {
                results = emptyList()
                return@LaunchedEffect
            }
            progress = 0f
            results = try {
                withContext(Dispatchers.IO) { hashFiles(context, uris, needed) { progress = it } }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorMessage = "Could not read the files. Choose them again."
                emptyList()
            }
            progress = null
        }

        // One labeled row per (file, algorithm) pair.
        val rows: List<Pair<String, ByteArray?>> = if (results.isEmpty()) algorithms.map { it to null } else results.flatMap { f ->
            algorithms.map { a ->
                val label = when {
                    results.size == 1 -> a
                    algorithms.size == 1 -> f.name
                    else -> "${f.name} · $a"
                }
                label to f.digests[a]
            }
        }
        val match = rows.firstOrNull { (_, v) -> v != null && checksumMatches(compareHash, v) }?.first
        val allText = if (isFileMode) checksumList(results, algorithms)
        else rows.joinToString("\n") { (label, v) -> "$label: ${v?.let { formatBytes(it, outputFormat) }.orEmpty()}" }

        ToolScaffold(descriptor.title, modifier = modifier) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf("Text", "Files").forEachIndexed { i, label ->
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
                val count = fileUris.lines().count { it.isNotEmpty() }
                OutlinedButton(onClick = { filePicker.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    val label = when {
                        count == 0 -> "Choose files"
                        results.size == 1 -> results[0].name
                        else -> "$count ${if (count == 1) "file" else "files"}"
                    }
                    Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (sumsName == null) {
                    TextButton(onClick = openSums) { Text("Verify with a checksum file") }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Checking against $sumsName",
                            Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        IconButton(onClick = { sumsName = null; sumsText = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Stop verifying")
                        }
                    }
                }
            } else {
                MonoTextField(textInput, { textInput = it }, "Text", minLines = 3)
                Spacer(Modifier.height(8.dp))
                ChoiceRow("Input", INPUT_ENCODINGS, inputEncoding, { inputEncoding = it })
            }
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HASH_ALGORITHMS.forEach { a ->
                    FilterChip(
                        selected = a in selectedSet,
                        onClick = {
                            val next = if (a in selectedSet) selectedSet - a else selectedSet + a
                            if (next.any { it in HASH_ALGORITHMS }) selected = next.joinToString(",")
                        },
                        label = { Text(a) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            ChoiceRow("Output", OUTPUT_FORMATS, outputFormat, { outputFormat = it })
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = compareHash,
                onValueChange = { compareHash = it },
                label = { Text("Compare with a checksum (optional)") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = CodeTextStyle,
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                trailingIcon = {
                    IconButton(onClick = { clipboard.getText()?.text?.let { compareHash = it.trim() } }) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste checksum")
                    }
                },
                supportingText = if (compareHash.isBlank() || results.isEmpty()) null else {
                    {
                        if (match != null) Text("Matches $match", color = MaterialTheme.colorScheme.primary)
                        else Text("No match. Check that the checksum is complete and its algorithm is selected.")
                    }
                }
            )
            Spacer(Modifier.height(16.dp))

            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(16.dp))
            }
            progress?.let { p ->
                LinearProgressIndicator(progress = { p }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
            }
            if (isFileMode && sumEntries.isNotEmpty()) {
                VerifySlab(verifyChecksums(sumEntries, results))
                Spacer(Modifier.height(16.dp))
            } else if (isFileMode && sumsName != null) {
                Text("No checksum lines found in $sumsName. Use the sha256sum or BSD format.", color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(16.dp))
            }
            ResultSlab(rows.map { (label, v) -> label to (v?.let { formatBytes(it, outputFormat) } ?: "") }, marked = match)
            if (results.isNotEmpty()) {
                Row {
                    TextButton(onClick = { clipboard.setText(AnnotatedString(allText)) }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                        Text("Copy all")
                    }
                    TextButton(onClick = { shareText(context, allText) }) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                        Text("Share")
                    }
                }
            }
        }
    }
}

@Composable
private fun VerifySlab(rows: List<Pair<String, VerifyStatus>>) {
    Surface(Modifier.fillMaxWidth(), color = LocalSlabColor.current, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            rows.forEach { (name, status) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, Modifier.weight(1f), style = CodeTextStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        status.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = when (status) {
                            VerifyStatus.OK -> MaterialTheme.colorScheme.primary
                            VerifyStatus.FAILED -> MaterialTheme.colorScheme.error
                            VerifyStatus.MISSING -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}
