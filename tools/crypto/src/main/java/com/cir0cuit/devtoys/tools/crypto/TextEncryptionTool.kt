package com.cir0cuit.devtoys.tools.crypto

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.MonoTextField
import com.cir0cuit.devtoys.core.ui.OutputPanel
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolCategory
import com.cir0cuit.devtoys.tools.base.ToolDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.AEADBadTagException
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

private val OPENSSL_MAGIC = "Salted__".toByteArray()
private val random = SecureRandom()
internal const val DEFAULT_PBKDF2_ITERATIONS = 600_000

private fun randomBytes(n: Int) = ByteArray(n).also { random.nextBytes(it) }

internal fun pbkdf2Sha256(password: String, salt: ByteArray, iterations: Int, bytes: Int): ByteArray =
    SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        .generateSecret(PBEKeySpec(password.toCharArray(), salt, iterations, bytes * 8)).encoded

/** AES key from hex (32/48/64 digits) or Base64 (16/24/32 bytes). */
internal fun parseRawAesKey(text: String): ByteArray {
    val t = text.trim()
    val bytes = if (t.length in setOf(32, 48, 64) && t.all { it in '0'..'9' || it.lowercaseChar() in 'a'..'f' }) {
        t.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    } else {
        runCatching { Base64.getDecoder().decode(t) }.getOrNull()
    }
    require(bytes != null && bytes.size in setOf(16, 24, 32)) {
        "The key must be 16, 24 or 32 bytes: 32, 48 or 64 hex digits, or Base64."
    }
    return bytes
}

private fun aes(gcm: Boolean, mode: Int, key: ByteArray, iv: ByteArray): Cipher =
    Cipher.getInstance(if (gcm) "AES/GCM/NoPadding" else "AES/CBC/PKCS5Padding").apply {
        init(mode, SecretKeySpec(key, "AES"), if (gcm) GCMParameterSpec(128, iv) else IvParameterSpec(iv))
    }

/**
 * Encrypts [plain] to Base64.
 * - Password + CBC: OpenSSL format (`openssl enc -aes-256-cbc -pbkdf2 -iter N -base64`), 64-char lines.
 * - Password + GCM: salt(16) || iv(12) || ciphertext+tag, AES-256 key from PBKDF2-HMAC-SHA256.
 * - Raw key: iv || ciphertext (GCM: 12-byte iv and appended tag; CBC: 16-byte iv).
 */
internal fun encryptText(plain: String, gcm: Boolean, usePassword: Boolean, secret: String, iterations: Int): String {
    val data = plain.toByteArray()
    if (!usePassword) {
        val iv = randomBytes(if (gcm) 12 else 16)
        return Base64.getEncoder().encodeToString(iv + aes(gcm, Cipher.ENCRYPT_MODE, parseRawAesKey(secret), iv).doFinal(data))
    }
    require(secret.isNotEmpty()) { "Enter a password." }
    if (!gcm) {
        val salt = randomBytes(8)
        val km = pbkdf2Sha256(secret, salt, iterations, 48)
        val ct = aes(false, Cipher.ENCRYPT_MODE, km.copyOf(32), km.copyOfRange(32, 48)).doFinal(data)
        return Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(OPENSSL_MAGIC + salt + ct)
    }
    val salt = randomBytes(16)
    val iv = randomBytes(12)
    val ct = aes(true, Cipher.ENCRYPT_MODE, pbkdf2Sha256(secret, salt, iterations, 32), iv).doFinal(data)
    return Base64.getEncoder().encodeToString(salt + iv + ct)
}

/** Reverses [encryptText]; the input may contain line breaks. */
internal fun decryptText(blob: String, gcm: Boolean, usePassword: Boolean, secret: String, iterations: Int): String {
    val data = try {
        // Encrypted output is always padded, so a length that is not a multiple of 4 is not ours.
        val cleaned = blob.filterNot { it.isWhitespace() }
        require(cleaned.length % 4 == 0)
        Base64.getDecoder().decode(cleaned)
    } catch (e: IllegalArgumentException) {
        throw IllegalArgumentException("The input is not valid Base64. Paste the whole encrypted text.")
    }
    val plain = if (!usePassword) {
        val ivLen = if (gcm) 12 else 16
        require(data.size > ivLen) { "The input is too short to hold an IV and ciphertext." }
        aes(gcm, Cipher.DECRYPT_MODE, parseRawAesKey(secret), data.copyOf(ivLen)).doFinal(data, ivLen, data.size - ivLen)
    } else {
        require(secret.isNotEmpty()) { "Enter a password." }
        if (!gcm) {
            require(data.size >= 32 && data.copyOf(8).contentEquals(OPENSSL_MAGIC)) {
                "The input has no OpenSSL Salted__ header. Was it encrypted with AES-GCM or a raw key?"
            }
            val km = pbkdf2Sha256(secret, data.copyOfRange(8, 16), iterations, 48)
            aes(false, Cipher.DECRYPT_MODE, km.copyOf(32), km.copyOfRange(32, 48)).doFinal(data, 16, data.size - 16)
        } else {
            require(data.size >= 16 + 12 + 16) { "The input is too short to hold a salt, IV and tag." }
            val key = pbkdf2Sha256(secret, data.copyOf(16), iterations, 32)
            aes(true, Cipher.DECRYPT_MODE, key, data.copyOfRange(16, 28)).doFinal(data, 28, data.size - 28)
        }
    }
    return plain.toString(Charsets.UTF_8)
}

internal fun cipherErrorMessage(e: Throwable, usePassword: Boolean): String = when (e) {
    is AEADBadTagException -> "Authentication failed: wrong " +
        (if (usePassword) "password or iteration count" else "key") + ", or the data was changed."
    is BadPaddingException -> "Decryption failed: wrong " +
        (if (usePassword) "password or iteration count" else "key") + ", or the data is corrupted."
    is IllegalArgumentException -> e.message ?: "Invalid input."
    is GeneralSecurityException -> "The cipher failed (${e.message}). Check the key and the input."
    else -> e.message ?: "Something went wrong. Check the input."
}

class TextEncryptionTool : Tool {
    override val descriptor = ToolDescriptor(
        id = "encrypt",
        title = "Text Encryption",
        description = "Encrypt and decrypt text with AES",
        icon = Icons.Default.Lock,
        category = ToolCategory.CRYPTOGRAPHY,
        keywords = listOf("aes", "gcm", "cbc", "encrypt", "decrypt", "cipher", "openssl", "pbkdf2", "password")
    )

    @Composable
    override fun Content(modifier: Modifier) {
        var decrypt by rememberSaveable { mutableStateOf(false) }
        var input by rememberSaveableText()
        // Not saveable on purpose: passwords and keys are not written to saved instance state.
        var secret by remember { mutableStateOf("") }
        var usePassword by rememberSaveable { mutableStateOf(true) }
        var gcm by rememberSaveable { mutableStateOf(true) }
        var iterationsText by rememberSaveable { mutableStateOf(DEFAULT_PBKDF2_ITERATIONS.toString()) }
        var output by remember { mutableStateOf<Result<String>?>(null) }
        var running by remember { mutableStateOf(false) }
        var job by remember { mutableStateOf<Job?>(null) }
        val scope = rememberCoroutineScope()

        val iterations = iterationsText.toIntOrNull()?.takeIf { it in 1_000..10_000_000 }
        LaunchedEffect(decrypt, input, secret, usePassword, gcm, iterationsText) {
            job?.cancel()
            running = false
            output = null
        }

        ToolScaffold(descriptor.title, modifier = modifier) {
            Segmented(listOf("Encrypt", "Decrypt"), if (decrypt) 1 else 0, { decrypt = it == 1 })
            Spacer(Modifier.height(16.dp))
            MonoTextField(input, { input = it }, if (decrypt) "Encrypted text (Base64)" else "Plain text", minLines = 4,
                isError = output?.exceptionOrNull() is IllegalArgumentException)
            Spacer(Modifier.height(8.dp))
            SecretField(
                secret, { secret = it },
                label = if (usePassword) "Password" else "Key (hex or Base64)",
                supportingText = if (usePassword) null else "AES-128, 192 or 256 key: 16, 24 or 32 bytes"
            )
            Spacer(Modifier.height(16.dp))
            Segmented(listOf("Password", "Raw key"), if (usePassword) 0 else 1, { usePassword = it == 0 })
            Spacer(Modifier.height(8.dp))
            Segmented(listOf("AES-GCM", "AES-CBC"), if (gcm) 0 else 1, { gcm = it == 0 })
            if (usePassword) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    iterationsText, { iterationsText = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(),
                    label = { Text("PBKDF2 iterations") }, singleLine = true, shape = MaterialTheme.shapes.medium,
                    isError = iterations == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = {
                        Text(
                            when {
                                iterations == null -> "Enter 1000 to 10000000 iterations."
                                gcm -> "AES-256 key from PBKDF2-HMAC-SHA256. Output: salt, IV, ciphertext and tag."
                                else -> "OpenSSL format: openssl enc -aes-256-cbc -pbkdf2 -iter $iterations -base64"
                            }
                        )
                    }
                )
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    job?.cancel()
                    val text = input
                    val key = secret
                    val dec = decrypt
                    val isGcm = gcm
                    val pw = usePassword
                    val iter = iterations ?: 0
                    running = true
                    job = scope.launch {
                        val r = withContext(Dispatchers.Default) {
                            runCatching {
                                if (dec) decryptText(text, isGcm, pw, key, iter) else encryptText(text, isGcm, pw, key, iter)
                            }
                        }
                        output = r
                        running = false
                    }
                },
                enabled = !running && input.isNotEmpty() && secret.isNotEmpty() && (iterations != null || !usePassword),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (decrypt) "Decrypt" else "Encrypt") }
            Spacer(Modifier.height(16.dp))
            if (running) LinearProgressIndicator(Modifier.fillMaxWidth())
            output?.onSuccess { OutputPanel(it, label = if (decrypt) "Plain text" else "Encrypted (Base64)") }
                ?.onFailure { Text(cipherErrorMessage(it, usePassword), color = MaterialTheme.colorScheme.error) }
        }
    }
}
