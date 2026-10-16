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
    override val descriptor = ToolDescriptor("regex", android.R.string.untitled, android.R.string.untitled, Icons.Default.FindInPage, ToolCategory.TEXT, listOf("regex", "tester"))
    @Composable override fun Content(modifier: Modifier) {
        var regex by rememberSaveable { mutableStateOf("") }
        var input by rememberSaveable { mutableStateOf("") }
        var output by remember { mutableStateOf("") }
        LaunchedEffect(regex, input) {
            if (regex.isEmpty()) {
                output = ""
                return@LaunchedEffect
            }
            delay(250)
            val slowNotice = launch {
                delay(2000)
                output = "Still evaluating... the pattern may be too expensive (catastrophic backtracking)"
            }
            output = withContext(Dispatchers.IO) { evaluate(regex, input) }
            slowNotice.cancel()
        }
        ToolScaffold("Regex Tester", modifier = modifier) {
            MonoTextField(regex, { regex = it }, "Regex Pattern", minLines = 2)
            Spacer(modifier = Modifier.height(16.dp))
            MonoTextField(input, { input = it }, "Text Input")
            Spacer(modifier = Modifier.height(16.dp))
            OutputPanel(output)
        }
    }

    internal fun evaluate(regex: String, input: String): String = try {
        val re = Regex(regex, setOf(RegexOption.MULTILINE))
        val matches = mutableListOf<String>()
        var count = 0
        for (matchResult in re.findAll(input)) {
            if (count == MAX_MATCHES) {
                matches.add("... (truncated after $MAX_MATCHES matches)")
                break
            }
            count++
            matches.add("Match $count: ${matchResult.value}")
            if (matchResult.groups.size > 1) {
                matchResult.groups.drop(1).forEachIndexed { groupIndex, group ->
                    matches.add("  Group ${groupIndex + 1}: ${group?.value ?: "null"}")
                }
            }
        }
        if (count == 0) "No matches" else matches.joinToString("\n")
    } catch (e: Throwable) {
        if (e is CancellationException) throw e
        "Error: ${e.message}"
    }
}