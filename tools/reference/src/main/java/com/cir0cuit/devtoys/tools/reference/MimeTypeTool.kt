package com.cir0cuit.devtoys.tools.reference

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.designsystem.CodeTextStyle
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolCategory
import com.cir0cuit.devtoys.tools.base.ToolDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class MimeType(val type: String, val extensions: List<String>)

private fun m(type: String, extensions: String = "") =
    MimeType(type, extensions.split(' ').filter { it.isNotEmpty() })

/** Common types; Android's MimeTypeMap returns one extension per type, so the reverse lookup uses this list. */
internal val MIME_TYPES = listOf(
    m("text/plain", "txt text log conf ini"),
    m("text/html", "html htm"),
    m("text/css", "css"),
    m("text/javascript", "js mjs cjs"),
    m("text/csv", "csv"),
    m("text/tab-separated-values", "tsv"),
    m("text/markdown", "md markdown"),
    m("text/calendar", "ics"),
    m("text/vcard", "vcf"),
    m("text/vtt", "vtt"),
    m("text/x-python", "py"),
    m("text/x-java-source", "java"),
    m("text/x-kotlin", "kt kts"),
    m("text/x-c", "c h"),
    m("text/x-c++", "cpp cc cxx hpp"),
    m("application/x-ruby", "rb"),
    m("text/x-diff", "diff patch"),
    m("text/event-stream"),
    m("application/json", "json"),
    m("application/ld+json", "jsonld"),
    m("application/geo+json", "geojson"),
    m("application/manifest+json", "webmanifest"),
    m("application/x-ndjson", "ndjson jsonl"),
    m("application/x-ipynb+json", "ipynb"),
    m("application/xml", "xml xsd xsl"),
    m("application/xhtml+xml", "xhtml"),
    m("application/atom+xml", "atom"),
    m("application/rss+xml", "rss"),
    m("application/yaml", "yaml yml"),
    m("application/toml", "toml"),
    m("application/sql", "sql"),
    m("application/x-www-form-urlencoded"),
    m("multipart/form-data"),
    m("multipart/byteranges"),
    m("application/octet-stream", "bin"),
    m("application/pdf", "pdf"),
    m("application/rtf", "rtf"),
    m("application/epub+zip", "epub"),
    m("application/postscript", "ps eps ai"),
    m("application/x-tex", "tex"),
    m("application/x-latex", "latex"),
    m("application/msword", "doc"),
    m("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx"),
    m("application/vnd.ms-excel", "xls"),
    m("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),
    m("application/vnd.ms-powerpoint", "ppt"),
    m("application/vnd.openxmlformats-officedocument.presentationml.presentation", "pptx"),
    m("application/vnd.oasis.opendocument.text", "odt"),
    m("application/vnd.oasis.opendocument.spreadsheet", "ods"),
    m("application/vnd.oasis.opendocument.presentation", "odp"),
    m("application/vnd.oasis.opendocument.graphics", "odg"),
    m("application/vnd.visio", "vsd"),
    m("application/vnd.ms-outlook", "msg"),
    m("message/rfc822", "eml mht"),
    m("application/mbox", "mbox"),
    m("application/zip", "zip"),
    m("application/gzip", "gz tgz"),
    m("application/x-tar", "tar"),
    m("application/x-bzip2", "bz2"),
    m("application/x-xz", "xz"),
    m("application/zstd", "zst"),
    m("application/x-lz4", "lz4"),
    m("application/x-7z-compressed", "7z"),
    m("application/vnd.rar", "rar"),
    m("application/vnd.ms-cab-compressed", "cab"),
    m("application/x-iso9660-image", "iso"),
    m("application/x-apple-diskimage", "dmg"),
    m("application/java-archive", "jar war ear"),
    m("application/java-vm", "class"),
    m("application/x-java-keystore", "jks"),
    m("application/vnd.android.package-archive", "apk"),
    m("application/vnd.apple.pkpass", "pkpass"),
    m("application/vnd.microsoft.portable-executable", "exe dll"),
    m("application/x-msi", "msi"),
    m("application/x-debian-package", "deb"),
    m("application/x-rpm", "rpm"),
    m("application/x-sh", "sh bash"),
    m("application/x-perl", "pl pm"),
    m("application/x-sharedlib", "so"),
    m("application/wasm", "wasm"),
    m("application/vnd.sqlite3", "sqlite sqlite3 db"),
    m("application/x-bittorrent", "torrent"),
    m("application/vnd.apple.mpegurl", "m3u8"),
    m("application/dash+xml", "mpd"),
    m("application/vnd.google-earth.kml+xml", "kml"),
    m("application/vnd.google-earth.kmz", "kmz"),
    m("application/gpx+xml", "gpx"),
    m("application/ogg", "ogx"),
    m("application/pkix-cert", "cer"),
    m("application/x-x509-ca-cert", "crt der"),
    m("application/x-pem-file", "pem"),
    m("application/x-pkcs12", "p12 pfx"),
    m("application/pkcs10", "csr p10"),
    m("application/pkcs8", "p8"),
    m("application/x-pkcs7-certificates", "p7b"),
    m("application/pkix-crl", "crl"),
    m("application/pgp-signature", "sig"),
    m("application/pgp-encrypted", "pgp gpg"),
    m("application/vnd.ms-fontobject", "eot"),
    m("font/woff", "woff"),
    m("font/woff2", "woff2"),
    m("font/ttf", "ttf"),
    m("font/otf", "otf"),
    m("font/collection", "ttc"),
    m("image/png", "png"),
    m("image/apng", "apng"),
    m("image/jpeg", "jpg jpeg jpe jfif"),
    m("image/gif", "gif"),
    m("image/webp", "webp"),
    m("image/avif", "avif"),
    m("image/heic", "heic"),
    m("image/heif", "heif"),
    m("image/jxl", "jxl"),
    m("image/svg+xml", "svg svgz"),
    m("image/bmp", "bmp"),
    m("image/vnd.microsoft.icon", "ico"),
    m("image/tiff", "tif tiff"),
    m("image/x-adobe-dng", "dng"),
    m("image/vnd.adobe.photoshop", "psd"),
    m("audio/mpeg", "mp3"),
    m("audio/aac", "aac"),
    m("audio/mp4", "m4a"),
    m("audio/ogg", "ogg oga"),
    m("audio/opus", "opus"),
    m("audio/wav", "wav"),
    m("audio/flac", "flac"),
    m("audio/webm", "weba"),
    m("audio/midi", "mid midi"),
    m("audio/amr", "amr"),
    m("audio/x-matroska", "mka"),
    m("video/mp4", "mp4 m4v"),
    m("video/webm", "webm"),
    m("video/ogg", "ogv"),
    m("video/quicktime", "mov"),
    m("video/x-matroska", "mkv"),
    m("video/x-msvideo", "avi"),
    m("video/mpeg", "mpeg mpg"),
    m("video/mp2t", "ts m2ts"),
    m("video/3gpp", "3gp"),
    m("video/x-flv", "flv"),
    m("video/x-ms-wmv", "wmv"),
    m("model/gltf+json", "gltf"),
    m("model/gltf-binary", "glb"),
    m("model/stl", "stl"),
    m("model/obj", "obj")
)

/**
 * Matches a MIME type fragment, an extension (with or without the dot) or a file name.
 * Exact extension matches come first.
 */
internal fun lookupMime(query: String): List<MimeType> {
    val q = query.trim().lowercase().removePrefix("*")
    if (q.isEmpty()) return MIME_TYPES
    val ext = if ('/' in q) "" else q.substringAfterLast('.')
    return MIME_TYPES
        .filter { t -> t.type.contains(q) || (ext.isNotEmpty() && t.extensions.any { it.startsWith(ext) }) }
        .sortedByDescending { ext in it.extensions }
}

private class Signature(val label: String, val parts: List<Pair<Int, ByteArray>>)

private fun hex(s: String) = s.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
private fun ascii(s: String) = s.toByteArray(Charsets.ISO_8859_1)
private fun sig(label: String, vararg parts: Pair<Int, ByteArray>) = Signature(label, parts.toList())

/** First match wins, so specific signatures come before short or generic ones. */
private val SIGNATURES = listOf(
    sig("PNG image (image/png)", 0 to hex("89504E470D0A1A0A")),
    sig("JPEG image (image/jpeg)", 0 to hex("FFD8FF")),
    sig("GIF image (image/gif)", 0 to ascii("GIF8")),
    sig("WebP image (image/webp)", 0 to ascii("RIFF"), 8 to ascii("WEBP")),
    sig("WAV audio (audio/wav)", 0 to ascii("RIFF"), 8 to ascii("WAVE")),
    sig("AVI video (video/x-msvideo)", 0 to ascii("RIFF"), 8 to ascii("AVI ")),
    sig("AVIF image (image/avif)", 4 to ascii("ftypavif")),
    sig("HEIC image (image/heic)", 4 to ascii("ftypheic")),
    sig("HEIF image (image/heif)", 4 to ascii("ftypmif1")),
    sig("QuickTime video (video/quicktime)", 4 to ascii("ftypqt  ")),
    sig("M4A audio (audio/mp4)", 4 to ascii("ftypM4A ")),
    sig("MP4 / ISO media (video/mp4)", 4 to ascii("ftyp")),
    sig("JPEG XL image (image/jxl)", 0 to hex("FF0A")),
    sig("PDF document (application/pdf)", 0 to ascii("%PDF-")),
    sig("SQLite database (application/vnd.sqlite3)", 0 to ascii("SQLite format 3")),
    sig("ZIP archive: also docx, xlsx, jar, apk, epub (application/zip)", 0 to hex("504B0304")),
    sig("Empty ZIP archive (application/zip)", 0 to hex("504B0506")),
    sig("gzip (application/gzip)", 0 to hex("1F8B")),
    sig("bzip2 (application/x-bzip2)", 0 to ascii("BZh")),
    sig("xz (application/x-xz)", 0 to hex("FD377A585A00")),
    sig("Zstandard (application/zstd)", 0 to hex("28B52FFD")),
    sig("LZ4 (application/x-lz4)", 0 to hex("04224D18")),
    sig("7-Zip archive (application/x-7z-compressed)", 0 to hex("377ABCAF271C")),
    sig("RAR archive (application/vnd.rar)", 0 to ascii("Rar!")),
    sig("Cabinet archive (application/vnd.ms-cab-compressed)", 0 to ascii("MSCF")),
    sig("ar archive or .deb package", 0 to ascii("!<arch>")),
    sig("RPM package (application/x-rpm)", 0 to hex("EDABEEDB")),
    sig("ELF executable or shared library", 0 to hex("7F454C46")),
    sig("Mach-O 64-bit binary", 0 to hex("CFFAEDFE")),
    sig("Java class or Mach-O universal binary", 0 to hex("CAFEBABE")),
    sig("Android DEX bytecode", 0 to ascii("dex\n")),
    sig("Java KeyStore (JKS)", 0 to hex("FEEDFEED")),
    sig("WebAssembly module (application/wasm)", 0 to hex("0061736D")),
    sig("Ogg container (audio/ogg)", 0 to ascii("OggS")),
    sig("FLAC audio (audio/flac)", 0 to ascii("fLaC")),
    sig("MP3 audio with ID3 tag (audio/mpeg)", 0 to ascii("ID3")),
    sig("MP3 audio (audio/mpeg)", 0 to hex("FFFB")),
    sig("MIDI audio (audio/midi)", 0 to ascii("MThd")),
    sig("Matroska / WebM (video/x-matroska)", 0 to hex("1A45DFA3")),
    sig("Photoshop image (image/vnd.adobe.photoshop)", 0 to ascii("8BPS")),
    sig("TIFF image, little-endian (image/tiff)", 0 to hex("49492A00")),
    sig("TIFF image, big-endian (image/tiff)", 0 to hex("4D4D002A")),
    sig("ICO icon (image/vnd.microsoft.icon)", 0 to hex("00000100")),
    sig("WOFF font (font/woff)", 0 to ascii("wOFF")),
    sig("WOFF2 font (font/woff2)", 0 to ascii("wOF2")),
    sig("OpenType font (font/otf)", 0 to ascii("OTTO")),
    sig("TrueType font (font/ttf)", 0 to hex("0001000000")),
    sig("pcap capture", 0 to hex("D4C3B2A1")),
    sig("pcap capture", 0 to hex("A1B2C3D4")),
    sig("PEM text (certificate or key)", 0 to ascii("-----BEGIN")),
    sig("XML document (application/xml)", 0 to ascii("<?xml")),
    sig("UTF-8 text with byte order mark", 0 to hex("EFBBBF")),
    sig("UTF-16 text, little-endian", 0 to hex("FFFE")),
    sig("UTF-16 text, big-endian", 0 to hex("FEFF")),
    sig("Script with a shebang line", 0 to ascii("#!")),
    sig("Windows executable (application/vnd.microsoft.portable-executable)", 0 to ascii("MZ")),
    sig("BMP image (image/bmp)", 0 to ascii("BM"))
)

/** Names the file format from its first bytes, or null when no known signature matches. */
internal fun detectFileType(header: ByteArray): String? =
    SIGNATURES.firstOrNull { s ->
        s.parts.all { (offset, magic) ->
            header.size >= offset + magic.size && magic.indices.all { header[offset + it] == magic[it] }
        }
    }?.label

internal fun toHex(bytes: ByteArray): String = bytes.joinToString(" ") { (it.toInt() and 0xFF).toString(16).padStart(2, '0').uppercase() }

private const val HEADER_BYTES = 16

/** Reads only the first bytes, so any file size is fine. Runs on the IO dispatcher. */
private fun inspectFile(context: Context, uri: Uri): List<Pair<String, String>> = runCatching {
    val resolver = context.contentResolver
    val header = resolver.openInputStream(uri)?.use { input ->
        val buf = ByteArray(HEADER_BYTES)
        var n = 0
        while (n < HEADER_BYTES) {
            val r = input.read(buf, n, HEADER_BYTES - n)
            if (r < 0) break
            n += r
        }
        buf.copyOf(n)
    } ?: error("no stream")
    val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) c.getString(0) else null
    } ?: uri.lastPathSegment ?: "file"
    listOf(
        "File" to name,
        "Reported type" to (resolver.getType(uri) ?: "Not reported"),
        "Detected" to (detectFileType(header) ?: if (header.isEmpty()) "Empty file" else "Unknown signature"),
        "First bytes" to toHex(header)
    )
}.getOrElse {
    listOf("Error" to "Could not read the file. Pick it again, or copy it to local storage first.")
}

/** What Android's own table says for an extension or a MIME type. */
private fun platformLookup(query: String): String? {
    val q = query.trim().lowercase().removePrefix("*")
    if (q.isEmpty()) return null
    val map = MimeTypeMap.getSingleton()
    return if ('/' in q) {
        map.getExtensionFromMimeType(q)?.let { "$q → .$it" }
    } else {
        val ext = q.substringAfterLast('.')
        map.getMimeTypeFromExtension(ext)?.let { ".$ext → $it" }
    }
}

class MimeTypeTool : Tool {
    override val descriptor = ToolDescriptor(
        "mime-types",
        "MIME Types",
        "Look up MIME types and file extensions",
        Icons.Default.Description,
        ToolCategory.NETWORK_SYSTEM,
        listOf("mime", "content type", "media type", "extension", "file type", "magic bytes")
    )

    @Composable
    override fun Content(modifier: Modifier) {
        var query by rememberSaveable { mutableStateOf("") }
        var fileInfo by rememberSaveable { mutableStateOf(listOf<Pair<String, String>>()) }
        val context = LocalContext.current
        val clipboardManager = LocalClipboardManager.current
        val scope = rememberCoroutineScope()
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) scope.launch { fileInfo = withContext(Dispatchers.IO) { inspectFile(context, uri) } }
        }

        val results = remember(query) { lookupMime(query) }
        val platform = remember(query) { platformLookup(query) }

        ToolScaffold(descriptor.title, modifier = modifier, scrollable = false) {
            SearchField(query, { query = it }, "Extension, file name or MIME type")
            OutlinedButton(onClick = { launcher.launch(arrayOf("*/*")) }, modifier = Modifier.padding(top = 8.dp)) {
                Text("Inspect a file")
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (fileInfo.isNotEmpty()) {
                    item(key = "file") { LabeledValueSlab(fileInfo, Modifier.padding(top = 8.dp)) }
                }
                if (platform != null) {
                    item(key = "platform") { LabeledValueSlab(listOf("Android system" to platform), Modifier.padding(top = 8.dp)) }
                }
                if (results.isEmpty()) {
                    item { EmptyHint("No common type matches \"${query.trim()}\". Try an extension like png or a type like image/.") }
                }
                items(results, key = { it.type }) { type ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable(onClickLabel = "Copy MIME type") {
                                clipboardManager.setText(AnnotatedString(type.type))
                                confirmCopied(context, "MIME type")
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        Text(type.type, style = CodeTextStyle, color = MaterialTheme.colorScheme.primary)
                        Text(
                            if (type.extensions.isEmpty()) "No file extension" else type.extensions.joinToString("  ") { ".$it" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}
