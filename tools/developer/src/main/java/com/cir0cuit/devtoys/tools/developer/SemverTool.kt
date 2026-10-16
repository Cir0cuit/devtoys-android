package com.cir0cuit.devtoys.tools.developer

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*

/** ponytail: range checks run on the main thread; capped so a huge paste stays cheap. */
private const val MAX_VERSIONS = 500

class SemverTool : Tool {
    override val descriptor = ToolDescriptor(
        id = "semver",
        title = "Semver Compare",
        description = "Compare, sort and range-check versions",
        icon = Icons.Default.NewReleases,
        category = ToolCategory.TEXT,
        keywords = listOf("semver", "version", "compare", "sort", "range", "npm", "cargo", "caret", "tilde", "dependency")
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable override fun Content(modifier: Modifier) {
        var mode by rememberSaveable { mutableStateOf(0) }
        var a by rememberSaveable { mutableStateOf("1.2.3") }
        var b by rememberSaveable { mutableStateOf("1.10.0-rc.1") }
        var list by rememberSaveable { mutableStateOf("1.10.0\n1.2.3\n1.10.0-rc.1\n2.0.0\n1.2.3-beta") }
        var range by rememberSaveable { mutableStateOf("^1.2.3") }
        var cargo by rememberSaveable { mutableStateOf(false) }
        var descending by rememberSaveable { mutableStateOf(false) }

        ToolScaffold(descriptor.title, modifier = modifier) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf("Compare", "Sort", "Range").forEachIndexed { i, label ->
                    SegmentedButton(selected = mode == i, onClick = { mode = i }, shape = SegmentedButtonDefaults.itemShape(i, 3)) { Text(label) }
                }
            }
            Spacer(Modifier.height(16.dp))
            when (mode) {
                0 -> {
                    MonoTextField(a, { a = it }, "Version A", minLines = 1, maxLines = 1)
                    Spacer(Modifier.height(8.dp))
                    MonoTextField(b, { b = it }, "Version B", minLines = 1, maxLines = 1)
                    Spacer(Modifier.height(16.dp))
                    val va = parseSemver(a)
                    val vb = parseSemver(b)
                    when {
                        va == null -> SemverError("Version A is not SemVer. Use MAJOR.MINOR.PATCH, like 1.2.3 or 1.2.3-rc.1.")
                        vb == null -> SemverError("Version B is not SemVer. Use MAJOR.MINOR.PATCH, like 1.2.3 or 1.2.3-rc.1.")
                        else -> {
                            val (result, why) = explainCompare(va, vb)
                            LabeledValueSlab(listOf("Result" to result, "Decided by" to why))
                        }
                    }
                }
                1 -> {
                    MonoTextField(list, { list = it }, "Versions, one per line", minLines = 4)
                    Spacer(Modifier.height(8.dp))
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        listOf("Ascending", "Descending").forEachIndexed { i, label ->
                            SegmentedButton(selected = descending == (i == 1), onClick = { descending = i == 1 }, shape = SegmentedButtonDefaults.itemShape(i, 2)) { Text(label) }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    val (sorted, bad) = remember(list, descending) { sortVersions(list.lines().take(MAX_VERSIONS), descending) }
                    OutputPanel(sorted.joinToString("\n"), label = "Sorted")
                    if (bad.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        SemverError("Skipped ${bad.size} line(s) that are not SemVer: ${bad.take(3).joinToString(", ")}${if (bad.size > 3) ", ..." else ""}")
                    }
                }
                else -> {
                    MonoTextField(list, { list = it }, "Versions, one per line", minLines = 3)
                    Spacer(Modifier.height(8.dp))
                    MonoTextField(range, { range = it }, if (cargo) "Requirement, like >=1.2, <1.5" else "Range, like ^1.2.3 || >=2.0 <3", minLines = 1, maxLines = 2)
                    Spacer(Modifier.height(8.dp))
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        listOf("npm", "Cargo").forEachIndexed { i, label ->
                            SegmentedButton(selected = cargo == (i == 1), onClick = { cargo = i == 1 }, shape = SegmentedButtonDefaults.itemShape(i, 2)) { Text(label) }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    val parsedRange = remember(range, cargo) { runCatching { parseRange(range, cargo) } }
                    val sets = parsedRange.getOrNull()
                    if (sets == null) {
                        SemverError("${parsedRange.exceptionOrNull()?.message ?: "Can't read this range."} Use forms like ^1.2.3, ~1.2, 1.x or >=1.0 <2.0.")
                    } else {
                        val lines = list.lines().map { it.trim() }.filter { it.isNotEmpty() }.take(MAX_VERSIONS)
                        val rows = remember(lines, sets) {
                            val checked = lines.map { line -> line to parseSemver(line)?.let { rangeFailure(it, sets) to it } }
                            val best = checked.mapNotNull { (_, r) -> r?.takeIf { it.first == null }?.second }.maxOrNull()
                            listOf("Range means" to sets.joinToString(" || ") { it.joinToString(" ") }) +
                                ("Highest match" to (best?.toString() ?: "None")) +
                                checked.map { (line, r) ->
                                    line to when {
                                        r == null -> "Not SemVer. Use MAJOR.MINOR.PATCH."
                                        r.first == null -> "Satisfies"
                                        else -> r.first!!
                                    }
                                }
                        }
                        LabeledValueSlab(rows)
                    }
                }
            }
        }
    }
}

@Composable
private fun SemverError(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
}
