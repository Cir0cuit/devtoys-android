package com.cir0cuit.devtoys.tools.developer

/** SemVer 2.0.0 version; [build] is kept for display but never affects precedence. */
internal data class Semver(val major: Long, val minor: Long, val patch: Long, val pre: List<String> = emptyList(), val build: String = "") : Comparable<Semver> {
    override fun compareTo(other: Semver): Int = compareBy<Semver>({ it.major }, { it.minor }, { it.patch }).compare(this, other)
        .takeIf { it != 0 } ?: comparePre(pre, other.pre)

    override fun toString() = "$major.$minor.$patch" + (if (pre.isEmpty()) "" else "-" + pre.joinToString(".")) + (if (build.isEmpty()) "" else "+$build")
    val core get() = Triple(major, minor, patch)
}

private const val IDENT = "[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*"
private val SEMVER = Regex("^[vV=]?\\s*(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-($IDENT))?(?:\\+($IDENT))?$")

/** Strict SemVer, with an optional leading v or =. Null when invalid. */
internal fun parseSemver(text: String): Semver? {
    val m = SEMVER.matchEntire(text.trim()) ?: return null
    val (ma, mi, pa, pre, build) = m.destructured
    val ids = if (pre.isEmpty()) emptyList() else pre.split(".")
    // Numeric pre-release identifiers must not have leading zeros.
    if (ids.any { it.length > 1 && it.startsWith("0") && it.all(Char::isDigit) }) return null
    return Semver(ma.toLongOrNull() ?: return null, mi.toLongOrNull() ?: return null, pa.toLongOrNull() ?: return null, ids, build)
}

private fun isNumeric(s: String) = s.all(Char::isDigit)

private fun compareIdent(a: String, b: String): Int = when {
    isNumeric(a) && isNumeric(b) -> compareValuesBy(a, b, { it.length }, { it }) // no leading zeros, so length first is numeric order
    isNumeric(a) -> -1
    isNumeric(b) -> 1
    else -> a.compareTo(b)
}

private fun comparePre(a: List<String>, b: List<String>): Int {
    if (a.isEmpty() || b.isEmpty()) return b.size.coerceAtMost(1) - a.size.coerceAtMost(1) // a release outranks its pre-releases
    for (i in 0 until minOf(a.size, b.size)) compareIdent(a[i], b[i]).let { if (it != 0) return it }
    return a.size.compareTo(b.size)
}

/** "1.2.3 < 1.10.0" plus which part decided it. */
internal fun explainCompare(a: Semver, b: Semver): Pair<String, String> {
    val c = a.compareTo(b)
    val sign = if (c < 0) "<" else if (c > 0) ">" else "="
    val why = when {
        a.major != b.major -> "Major ${a.major} $sign ${b.major}"
        a.minor != b.minor -> "Minor ${a.minor} $sign ${b.minor}"
        a.patch != b.patch -> "Patch ${a.patch} $sign ${b.patch}"
        a.pre.isEmpty() != b.pre.isEmpty() -> "A pre-release sorts before its release"
        c != 0 -> {
            val i = a.pre.indices.firstOrNull { it >= b.pre.size || a.pre[it] != b.pre[it] } ?: a.pre.size
            if (i >= a.pre.size || i >= b.pre.size) "The longer pre-release wins when the shared identifiers are equal"
            else "Pre-release identifier ${i + 1}: ${a.pre[i]} $sign ${b.pre[i]}" + if (isNumeric(a.pre[i]) != isNumeric(b.pre[i])) " (numbers sort before text)" else ""
        }
        a.build != b.build -> "Same precedence; build metadata is ignored"
        else -> "Identical"
    }
    return "$a $sign $b" to why
}

/** One comparator after desugaring, like ">=1.2.3" or "<2.0.0-0". */
internal data class RangeComparator(val op: String, val v: Semver) {
    fun test(x: Semver): Boolean {
        val c = x.compareTo(v)
        return when (op) {
            ">" -> c > 0
            ">=" -> c >= 0
            "<" -> c < 0
            "<=" -> c <= 0
            else -> c == 0
        }
    }
    override fun toString() = "$op$v"
}

private class PartialVersion(val major: Long?, val minor: Long?, val patch: Long?, val pre: List<String>) {
    fun floor() = Semver(major ?: 0, minor ?: 0, patch ?: 0, pre)
    val full get() = patch != null
}

private val PARTIAL = Regex("^[vV]?(\\d+|[xX*])(?:\\.(\\d+|[xX*]))?(?:\\.(\\d+|[xX*]))?(?:-($IDENT))?(?:\\+$IDENT)?$")

private fun parsePartial(s: String): PartialVersion {
    if (s.isEmpty()) return PartialVersion(null, null, null, emptyList())
    val m = PARTIAL.matchEntire(s) ?: throw IllegalArgumentException("Can't read \"$s\" as a version.")
    fun num(g: String) = g.takeIf { it.isNotEmpty() && it.first().isDigit() }?.toLong()
    val ma = num(m.groupValues[1])
    val mi = if (ma == null) null else num(m.groupValues[2])
    val pa = if (mi == null) null else num(m.groupValues[3])
    return PartialVersion(ma, mi, pa, m.groupValues[4].takeIf { it.isNotEmpty() }?.split(".").orEmpty())
}

private fun upper(major: Long, minor: Long = 0, patch: Long = 0) = RangeComparator("<", Semver(major, minor, patch, listOf("0")))

/** Upper bound just past [p]'s last given part: 1 -> <2.0.0-0, 1.2 -> <1.3.0-0. */
private fun nextAfter(p: PartialVersion) = when {
    p.minor == null -> upper(p.major!! + 1)
    else -> upper(p.major!!, p.minor + 1)
}

private fun desugar(token: String, cargo: Boolean): List<RangeComparator> {
    val (rawOp, rest) = Regex("^(<=|>=|<|>|=|\\^|~)?(.*)$").matchEntire(token)!!.destructured
    val p = parsePartial(rest)
    val op = if (rawOp.isEmpty() && cargo && !Regex("[xX*]").containsMatchIn(rest) && p.major != null) "^" else rawOp
    if (p.major == null) return if (op == "<" || op == ">") listOf(RangeComparator("<", Semver(0, 0, 0, listOf("0")))) else listOf(RangeComparator(">=", Semver(0, 0, 0)))
    return when (op) {
        "^" -> listOf(RangeComparator(">=", p.floor()), when {
            p.major > 0 || p.minor == null -> upper(p.major + 1)
            p.minor!! > 0 || p.patch == null -> upper(0, p.minor + 1)
            else -> upper(0, 0, p.patch + 1)
        })
        "~" -> listOf(RangeComparator(">=", p.floor()), nextAfter(PartialVersion(p.major, p.minor, null, emptyList())))
        ">" -> listOf(if (p.full) RangeComparator(">", p.floor()) else RangeComparator(">=", nextAfter(p).v.copy(pre = emptyList())))
        ">=" -> listOf(RangeComparator(">=", p.floor()))
        "<" -> listOf(if (p.full) RangeComparator("<", p.floor()) else RangeComparator("<", p.floor().copy(pre = listOf("0"))))
        "<=" -> listOf(if (p.full) RangeComparator("<=", p.floor()) else nextAfter(p))
        else -> if (p.full) listOf(RangeComparator("=", p.floor())) else listOf(RangeComparator(">=", p.floor()), nextAfter(p))
    }
}

/**
 * npm ranges (`^1.2.3`, `~1.2`, `>=1.0 <2.0`, `1.x`, `1.2.3 - 2.3.4`, `||`), or Cargo requirements
 * (comma-separated, a bare version means caret). Returns OR-ed sets of AND-ed comparators; throws when unreadable.
 */
internal fun parseRange(text: String, cargo: Boolean = false): List<List<RangeComparator>> {
    val sets = if (cargo) listOf(text) else text.split("||")
    return sets.map { set ->
        var s = set.trim().replace(Regex("(<=|>=|<|>|=|\\^|~)\\s+"), "$1")
        if (cargo) s = s.replace(",", " ")
        val hyphen = Regex("^(\\S+)\\s+-\\s+(\\S+)$").matchEntire(s)
        if (!cargo && hyphen != null) {
            val (lo, hi) = hyphen.destructured
            val h = parsePartial(hi)
            listOfNotNull(RangeComparator(">=", parsePartial(lo).floor()), if (h.major == null) null else if (h.full) RangeComparator("<=", h.floor()) else nextAfter(h))
        } else {
            s.split(Regex("\\s+")).filter { it.isNotEmpty() }.ifEmpty { listOf("*") }.flatMap { desugar(it, cargo) }
        }
    }
}

/** Null when [v] satisfies [range]; otherwise why not, from the set that came closest. */
internal fun rangeFailure(v: Semver, range: List<List<RangeComparator>>): String? {
    var best: String? = null
    for (set in range) {
        val failed = set.firstOrNull { !it.test(v) }
        // A pre-release only matches a set that names a pre-release of the same major.minor.patch.
        val preBlocked = failed == null && v.pre.isNotEmpty() && set.none { it.v.pre.isNotEmpty() && it.v.core == v.core }
        when {
            failed == null && !preBlocked -> return null
            preBlocked -> best = best ?: "Pre-release excluded: the range names no pre-release of ${v.core.toList().joinToString(".")}"
            else -> best = best ?: "Fails $failed"
        }
    }
    return best ?: "Fails the range"
}

/** Versions sorted by precedence; lines that are not SemVer come back separately. */
internal fun sortVersions(lines: List<String>, descending: Boolean = false): Pair<List<Semver>, List<String>> {
    val parsed = lines.map { it.trim() }.filter { it.isNotEmpty() }.map { it to parseSemver(it) }
    val ok = parsed.mapNotNull { it.second }.sorted()
    return (if (descending) ok.reversed() else ok) to parsed.filter { it.second == null }.map { it.first }
}
