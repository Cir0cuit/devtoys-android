package com.cir0cuit.devtoys.tools.generators
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Abc
import androidx.compose.material3.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*
import kotlin.random.Random

internal val LOREM_WORDS = ("lorem ipsum dolor sit amet consectetur adipiscing elit sed do eiusmod tempor incididunt ut " +
    "labore et dolore magna aliqua enim ad minim veniam quis nostrud exercitation ullamco laboris nisi aliquip ex ea " +
    "commodo consequat duis aute irure in reprehenderit voluptate velit esse cillum fugiat nulla pariatur excepteur " +
    "sint occaecat cupidatat non proident sunt culpa qui officia deserunt mollit anim id est laborum").split(" ")

private const val LOREM_OPENING = "Lorem ipsum dolor sit amet, consectetur adipiscing elit."

private fun randomSentence(random: Random): String =
    List(random.nextInt(6, 16)) { LOREM_WORDS[random.nextInt(LOREM_WORDS.size)] }
        .joinToString(" ").replaceFirstChar { it.uppercase() } + "."

private fun loremSentences(n: Int, classic: Boolean, random: Random): List<String> =
    List(n) { i -> if (i == 0 && classic) LOREM_OPENING else randomSentence(random) }

internal fun generateLorem(sentences: Int, random: Random): String =
    loremSentences(sentences, true, random).joinToString(" ")

internal enum class LoremUnit(val label: String, val max: Int) { WORDS("Words", 2000), SENTENCES("Sentences", 500), PARAGRAPHS("Paragraphs", 100), CHARACTERS("Characters", 20_000) }
internal enum class LoremFormat(val label: String) { PLAIN("Plain"), HTML("HTML"), JSON("JSON") }

/** Text blocks (one per paragraph, or a single block for other units). [CHARACTERS] gives exactly [count] chars. */
internal fun loremBlocks(unit: LoremUnit, count: Int, classic: Boolean, random: Random): List<String> {
    if (count <= 0) return emptyList()
    return when (unit) {
        LoremUnit.SENTENCES -> listOf(loremSentences(count, classic, random).joinToString(" "))
        LoremUnit.PARAGRAPHS -> List(count) { p -> loremSentences(random.nextInt(3, 8), classic && p == 0, random).joinToString(" ") }
        LoremUnit.WORDS -> {
            val opening = if (classic) LOREM_OPENING.lowercase().replace(Regex("[,.]"), "").split(" ") else emptyList()
            val words = List(count) { i -> opening.getOrNull(i) ?: LOREM_WORDS[random.nextInt(LOREM_WORDS.size)] }
            listOf(words.joinToString(" ").replaceFirstChar { it.uppercase() } + ".")
        }
        LoremUnit.CHARACTERS -> {
            val sb = StringBuilder()
            var i = 0
            while (sb.length < count) { if (sb.isNotEmpty()) sb.append(' '); sb.append(if (i++ == 0 && classic) LOREM_OPENING else randomSentence(random)) }
            val text = sb.substring(0, count)
            listOf(if (text.endsWith(' ')) text.dropLast(1) + "." else text)
        }
    }
}

internal fun formatLorem(blocks: List<String>, format: LoremFormat): String = when (format) {
    LoremFormat.PLAIN -> blocks.joinToString("\n\n")
    LoremFormat.HTML -> blocks.joinToString("\n") { "<p>$it</p>" }
    // Lorem text contains no quotes, backslashes or control characters, so no escaping is needed.
    LoremFormat.JSON -> blocks.joinToString(",\n  ", "[\n  ", "\n]") { "\"$it\"" }
}

class LoremIpsumTool : Tool {
    override val descriptor = ToolDescriptor("lorem-ipsum", "Lorem Ipsum Generator", "Generate placeholder text", Icons.Default.Abc, ToolCategory.GENERATORS, listOf("lorem", "ipsum", "text", "placeholder", "dummy"))
    @Composable override fun Content(modifier: Modifier) {
        var unit by rememberSaveable { mutableStateOf(LoremUnit.PARAGRAPHS) }
        var countText by rememberSaveable { mutableStateOf("3") }
        var classic by rememberSaveable { mutableStateOf(true) }
        var format by rememberSaveable { mutableStateOf(LoremFormat.PLAIN) }
        var seed by rememberSaveable { mutableStateOf(Random.nextInt()) }
        val count = countText.toIntOrNull()?.takeIf { it in 1..unit.max }
        // Derived from saved inputs, so the (possibly large) text never goes into saved state.
        val blocks = remember(unit, count, classic, seed) { count?.let { loremBlocks(unit, it, classic, Random(seed)) } }
        val output = remember(blocks, format) { blocks?.let { formatLorem(it, format) } }

        ToolScaffold(descriptor.title, modifier = modifier) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = countText,
                    onValueChange = { countText = it.filter(Char::isDigit).take(6) },
                    label = { Text("How many") },
                    singleLine = true,
                    isError = count == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                SelectField("Unit", unit, LoremUnit.entries.toList(), { unit = it }, Modifier.weight(1.3f), optionLabel = { it.label })
            }
            Spacer(Modifier.height(16.dp))
            Text("Output", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            ChoiceRow(LoremFormat.entries.toList(), format, { format = it }, { it.label })
            Spacer(Modifier.height(8.dp))
            FilterChip(classic, { classic = !classic }, { Text("Start with \"Lorem ipsum\"") })
            Spacer(Modifier.height(8.dp))
            RegenerateButton("Regenerate", enabled = count != null) { seed = Random.nextInt() }
            Spacer(Modifier.height(16.dp))
            if (output == null) {
                Text("Enter a whole number from 1 to ${unit.max}.", color = MaterialTheme.colorScheme.error)
            } else {
                val words = blocks.orEmpty().sumOf { b -> b.split(' ').count { it.isNotEmpty() } }
                val chars = blocks.orEmpty().sumOf { it.length }
                OutputPanel(output, label = "Text · %,d words · %,d chars".format(words, chars))
            }
        }
    }
}
