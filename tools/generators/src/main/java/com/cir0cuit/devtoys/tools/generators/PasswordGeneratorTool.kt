package com.cir0cuit.devtoys.tools.generators
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Password
import androidx.compose.material3.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import kotlin.math.log2
import kotlin.math.pow
import kotlin.random.asKotlinRandom

internal const val PASSWORD_UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
internal const val PASSWORD_LOWER = "abcdefghijklmnopqrstuvwxyz"
internal const val PASSWORD_DIGITS = "0123456789"
internal const val PASSWORD_SYMBOLS = "!@#$%^&*()_+-=[]{}|;:,.<>?"
internal const val PASSWORD_LOOKALIKES = "0O1lI|"
internal const val EFF_WORDLIST_ASSET = "eff_large_wordlist.txt"

/**
 * Returns a password of max(length, sets.size * minEach) characters containing at least [minEach]
 * characters from every set in [sets]. With [noRepeat] no character occurs twice.
 * Returns null when no set is selected or the constraints cannot be met.
 */
internal fun generatePassword(length: Int, sets: List<String>, rng: java.util.Random, minEach: Int = 1, noRepeat: Boolean = false): String? {
    val sets = sets.filter { it.isNotEmpty() }
    if (sets.isEmpty()) return null
    val pool = sets.joinToString("").toMutableList()
    val len = length.coerceAtLeast(sets.size * minEach)
    if (noRepeat && len > pool.toSet().size) return null
    val result = ArrayList<Char>(len)
    fun take(from: List<Char>): Char? {
        if (from.isEmpty()) return null
        val c = from[rng.nextInt(from.size)]
        if (noRepeat) pool.removeAll { it == c }
        return c
    }
    for (s in sets) repeat(minEach) { result += take(if (noRepeat) s.filter { it in pool }.toList() else s.toList()) ?: return null }
    while (result.size < len) result += take(pool) ?: return null
    result.shuffle(rng.asKotlinRandom())
    return result.joinToString("")
}

internal fun entropyBits(symbols: Int, poolSize: Int): Double = if (poolSize < 2) 0.0 else symbols * log2(poolSize.toDouble())

internal fun strengthLabel(bits: Double): String = when {
    bits < 40 -> "Weak"
    bits < 60 -> "Fair"
    bits < 80 -> "Strong"
    else -> "Very strong"
}

/** Average time to guess a secret of [bits] at 10^10 guesses per second. */
internal fun crackTime(bits: Double): String {
    val seconds = 2.0.pow(bits - 1) / 1e10
    val units = listOf("seconds" to 1.0, "minutes" to 60.0, "hours" to 3600.0, "days" to 86400.0, "years" to 31_557_600.0)
    return when {
        seconds < 1 -> "less than a second"
        seconds >= 31_557_600.0 * 1e6 -> "more than a million years"
        else -> units.last { seconds >= it.second }.let { (name, size) -> "%.0f %s".format(seconds / size, name) }
    }
}

internal fun parseWordlist(lines: Sequence<String>): List<String> =
    lines.map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.toList()

internal fun generatePassphrase(words: List<String>, count: Int, separator: String, capitalize: Boolean, addDigit: Boolean, rng: java.util.Random): String {
    val picked = MutableList(count) { words[rng.nextInt(words.size)].let { w -> if (capitalize) w.replaceFirstChar(Char::uppercase) else w } }
    if (addDigit) rng.nextInt(count).let { i -> picked[i] = picked[i] + rng.nextInt(10) }
    return picked.joinToString(separator)
}

internal fun passphraseBits(listSize: Int, count: Int, addDigit: Boolean): Double =
    entropyBits(count, listSize) + if (addDigit) log2(10.0 * count) else 0.0

/** All one digit, or a run that steps by +1 or -1 (mod 10), such as 0000, 1234, 9876, 7890. */
internal fun isTrivialPin(pin: String): Boolean {
    if (pin.length < 2) return false
    val step = (pin[1] - pin[0] + 10) % 10
    return step in setOf(0, 1, 9) && pin.zipWithNext().all { (a, b) -> (b - a + 10) % 10 == step }
}

internal fun generatePin(length: Int, rejectTrivial: Boolean, rng: java.util.Random): String {
    while (true) {
        val pin = String(CharArray(length) { '0' + rng.nextInt(10) })
        if (!rejectTrivial || !isTrivialPin(pin)) return pin
    }
}

private enum class PasswordMode(val label: String, val output: String) { CHARACTERS("Characters", "Password"), PASSPHRASE("Passphrase", "Passphrase"), PIN("PIN", "PIN") }
private val SEPARATORS = listOf("-", ".", "_", " ")
private val BATCH_SIZES = listOf(1, 5, 10)

class PasswordGeneratorTool : Tool {
    override val descriptor = ToolDescriptor("password-gen", "Password Generator", "Passwords, passphrases and PINs", Icons.Default.Password, ToolCategory.GENERATORS, listOf("password", "generator", "passphrase", "diceware", "pin", "entropy"))
    @OptIn(ExperimentalLayoutApi::class)
    @Composable override fun Content(modifier: Modifier) {
        var mode by rememberSaveable { mutableStateOf(PasswordMode.CHARACTERS) }
        var length by rememberSaveable { mutableStateOf(16) }
        var includeUpper by rememberSaveable { mutableStateOf(true) }
        var includeLower by rememberSaveable { mutableStateOf(true) }
        var includeNumbers by rememberSaveable { mutableStateOf(true) }
        var includeSymbols by rememberSaveable { mutableStateOf(true) }
        var symbols by rememberSaveable { mutableStateOf(PASSWORD_SYMBOLS) }
        var noLookalikes by rememberSaveable { mutableStateOf(false) }
        var noRepeat by rememberSaveable { mutableStateOf(false) }
        var minEach by rememberSaveable { mutableStateOf(1) }
        var wordCount by rememberSaveable { mutableStateOf(6) }
        var separator by rememberSaveable { mutableStateOf("-") }
        var capitalize by rememberSaveable { mutableStateOf(false) }
        var addDigit by rememberSaveable { mutableStateOf(false) }
        var pinLength by rememberSaveable { mutableStateOf(6) }
        var rejectTrivial by rememberSaveable { mutableStateOf(true) }
        var batch by rememberSaveable { mutableStateOf(1) }
        var nonce by rememberSaveable { mutableStateOf(0) }
        val context = LocalContext.current

        val words by produceState<List<String>>(emptyList()) {
            value = withContext(Dispatchers.IO) {
                context.assets.open(EFF_WORDLIST_ASSET).bufferedReader().useLines(::parseWordlist)
            }
        }
        val sets = buildList {
            if (includeUpper) add(PASSWORD_UPPER)
            if (includeLower) add(PASSWORD_LOWER)
            if (includeNumbers) add(PASSWORD_DIGITS)
            if (includeSymbols) add(symbols.toSet().filterNot { it.isWhitespace() }.joinToString(""))
        }.map { s -> if (noLookalikes) s.filterNot { it in PASSWORD_LOOKALIKES } else s }.filter { it.isNotEmpty() }
        val poolSize = sets.joinToString("").toSet().size

        // Not saveable on purpose: the password is not written to saved instance state.
        val output: String? = remember(mode, length, sets, noRepeat, minEach, wordCount, separator, capitalize, addDigit, pinLength, rejectTrivial, batch, words, nonce) {
            val rng = SecureRandom()
            when (mode) {
                PasswordMode.CHARACTERS -> List(batch) { generatePassword(length, sets, rng, minEach, noRepeat) ?: return@remember null }
                PasswordMode.PASSPHRASE -> if (words.isEmpty()) return@remember null else List(batch) { generatePassphrase(words, wordCount, separator, capitalize, addDigit, rng) }
                PasswordMode.PIN -> List(batch) { generatePin(pinLength, rejectTrivial, rng) }
            }.joinToString("\n")
        }
        val bits = when (mode) {
            PasswordMode.CHARACTERS -> entropyBits(length.coerceAtLeast(sets.size * minEach), poolSize)
            PasswordMode.PASSPHRASE -> passphraseBits(words.size, wordCount, addDigit)
            PasswordMode.PIN -> entropyBits(pinLength, 10)
        }

        ToolScaffold(descriptor.title, modifier = modifier) {
            ChoiceRow(PasswordMode.entries.toList(), mode, { mode = it }, { it.label })
            Spacer(Modifier.height(16.dp))
            when (mode) {
                PasswordMode.CHARACTERS -> {
                    SliderSetting("Length", length, 4..128) { length = it }
                    Spacer(Modifier.height(8.dp))
                    Text("Characters", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(includeUpper, { includeUpper = !includeUpper }, { Text("A–Z") })
                        FilterChip(includeLower, { includeLower = !includeLower }, { Text("a–z") })
                        FilterChip(includeNumbers, { includeNumbers = !includeNumbers }, { Text("0–9") })
                        FilterChip(includeSymbols, { includeSymbols = !includeSymbols }, { Text("Symbols") })
                        FilterChip(noLookalikes, { noLookalikes = !noLookalikes }, { Text("No look-alikes") })
                        FilterChip(noRepeat, { noRepeat = !noRepeat }, { Text("No repeats") })
                    }
                    if (includeSymbols) {
                        Spacer(Modifier.height(8.dp))
                        SingleLineField(symbols, { symbols = it.take(64) }, "Allowed symbols")
                    }
                    Spacer(Modifier.height(8.dp))
                    SliderSetting("Minimum from each set", minEach, 1..5) { minEach = it }
                }
                PasswordMode.PASSPHRASE -> {
                    SliderSetting("Words", wordCount, 4..10) { wordCount = it }
                    Spacer(Modifier.height(8.dp))
                    Text("Separator", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    ChoiceRow(SEPARATORS, separator, { separator = it }, { if (it == " ") "Space" else it })
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(capitalize, { capitalize = !capitalize }, { Text("Capitalize") })
                        FilterChip(addDigit, { addDigit = !addDigit }, { Text("Add a digit") })
                    }
                }
                PasswordMode.PIN -> {
                    SliderSetting("Digits", pinLength, 4..12) { pinLength = it }
                    Spacer(Modifier.height(8.dp))
                    FilterChip(rejectTrivial, { rejectTrivial = !rejectTrivial }, { Text("Reject 1234, 0000 and similar") })
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("How many", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            ChoiceRow(BATCH_SIZES, batch, { batch = it })
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RegenerateButton("Generate", enabled = output != null) { nonce++ }
                SensitiveCopyButton(enabled = output != null) { output?.let { copySensitive(context, "password", it) } }
            }
            Spacer(Modifier.height(16.dp))
            when {
                output != null -> {
                    OutputPanel(output, isCopyable = false, label = if (batch == 1) mode.output else "$batch × ${mode.output}")
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("${strengthLabel(bits)} · %.0f bits".format(bits), style = MaterialTheme.typography.titleSmall, maxLines = 1, modifier = Modifier.padding(end = 12.dp))
                        LinearProgressIndicator(progress = { (bits / 128).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "About ${crackTime(bits)} to guess at 10 billion guesses per second.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                mode == PasswordMode.PASSPHRASE -> Unit // word list still loading
                sets.isEmpty() -> Text("No character set is selected. Turn on at least one to generate a password.", color = MaterialTheme.colorScheme.error)
                else -> Text(
                    "Not enough different characters for these rules. Turn off No repeats, lower the minimum per set or shorten the password.",
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (mode == PasswordMode.PASSPHRASE) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Words from the EFF Large Wordlist (CC BY 3.0 US, Electronic Frontier Foundation).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
