package com.cir0cuit.devtoys.tools.developer

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material3.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_MATCHES = 1000

class RegexTesterTool : Tool {
    override val descriptor = ToolDescriptor(
        id = "regex",
        title = "Regex Tester",
        description = "Check a regex against sample text",
        icon = Icons.Default.FindInPage,
        category = ToolCategory.TEXT,
        keywords = listOf("regex", "tester")
    )

    @OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
    @Composable override fun Content(modifier: Modifier) {
        var regex by rememberSaveable { mutableStateOf("""(\w+)@(\w+\.\w+)""") }
        var input by rememberSaveable { mutableStateOf("Write to ada@example.com or grace@example.org.") }
        var ignoreCase by rememberSaveable { mutableStateOf(false) }
        var multiline by rememberSaveable { mutableStateOf(true) }
        var dotAll by rememberSaveable { mutableStateOf(false) }
        var output by remember { mutableStateOf("") }
        LaunchedEffect(regex, input, ignoreCase, multiline, dotAll) {
            if (regex.isEmpty()) {
                output = ""
                return@LaunchedEffect
            }
            delay(250)
            val options = buildSet {
                if (ignoreCase) add(RegexOption.IGNORE_CASE)
                if (multiline) add(RegexOption.MULTILINE)
                if (dotAll) add(RegexOption.DOT_MATCHES_ALL)
            }
            val slowNotice = launch {
                delay(2000)
                output = "Still matching. The pattern may backtrack heavily; simplify nested repeats like (a+)+."
            }
            output = withContext(Dispatchers.IO) { evaluate(regex, input, options) }
            slowNotice.cancel()
        }
        ToolScaffold(descriptor.title, modifier = modifier) {
            MonoTextField(regex, { regex = it }, "Pattern", minLines = 1)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(ignoreCase, { ignoreCase = !ignoreCase }, label = { Text("Ignore case") })
                FilterChip(multiline, { multiline = !multiline }, label = { Text("Multiline") })
                FilterChip(dotAll, { dotAll = !dotAll }, label = { Text("Dot matches newline") })
            }
            Spacer(modifier = Modifier.height(16.dp))
            MonoTextField(input, { input = it }, "Sample text", minLines = 4)
            Spacer(modifier = Modifier.height(16.dp))
            OutputPanel(output, label = "Matches")
        }
    }

    internal fun evaluate(regex: String, input: String, options: Set<RegexOption> = setOf(RegexOption.MULTILINE)): String = try {
        val re = Regex(regex, options)
        val matches = mutableListOf<String>()
        var count = 0
        for (matchResult in re.findAll(input)) {
            if (count == MAX_MATCHES) {
                matches.add("... (truncated after $MAX_MATCHES matches)")
                break
            }
            count++
            matches.add("Match $count: ${matchResult.value}")
            matchResult.groups.drop(1).forEachIndexed { groupIndex, group ->
                matches.add("  Group ${groupIndex + 1}: ${group?.value ?: "null"}")
            }
        }
        if (count == 0) "No matches" else matches.joinToString("\n")
    } catch (e: Throwable) {
        if (e is CancellationException) throw e
        "Error: ${e.message}"
    }
}