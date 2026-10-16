package com.cir0cuit.devtoys.tools.developer

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material3.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*

// Kotlin has no octal literals: 0x800 = 04000, 0x400 = 02000, 0x200 = 01000, 0x1B6 = 0666, 0x1FF = 0777.
private const val SETUID = 0x800
private const val SETGID = 0x400
private const val STICKY = 0x200
private const val FILE_BASE = 0x1B6
private const val DIR_BASE = 0x1FF

class ChmodCalculatorTool : Tool {
    override val descriptor = ToolDescriptor(
        id = "chmod",
        title = "Chmod Calculator",
        description = "Turn file permissions into chmod values",
        icon = Icons.Default.AdminPanelSettings,
        category = ToolCategory.NETWORK_SYSTEM,
        keywords = listOf("chmod", "permissions", "linux", "octal", "umask", "setuid", "sticky")
    )

    @Composable
    override fun Content(modifier: Modifier) {
        var tab by rememberSaveable { mutableIntStateOf(0) }
        // Twelve mode bits: special bits, then owner rwx in the high permission bits: 0b111_101_101 = 755.
        var mode by rememberSaveable { mutableIntStateOf(0b111_101_101) }
        // Mode that a chmod expression like u+x is applied to; updated by every non-expression edit.
        var base by rememberSaveable { mutableIntStateOf(0b111_101_101) }
        var fileType by rememberSaveable { mutableStateOf("") }
        var text by rememberSaveable { mutableStateOf("755") }
        var umaskText by rememberSaveable { mutableStateOf("022") }
        var desiredText by rememberSaveable { mutableStateOf("644") }

        fun setFromGrid(newMode: Int) {
            mode = newMode
            base = newMode
            text = octalMode(newMode)
        }

        ToolScaffold(descriptor.title, modifier = modifier) {
            NetChoiceRow(listOf("Permissions", "umask"), tab, { tab = it })
            Spacer(Modifier.height(16.dp))
            if (tab == 1) {
                UmaskMode(umaskText, { umaskText = it }, desiredText, { desiredText = it })
            } else {
            MonoTextField(
                value = text,
                onValueChange = { t ->
                    text = t
                    val literal = parseChmodLiteral(t)
                    if (literal != null) {
                        mode = literal.first
                        base = literal.first
                        fileType = literal.second?.toString().orEmpty()
                    } else {
                        applyChmodExpression(t.trim(), base, fileType == "d")?.let { mode = it }
                    }
                },
                label = "Octal, symbolic or expression",
                minLines = 1,
                maxLines = 1
            )
            if (text.isNotBlank() && parseChmodLiteral(text) == null && applyChmodExpression(text.trim(), base, fileType == "d") == null) {
                Text(
                    "Enter octal like 755 or 4755, symbolic like rwxr-xr-x or -rw-r--r--, or an expression like u+x,go-w.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(72.dp))
                listOf("Read", "Write", "Execute").forEach {
                    Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                }
            }
            listOf("Owner", "Group", "Public").forEachIndexed { row, who ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(who, style = MaterialTheme.typography.titleSmall, modifier = Modifier.width(72.dp))
                    for (col in 0..2) {
                        val bit = 1 shl (8 - row * 3 - col)
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Checkbox(checked = mode and bit != 0, onCheckedChange = { setFromGrid(mode xor bit) })
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("setuid" to SETUID, "setgid" to SETGID, "sticky" to STICKY).forEach { (label, bit) ->
                    FilterChip(selected = mode and bit != 0, onClick = { setFromGrid(mode xor bit) }, label = { Text(label) })
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            val octal = octalMode(mode)
            LabeledValueSlab(
                listOfNotNull(
                    "Octal" to octal,
                    "Symbolic" to fileType + chmodSymbolic(mode),
                    fileTypeName(fileType)?.let { "File type" to it },
                    "Command" to "chmod $octal"
                )
            )
            }
        }
    }
}

@Composable
private fun UmaskMode(umaskText: String, onUmask: (String) -> Unit, desiredText: String, onDesired: (String) -> Unit) {
    MonoTextField(umaskText, onUmask, "umask", minLines = 1, maxLines = 1)
    Spacer(Modifier.height(16.dp))
    val umask = umaskText.trim().takeIf { it.length in 1..4 && it.all { c -> c in '0'..'7' } }?.toInt(8)?.and(DIR_BASE)
    if (umask == null) {
        NetError("Enter the umask as octal digits, like 022 or 077.")
    } else {
        val file = FILE_BASE and umask.inv()
        val dir = DIR_BASE and umask.inv()
        LabeledValueSlab(
            listOf(
                "New files" to "${octalMode(file)}  ${chmodSymbolic(file)}",
                "New directories" to "${octalMode(dir)}  ${chmodSymbolic(dir)}"
            )
        )
    }
    Spacer(Modifier.height(16.dp))
    MonoTextField(desiredText, onDesired, "Desired mode, like 644 or rwxr-x---", minLines = 1, maxLines = 1)
    Spacer(Modifier.height(16.dp))
    val desired = parseChmodLiteral(desiredText)?.first?.and(DIR_BASE)
    if (desired == null) {
        NetError("Enter the desired mode as octal like 644 or symbolic like rw-r--r--.")
    } else {
        LabeledValueSlab(
            listOf(
                "umask for files" to if (desired and 0b001_001_001 != 0) "None: new files never get execute bits" else octalMode(FILE_BASE and desired.inv()),
                "umask for directories" to octalMode(DIR_BASE and desired.inv())
            )
        )
    }
}

/** Three octal digits, or four when a special bit is set. */
internal fun octalMode(mode: Int): String = if (mode > DIR_BASE) "%04o".format(mode) else "%03o".format(mode)

/** ls-style permission string: rwsr-xr-x, rwxrwxrwt, rw-r-Sr--. */
internal fun chmodSymbolic(mode: Int): String = buildString {
    for (who in 0..2) {
        val bits = mode shr (6 - who * 3) and 7
        append(if (bits and 4 != 0) 'r' else '-')
        append(if (bits and 2 != 0) 'w' else '-')
        val x = bits and 1 != 0
        val special = mode and (SETUID shr who) != 0
        append(
            when {
                !special -> if (x) 'x' else '-'
                who == 2 -> if (x) 't' else 'T'
                else -> if (x) 's' else 'S'
            }
        )
    }
}

/** Parses a nine-character permission string such as rwsr-xr-x. */
internal fun parseSymbolic(s: String): Int? {
    if (s.length != 9) return null
    var mode = 0
    for (who in 0..2) {
        val shift = 6 - who * 3
        val r = s[who * 3]
        val w = s[who * 3 + 1]
        val x = s[who * 3 + 2]
        mode = mode or when (r) { 'r' -> 4 shl shift; '-' -> 0; else -> return null }
        mode = mode or when (w) { 'w' -> 2 shl shift; '-' -> 0; else -> return null }
        val sp = if (who == 2) 't' else 's'
        mode = mode or when (x) {
            'x' -> 1 shl shift
            '-' -> 0
            sp -> (1 shl shift) or (SETUID shr who)
            sp.uppercaseChar() -> SETUID shr who
            else -> return null
        }
    }
    return mode
}

/**
 * Parses an absolute mode: 3 or 4 octal digits, a nine-character symbolic string, or an ls -l
 * column with its file type char and optional trailing @, + or . marker. Returns the mode and the
 * file type char, if any.
 */
internal fun parseChmodLiteral(text: String): Pair<Int, Char?>? {
    val t = text.trim()
    if (t.length in 3..4 && t.all { it in '0'..'7' }) return t.toInt(8) to null
    parseSymbolic(t)?.let { return it to null }
    val ls = t.trimEnd('@', '+', '.')
    if (ls.length == 10 && ls[0] in "-dlcbps") parseSymbolic(ls.substring(1))?.let { return it to ls[0] }
    return null
}

internal fun fileTypeName(c: String): String? = when (c) {
    "-" -> "Regular file"
    "d" -> "Directory"
    "l" -> "Symbolic link"
    "c" -> "Character device"
    "b" -> "Block device"
    "p" -> "Named pipe"
    "s" -> "Socket"
    else -> null
}

/**
 * Applies a chmod symbolic expression ("u+x,g-w,o=r", "a+rX", "go-rwx", "g=u") to [start].
 * A missing who means all (the umask is ignored). Returns null when the expression is invalid.
 */
internal fun applyChmodExpression(expr: String, start: Int, isDirectory: Boolean = false): Int? {
    if (expr.isEmpty()) return null
    val whoBits = listOf(4 to 6, 2 to 3, 1 to 0) // u, g, o -> shift
    var mode = start
    for (clause in expr.split(',')) {
        var i = 0
        var who = 0
        while (i < clause.length && clause[i] in "ugoa") {
            who = who or when (clause[i]) { 'u' -> 4; 'g' -> 2; 'o' -> 1; else -> 7 }
            i++
        }
        if (who == 0) who = 7
        if (i >= clause.length) return null
        while (i < clause.length) {
            val op = clause[i++]
            if (op !in "+-=") return null
            var perm = 0
            var special = 0
            if (i < clause.length && clause[i] in "ugo") {
                perm = mode shr when (clause[i]) { 'u' -> 6; 'g' -> 3; else -> 0 } and 7
                i++
            } else {
                while (i < clause.length && clause[i] in "rwxXst") {
                    when (clause[i]) {
                        'r' -> perm = perm or 4
                        'w' -> perm = perm or 2
                        'x' -> perm = perm or 1
                        'X' -> if (isDirectory || mode and 0b001_001_001 != 0) perm = perm or 1
                        's' -> special = special or (if (who and 4 != 0) SETUID else 0) or (if (who and 2 != 0) SETGID else 0)
                        't' -> if (who and 1 != 0) special = special or STICKY
                    }
                    i++
                }
            }
            var bits = special
            var clear = 0
            for ((w, shift) in whoBits) if (who and w != 0) {
                bits = bits or (perm shl shift)
                clear = clear or (7 shl shift) or (SETUID shr (2 - shift / 3))
            }
            mode = when (op) {
                '+' -> mode or bits
                '-' -> mode and bits.inv()
                else -> (mode and clear.inv()) or bits
            }
        }
    }
    return mode and 0xFFF
}
