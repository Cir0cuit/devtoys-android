package com.cir0cuit.devtoys.tools.crypto

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.MonoTextField
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolCategory
import com.cir0cuit.devtoys.tools.base.ToolDescriptor
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Date
import java.util.concurrent.TimeUnit

private fun X509Certificate.fields(now: Date = Date()): List<Pair<String, String>> {
    val status = when {
        now.after(notAfter) -> "Expired"
        now.before(notBefore) -> "Not yet valid"
        else -> "Valid, expires in ${TimeUnit.MILLISECONDS.toDays(notAfter.time - now.time)} days"
    }
    return listOf(
        "Subject" to subjectX500Principal.toString(),
        "Issuer" to issuerX500Principal.toString(),
        "Status" to status,
        "Valid from" to notBefore.toInstant().toString(),
        "Valid to" to notAfter.toInstant().toString(),
        "Serial number" to serialNumber.toString(16),
        "Signature algorithm" to sigAlgName,
        "Version" to version.toString(),
        "SHA-256 fingerprint" to MessageDigest.getInstance("SHA-256").digest(encoded).toLowerHex(),
    )
}

class X509DecoderTool : Tool {
    override val descriptor = ToolDescriptor(
        id = "x509",
        title = "X.509 Certificate Decoder",
        description = "Read the fields of a PEM certificate",
        icon = Icons.Default.VerifiedUser,
        category = ToolCategory.CRYPTOGRAPHY,
        keywords = listOf("x509", "certificate", "pem", "ssl")
    )

    @Composable
    override fun Content(modifier: Modifier) {
        var input by rememberSaveable { mutableStateOf("") }
        val clipboard = LocalClipboardManager.current

        // Fields, or null when the input is not a certificate.
        val fields = remember(input) {
            if (input.isBlank()) return@remember emptyList()
            try {
                val trimmed = input.trim()
                val pem = if ("-----BEGIN CERTIFICATE-----" in trimmed) trimmed
                else "-----BEGIN CERTIFICATE-----\n" + trimmed.chunked(64).joinToString("\n") + "\n-----END CERTIFICATE-----"
                (CertificateFactory.getInstance("X.509").generateCertificate(pem.byteInputStream()) as X509Certificate).fields()
            } catch (e: Exception) {
                null
            }
        }

        ToolScaffold(descriptor.title, modifier = modifier) {
            MonoTextField(input, { input = it }, "PEM certificate", minLines = 5, maxLines = 10)
            TextButton(
                onClick = { clipboard.getText()?.text?.let { input = it } },
                contentPadding = ButtonDefaults.TextButtonWithIconContentPadding
            ) {
                Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text("Paste")
            }
            Spacer(Modifier.height(8.dp))
            when {
                fields == null -> Text(
                    "Could not read the certificate. Paste a full PEM block or its Base64 body.",
                    color = MaterialTheme.colorScheme.error
                )
                fields.isEmpty() -> Text(
                    "Paste a certificate to see its subject, issuer, validity and fingerprint.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> ResultSlab(fields)
            }
        }
    }
}
