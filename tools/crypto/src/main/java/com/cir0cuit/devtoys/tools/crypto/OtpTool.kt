package com.cir0cuit.devtoys.tools.crypto

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.designsystem.CodeTextStyle
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolCategory
import com.cir0cuit.devtoys.tools.base.ToolDescriptor
import kotlinx.coroutines.delay
import java.io.ByteArrayOutputStream
import java.net.URI
import java.net.URISyntaxException
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private const val BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
internal val OTP_ALGORITHMS = listOf("SHA1", "SHA256", "SHA512")

/** RFC 4648 Base32 decode; ignores case, spaces, dashes and padding. */
internal fun base32Decode(input: String): ByteArray {
    val out = ByteArrayOutputStream()
    var buffer = 0
    var bits = 0
    for (c in input.filterNot { it.isWhitespace() || it == '-' }.trimEnd('=').uppercase()) {
        val v = BASE32_ALPHABET.indexOf(c)
        require(v >= 0) { "'$c' is not a Base32 character. Use only the letters A-Z and the digits 2-7." }
        buffer = (buffer shl 5) or v
        bits += 5
        if (bits >= 8) {
            bits -= 8
            out.write((buffer shr bits) and 0xff)
        }
    }
    return out.toByteArray()
}

/** RFC 4648 Base32 encode without padding, as authenticator apps expect. */
internal fun base32Encode(data: ByteArray): String {
    val sb = StringBuilder()
    var buffer = 0
    var bits = 0
    for (b in data) {
        buffer = (buffer shl 8) or (b.toInt() and 0xff)
        bits += 8
        while (bits >= 5) {
            bits -= 5
            sb.append(BASE32_ALPHABET[(buffer shr bits) and 31])
        }
    }
    if (bits > 0) sb.append(BASE32_ALPHABET[(buffer shl (5 - bits)) and 31])
    return sb.toString()
}

/** RFC 4226 HOTP value for [counter]; [algorithm] is SHA1, SHA256 or SHA512 (RFC 6238). */
internal fun hotp(key: ByteArray, counter: Long, algorithm: String = "SHA1", digits: Int = 6): String {
    val mac = Mac.getInstance("Hmac$algorithm")
    mac.init(SecretKeySpec(key, "Hmac$algorithm"))
    val h = mac.doFinal(ByteBuffer.allocate(8).putLong(counter).array())
    val o = h.last().toInt() and 0x0f
    val bin = ((h[o].toInt() and 0x7f) shl 24) or ((h[o + 1].toInt() and 0xff) shl 16) or
        ((h[o + 2].toInt() and 0xff) shl 8) or (h[o + 3].toInt() and 0xff)
    var mod = 1L
    repeat(digits) { mod *= 10 }
    return (bin % mod).toString().padStart(digits, '0')
}

/** RFC 6238 time step for [epochSeconds]. */
internal fun totpCounter(epochSeconds: Long, period: Int): Long = Math.floorDiv(epochSeconds, period.toLong())

/** Step offset (0, -1 or +1) at which [code] matches around [counter], or null when it does not. */
internal fun otpMatchOffset(code: String, key: ByteArray, counter: Long, algorithm: String, digits: Int): Int? =
    listOf(0, -1, 1).firstOrNull { counter + it >= 0 && hotp(key, counter + it, algorithm, digits) == code.filterNot { c -> c.isWhitespace() } }

internal fun randomBase32Secret(bytes: Int = 20): String = base32Encode(ByteArray(bytes).also { SecureRandom().nextBytes(it) })

internal data class OtpParams(
    val type: String = "totp",
    val secret: String = "",
    val issuer: String = "",
    val account: String = "",
    val algorithm: String = "SHA1",
    val digits: Int = 6,
    val period: Int = 30,
    val counter: Long = 0,
)

/** Parses an `otpauth://totp|hotp/Issuer:account?secret=...` URI (Key Uri Format). */
internal fun parseOtpAuthUri(text: String): OtpParams {
    val uri = try {
        URI(text.trim())
    } catch (e: URISyntaxException) {
        throw IllegalArgumentException("The otpauth URI is malformed. Write spaces in the label as %20.")
    }
    require(uri.scheme.equals("otpauth", ignoreCase = true)) { "The URI must start with otpauth://." }
    val type = uri.host?.lowercase()
    require(type == "totp" || type == "hotp") { "The URI type must be totp or hotp, as in otpauth://totp/..." }
    val query = uri.rawQuery.orEmpty().split('&').filter { '=' in it }.associate {
        val (k, v) = it.split('=', limit = 2)
        k.lowercase() to URLDecoder.decode(v, "UTF-8")
    }
    val label = uri.path.orEmpty().removePrefix("/")
    val labelIssuer = label.substringBefore(':', "").trim()
    val account = label.substringAfter(':').trim()
    val secret = query["secret"]?.filterNot { it.isWhitespace() }?.uppercase()?.trimEnd('=')
    require(!secret.isNullOrEmpty()) { "The URI has no secret parameter." }
    base32Decode(secret)
    val algorithm = query["algorithm"]?.uppercase()?.replace("-", "") ?: "SHA1"
    require(algorithm in OTP_ALGORITHMS) { "Unsupported algorithm $algorithm. Use SHA1, SHA256 or SHA512." }
    val digits = query["digits"]?.toIntOrNull() ?: 6
    require(digits in 6..8) { "Digits must be 6, 7 or 8." }
    val period = query["period"]?.toIntOrNull() ?: 30
    require(period > 0) { "The period must be a positive number of seconds." }
    val counter = query["counter"]?.toLongOrNull() ?: 0L
    return OtpParams(type, secret, query["issuer"] ?: labelIssuer, account, algorithm, digits, period, counter)
}

internal fun buildOtpAuthUri(p: OtpParams): String {
    fun enc(s: String) = URLEncoder.encode(s.trim(), "UTF-8").replace("+", "%20")
    val label = if (p.issuer.isBlank()) enc(p.account) else "${enc(p.issuer)}:${enc(p.account)}"
    val params = buildList {
        add("secret=${p.secret.filterNot { it.isWhitespace() || it == '-' }.uppercase().trimEnd('=')}")
        if (p.issuer.isNotBlank()) add("issuer=${enc(p.issuer)}")
        add("algorithm=${p.algorithm}")
        add("digits=${p.digits}")
        add(if (p.type == "hotp") "counter=${p.counter}" else "period=${p.period}")
    }
    return "otpauth://${p.type}/$label?${params.joinToString("&")}"
}

class OtpTool : Tool {
    override val descriptor = ToolDescriptor(
        id = "otp",
        title = "OTP Generator",
        description = "Generate and check TOTP and HOTP codes",
        icon = Icons.Default.Pin,
        category = ToolCategory.CRYPTOGRAPHY,
        keywords = listOf("otp", "totp", "hotp", "2fa", "mfa", "otpauth", "authenticator")
    )

    @Composable
    override fun Content(modifier: Modifier) {
        // Not saveable on purpose: the secret is not written to saved instance state.
        var secret by remember { mutableStateOf("") }
        var importError by remember { mutableStateOf<String?>(null) }
        var issuer by rememberSaveable { mutableStateOf("") }
        var account by rememberSaveable { mutableStateOf("") }
        var isHotp by rememberSaveable { mutableStateOf(false) }
        var algorithm by rememberSaveable { mutableStateOf("SHA1") }
        var digits by rememberSaveable { mutableStateOf(6) }
        var periodText by rememberSaveable { mutableStateOf("30") }
        var counterText by rememberSaveable { mutableStateOf("0") }
        var check by rememberSaveable { mutableStateOf("") }

        var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
        LaunchedEffect(Unit) {
            while (true) {
                now = System.currentTimeMillis()
                delay(1000 - now % 1000)
            }
        }

        val key = remember(secret) { runCatching { base32Decode(secret) } }
        val period = periodText.toIntOrNull()?.takeIf { it in 1..86_400 }
        val counter = counterText.toLongOrNull()?.takeIf { it >= 0 }
        val keyBytes = key.getOrNull()?.takeIf { it.isNotEmpty() }
        val step = if (isHotp) counter else period?.let { totpCounter(now / 1000, it) }

        ToolScaffold(descriptor.title, modifier = modifier) {
            SecretField(
                value = secret,
                onValueChange = { text ->
                    importError = null
                    if (text.trim().startsWith("otpauth://", ignoreCase = true)) {
                        runCatching { parseOtpAuthUri(text) }
                            .onSuccess { p ->
                                secret = p.secret; issuer = p.issuer; account = p.account
                                isHotp = p.type == "hotp"; algorithm = p.algorithm; digits = p.digits
                                periodText = p.period.toString(); counterText = p.counter.toString()
                            }
                            .onFailure { secret = text; importError = it.message }
                    } else secret = text
                },
                label = "Secret (Base32) or otpauth URI",
                isError = importError != null || key.isFailure,
                supportingText = importError ?: key.exceptionOrNull()?.message,
                extraAction = {
                    IconButton(onClick = { secret = randomBase32Secret(); importError = null }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Generate a secret")
                    }
                }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(issuer, { issuer = it }, Modifier.weight(1f), label = { Text("Issuer") }, singleLine = true, shape = MaterialTheme.shapes.medium)
                OutlinedTextField(account, { account = it }, Modifier.weight(1f), label = { Text("Account") }, singleLine = true, shape = MaterialTheme.shapes.medium)
            }
            Spacer(Modifier.height(16.dp))
            Segmented(listOf("TOTP (time)", "HOTP (counter)"), if (isHotp) 1 else 0, { isHotp = it == 1 })
            Spacer(Modifier.height(8.dp))
            Segmented(listOf("SHA-1", "SHA-256", "SHA-512"), OTP_ALGORITHMS.indexOf(algorithm), { algorithm = OTP_ALGORITHMS[it] })
            Spacer(Modifier.height(8.dp))
            Segmented(listOf("6 digits", "7 digits", "8 digits"), digits - 6, { digits = it + 6 })
            Spacer(Modifier.height(8.dp))
            if (isHotp) {
                OutlinedTextField(
                    counterText, { counterText = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(),
                    label = { Text("Counter") }, singleLine = true, shape = MaterialTheme.shapes.medium,
                    isError = counter == null,
                    supportingText = if (counter == null) { { Text("Enter a whole number, 0 or more.") } } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            } else {
                OutlinedTextField(
                    periodText, { periodText = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(),
                    label = { Text("Period (seconds)") }, singleLine = true, shape = MaterialTheme.shapes.medium,
                    isError = period == null,
                    supportingText = if (period == null) { { Text("Enter a period from 1 to 86400 seconds; 30 is standard.") } } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            Spacer(Modifier.height(16.dp))

            if (keyBytes == null || step == null) {
                if (secret.isBlank()) Text(
                    "Enter a Base32 secret, paste an otpauth URI, or generate a secret.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                fun code(c: Long) = if (c < 0) "" else hotp(keyBytes, c, algorithm, digits)
                if (!isHotp && period != null) {
                    val left = period - (now / 1000 % period).toInt()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(progress = { left / period.toFloat() }, modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                        Spacer(Modifier.size(12.dp))
                        Text("Next code in ${left}s", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                }
                val uri = buildOtpAuthUri(
                    OtpParams(if (isHotp) "hotp" else "totp", secret, issuer, account, algorithm, digits, period ?: 30, counter ?: 0L)
                )
                ResultSlab(
                    if (isHotp) listOf("Code (counter $step)" to code(step), "Next code (counter ${step + 1})" to code(step + 1), "otpauth URI" to uri)
                    else listOf("Current code" to code(step), "Previous code" to code(step - 1), "Next code" to code(step + 1), "otpauth URI" to uri)
                )
                Spacer(Modifier.height(16.dp))
                val offset = if (check.isBlank()) null else otpMatchOffset(check, keyBytes, step, algorithm, digits)
                OutlinedTextField(
                    check, { check = it.filter(Char::isDigit).take(8) }, Modifier.fillMaxWidth(),
                    label = { Text("Check a code") }, singleLine = true, textStyle = CodeTextStyle, shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = check.isNotBlank() && offset == null,
                    supportingText = if (check.isBlank()) null else {
                        {
                            when (offset) {
                                0 -> Text("Valid for the current step.", color = MaterialTheme.colorScheme.primary)
                                -1 -> Text("Valid for the previous step. The other clock or counter is one step behind.", color = MaterialTheme.colorScheme.primary)
                                1 -> Text("Valid for the next step. The other clock or counter is one step ahead.", color = MaterialTheme.colorScheme.primary)
                                else -> Text("Not valid within one step. Check the secret, algorithm, digits and the device clock.")
                            }
                        }
                    }
                )
            }
        }
    }
}
