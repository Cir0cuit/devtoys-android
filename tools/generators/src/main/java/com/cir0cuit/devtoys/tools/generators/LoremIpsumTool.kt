package com.cir0cuit.devtoys.tools.generators
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Abc

import androidx.compose.material3.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*
import kotlin.random.Random

internal val LOREM_WORDS = ("lorem ipsum dolor sit amet consectetur adipiscing elit sed do eiusmod tempor incididunt ut " +
    "labore et dolore magna aliqua enim ad minim veniam quis nostrud exercitation ullamco laboris nisi aliquip ex ea " +
    "commodo consequat duis aute irure in reprehenderit voluptate velit esse cillum fugiat nulla pariatur excepteur " +
    "sint occaecat cupidatat non proident sunt culpa qui officia deserunt mollit anim id est laborum").split(" ")

internal fun generateLorem(sentences: Int, random: Random): String =
    (0 until sentences).joinToString(" ") { i ->
        if (i == 0) "Lorem ipsum dolor sit amet, consectetur adipiscing elit."
        else List(random.nextInt(6, 16)) { LOREM_WORDS[random.nextInt(LOREM_WORDS.size)] }
            .joinToString(" ").replaceFirstChar { it.uppercase() } + "."
    }

class LoremIpsumTool : Tool {
    override val descriptor = ToolDescriptor("lorem-ipsum", android.R.string.untitled, android.R.string.untitled, Icons.Default.Abc, ToolCategory.GENERATORS, listOf("lorem", "ipsum", "text"))
    @Composable override fun Content(modifier: Modifier) {
        var count by rememberSaveable { mutableStateOf(5f) }
        val sentences = count.toInt()
        var seed by rememberSaveable { mutableStateOf(Random.nextInt()) }
        val output = remember(sentences, seed) { generateLorem(sentences, Random(seed)) }
        ToolScaffold("Lorem Ipsum Generator", modifier = modifier) {
            Text("Sentences: ${count.toInt()}")
            Slider(value = count, onValueChange = { count = it }, valueRange = 1f..50f)
            Button(onClick = { seed = Random.nextInt() }) { Text("Regenerate") }
            Spacer(modifier = Modifier.height(16.dp))
            OutputPanel(output)
        }
    }
}