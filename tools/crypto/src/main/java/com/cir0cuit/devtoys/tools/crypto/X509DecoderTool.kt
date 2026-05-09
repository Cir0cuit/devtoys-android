package com.cir0cuit.devtoys.tools.crypto

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

class X509DecoderTool : Tool {
    override val descriptor = ToolDescriptor(
        "x509",
        android.R.string.untitled,
        android.R.string.untitled,
        Icons.Default.VerifiedUser,
        ToolCategory.CRYPTOGRAPHY,
        listOf("x509", "certificate", "pem", "ssl")
    )

    @Composable
    override fun Content(modifier: Modifier) {
        var input by rememberSaveable { mutableStateOf("") }
        
        val output = remember(input) {
            if (input.isEmpty()) return@remember ""
            try {
                var cleanInput = input.trim()
                if (!cleanInput.contains("-----BEGIN CERTIFICATE-----")) {
                    cleanInput = "-----BEGIN CERTIFICATE-----\n" + cleanInput.chunked(64).joinToString("\n") + "\n-----END CERTIFICATE-----"
                }
                val factory = CertificateFactory.getInstance("X.509")
                val cert = factory.generateCertificate(ByteArrayInputStream(cleanInput.toByteArray())) as X509Certificate
                
                """
                    Subject: ${cert.subjectX500Principal}
                    Issuer: ${cert.issuerX500Principal}
                    Valid From: ${cert.notBefore}
                    Valid To: ${cert.notAfter}
                    Serial Number: ${cert.serialNumber.toString(16)}
                    Algorithm: ${cert.sigAlgName}
                    Version: ${cert.version}
                """.trimIndent()
            } catch (e: Exception) {
                "Error parsing certificate. Please ensure it's a valid Base64 PEM or raw Base64 string."
            }
        }

        ToolScaffold("X.509 Decoder", modifier = modifier) {
            MonoTextField(input, { input = it }, "PEM Certificate", minLines = 5)
            Spacer(modifier = Modifier.height(16.dp))
            OutputPanel(output)
        }
    }
}
