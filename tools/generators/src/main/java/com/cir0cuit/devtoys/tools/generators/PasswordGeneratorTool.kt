package com.cir0cuit.devtoys.tools.generators
import android.content.ClipData
import android.content.ClipboardManager
import android.os.PersistableBundle
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Password
import androidx.compose.material3.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*
import java.security.SecureRandom
import kotlin.random.asKotlinRandom

internal const val PASSWORD_UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
internal const val PASSWORD_LOWER = "abcdefghijklmnopqrstuvwxyz"
internal const val PASSWORD_DIGITS = "0123456789"
internal const val PASSWORD_SYMBOLS = "!@#$%^&*()_+-=[]{}|;:,.<>?"

/**
 * Returns a password of max(length, sets.size) characters containing at least one character
 * from every set in [sets], or null when no character set is selected.
 */
internal fun generatePassword(length: Int, sets: List<String>, rng: java.util.Random): String? {
    val chars = sets.joinToString("")
    if (chars.isEmpty()) return null
    val len = length.coerceAtLeast(sets.size)
    val result = CharArray(len)
    sets.forEachIndexed { i, s -> result[i] = s[rng.nextInt(s.length)] }
    for (i in sets.size until len) result[i] = chars[rng.nextInt(chars.length)]
    result.shuffle(rng.asKotlinRandom())
    return String(result)
}

class PasswordGeneratorTool : Tool {
    override val descriptor = ToolDescriptor("password-gen", "Password Generator", "Generate strong random passwords", Icons.Default.Password, ToolCategory.GENERATORS, listOf("password", "generator"))
    @OptIn(ExperimentalLayoutApi::class)
    @Composable override fun Content(modifier: Modifier) {
        var length by rememberSaveable { mutableStateOf(16) }
        var includeUpper by rememberSaveable { mutableStateOf(true) }
        var includeLower by rememberSaveable { mutableStateOf(true) }
        var includeNumbers by rememberSaveable { mutableStateOf(true) }
        var includeSymbols by rememberSaveable { mutableStateOf(true) }
        // Not saveable on purpose: the password is not written to saved instance state.
        var output by remember { mutableStateOf<String?>(null) }
        val context = LocalContext.current

        fun generate() {
            val sets = buildList {
                if (includeUpper) add(PASSWORD_UPPER)
                if (includeLower) add(PASSWORD_LOWER)
                if (includeNumbers) add(PASSWORD_DIGITS)
                if (includeSymbols) add(PASSWORD_SYMBOLS)
            }
            output = generatePassword(length, sets, SecureRandom())
        }
        LaunchedEffect(length, includeUpper, includeLower, includeNumbers, includeSymbols) { generate() }

        fun copySensitive() {
            val clip = ClipData.newPlainText("password", output)
            // Value of ClipDescription.EXTRA_IS_SENSITIVE (API 33); older versions ignore it.
            clip.description.extras = PersistableBundle().apply { putBoolean("android.content.extra.IS_SENSITIVE", true) }
            context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(clip)
        }

        ToolScaffold(descriptor.title, modifier = modifier) {
            SliderSetting("Length", length, 4..64) { length = it }
            Spacer(Modifier.height(8.dp))
            Text("Characters", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(includeUpper, { includeUpper = !includeUpper }, { Text("A–Z") })
                FilterChip(includeLower, { includeLower = !includeLower }, { Text("a–z") })
                FilterChip(includeNumbers, { includeNumbers = !includeNumbers }, { Text("0–9") })
                FilterChip(includeSymbols, { includeSymbols = !includeSymbols }, { Text("Symbols") })
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RegenerateButton("Generate", enabled = output != null) { generate() }
                OutlinedButton(onClick = { copySensitive() }, enabled = output != null, contentPadding = ButtonDefaults.ButtonWithIconContentPadding) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text("Copy")
                }
            }
            Spacer(Modifier.height(16.dp))
            val password = output
            if (password == null) {
                Text("No character set is selected. Turn on at least one to generate a password.", color = MaterialTheme.colorScheme.error)
            } else {
                OutputPanel(password, isCopyable = false, label = "Password")
            }
        }
    }
}
