package com.cir0cuit.devtoys.tools.text

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.MonoTextField
import com.cir0cuit.devtoys.core.ui.OutputPanel
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolCategory
import com.cir0cuit.devtoys.tools.base.ToolDescriptor
import java.text.BreakIterator
import java.util.Locale

internal data class TextStats(
    val chars: Int,
    val codePoints: Int,
    val graphemes: Int,
    val words: Int,
    val sentences: Int,
    val lines: Int,
    val nonBlankLines: Int,
    val longestLine: Int,
    val utf8Bytes: Int,
    val latin1Bytes: Int?,
    val lineEndings: String,
    val topWords: List<Pair<String, Int>>
)

private val EMPTY_STATS = computeTextStats("")

class TextStatsTool : Tool {
    override val descriptor = ToolDescriptor(
        "text-stats", "Text Statistics", "Count characters, words, lines and bytes",
        Icons.Default.QueryStats, ToolCategory.TEXT,
        listOf("count", "statistics", "analyzer", "length", "words", "characters", "bytes", "graphemes", "lines")
    )

    @Composable override fun Content(modifier: Modifier) {
        var input by rememberSaveable(saver = boundedTextSaver) { mutableStateOf("") }
        val stats by rememberDebounced(EMPTY_STATS, input) { computeTextStats(input) }

        ToolScaffold(descriptor.title, modifier = modifier) {
            MonoTextField(input, { input = it }, "Text", minLines = 6, maxLines = 14)
            Spacer(Modifier.height(16.dp))
            ValueRowsSlab(statRows(stats))
            if (stats.topWords.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                OutputPanel(stats.topWords.joinToString("\n") { (w, n) -> "$w\t$n" }, label = "Top words")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

internal fun statRows(s: TextStats): List<Pair<String, String>> {
    val minutes = s.words / 230.0
    return listOf(
        "Characters (UTF-16 units)" to "${s.chars}",
        "Code points" to "${s.codePoints}",
        "Graphemes (visible characters)" to "${s.graphemes}",
        "Words" to "${s.words}",
        "Sentences" to "${s.sentences}",
        "Lines" to "${s.lines} (${s.nonBlankLines} not blank)",
        "Longest line" to "${s.longestLine} characters",
        "Line endings" to s.lineEndings,
        "Bytes" to "UTF-8 ${s.utf8Bytes} · UTF-16 ${s.chars * 2} · Latin-1 ${s.latin1Bytes ?: "not encodable"}",
        "Reading time" to if (s.words == 0) "0 min" else if (minutes < 1) "under 1 min" else "${Math.round(minutes)} min"
    )
}

private fun BreakIterator.segments(text: String): Sequence<String> {
    setText(text)
    var start = first()
    return generateSequence { next().takeIf { it != BreakIterator.DONE }?.let { end -> text.substring(start, end).also { start = end } } }
}

internal fun computeTextStats(text: String, topWordLimit: Int = 10): TextStats {
    val words = BreakIterator.getWordInstance(Locale.ROOT).segments(text)
        .filter { w -> w.any { it.isLetterOrDigit() } }.toList()
    val lines = if (text.isEmpty()) emptyList() else text.removeSuffix("\n").removeSuffix("\r").lines()
    val crlf = (0 until text.length - 1).count { text[it] == '\r' && text[it + 1] == '\n' }
    val lf = text.count { it == '\n' } - crlf
    val cr = text.count { it == '\r' } - crlf
    val endings = listOf("LF" to lf, "CRLF" to crlf, "CR" to cr).filter { it.second > 0 }
    return TextStats(
        chars = text.length,
        codePoints = text.codePointCount(0, text.length),
        graphemes = BreakIterator.getCharacterInstance(Locale.ROOT).segments(text).count(),
        words = words.size,
        sentences = BreakIterator.getSentenceInstance(Locale.ROOT).segments(text).count { it.isNotBlank() },
        lines = lines.size,
        nonBlankLines = lines.count { it.isNotBlank() },
        longestLine = lines.maxOfOrNull { it.length } ?: 0,
        utf8Bytes = text.toByteArray(Charsets.UTF_8).size,
        latin1Bytes = text.length.takeIf { text.all { it.code <= 0xFF } },
        lineEndings = when (endings.size) {
            0 -> "None"
            1 -> endings[0].first
            else -> "Mixed (" + endings.joinToString { "${it.second} ${it.first}" } + ")"
        },
        topWords = words.groupingBy { it.lowercase() }.eachCount().entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(topWordLimit).map { it.key to it.value }
    )
}
