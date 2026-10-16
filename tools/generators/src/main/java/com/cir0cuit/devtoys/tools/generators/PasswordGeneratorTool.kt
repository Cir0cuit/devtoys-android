package com.cir0cuit.devtoys.tools.generators
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.os.Build
import android.os.PersistableBundle
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
import java.security.SecureRandom

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
    for (i in len - 1 downTo 1) {
        val j = rng.nextInt(i + 1)
        val t = result[i]
        result[i] = result[j]
        result[j] = t
    }
    return String(result)
}

class PasswordGeneratorTool : Tool {
    override val descriptor = ToolDescriptor("password-gen", android.R.string.untitled, android.R.string.untitled, Icons.Default.Password, ToolCategory.GENERATORS, listOf("password", "generator"))
    @Composable override fun Content(modifier: Modifier) {
        var length by rememberSaveable { mutableStateOf(16f) }
        var includeUpper by rememberSaveable { mutableStateOf(true) }
        var includeLower by rememberSaveable { mutableStateOf(true) }
        var includeNumbers by rememberSaveable { mutableStateOf(true) }
        var includeSymbols by rememberSaveable { mutableStateOf(true) }
        var output by remember { mutableStateOf("") }
        var hasPassword by remember { mutableStateOf(false) }
        val context = LocalContext.current
        
        fun generate() {
            val sets = buildList {
                if (includeUpper) add(PASSWORD_UPPER)
                if (includeLower) add(PASSWORD_LOWER)
                if (includeNumbers) add(PASSWORD_DIGITS)
                if (includeSymbols) add(PASSWORD_SYMBOLS)
            }
            val result = generatePassword(length.toInt(), sets, SecureRandom())
            if (result == null) {
                output = "Select at least one character set."
                hasPassword = false
                return
            }
            output = result
            hasPassword = true
        }

        fun copySensitive() {
            val clip = ClipData.newPlainText("password", output)
            clip.description.extras = PersistableBundle().apply {
                putBoolean(
                    if (Build.VERSION.SDK_INT >= 33) ClipDescription.EXTRA_IS_SENSITIVE else "android.content.extra.IS_SENSITIVE",
                    true
                )
            }
            context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(clip)
        }

        ToolScaffold("Password Generator", modifier = modifier) {
            Text("Length: ${length.toInt()}")
            Slider(value = length, onValueChange = { length = it }, valueRange = 4f..64f)
            Row {
                Checkbox(checked = includeUpper, onCheckedChange = { includeUpper = it })
                Text("Uppercase", modifier = Modifier.padding(top=14.dp, end=8.dp))
                Checkbox(checked = includeLower, onCheckedChange = { includeLower = it })
                Text("Lowercase", modifier = Modifier.padding(top=14.dp))
            }
            Row {
                Checkbox(checked = includeNumbers, onCheckedChange = { includeNumbers = it })
                Text("Numbers", modifier = Modifier.padding(top=14.dp, end=8.dp))
                Checkbox(checked = includeSymbols, onCheckedChange = { includeSymbols = it })
                Text("Symbols", modifier = Modifier.padding(top=14.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { generate() }) { Text("Generate") }
                OutlinedButton(onClick = { copySensitive() }, enabled = hasPassword) { Text("Copy") }
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutputPanel(output, isCopyable = false)
        }
    }
}