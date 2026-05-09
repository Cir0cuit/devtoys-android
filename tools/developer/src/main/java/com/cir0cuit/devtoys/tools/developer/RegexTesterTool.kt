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


class RegexTesterTool : Tool {
    override val descriptor = ToolDescriptor("regex", android.R.string.untitled, android.R.string.untitled, Icons.Default.FindInPage, ToolCategory.TEXT, listOf("regex", "tester"))
    @Composable override fun Content(modifier: Modifier) {
        var regex by rememberSaveable { mutableStateOf("") }
        var input by rememberSaveable { mutableStateOf("") }
        val output = remember(regex, input) {
            if (regex.isEmpty()) "" else try {
                val re = Regex(regex, setOf(RegexOption.MULTILINE))
                val matchResults = re.findAll(input).toList()
                if (matchResults.isEmpty()) {
                    "No matches"
                } else {
                    val matches = mutableListOf<String>()
                    matchResults.forEachIndexed { index, matchResult ->
                        matches.add("Match ${index + 1}: ${matchResult.value}")
                        if (matchResult.groups.size > 1) {
                            matchResult.groups.drop(1).forEachIndexed { groupIndex, group ->
                                matches.add("  Group ${groupIndex + 1}: ${group?.value ?: "null"}")
                            }
                        }
                    }
                    matches.joinToString("\n")
                }
            } catch (e: Exception) { "Error: ${e.message}" }
        }
        ToolScaffold("Regex Tester", modifier = modifier) {
            MonoTextField(regex, { regex = it }, "Regex Pattern", minLines = 2)
            Spacer(modifier = Modifier.height(16.dp))
            MonoTextField(input, { input = it }, "Text Input")
            Spacer(modifier = Modifier.height(16.dp))
            OutputPanel(output)
        }
    }
}