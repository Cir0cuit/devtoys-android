package com.cir0cuit.devtoys.tools.crypto

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.OutputPanel
import com.cir0cuit.devtoys.core.ui.SelectField
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.core.ui.rememberFileSaver
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolCategory
import com.cir0cuit.devtoys.tools.base.ToolDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PublicKey
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPrivateCrtKey
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.ECGenParameterSpec
import java.util.Base64

internal enum class KeyType(val label: String, val algorithm: String, val size: Int, val curve: String? = null) {
    RSA_2048("RSA 2048", "RSA", 2048),
    RSA_3072("RSA 3072", "RSA", 3072),
    RSA_4096("RSA 4096", "RSA", 4096),
    EC_P256("EC P-256", "EC", 256, "secp256r1"),
    EC_P384("EC P-384", "EC", 384, "secp384r1"),
    EC_P521("EC P-521", "EC", 521, "secp521r1"),
}

internal fun generateKeyPair(type: KeyType): KeyPair = KeyPairGenerator.getInstance(type.algorithm).run {
    if (type.curve == null) initialize(type.size) else initialize(ECGenParameterSpec(type.curve))
    generateKeyPair()
}

internal fun toPem(label: String, der: ByteArray): String =
    "-----BEGIN $label-----\n" + Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(der) + "\n-----END $label-----\n"

/** Big-endian magnitude without the sign byte, left-padded to [length] when given. */
private fun BigInteger.unsigned(length: Int = 0): ByteArray {
    val b = toByteArray().let { if (it.size > 1 && it[0] == 0.toByte()) it.copyOfRange(1, it.size) else it }
    return if (b.size >= length) b else ByteArray(length - b.size) + b
}

private val ECPublicKey.bits get() = params.curve.field.fieldSize
private fun coordinateLength(bits: Int) = (bits + 7) / 8

/** SSH wire-format public key blob (RFC 4253 ssh-rsa, RFC 5656 ecdsa-sha2-nistp*). */
internal fun sshPublicKeyBlob(key: PublicKey): ByteArray {
    val bytes = ByteArrayOutputStream()
    val out = DataOutputStream(bytes)
    fun string(b: ByteArray) = out.run { writeInt(b.size); write(b) }
    when (key) {
        is RSAPublicKey -> {
            string("ssh-rsa".toByteArray())
            string(key.publicExponent.toByteArray()) // two's complement == SSH mpint
            string(key.modulus.toByteArray())
        }
        is ECPublicKey -> {
            val curve = "nistp${key.bits}"
            val len = coordinateLength(key.bits)
            string("ecdsa-sha2-$curve".toByteArray())
            string(curve.toByteArray())
            string(byteArrayOf(4) + key.w.affineX.unsigned(len) + key.w.affineY.unsigned(len))
        }
        else -> throw IllegalArgumentException("Unsupported key type ${key.algorithm}")
    }
    return bytes.toByteArray()
}

internal fun sshPublicKeyLine(key: PublicKey, comment: String): String {
    val blob = sshPublicKeyBlob(key)
    val type = String(blob, 4, ((blob[2].toInt() and 0xff) shl 8) or (blob[3].toInt() and 0xff))
    return "$type ${Base64.getEncoder().encodeToString(blob)}" + if (comment.isBlank()) "" else " ${comment.trim()}"
}

internal fun sshFingerprint(key: PublicKey): String =
    "SHA256:" + Base64.getEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(sshPublicKeyBlob(key)))

/** RFC 7517/7518 JWK; includes the private members when [includePrivate]. */
internal fun toJwk(pair: KeyPair, includePrivate: Boolean): String {
    fun b64u(b: ByteArray) = Base64.getUrlEncoder().withoutPadding().encodeToString(b)
    val f = linkedMapOf<String, String>()
    when (val pub = pair.public) {
        is RSAPublicKey -> {
            f["kty"] = "RSA"
            f["n"] = b64u(pub.modulus.unsigned())
            f["e"] = b64u(pub.publicExponent.unsigned())
            if (includePrivate) when (val p = pair.private) {
                is RSAPrivateCrtKey -> {
                    f["d"] = b64u(p.privateExponent.unsigned())
                    f["p"] = b64u(p.primeP.unsigned())
                    f["q"] = b64u(p.primeQ.unsigned())
                    f["dp"] = b64u(p.primeExponentP.unsigned())
                    f["dq"] = b64u(p.primeExponentQ.unsigned())
                    f["qi"] = b64u(p.crtCoefficient.unsigned())
                }
                is RSAPrivateKey -> f["d"] = b64u(p.privateExponent.unsigned())
            }
        }
        is ECPublicKey -> {
            val len = coordinateLength(pub.bits)
            f["kty"] = "EC"
            f["crv"] = "P-${pub.bits}"
            f["x"] = b64u(pub.w.affineX.unsigned(len))
            f["y"] = b64u(pub.w.affineY.unsigned(len))
            if (includePrivate) (pair.private as? ECPrivateKey)?.let { f["d"] = b64u(it.s.unsigned(len)) }
        }
    }
    return f.entries.joinToString(",\n", "{\n", "\n}") { (k, v) -> "  \"$k\": \"$v\"" }
}

private class KeyOutput(val pair: KeyPair, val privatePem: String, val publicPem: String, val publicJwk: String, val privateJwk: String)

class KeyPairGeneratorTool : Tool {
    override val descriptor = ToolDescriptor(
        id = "keypair",
        title = "Key Pair Generator",
        description = "Generate RSA and EC keys as PEM, SSH or JWK",
        icon = Icons.Default.Key,
        category = ToolCategory.CRYPTOGRAPHY,
        keywords = listOf("rsa", "ec", "ecdsa", "ssh", "pem", "jwk", "public key", "private key", "openssh")
    )

    @Composable
    override fun Content(modifier: Modifier) {
        var type by rememberSaveable { mutableStateOf(KeyType.EC_P256) }
        var comment by rememberSaveable { mutableStateOf("") }
        // Keys stay in memory only; they are not written to saved instance state.
        var result by remember { mutableStateOf<KeyOutput?>(null) }
        var error by remember { mutableStateOf<String?>(null) }
        var running by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        val saveFile = rememberFileSaver("application/x-pem-file")

        ToolScaffold(descriptor.title, modifier = modifier) {
            SelectField(
                label = "Key type",
                selected = type,
                options = KeyType.entries,
                // A key of the old type would sit under the new type's label.
                onSelect = { if (it != type) { type = it; result = null; error = null } },
                enabled = !running,
                modifier = Modifier.fillMaxWidth(),
                optionLabel = { it.label }
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                comment, { comment = it }, Modifier.fillMaxWidth(),
                label = { Text("SSH comment (optional)") }, placeholder = { Text("user@host") },
                singleLine = true, shape = MaterialTheme.shapes.medium
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val t = type
                    error = null
                    result = null
                    running = true
                    scope.launch {
                        val r = runCatching {
                            withContext(Dispatchers.Default) {
                                val pair = generateKeyPair(t)
                                KeyOutput(
                                    pair,
                                    toPem("PRIVATE KEY", pair.private.encoded),
                                    toPem("PUBLIC KEY", pair.public.encoded),
                                    toJwk(pair, includePrivate = false),
                                    toJwk(pair, includePrivate = true)
                                )
                            }
                        }
                        // Cancellation only happens when the screen leaves composition; nothing to update then.
                        r.onSuccess { result = it }.onFailure { error = "Could not generate the key (${it.message}). Try another key type." }
                        running = false
                    }
                },
                enabled = !running,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Generate") }
            Spacer(Modifier.height(16.dp))
            if (running) {
                Text("Generating… RSA 4096 can take several seconds.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            result?.let { r ->
                ResultSlab(
                    listOf(
                        "OpenSSH public key" to sshPublicKeyLine(r.pair.public, comment),
                        "SSH fingerprint" to sshFingerprint(r.pair.public)
                    )
                )
                Spacer(Modifier.height(16.dp))
                OutputPanel(r.publicPem, label = "Public key (SPKI PEM)")
                OutlinedButton(onClick = { saveFile("public_key.pem", r.publicPem.toByteArray()) }, Modifier.fillMaxWidth()) {
                    Text("Save public key")
                }
                Spacer(Modifier.height(16.dp))
                OutputPanel(r.privatePem, label = "Private key (PKCS#8 PEM)")
                OutlinedButton(onClick = { saveFile("private_key.pem", r.privatePem.toByteArray()) }, Modifier.fillMaxWidth()) {
                    Text("Save private key")
                }
                Spacer(Modifier.height(16.dp))
                OutputPanel(r.publicJwk, label = "Public JWK")
                Spacer(Modifier.height(16.dp))
                OutputPanel(r.privateJwk, label = "Private JWK")
            }
        }
    }
}
