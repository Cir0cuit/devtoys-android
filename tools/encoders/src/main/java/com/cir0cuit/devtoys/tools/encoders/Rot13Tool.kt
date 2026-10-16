package com.cir0cuit.devtoys.tools.encoders

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.designsystem.CodeTextStyle
import com.cir0cuit.devtoys.core.ui.MonoTextField
import com.cir0cuit.devtoys.core.ui.OutputPanel
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolCategory
import com.cir0cuit.devtoys.tools.base.ToolDescriptor
import kotlin.experimental.xor

internal fun rotN(input: String, s: Int): String =
    input.map { c ->
        when (c) {
            in 'a'..'z' -> 'a' + (c - 'a' + s) % 26
            in 'A'..'Z' -> 'A' + (c - 'A' + s) % 26
            else -> c
        }
    }.joinToString("")

/** ROT47: rotates the 94 printable ASCII characters `!`..`~`. */
internal fun rot47(input: String): String =
    input.map { c -> if (c in '!'..'~') '!' + (c - '!' + 47) % 94 else c }.joinToString("")

/** ROT18: ROT13 on letters plus ROT5 on digits. */
internal fun rot18(input: String): String =
    rotN(input, 13).map { c -> if (c in '0'..'9') '0' + (c - '0' + 5) % 10 else c }.joinToString("")

internal fun atbash(input: String): String =
    input.map { c ->
        when (c) {
            in 'a'..'z' -> 'z' - (c - 'a')
            in 'A'..'Z' -> 'Z' - (c - 'A')
            else -> c
        }
    }.joinToString("")

/** Vigenère over ASCII letters; the key position advances only on letters. Null when [key] has no letters. */
internal fun vigenere(input: String, key: String, decrypt: Boolean): String? {
    val shifts = key.filter { it in 'a'..'z' || it in 'A'..'Z' }.map { it.lowercaseChar() - 'a' }
    if (shifts.isEmpty()) return null
    var i = 0
    return input.map { c ->
        if (c !in 'a'..'z' && c !in 'A'..'Z') return@map c
        val s = shifts[i++ % shifts.size].let { if (decrypt) 26 - it else it }
        rotN(c.toString(), s)[0]
    }.joinToString("")
}

/** Repeating-key XOR. */
internal fun xorBytes(data: ByteArray, key: ByteArray): ByteArray =
    ByteArray(data.size) { data[it] xor key[it % key.size] }

private val ENGLISH_FREQ = doubleArrayOf(
    8.2, 1.5, 2.8, 4.3, 12.7, 2.2, 2.0, 6.1, 7.0, 0.15, 0.77, 4.0, 2.4,
    6.7, 7.5, 1.9, 0.095, 6.0, 6.3, 9.1, 2.8, 0.98, 2.4, 0.15, 2.0, 0.074
)

/** Chi-squared distance of the text's letter counts from English; lower is more English-like. */
internal fun englishScore(text: String): Double {
    val counts = IntArray(26)
    text.forEach { c -> if (c in 'a'..'z') counts[c - 'a']++ else if (c in 'A'..'Z') counts[c - 'A']++ }
    val total = counts.sum().takeIf { it > 0 } ?: return Double.MAX_VALUE
    return counts.indices.sumOf { i -> val expected = total * ENGLISH_FREQ[i] / 100; (counts[i] - expected).let { it * it } / expected }
}

/** All 25 Caesar shifts as (shift, text), most English-like first. */
internal fun allShifts(input: String): List<Pair<Int, String>> =
    (1..25).map { it to rotN(input, it) }.sortedBy { englishScore(it.second) }

internal enum class Cipher(val label: String) {
    CAESAR("Caesar / ROT-N"), ROT47("ROT47"), ROT18("ROT18 (letters + digits)"),
    ATBASH("Atbash"), VIGENERE("Vigenère"), XOR("XOR (repeating key)")
}

private const val SHIFTS_PREVIEW_CHARS = 2_000

class Rot13Tool : Tool {
    override val descriptor = ToolDescriptor(
        id = "rot13",
        title = "ROT13 / Classic Ciphers",
        description = "ROT13, Caesar, Vigenère, XOR and more",
        icon = Icons.AutoMirrored.Filled.RotateRight,
        category = ToolCategory.ENCODERS_DECODERS,
        keywords = listOf("rot13", "cipher", "caesar", "rot47", "atbash", "vigenere", "xor", "ctf")
    )

    @OptIn(ExperimentalMaterial3Api::class) // FilterChip
    @Composable
    override fun Content(modifier: Modifier) {
        var input by rememberSaveableText()
        var cipher by rememberSaveable { mutableStateOf(Cipher.CAESAR) }
        var shift by rememberSaveable { mutableStateOf(13f) }
        var showAll by rememberSaveable { mutableStateOf(false) }
        var key by rememberSaveable { mutableStateOf("") }
        var decrypt by rememberSaveable { mutableStateOf(false) }

        ToolScaffold(descriptor.title, modifier = modifier) {
            MonoTextField(
                input, { input = it },
                if (cipher == Cipher.XOR && decrypt) "Hex to decrypt" else "Text",
                minLines = 4
            )
            Spacer(Modifier.height(16.dp))
            SelectField("Cipher", cipher, Cipher.entries, { cipher = it }, optionLabel = { it.label })
            Spacer(Modifier.height(16.dp))
            when (cipher) {
                Cipher.CAESAR -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Shift", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        Text(shift.toInt().toString(), style = CodeTextStyle)
                        // ROT13 is the common case; offer a way back after sliding away from it.
                        TextButton(onClick = { shift = 13f }, enabled = shift.toInt() != 13) { Text("ROT13") }
                    }
                    Slider(value = shift, onValueChange = { shift = it }, valueRange = 1f..25f, steps = 23)
                    FilterChip(selected = showAll, onClick = { showAll = !showAll }, label = { Text("Show all shifts") })
                    Spacer(Modifier.height(8.dp))
                }
                Cipher.VIGENERE, Cipher.XOR -> {
                    MonoTextField(key, { key = it }, "Key", minLines = 1, maxLines = 1)
                    Spacer(Modifier.height(8.dp))
                    ModeSelector(listOf("Encrypt", "Decrypt"), if (decrypt) 1 else 0, { decrypt = it == 1 })
                    Spacer(Modifier.height(16.dp))
                }
                else -> Unit
            }

            if (cipher == Cipher.XOR) {
                val result = remember(input, key, decrypt) {
                    runCatching {
                        when {
                            input.isEmpty() -> ByteArray(0)
                            key.isEmpty() -> throw IllegalArgumentException("Enter a key to XOR with.")
                            decrypt -> xorBytes(hexDecodeBytes(input), key.toByteArray())
                            else -> xorBytes(input.toByteArray(), key.toByteArray())
                        }
                    }
                }
                result.fold(
                    onSuccess = { bytes ->
                        if (decrypt) BytesOutput(bytes, "Decrypted")
                        else OutputPanel(bytes.joinToString("") { "%02x".format(it) }, label = "Encrypted (hex)")
                    },
                    onFailure = { ErrorText(it.message ?: "Could not XOR this input.") }
                )
            } else {
                val output = remember(input, cipher, shift, key, decrypt) {
                    when (cipher) {
                        Cipher.CAESAR -> rotN(input, shift.toInt())
                        Cipher.ROT47 -> rot47(input)
                        Cipher.ROT18 -> rot18(input)
                        Cipher.ATBASH -> atbash(input)
                        Cipher.VIGENERE -> vigenere(input, key, decrypt)
                        Cipher.XOR -> ""
                    }
                }
                if (output == null) ErrorText("Enter a key that contains letters A-Z.")
                else OutputPanel(output, label = if (cipher == Cipher.VIGENERE && decrypt) "Decrypted" else "Result")

                if (cipher == Cipher.CAESAR && showAll && input.isNotEmpty()) {
                    val shifts = remember(input) { allShifts(input.take(SHIFTS_PREVIEW_CHARS)) }
                    Spacer(Modifier.height(16.dp))
                    Text("All shifts, most English-like first", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    ValueSlab(shifts.map { (s, text) -> "ROT$s" to text })
                }
            }
        }
    }
}
