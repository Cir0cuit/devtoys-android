package com.cir0cuit.devtoys.tools.encoders

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Abc
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.SelectField
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolCategory
import com.cir0cuit.devtoys.tools.base.ToolDescriptor
import java.math.BigInteger

private const val ALPHA_BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
private const val ALPHA_BASE32_HEX = "0123456789ABCDEFGHIJKLMNOPQRSTUV"
private const val ALPHA_BASE58 = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
private const val ALPHA_BASE36 = "0123456789abcdefghijklmnopqrstuvwxyz"
private const val ALPHA_Z85 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ.-:+=^!/*?&<>()[]{}@%$#"

internal enum class BaseN(val label: String) {
    BASE32("Base32"), BASE32_HEX("Base32hex"), BASE58("Base58 (Bitcoin)"), ASCII85("Ascii85"), Z85("Z85"), BASE36("Base36");

    fun encode(bytes: ByteArray): String = when (this) {
        BASE32 -> base32Encode(bytes, ALPHA_BASE32)
        BASE32_HEX -> base32Encode(bytes, ALPHA_BASE32_HEX)
        BASE58 -> bigBaseEncode(bytes, ALPHA_BASE58)
        ASCII85 -> base85Encode(bytes, null)
        Z85 -> base85Encode(bytes, ALPHA_Z85)
        BASE36 -> bigBaseEncode(bytes, ALPHA_BASE36)
    }

    /** Throws IllegalArgumentException with a user-facing message on bad input. */
    fun decode(text: String): ByteArray = when (this) {
        BASE32 -> base32Decode(text, ALPHA_BASE32)
        BASE32_HEX -> base32Decode(text, ALPHA_BASE32_HEX)
        BASE58 -> bigBaseDecode(text.filterNot { it.isWhitespace() }, ALPHA_BASE58)
        ASCII85 -> base85Decode(text, null)
        Z85 -> base85Decode(text, ALPHA_Z85)
        BASE36 -> bigBaseDecode(text.filterNot { it.isWhitespace() }.lowercase(), ALPHA_BASE36)
    }
}

/** RFC 4648 Base32 with `=` padding. */
internal fun base32Encode(bytes: ByteArray, alphabet: String): String = buildString {
    var buffer = 0
    var bits = 0
    for (b in bytes) {
        buffer = (buffer shl 8) or (b.toInt() and 0xFF)
        bits += 8
        while (bits >= 5) {
            append(alphabet[(buffer shr (bits - 5)) and 31])
            bits -= 5
        }
    }
    if (bits > 0) append(alphabet[(buffer shl (5 - bits)) and 31])
    while (length % 8 != 0) append('=')
}

/** Case-insensitive; padding and whitespace are optional. */
internal fun base32Decode(text: String, alphabet: String): ByteArray {
    val clean = text.filterNot { it.isWhitespace() }.trimEnd('=').uppercase()
    val out = java.io.ByteArrayOutputStream()
    var buffer = 0
    var bits = 0
    for (c in clean) {
        val v = alphabet.indexOf(c)
        require(v >= 0) { "'$c' is not a Base32 character. Use ${if (alphabet == ALPHA_BASE32) "A-Z and 2-7" else "0-9 and A-V"}." }
        buffer = (buffer shl 5) or v
        bits += 5
        if (bits >= 8) {
            out.write((buffer shr (bits - 8)) and 0xFF)
            bits -= 8
        }
    }
    require(clean.length % 8 !in setOf(1, 3, 6)) { "This Base32 has the wrong length. Check that it was copied completely." }
    return out.toByteArray()
}

/** Big-number encoding (Base58, Base36); each leading zero byte becomes one leading zero digit. */
internal fun bigBaseEncode(bytes: ByteArray, alphabet: String): String {
    val zeros = bytes.takeWhile { it == 0.toByte() }.size
    var n = BigInteger(1, bytes)
    val base = BigInteger.valueOf(alphabet.length.toLong())
    val sb = StringBuilder()
    while (n.signum() > 0) {
        val qr = n.divideAndRemainder(base)
        sb.append(alphabet[qr[1].toInt()])
        n = qr[0]
    }
    repeat(zeros) { sb.append(alphabet[0]) }
    return sb.reverse().toString()
}

internal fun bigBaseDecode(text: String, alphabet: String): ByteArray {
    val base = BigInteger.valueOf(alphabet.length.toLong())
    var n = BigInteger.ZERO
    for (c in text) {
        val v = alphabet.indexOf(c)
        require(v >= 0) { "'$c' is not in the Base${alphabet.length} alphabet." }
        n = n * base + BigInteger.valueOf(v.toLong())
    }
    val zeros = text.takeWhile { it == alphabet[0] }.length
    val body = n.toByteArray().let { if (it.size > 1 && it[0] == 0.toByte()) it.copyOfRange(1, it.size) else it }
    return ByteArray(zeros) + if (n.signum() == 0) ByteArray(0) else body
}

/**
 * Ascii85 (btoa style, `z` for four zero bytes, optional `<~ ~>`) when [alphabet] is null;
 * Z85 (ZeroMQ, input length a multiple of 4 bytes) otherwise.
 */
internal fun base85Encode(bytes: ByteArray, alphabet: String?): String {
    require(alphabet == null || bytes.size % 4 == 0) { "Z85 needs a multiple of 4 bytes. This input has ${bytes.size}; use Ascii85 for any length." }
    val sb = StringBuilder()
    for (i in bytes.indices step 4) {
        val n = minOf(4, bytes.size - i)
        var word = 0L
        for (j in 0 until 4) word = (word shl 8) or (if (j < n) bytes[i + j].toLong() and 0xFF else 0L)
        if (alphabet == null && n == 4 && word == 0L) {
            sb.append('z')
            continue
        }
        val chars = CharArray(5)
        for (j in 4 downTo 0) {
            val d = (word % 85).toInt()
            chars[j] = alphabet?.get(d) ?: ('!' + d)
            word /= 85
        }
        sb.append(chars, 0, n + 1)
    }
    return sb.toString()
}

internal fun base85Decode(text: String, alphabet: String?): ByteArray {
    var clean = text.filterNot { it.isWhitespace() }
    if (alphabet == null) clean = clean.removePrefix("<~").removeSuffix("~>").replace("z", "!!!!!")
    require(alphabet == null || clean.length % 5 == 0) { "Z85 text must be a multiple of 5 characters. Check that it was copied completely." }
    require(clean.length % 5 != 1) { "This Ascii85 has one character too many or too few. Check that it was copied completely." }
    val out = java.io.ByteArrayOutputStream()
    for (i in clean.indices step 5) {
        val n = minOf(5, clean.length - i)
        var word = 0L
        for (j in 0 until 5) {
            val d = if (j < n) {
                val c = clean[i + j]
                (alphabet?.indexOf(c) ?: (c - '!').takeIf { c in '!'..'u' } ?: -1)
                    .also { require(it >= 0) { "'$c' is not a valid ${if (alphabet == null) "Ascii85" else "Z85"} character." } }
            } else 84 // pad with 'u'
            word = word * 85 + d
        }
        require(word <= 0xFFFFFFFFL) { "This ${if (alphabet == null) "Ascii85" else "Z85"} group is out of range. Check the input." }
        for (j in 0 until n - 1) out.write(((word shr (24 - 8 * j)) and 0xFF).toInt())
    }
    return out.toByteArray()
}

class BaseNTool : Tool {
    override val descriptor = ToolDescriptor(
        id = "base-n",
        title = "Base32 / Base58 / Base85",
        description = "Encode text as Base32, Base58, Base85 or Base36",
        icon = Icons.Default.Abc,
        category = ToolCategory.ENCODERS_DECODERS,
        keywords = listOf("base32", "base58", "base85", "ascii85", "z85", "base36", "totp", "bitcoin", "encode", "decode")
    )

    @Composable
    override fun Content(modifier: Modifier) {
        var codec by rememberSaveable { mutableStateOf(BaseN.BASE32) }
        ToolScaffold(descriptor.title, modifier = modifier) {
            EncodeDecodePane(
                encode = { s -> runCatching { codec.encode(s.toByteArray()) }.getOrElse { "Error: ${it.message}" } },
                decodeBytes = { codec.decode(it) },
                optionsKey = codec,
                options = {
                    SelectField("Encoding", codec, BaseN.entries, { codec = it }, optionLabel = { it.label })
                    Spacer(Modifier.height(16.dp))
                }
            )
        }
    }
}
