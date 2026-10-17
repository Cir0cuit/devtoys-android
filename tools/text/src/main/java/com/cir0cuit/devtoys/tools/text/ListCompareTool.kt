package com.cir0cuit.devtoys.tools.text

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.JoinInner
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.NumberedTextField
import com.cir0cuit.devtoys.core.ui.SelectField
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolCategory
import com.cir0cuit.devtoys.tools.base.ToolDescriptor

internal data class ListComparison(val both: List<String>, val onlyA: List<String>, val onlyB: List<String>, val union: List<String>) {
    val eitherNotBoth get() = onlyA + onlyB
}

private enum class ListSet(val label: String, val pick: (ListComparison) -> List<String>) {
    BOTH("In both", { it.both }),
    ONLY_A("Only in A", { it.onlyA }),
    ONLY_B("Only in B", { it.onlyB }),
    UNION("In either (union)", { it.union }),
    EITHER_NOT_BOTH("In one, not both", { it.eitherNotBoth })
}

private val EMPTY_COMPARISON = ListComparison(emptyList(), emptyList(), emptyList(), emptyList())

class ListCompareTool : Tool {
    override val descriptor = ToolDescriptor(
        "list-compare", "List Comparer", "Find lines in both lists or only in one",
        Icons.Default.JoinInner, ToolCategory.TEXT,
        listOf("list", "compare", "set", "intersection", "union", "difference", "missing", "lines")
    )

    @OptIn(ExperimentalLayoutApi::class)
    @Composable override fun Content(modifier: Modifier) {
        var tab by rememberSaveable { mutableStateOf(0) }
        var listA by rememberSaveable(saver = boundedTextSaver) { mutableStateOf("") }
        var listB by rememberSaveable(saver = boundedTextSaver) { mutableStateOf("") }
        var ignoreCase by rememberSaveable { mutableStateOf(false) }
        var trim by rememberSaveable { mutableStateOf(true) }
        var skipEmpty by rememberSaveable { mutableStateOf(true) }
        var setName by rememberSaveable { mutableStateOf(ListSet.ONLY_A.name) }
        val set = ListSet.entries.firstOrNull { it.name == setName } ?: ListSet.ONLY_A

        val comparison by rememberDebounced(EMPTY_COMPARISON, listA, listB, ignoreCase, trim, skipEmpty) {
            compareLists(listA, listB, ignoreCase, trim, skipEmpty)
        }

        ToolScaffold(descriptor.title, modifier = modifier, scrollable = false) {
            TabRow(selectedTabIndex = tab) {
                listOf("List A", "List B", "Result").forEachIndexed { i, label ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(label, maxLines = 1) })
                }
            }
            Spacer(Modifier.height(8.dp))
            val paneModifier = Modifier.weight(1f).fillMaxWidth()
            when (tab) {
                0 -> {
                    PaneHeader("List A, one item per line", onPaste = { listA = it })
                    NumberedTextField(listA, { listA = it }, modifier = paneModifier)
                }
                1 -> {
                    PaneHeader("List B, one item per line", onPaste = { listB = it })
                    NumberedTextField(listB, { listB = it }, modifier = paneModifier)
                }
                else -> {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = ignoreCase, onClick = { ignoreCase = !ignoreCase }, label = { Text("Ignore case", maxLines = 1) })
                        FilterChip(selected = trim, onClick = { trim = !trim }, label = { Text("Trim spaces", maxLines = 1) })
                        FilterChip(selected = skipEmpty, onClick = { skipEmpty = !skipEmpty }, label = { Text("Skip empty", maxLines = 1) })
                    }
                    SelectField("Show", set, ListSet.entries, { setName = it.name }, Modifier.fillMaxWidth()) {
                        "${it.label} (${it.pick(comparison).size})"
                    }
                    val result = set.pick(comparison)
                    val text = result.joinToString("\n")
                    PaneHeader(if (result.size == 1) "1 item" else "${result.size} items", copyText = text)
                    NumberedTextField(text, {}, modifier = paneModifier, readOnly = true)
                }
            }
        }
    }
}

/**
 * Set operations on two line lists. Order follows first appearance (A before B); duplicates collapse.
 * With [ignoreCase], the first spelling seen is kept.
 */
internal fun compareLists(a: String, b: String, ignoreCase: Boolean, trim: Boolean, skipEmpty: Boolean): ListComparison {
    fun items(s: String): LinkedHashMap<String, String> {
        val map = LinkedHashMap<String, String>()
        if (s.isEmpty()) return map
        for (raw in s.lines()) {
            val line = if (trim) raw.trim() else raw
            if (skipEmpty && line.isEmpty()) continue
            map.putIfAbsent(if (ignoreCase) line.lowercase() else line, line)
        }
        return map
    }
    val mapA = items(a)
    val mapB = items(b)
    return ListComparison(
        both = mapA.filterKeys { it in mapB }.values.toList(),
        onlyA = mapA.filterKeys { it !in mapB }.values.toList(),
        onlyB = mapB.filterKeys { it !in mapA }.values.toList(),
        union = (mapA + mapB.filterKeys { it !in mapA }).values.toList()
    )
}
