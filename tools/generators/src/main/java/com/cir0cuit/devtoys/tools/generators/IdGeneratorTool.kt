package com.cir0cuit.devtoys.tools.generators

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.OutputPanel
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolCategory
import com.cir0cuit.devtoys.tools.base.ToolDescriptor
import java.security.SecureRandom
import java.util.Random

internal enum class IdKind(val label: String) { ULID("ULID"), NANOID("NanoID"), SNOWFLAKE("Snowflake"), KSUID("KSUID"), TOKEN("Random token") }

internal enum class NanoAlphabet(val label: String, val chars: String) {
    URL("URL-safe", NANOID_URL),
    ALNUM("Alphanumeric", BASE62),
    HEX("Hex", "0123456789abcdef"),
    DIGITS("Numbers", "0123456789"),
    NO_LOOKALIKES("No look-alikes", NANOID_NO_LOOKALIKES),
    CUSTOM("Custom", ""),
}

private val ID_COUNTS = listOf(1, 5, 10, 25)
private val TOKEN_BYTES = listOf(16, 20, 32, 64)

internal data class IdOptions(
    val nanoSize: Int = 21,
    val alphabet: String = NANOID_URL,
    val snowflakeEpoch: Long = DISCORD_EPOCH,
    val machine: Int = 0,
    val tokenBytes: Int = 32,
    val encoding: TokenEncoding = TokenEncoding.BASE64URL,
    val prefix: String = "",
)

/** [count] IDs of [kind]; time-ordered kinds are sorted so a batch is monotonic. */
internal fun generateIds(kind: IdKind, count: Int, o: IdOptions, nowMs: Long, rng: Random): List<String> = when (kind) {
    // ponytail: sorting the batch gives in-batch monotonicity; no cross-call counter.
    IdKind.ULID -> List(count) { ulid(nowMs, rng) }.sorted()
    IdKind.NANOID -> List(count) { nanoId(o.nanoSize, o.alphabet, rng) }
    IdKind.SNOWFLAKE -> List(count) { snowflake(nowMs, o.snowflakeEpoch, o.machine, it).toString() }
    IdKind.KSUID -> List(count) { ksuid(nowMs / 1000, rng) }.sorted()
    IdKind.TOKEN -> List(count) { o.prefix + encodeToken(ByteArray(o.tokenBytes).also(rng::nextBytes), o.encoding) }
}

/** Human-readable count such as "2.3 billion" for the collision hint. */
internal fun roughCount(n: Double): String = when {
    n >= 1e18 -> "more than 10^18"
    n >= 1e12 -> "%.1f trillion".format(n / 1e12)
    n >= 1e9 -> "%.1f billion".format(n / 1e9)
    n >= 1e6 -> "%.1f million".format(n / 1e6)
    else -> "%,d".format(n.toLong())
}

class IdGeneratorTool : Tool {
    override val descriptor = ToolDescriptor(
        id = "id-gen",
        title = "ID & Token Generator",
        description = "ULID, NanoID, Snowflake, KSUID, tokens",
        icon = Icons.Default.Tag,
        category = ToolCategory.GENERATORS,
        keywords = listOf("ulid", "nanoid", "snowflake", "ksuid", "objectid", "token", "secret", "api key", "id", "inspect", "decode")
    )

    @Composable
    override fun Content(modifier: Modifier) {
        var tab by rememberSaveable { mutableStateOf(0) }
        ToolScaffold(title = descriptor.title, modifier = modifier) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Generate") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Inspect") })
            }
            Spacer(Modifier.height(16.dp))
            if (tab == 0) GeneratePane() else InspectPane("UUID, ULID, KSUID, ObjectId or Snowflake")
        }
    }

    @Composable
    private fun GeneratePane() {
        var kind by rememberSaveable { mutableStateOf(IdKind.ULID) }
        var count by rememberSaveable { mutableStateOf(1) }
        var nanoSize by rememberSaveable { mutableStateOf(21) }
        var alphabetChoice by rememberSaveable { mutableStateOf(NanoAlphabet.URL) }
        var customAlphabet by rememberSaveable { mutableStateOf("") }
        var discordEpoch by rememberSaveable { mutableStateOf(true) }
        var machineText by rememberSaveable { mutableStateOf("0") }
        var tokenBytes by rememberSaveable { mutableStateOf(32) }
        var encoding by rememberSaveable { mutableStateOf(TokenEncoding.BASE64URL) }
        var prefix by rememberSaveable { mutableStateOf("") }
        var nonce by rememberSaveable { mutableStateOf(0) }
        val context = LocalContext.current

        val alphabet = if (alphabetChoice == NanoAlphabet.CUSTOM) customAlphabet.toSet().joinToString("") else alphabetChoice.chars
        val machine = machineText.toIntOrNull()?.takeIf { it in 0..1023 }
        val error = when {
            kind == IdKind.NANOID && alphabet.length < 2 -> "The custom alphabet needs at least 2 different characters."
            kind == IdKind.SNOWFLAKE && machine == null -> "Machine ID must be a whole number from 0 to 1023."
            else -> null
        }
        val options = IdOptions(nanoSize, alphabet, if (discordEpoch) DISCORD_EPOCH else TWITTER_EPOCH, machine ?: 0, tokenBytes, encoding, prefix.take(32))
        // Not saveable on purpose: tokens are secrets and must not reach saved instance state.
        val output = remember(kind, count, options, nonce) {
            if (error != null) "" else generateIds(kind, count, options, System.currentTimeMillis(), SecureRandom()).joinToString("\n")
        }

        SelectField("Type", kind, IdKind.entries.toList(), { kind = it }, optionLabel = { it.label })
        when (kind) {
            IdKind.NANOID -> {
                Spacer(Modifier.height(8.dp))
                SelectField("Alphabet", alphabetChoice, NanoAlphabet.entries.toList(), { alphabetChoice = it }, optionLabel = { it.label })
                if (alphabetChoice == NanoAlphabet.CUSTOM) {
                    Spacer(Modifier.height(8.dp))
                    SingleLineField(customAlphabet, { customAlphabet = it.take(256) }, "Custom alphabet", isError = error != null)
                }
                Spacer(Modifier.height(8.dp))
                SliderSetting("Size", nanoSize, 4..64) { nanoSize = it }
            }
            IdKind.SNOWFLAKE -> {
                Spacer(Modifier.height(16.dp))
                Text("Epoch", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                ChoiceRow(listOf(true, false), discordEpoch, { discordEpoch = it }, { if (it) "Discord" else "Twitter" })
                Spacer(Modifier.height(8.dp))
                SingleLineField(machineText, { machineText = it.filter(Char::isDigit).take(4) }, "Machine ID (0-1023)", isError = machine == null)
            }
            IdKind.TOKEN -> {
                Spacer(Modifier.height(16.dp))
                Text("Bytes", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                ChoiceRow(TOKEN_BYTES, tokenBytes, { tokenBytes = it })
                Spacer(Modifier.height(8.dp))
                SelectField("Encoding", encoding, TokenEncoding.entries.toList(), { encoding = it }, optionLabel = { it.label })
                Spacer(Modifier.height(8.dp))
                SingleLineField(prefix, { prefix = it.take(32) }, "Prefix, e.g. sk_live_")
            }
            else -> Unit
        }
        Spacer(Modifier.height(16.dp))
        Text("How many", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        ChoiceRow(ID_COUNTS, count, { count = it })
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RegenerateButton("Generate", enabled = error == null) { nonce++ }
            if (kind == IdKind.TOKEN) SensitiveCopyButton(enabled = output.isNotEmpty()) { copySensitive(context, "token", output) }
        }
        Spacer(Modifier.height(16.dp))
        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error)
        } else {
            if (kind == IdKind.NANOID || kind == IdKind.TOKEN) {
                val bits = if (kind == IdKind.NANOID) idEntropyBits(nanoSize, alphabet.length) else tokenBytes * 8.0
                Text(
                    "%.0f bits of entropy. 1%% collision chance after about %s IDs.".format(bits, roughCount(idsForOnePercentCollision(bits))),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
            }
            OutputPanel(content = output, isCopyable = kind != IdKind.TOKEN, label = if (count == 1) kind.label else "$count × ${kind.label}")
        }
    }
}
