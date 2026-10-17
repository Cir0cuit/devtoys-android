package com.cir0cuit.devtoys.tools.data
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val SQL_LAYOUTS = listOf("2 spaces", "4 spaces", "Minify")
private val SQL_TYPES = arrayOf("application/sql", "text/*", "application/octet-stream")
private val SQL_CASES = listOf("Uppercase" to SqlKeywordCase.UPPER, "Lowercase" to SqlKeywordCase.LOWER, "Keep case" to SqlKeywordCase.KEEP)

class SqlFormatterTool : Tool {
    override val descriptor = ToolDescriptor("sql-formatter", "SQL Formatter", "Format or minify SQL queries", Icons.Default.Storage, ToolCategory.FORMATTERS, listOf("sql", "format", "minify", "query"), accepts = setOf(ShareKind.TEXT), fileTypes = setOf("sql"))
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable override fun Content(modifier: Modifier) {
        var input by rememberCappedText("select id, name from users where active = 1 and age > 18 order by name")
        OnSharedText { input = it }
        var layout by rememberSaveable { mutableStateOf(0) }
        var case by rememberSaveable { mutableStateOf(0) }
        var output by remember { mutableStateOf("") }
        var unterminated by remember { mutableStateOf(false) }
        val debounce = rememberDebounce()
        LaunchedEffect(input, layout, case) {
            if (input.isBlank()) {
                output = ""
                unterminated = false
                return@LaunchedEffect
            }
            debounce.await(200)
            val keywordCase = SQL_CASES[case].second
            val (text, open) = withContext(Dispatchers.Default) {
                val formatted = if (layout == 2) minifySql(input, keywordCase) else formatSql(input, if (layout == 0) 2 else 4, keywordCase)
                formatted to hasUnterminatedSql(input)
            }
            output = text
            unterminated = open
        }
        ToolScaffold(descriptor.title, modifier = modifier) {
            FileInput(input, { input = it }, "SQL input", mimeTypes = SQL_TYPES)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SQL_LAYOUTS.forEachIndexed { i, label ->
                    SegmentedButton(
                        selected = layout == i,
                        onClick = { layout = i },
                        shape = SegmentedButtonDefaults.itemShape(i, SQL_LAYOUTS.size),
                        icon = {}
                    ) { Text(label, maxLines = 1) }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SQL_CASES.forEachIndexed { i, (label, _) ->
                    SegmentedButton(
                        selected = case == i,
                        onClick = { case = i },
                        shape = SegmentedButtonDefaults.itemShape(i, SQL_CASES.size),
                        icon = {}
                    ) { Text(label, maxLines = 1) }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (unterminated) {
                ErrorText("A string, quoted name or comment is not closed, so everything after its start is kept as is.")
                Spacer(modifier = Modifier.height(8.dp))
            }
            OutputPanel(output, label = if (layout == 2) "Minified SQL" else "Formatted SQL")
            if (output.isNotEmpty()) OutputActions(output, "formatted.sql", "application/sql")
        }
    }
}

internal enum class SqlKeywordCase { UPPER, LOWER, KEEP }

private enum class SqlKind { WORD, LITERAL, LINE_COMMENT, BLOCK_COMMENT, OTHER }

private class SqlToken(val text: String, val kind: SqlKind, val spaceBefore: Boolean)

// Strings, quoted identifiers and comments are single tokens, so nothing inside them is ever rewritten.
// An unclosed string, quoted identifier or block comment runs to the end of the input.
private val SQL_TOKEN = Regex(
    "'(?:[^']|'')*'?|\"(?:[^\"]|\"\")*\"?|`[^`]*`?|\\[[^\\]]*]|--[^\\n]*|/\\*[\\s\\S]*?(?:\\*/|\\z)" +
        "|[\\p{L}_][\\p{L}\\p{N}_$]*|\\d+(?:\\.\\d*)?(?:[eE][+-]?\\d+)?|\\.\\d+" +
        "|::|<=|>=|<>|!=|\\|\\||->>|->|\\S"
)

private fun tokenizeSql(input: String): List<SqlToken> {
    val tokens = mutableListOf<SqlToken>()
    var end = 0
    for (m in SQL_TOKEN.findAll(input)) {
        val t = m.value
        val kind = when {
            t.startsWith("--") -> SqlKind.LINE_COMMENT
            t.startsWith("/*") -> SqlKind.BLOCK_COMMENT
            t[0] == '\'' || t[0] == '"' || t[0] == '`' || (t[0] == '[' && t.length > 1) -> SqlKind.LITERAL
            t[0].isLetter() || t[0] == '_' -> SqlKind.WORD
            else -> SqlKind.OTHER
        }
        tokens += SqlToken(t, kind, m.range.first > end)
        end = m.range.last + 1
    }
    return tokens
}

private val CLOSED_SQL_QUOTE = Regex("'(?:[^']|'')*'|\"(?:[^\"]|\"\")*\"|`[^`]*`|\\[[^\\]]*]")

/** True when a string, quoted identifier or block comment in [input] is never closed. */
internal fun hasUnterminatedSql(input: String): Boolean = tokenizeSql(input).any {
    when (it.kind) {
        SqlKind.LITERAL -> !CLOSED_SQL_QUOTE.matches(it.text)
        SqlKind.BLOCK_COMMENT -> it.text.length < 4 || !it.text.endsWith("*/")
        else -> false
    }
}

private val SQL_KEYWORDS = setOf(
    "SELECT", "DISTINCT", "FROM", "WHERE", "AND", "OR", "NOT", "IN", "IS", "NULL", "LIKE", "ILIKE", "BETWEEN", "AS", "ON",
    "USING", "JOIN", "LEFT", "RIGHT", "INNER", "OUTER", "FULL", "CROSS", "NATURAL", "GROUP", "ORDER", "BY", "HAVING",
    "LIMIT", "OFFSET", "FETCH", "NEXT", "ROWS", "ROW", "ONLY", "TOP", "UNION", "ALL", "EXCEPT", "INTERSECT", "INSERT",
    "INTO", "VALUES", "UPDATE", "SET", "DELETE", "WITH", "RECURSIVE", "CASE", "WHEN", "THEN", "ELSE", "END", "RETURNING",
    "ASC", "DESC", "EXISTS", "ANY", "SOME", "CREATE", "TABLE", "DROP", "ALTER", "ADD", "COLUMN", "PRIMARY", "KEY",
    "FOREIGN", "REFERENCES", "DEFAULT", "INDEX", "VIEW", "UNIQUE", "CHECK", "CONSTRAINT", "IF", "TRUE", "FALSE",
    "COUNT", "SUM", "AVG", "MIN", "MAX", "CAST", "COALESCE", "OVER", "PARTITION", "WINDOW", "CONFLICT", "DO", "NOTHING",
    "INTERVAL", "LATERAL", "BEGIN", "COMMIT", "ROLLBACK", "TRUNCATE", "REPLACE", "NULLS", "FIRST", "LAST",
)

private enum class PhraseKind { CLAUSE, SET_OPERATOR, JOIN }

// Longest first, so "LEFT OUTER JOIN" wins over "LEFT JOIN".
private val SQL_PHRASES: List<Pair<List<String>, PhraseKind>> = (
    listOf(
        "SELECT DISTINCT", "SELECT", "FROM", "WHERE", "GROUP BY", "HAVING", "ORDER BY", "LIMIT", "OFFSET", "INSERT INTO",
        "VALUES", "UPDATE", "SET", "DELETE FROM", "WITH RECURSIVE", "WITH", "RETURNING",
    ).map { it to PhraseKind.CLAUSE } +
        listOf("UNION ALL", "UNION", "EXCEPT", "INTERSECT").map { it to PhraseKind.SET_OPERATOR } +
        listOf(
            "LEFT OUTER JOIN", "RIGHT OUTER JOIN", "FULL OUTER JOIN", "LEFT JOIN", "RIGHT JOIN", "FULL JOIN",
            "INNER JOIN", "CROSS JOIN", "NATURAL JOIN", "JOIN",
        ).map { it to PhraseKind.JOIN }
    ).map { (p, k) -> p.split(' ') to k }.sortedByDescending { it.first.size }

private fun matchPhrase(tokens: List<SqlToken>, at: Int): Pair<Int, PhraseKind>? =
    SQL_PHRASES.firstOrNull { (words, _) ->
        at + words.size <= tokens.size && words.indices.all { j ->
            tokens[at + j].kind == SqlKind.WORD && tokens[at + j].text.equals(words[j], ignoreCase = true)
        }
    }?.let { (words, kind) -> words.size to kind }

private fun applyCase(word: String, case: SqlKeywordCase): String = when {
    case == SqlKeywordCase.KEEP || word.uppercase() !in SQL_KEYWORDS -> word
    case == SqlKeywordCase.UPPER -> word.uppercase()
    else -> word.lowercase()
}

private fun sqlNeedsSpace(prev: String?, text: String, spaceBefore: Boolean): Boolean = when {
    prev == null || prev == "(" || prev == "." -> false
    text == ")" || text == "," || text == ";" || text == "." -> false
    prev == "," -> true
    else -> spaceBefore
}

/** A paren or CASE scope. Commas, AND/OR and clause keywords only break lines in a [BLOCK] (statement or subquery). */
private class SqlFrame(val type: Int, val level: Int, val closeLevel: Int = 0) {
    companion object { const val BLOCK = 0; const val INLINE = 1; const val CASE = 2 }
}

/**
 * Puts each clause on its own line with its content indented below it, one select column or
 * condition per line, subqueries and CASE indented, and statements separated by a blank line.
 */
internal fun formatSql(input: String, indentWidth: Int = 2, keywordCase: SqlKeywordCase = SqlKeywordCase.KEEP): String {
    val tokens = tokenizeSql(input)
    val unit = " ".repeat(indentWidth)
    val out = StringBuilder()
    val frames = ArrayDeque<SqlFrame>().apply { add(SqlFrame(SqlFrame.BLOCK, 0)) }
    var lineLevel = 0
    var pendingBreak: Int? = null
    var blankLine = false
    var prev: String? = null
    var inBetween = false

    fun emit(text: String, spaceBefore: Boolean) {
        val level = pendingBreak
        when {
            out.isEmpty() -> lineLevel = 0
            level != null -> {
                out.append(if (blankLine) "\n\n" else "\n").append(unit.repeat(level))
                lineLevel = level
            }
            sqlNeedsSpace(prev, text, spaceBefore) -> out.append(' ')
        }
        out.append(text)
        pendingBreak = null
        blankLine = false
        prev = text
    }

    var i = 0
    while (i < tokens.size) {
        val t = tokens[i]
        val top = frames.last()
        val block = top.type == SqlFrame.BLOCK
        when (t.kind) {
            SqlKind.LINE_COMMENT -> {
                emit(t.text, true)
                pendingBreak = lineLevel
            }
            SqlKind.BLOCK_COMMENT, SqlKind.LITERAL -> emit(t.text, t.spaceBefore)
            SqlKind.OTHER -> when (t.text) {
                "(" -> {
                    var n = i + 1
                    while (n < tokens.size && (tokens[n].kind == SqlKind.LINE_COMMENT || tokens[n].kind == SqlKind.BLOCK_COMMENT)) n++
                    val next = tokens.getOrNull(n)
                    val subquery = next?.kind == SqlKind.WORD && (next.text.equals("SELECT", true) || next.text.equals("WITH", true))
                    emit("(", t.spaceBefore)
                    if (subquery) {
                        frames.add(SqlFrame(SqlFrame.BLOCK, lineLevel + 1, lineLevel))
                        pendingBreak = lineLevel + 1
                    } else {
                        frames.add(SqlFrame(SqlFrame.INLINE, lineLevel))
                    }
                }
                ")" -> {
                    while (frames.size > 1 && frames.last().type == SqlFrame.CASE) frames.removeLast()
                    if (frames.size > 1) {
                        val closed = frames.removeLast()
                        if (closed.type == SqlFrame.BLOCK) pendingBreak = closed.closeLevel
                    }
                    emit(")", t.spaceBefore)
                }
                "," -> {
                    emit(",", false)
                    if (block) pendingBreak = top.level + 1
                }
                ";" -> {
                    emit(";", false)
                    frames.clear()
                    frames.add(SqlFrame(SqlFrame.BLOCK, 0))
                    pendingBreak = 0
                    blankLine = true
                    inBetween = false
                }
                else -> emit(t.text, t.spaceBefore)
            }
            SqlKind.WORD -> {
                val phrase = if (block) matchPhrase(tokens, i) else null
                if (phrase != null) {
                    val (count, kind) = phrase
                    val text = tokens.subList(i, i + count).joinToString(" ") { applyCase(it.text, keywordCase) }
                    when (kind) {
                        PhraseKind.CLAUSE -> {
                            pendingBreak = top.level
                            emit(text, t.spaceBefore)
                            pendingBreak = top.level + 1
                        }
                        PhraseKind.SET_OPERATOR -> {
                            pendingBreak = top.level
                            emit(text, t.spaceBefore)
                            pendingBreak = top.level
                        }
                        PhraseKind.JOIN -> {
                            pendingBreak = top.level + 1
                            emit(text, t.spaceBefore)
                        }
                    }
                    i += count
                    continue
                }
                val upper = t.text.uppercase()
                val text = applyCase(t.text, keywordCase)
                when {
                    upper == "AND" && inBetween -> {
                        inBetween = false
                        emit(text, t.spaceBefore)
                    }
                    (upper == "AND" || upper == "OR") && block -> {
                        pendingBreak = top.level + 1
                        emit(text, t.spaceBefore)
                    }
                    upper == "BETWEEN" -> {
                        inBetween = true
                        emit(text, t.spaceBefore)
                    }
                    upper == "CASE" -> {
                        emit(text, t.spaceBefore)
                        frames.add(SqlFrame(SqlFrame.CASE, lineLevel + 1))
                    }
                    (upper == "WHEN" || upper == "ELSE") && top.type == SqlFrame.CASE -> {
                        pendingBreak = top.level
                        emit(text, t.spaceBefore)
                    }
                    upper == "END" && top.type == SqlFrame.CASE -> {
                        frames.removeLast()
                        pendingBreak = top.level - 1
                        emit(text, t.spaceBefore)
                    }
                    else -> emit(text, t.spaceBefore)
                }
            }
        }
        i++
    }
    return out.toString()
}

/** Collapses [input] onto one line and drops comments; literals and quoted identifiers are kept verbatim. */
internal fun minifySql(input: String, keywordCase: SqlKeywordCase = SqlKeywordCase.KEEP): String = buildString {
    var prev: String? = null
    var droppedComment = false
    for (t in tokenizeSql(input)) {
        if (t.kind == SqlKind.LINE_COMMENT || t.kind == SqlKind.BLOCK_COMMENT) {
            droppedComment = true
            continue
        }
        val text = if (t.kind == SqlKind.WORD) applyCase(t.text, keywordCase) else t.text
        if (sqlNeedsSpace(prev, text, t.spaceBefore || droppedComment)) append(' ')
        append(text)
        prev = text
        droppedComment = false
    }
}
