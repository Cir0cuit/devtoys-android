package com.cir0cuit.devtoys.tools.developer

import java.util.regex.Pattern

internal const val REGEX_MAX_MATCHES = 1000

internal enum class RegexMode { MATCH, REPLACE, SPLIT }

/** Flags shown as chips, in display order. LITERAL is applied with Pattern.quote so it works on every engine. */
internal val REGEX_FLAGS = listOf(
    Pattern.CASE_INSENSITIVE to "Ignore case",
    Pattern.MULTILINE to "Multiline",
    Pattern.DOTALL to "Dot all",
    Pattern.COMMENTS to "Comments",
    Pattern.LITERAL to "Literal",
    Pattern.UNIX_LINES to "Unix lines"
)

internal data class GroupHit(val label: String, val start: Int, val end: Int, val value: String?)
internal data class MatchHit(val start: Int, val end: Int, val value: String, val groups: List<GroupHit>)

/** Text output plus the match positions used for highlighting; [isError] when [text] explains a failure. */
internal data class RegexRun(val text: String, val hits: List<MatchHit> = emptyList(), val isError: Boolean = false)

internal fun compileRegex(pattern: String, flags: Int): Pattern =
    if (flags and Pattern.LITERAL != 0) Pattern.compile(Pattern.quote(pattern), flags and Pattern.LITERAL.inv())
    else Pattern.compile(pattern, flags)

/** Capturing group number to name for every (?<name>...) in [pattern]. */
internal fun groupNames(pattern: String): Map<Int, String> {
    val names = mutableMapOf<Int, String>()
    var group = 0
    var i = 0
    var inClass = false
    while (i < pattern.length) {
        val c = pattern[i]
        when {
            c == '\\' -> i++
            inClass -> if (c == ']') inClass = false
            c == '[' -> inClass = true
            c == '(' && pattern.startsWith("(?<", i) && i + 3 < pattern.length && pattern[i + 3] !in "=!" -> {
                group++
                names[group] = pattern.substring(i + 3, pattern.indexOf('>', i).takeIf { it > 0 } ?: pattern.length)
            }
            c == '(' && !pattern.startsWith("(?", i) -> group++
        }
        i++
    }
    return names
}

internal fun findMatches(pattern: String, input: String, flags: Int): Pair<List<MatchHit>, Boolean> {
    val m = compileRegex(pattern, flags).matcher(input)
    val names = if (flags and Pattern.LITERAL != 0) emptyMap() else groupNames(pattern)
    val hits = mutableListOf<MatchHit>()
    while (m.find()) {
        if (hits.size == REGEX_MAX_MATCHES) return hits to true
        val groups = (1..m.groupCount()).map { g -> GroupHit(names[g] ?: g.toString(), m.start(g), m.end(g), m.group(g)) }
        hits += MatchHit(m.start(), m.end(), m.group(), groups)
    }
    return hits to false
}

internal fun runRegex(mode: RegexMode, pattern: String, input: String, flags: Int, replacement: String = ""): RegexRun = try {
    when (mode) {
        RegexMode.MATCH -> {
            val (hits, truncated) = findMatches(pattern, input, flags)
            val lines = hits.flatMapIndexed { i, h ->
                listOf("Match ${i + 1} (${h.start}-${h.end}): ${h.value}") +
                    h.groups.map { g -> "  Group ${g.label}" + (if (g.value == null) ": null" else " (${g.start}-${g.end}): ${g.value}") }
            } + if (truncated) listOf("... (truncated after $REGEX_MAX_MATCHES matches)") else emptyList()
            RegexRun(if (hits.isEmpty()) "No matches" else lines.joinToString("\n"), hits)
        }
        RegexMode.REPLACE -> try {
            RegexRun(compileRegex(pattern, flags).matcher(input).replaceAll(replacement))
        } catch (e: IndexOutOfBoundsException) {
            RegexRun("Error: ${e.message}. Use \$1 to \$n for groups in the pattern, or \\\$ for a literal dollar sign.", isError = true)
        }
        RegexMode.SPLIT -> {
            val parts = compileRegex(pattern, flags).split(input, -1)
            val shown = parts.take(REGEX_MAX_MATCHES).mapIndexed { i, s -> "${i + 1}: $s" }
            RegexRun((shown + if (parts.size > REGEX_MAX_MATCHES) listOf("... (truncated after $REGEX_MAX_MATCHES parts)") else emptyList()).joinToString("\n"))
        }
    }
} catch (e: Throwable) {
    if (e is kotlinx.coroutines.CancellationException) throw e
    // StackOverflowError from heavy backtracking lands here too.
    RegexRun("Error: ${e.message ?: e.javaClass.simpleName}", isError = true)
}

/** Match-mode text, kept for tests. */
internal fun regexMatchText(regex: String, input: String, flags: Int = Pattern.MULTILINE): String = runRegex(RegexMode.MATCH, regex, input, flags).text

internal val CODE_LANGUAGES = listOf("Kotlin", "Java", "JavaScript", "Python", "Go", "grep")

/** The pattern and flags as source code in [language]. Flags the language lacks are listed in a comment. */
internal fun regexAsCode(pattern: String, flags: Int, language: String): String {
    fun has(f: Int) = flags and f != 0
    val literal = has(Pattern.LITERAL)
    // Languages without a literal flag get the metacharacters escaped instead.
    val p = if (literal && language !in setOf("Kotlin", "Java")) pattern.replace(Regex("""[\\.\[\]{}()*+?^$|/]"""), "\\\\$0") else pattern
    val missing = mutableListOf<String>()
    fun note(comment: String, code: String) =
        if (missing.isEmpty()) code else "$comment no equivalent for: ${missing.joinToString()}\n$code"
    return when (language) {
        "Kotlin" -> {
            val opts = listOf(
                Pattern.CASE_INSENSITIVE to "IGNORE_CASE", Pattern.MULTILINE to "MULTILINE", Pattern.DOTALL to "DOT_MATCHES_ALL",
                Pattern.COMMENTS to "COMMENTS", Pattern.LITERAL to "LITERAL", Pattern.UNIX_LINES to "UNIX_LINES"
            ).filter { has(it.first) }.map { "RegexOption.${it.second}" }
            val str = if ("\"\"\"" in p) javaString(p).replace("$", "\\$")
            else "\"\"\"" + p.replace(Regex("""\$(?=[A-Za-z_{])""")) { "\${'\$'}" } + "\"\"\""
            when (opts.size) {
                0 -> "Regex($str)"
                1 -> "Regex($str, ${opts[0]})"
                else -> "Regex($str, setOf(${opts.joinToString()}))"
            }
        }
        "Java" -> {
            val opts = listOf(
                Pattern.CASE_INSENSITIVE to "CASE_INSENSITIVE", Pattern.MULTILINE to "MULTILINE", Pattern.DOTALL to "DOTALL",
                Pattern.COMMENTS to "COMMENTS", Pattern.LITERAL to "LITERAL", Pattern.UNIX_LINES to "UNIX_LINES"
            ).filter { has(it.first) }.joinToString(" | ") { "Pattern.${it.second}" }
            "Pattern.compile(${javaString(p)}" + (if (opts.isEmpty()) ")" else ", $opts)")
        }
        "JavaScript" -> {
            if (has(Pattern.COMMENTS)) missing += "comments"
            if (has(Pattern.UNIX_LINES)) missing += "unix lines"
            val f = "g" + (if (has(Pattern.CASE_INSENSITIVE)) "i" else "") + (if (has(Pattern.MULTILINE)) "m" else "") + (if (has(Pattern.DOTALL)) "s" else "")
            val body = if (literal) p else p.replace(Regex("""(?<!\\)((?:\\\\)*)/"""), "$1\\\\/")
            note("//", "/${body.replace("\n", "\\n")}/$f")
        }
        "Python" -> {
            if (has(Pattern.UNIX_LINES)) missing += "unix lines"
            val f = listOf(Pattern.CASE_INSENSITIVE to "re.I", Pattern.MULTILINE to "re.M", Pattern.DOTALL to "re.S", Pattern.COMMENTS to "re.X")
                .filter { has(it.first) }.joinToString(" | ") { it.second }
            val body = p.replace("(?<", "(?P<").replace("(?P<=", "(?<=").replace("(?P<!", "(?<!")
                .replace("'", "\\'").replace("\n", "\\n")
            note("#", "re.compile(r'$body'" + (if (f.isEmpty()) ")" else ", $f)"))
        }
        "Go" -> {
            if (has(Pattern.COMMENTS)) missing += "comments"
            if (has(Pattern.UNIX_LINES)) missing += "unix lines"
            val f = (if (has(Pattern.CASE_INSENSITIVE)) "i" else "") + (if (has(Pattern.MULTILINE)) "m" else "") + (if (has(Pattern.DOTALL)) "s" else "")
            val body = (if (f.isEmpty()) "" else "(?$f)") + p
            note("//", "regexp.MustCompile(" + (if ('`' in body) javaString(body) else "`$body`") + ")")
        }
        else -> {
            // grep matches line by line, so multiline anchors already behave per line.
            if (has(Pattern.DOTALL)) missing += "dot all"
            if (has(Pattern.UNIX_LINES)) missing += "unix lines"
            val body = (if (has(Pattern.COMMENTS)) "(?x)" else "") + p
            note("#", "grep -oP" + (if (has(Pattern.CASE_INSENSITIVE)) "i" else "") + " '" + body.replace("'", "'\\''") + "'")
        }
    }
}

private fun javaString(s: String) = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\t", "\\t") + "\""
