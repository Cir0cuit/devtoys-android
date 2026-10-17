package com.cir0cuit.devtoys.tools.crypto

import com.cir0cuit.devtoys.tools.base.OnSharedFile
import android.net.Uri
import com.cir0cuit.devtoys.tools.base.ShareKind
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.MonoTextField
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.core.ui.rememberFileSaver
import com.cir0cuit.devtoys.core.ui.shareText
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolCategory
import com.cir0cuit.devtoys.tools.base.ToolDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPublicKey
import java.util.Base64
import java.util.Date
import java.util.concurrent.TimeUnit

private const val MAX_CERT_FILE_BYTES = 1024 * 1024

private val KEY_USAGES = listOf(
    "digitalSignature", "nonRepudiation", "keyEncipherment", "dataEncipherment", "keyAgreement",
    "keyCertSign", "cRLSign", "encipherOnly", "decipherOnly"
)

private val EXTENDED_KEY_USAGES = mapOf(
    "1.3.6.1.5.5.7.3.1" to "serverAuth",
    "1.3.6.1.5.5.7.3.2" to "clientAuth",
    "1.3.6.1.5.5.7.3.3" to "codeSigning",
    "1.3.6.1.5.5.7.3.4" to "emailProtection",
    "1.3.6.1.5.5.7.3.8" to "timeStamping",
    "1.3.6.1.5.5.7.3.9" to "OCSPSigning",
    "2.5.29.37.0" to "anyExtendedKeyUsage",
)

private val SAN_PREFIXES = mapOf(0 to "other", 1 to "email", 2 to "DNS", 3 to "X400", 4 to "DirName", 5 to "EDI", 6 to "URI", 7 to "IP", 8 to "RID")

private fun colonHex(bytes: ByteArray) = bytes.joinToString(":") { "%02X".format(it) }

private fun sha(algorithm: String, bytes: ByteArray) = MessageDigest.getInstance(algorithm).digest(bytes)

private class Der(val tag: Int, val content: ByteArray, val end: Int)

/** The DER element starting at [at] (single-byte tags only). */
private fun readDer(b: ByteArray, at: Int): Der {
    val tag = b[at].toInt() and 0xff
    var i = at + 1
    var length = b[i++].toInt() and 0xff
    if (length >= 0x80) {
        val n = length and 0x7f
        length = 0
        repeat(n) { length = (length shl 8) or (b[i++].toInt() and 0xff) }
    }
    return Der(tag, b.copyOfRange(i, i + length), i + length)
}

/** Subject (2.5.29.14) or authority (2.5.29.35) key identifier, or null when absent. */
private fun X509Certificate.keyIdentifier(oid: String): ByteArray? {
    val extension = getExtensionValue(oid) ?: return null
    return try {
        val value = readDer(readDer(extension, 0).content, 0)
        if (oid == "2.5.29.14") return value.content
        // AuthorityKeyIdentifier ::= SEQUENCE { keyIdentifier [0] IMPLICIT OCTET STRING OPTIONAL, ... }
        var at = 0
        while (at < value.content.size) {
            val e = readDer(value.content, at)
            if (e.tag == 0x80) return e.content
            at = e.end
        }
        null
    } catch (e: RuntimeException) {
        null
    }
}

private fun X509Certificate.publicKeyDescription(): String = when (val key = publicKey) {
    is RSAPublicKey -> "RSA ${key.modulus.bitLength()} bits"
    is ECPublicKey -> "EC ${key.params.curve.field.fieldSize} bits"
    else -> key.algorithm
}

/** Labeled fields of a certificate; extensions that are absent are left out. */
internal fun X509Certificate.fields(now: Date = Date()): List<Pair<String, String>> {
    val status = when {
        now.after(notAfter) -> "Expired"
        now.before(notBefore) -> "Not yet valid"
        else -> "Valid, expires in ${TimeUnit.MILLISECONDS.toDays(notAfter.time - now.time)} days"
    }
    val san = subjectAlternativeNames?.joinToString(", ") { entry ->
        "${SAN_PREFIXES[entry[0] as Int] ?: "other"}:${entry[1] as? String ?: "(binary)"}"
    }
    val keyUsageText = keyUsage?.let { bits -> KEY_USAGES.filterIndexed { i, _ -> bits.getOrElse(i) { false } }.joinToString(", ") }
    val extendedKeyUsageText = extendedKeyUsage?.joinToString(", ") { EXTENDED_KEY_USAGES[it] ?: it }
    val basicConstraintsText = if (getExtensionValue("2.5.29.19") == null) null else when (val path = basicConstraints) {
        -1 -> "Not a CA"
        Int.MAX_VALUE -> "CA, no path length limit"
        else -> "CA, path length $path"
    }
    return listOf(
        "Subject" to subjectX500Principal.toString(),
        "Subject alternative names" to san,
        "Issuer" to issuerX500Principal.toString(),
        "Status" to status,
        "Valid from" to notBefore.toInstant().toString(),
        "Valid to" to notAfter.toInstant().toString(),
        "Public key" to publicKeyDescription(),
        "Key usage" to keyUsageText,
        "Extended key usage" to extendedKeyUsageText,
        "Basic constraints" to basicConstraintsText,
        "Serial number" to serialNumber.toString(16),
        "Signature algorithm" to sigAlgName,
        "Version" to version.toString(),
        "SHA-256 fingerprint" to colonHex(sha("SHA-256", encoded)),
        "SHA-1 fingerprint" to colonHex(sha("SHA-1", encoded)),
        "Public key pin (SHA-256)" to Base64.getEncoder().encodeToString(sha("SHA-256", publicKey.encoded)),
        "Subject key identifier" to keyIdentifier("2.5.29.14")?.let(::colonHex),
        "Authority key identifier" to keyIdentifier("2.5.29.35")?.let(::colonHex),
    ).mapNotNull { (label, value) -> value?.let { label to it } }
}

/** Uses toPem(label, der) from KeyPairGeneratorTool.kt. */
internal fun X509Certificate.toPem(): String = toPem("CERTIFICATE", encoded)

/** Certificates in PEM text (one or more blocks, or PKCS#7) or a bare Base64 DER body. */
internal fun parseCertificates(text: String): List<X509Certificate> {
    val trimmed = text.trim().removePrefix(Char(0xFEFF).toString())
    val bytes = if ("-----BEGIN" in trimmed) trimmed.toByteArray() else parseBase64(trimmed)
    val certs = CertificateFactory.getInstance("X.509").generateCertificates(bytes.inputStream()).map { it as X509Certificate }
    require(certs.isNotEmpty()) { "No certificate found" }
    return certs
}

/** A certificate file's content as text: PEM files as is, binary DER or PKCS#7 converted to PEM blocks. */
internal fun certificateFileToText(bytes: ByteArray): String =
    if (bytes.firstOrNull() == 0x30.toByte()) {
        CertificateFactory.getInstance("X.509").generateCertificates(bytes.inputStream())
            .joinToString("") { (it as X509Certificate).toPem() }
    } else {
        bytes.toString(Charsets.UTF_8)
    }

private class DecodedCert(val cert: X509Certificate, val fields: List<Pair<String, String>>) {
    val text = fields.joinToString("\n") { (label, value) -> "$label: $value" }
    val fileName = (Regex("CN=([^,]+)").find(cert.subjectX500Principal.name)?.groupValues?.get(1) ?: "certificate")
        .replace(Regex("[^A-Za-z0-9._-]"), "_")
}

class X509DecoderTool : Tool {
    override val descriptor = ToolDescriptor(
        id = "x509",
        title = "X.509 Certificate Decoder",
        description = "Inspect PEM, DER and PKCS#7 certificates",
        icon = Icons.Default.VerifiedUser,
        category = ToolCategory.CRYPTOGRAPHY,
        keywords = listOf("x509", "certificate", "pem", "der", "crt", "cer", "p7b", "ssl", "tls", "pin"),
        accepts = setOf(ShareKind.FILE),
        fileTypes = setOf("pem", "crt", "cer", "der", "cert")
    )

    @Composable
    override fun Content(modifier: Modifier) {
        var input by rememberSaveableText()
        val context = LocalContext.current
        val clipboard = LocalClipboardManager.current
        val scope = rememberCoroutineScope()
        val savePem = rememberFileSaver("application/x-pem-file")
        val saveDer = rememberFileSaver("application/pkix-cert")

        // null while the input is blank; failure when it is not a certificate.
        var decoded by remember { mutableStateOf<Result<List<DecodedCert>>?>(null) }
        LaunchedEffect(input) {
            if (input.isBlank()) {
                decoded = null
                return@LaunchedEffect
            }
            delay(150)
            decoded = withContext(Dispatchers.Default) {
                runCatching { parseCertificates(input).map { DecodedCert(it, it.fields()) } }
            }
        }

        val loadFile: (Uri) -> Unit = { uri ->
            scope.launch {
                val text = withContext(Dispatchers.IO) {
                    runCatching {
                        val bytes = context.contentResolver.openInputStream(uri)?.use { stream ->
                            val out = ByteArrayOutputStream()
                            val buffer = ByteArray(8192)
                            while (true) {
                                val n = stream.read(buffer)
                                if (n < 0) break
                                out.write(buffer, 0, n)
                                require(out.size() <= MAX_CERT_FILE_BYTES) { "File is larger than 1 MB" }
                            }
                            out.toByteArray()
                        } ?: error("Could not open the file")
                        certificateFileToText(bytes)
                    }
                }
                text.onSuccess { input = it }
                    .onFailure { Toast.makeText(context, "Could not read a certificate from this file. Choose a .pem, .crt, .cer or .p7b file.", Toast.LENGTH_LONG).show() }
            }
        }
        val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? -> uri?.let(loadFile) }
        OnSharedFile { loadFile(it.uri) }

        ToolScaffold(descriptor.title, modifier = modifier) {
            MonoTextField(input, { input = it }, "PEM or Base64 certificate", minLines = 5, maxLines = 10)
            Row {
                TextButton(
                    onClick = { clipboard.getText()?.text?.let { input = it } },
                    contentPadding = ButtonDefaults.TextButtonWithIconContentPadding
                ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text("Paste")
                }
                TextButton(
                    onClick = {
                        openFile.launch(
                            arrayOf(
                                "application/x-x509-ca-cert", "application/pkix-cert", "application/x-pem-file",
                                "application/pkcs7-mime", "application/x-pkcs7-certificates", "*/*"
                            )
                        )
                    },
                    contentPadding = ButtonDefaults.TextButtonWithIconContentPadding
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text("Open file")
                }
            }
            Spacer(Modifier.height(8.dp))
            val certs = decoded?.getOrNull()
            when {
                decoded == null -> Text(
                    "Paste a certificate or open a .pem, .crt, .cer or .p7b file to see its fields.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                certs == null -> Text(
                    "Could not read the certificate. Paste a full PEM block or its Base64 body.",
                    color = MaterialTheme.colorScheme.error
                )
                else -> certs.forEachIndexed { i, c ->
                    var menuOpen by remember { mutableStateOf(false) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (certs.size == 1) "Certificate" else "Certificate ${i + 1} of ${certs.size}",
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.titleSmall
                        )
                        IconButton(onClick = { clipboard.setText(AnnotatedString(c.text)) }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy all fields")
                        }
                        IconButton(onClick = { shareText(context, c.text) }) {
                            Icon(Icons.Default.Share, contentDescription = "Share fields")
                        }
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More actions")
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(text = { Text("Copy Base64 body") }, onClick = {
                                    menuOpen = false
                                    clipboard.setText(AnnotatedString(Base64.getEncoder().encodeToString(c.cert.encoded)))
                                })
                                DropdownMenuItem(text = { Text("Save as PEM") }, onClick = {
                                    menuOpen = false
                                    savePem("${c.fileName}.pem", c.cert.toPem().toByteArray())
                                })
                                DropdownMenuItem(text = { Text("Save as DER (.crt)") }, onClick = {
                                    menuOpen = false
                                    saveDer("${c.fileName}.crt", c.cert.encoded)
                                })
                            }
                        }
                    }
                    ResultSlab(c.fields)
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}
