package com.cir0cuit.devtoys.tools.developer

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * Epochs a number can be read in. Linear ones are [origin] Unix seconds plus value x [unit] seconds;
 * Excel and FAT/DOS are wall-clock values and are read in the chosen zone.
 */
internal enum class Epoch(val label: String, val origin: Long = 0, unitExp: Int = 0) {
    UNIX_S("Unix seconds"),
    UNIX_MS("Unix milliseconds", unitExp = -3),
    UNIX_US("Unix microseconds", unitExp = -6),
    UNIX_NS("Unix nanoseconds", unitExp = -9),
    DOTNET(".NET ticks", -62_135_596_800, -7),
    FILETIME("Windows FILETIME / LDAP", -11_644_473_600, -7),
    WEBKIT("WebKit / Chrome", -11_644_473_600, -6),
    COCOA("Cocoa / Core Data", 978_307_200),
    HFS("HFS+", -2_082_844_800),
    /** GPS runs 18 s ahead of UTC (leap seconds since 1980, valid since 2017). */
    GPS("GPS (18 s leap offset)", 315_964_800 - 18),
    EXCEL_1900("Excel serial, 1900"),
    EXCEL_1904("Excel serial, 1904"),
    DOS("FAT / DOS packed");

    val unit: BigDecimal = BigDecimal.ONE.scaleByPowerOfTen(unitExp)
}

private const val DAY_SECONDS = 86_400L
private val EXCEL_1900_BASE = LocalDateTime.of(1899, 12, 30, 0, 0)
private val EXCEL_1904_BASE = LocalDateTime.of(1904, 1, 1, 0, 0)

private fun Instant.unixSeconds() = BigDecimal.valueOf(epochSecond).add(BigDecimal.valueOf(nano.toLong(), 9))

private fun instantOfSeconds(s: BigDecimal): Instant {
    val sec = s.setScale(0, RoundingMode.FLOOR)
    return Instant.ofEpochSecond(sec.longValueExact(), s.subtract(sec).movePointRight(9).setScale(0, RoundingMode.FLOOR).toLong())
}

private fun excelBase(e: Epoch) = if (e == Epoch.EXCEL_1900) EXCEL_1900_BASE else EXCEL_1904_BASE

/** Reads [text] as a value of [epoch]; null when it is not a number or out of range. FAT/DOS also accepts 0x hex. */
internal fun fromEpoch(epoch: Epoch, text: String, zone: ZoneId): Instant? = runCatching {
    val s = text.trim().replace("_", "")
    when (epoch) {
        Epoch.DOS -> {
            val v = if (s.startsWith("0x", true)) s.drop(2).toLong(16) else s.toLong()
            require(v in 0..0xFFFF_FFFFL)
            val date = (v ushr 16).toInt()
            val time = (v and 0xFFFF).toInt()
            LocalDateTime.of(1980 + (date ushr 9), (date ushr 5) and 15, date and 31, time ushr 11, (time ushr 5) and 63, (time and 31) * 2)
                .atZone(zone).toInstant()
        }
        Epoch.EXCEL_1900, Epoch.EXCEL_1904 -> {
            var days = BigDecimal(s)
            // Excel counts a 29 Feb 1900 that never existed, so serials before 61 are one day early.
            if (epoch == Epoch.EXCEL_1900 && days < BigDecimal(61)) days = days.add(BigDecimal.ONE)
            val secs = days.multiply(BigDecimal.valueOf(DAY_SECONDS))
            val whole = secs.setScale(0, RoundingMode.FLOOR)
            excelBase(epoch).plusSeconds(whole.longValueExact())
                .plusNanos(secs.subtract(whole).movePointRight(9).setScale(0, RoundingMode.HALF_UP).toLong())
                .atZone(zone).toInstant()
        }
        else -> instantOfSeconds(BigDecimal.valueOf(epoch.origin).add(BigDecimal(s).multiply(epoch.unit)))
    }
}.getOrNull()

/** [t] written in [epoch]; throws when out of range for it. Sub-second units are whole numbers. */
internal fun toEpoch(epoch: Epoch, t: Instant, zone: ZoneId): String = when (epoch) {
    Epoch.DOS -> {
        val z = t.atZone(zone)
        require(z.year in 1980..2107)
        val date = ((z.year - 1980) shl 9) or (z.monthValue shl 5) or z.dayOfMonth
        val time = (z.hour shl 11) or (z.minute shl 5) or (z.second / 2)
        "0x%08X".format((date.toLong() shl 16) or time.toLong())
    }
    Epoch.EXCEL_1900, Epoch.EXCEL_1904 -> {
        val local = t.atZone(zone).toLocalDateTime()
        val secs = BigDecimal.valueOf(ChronoUnit.SECONDS.between(excelBase(epoch), local))
            .add(BigDecimal.valueOf(local.nano.toLong(), 9))
        var days = secs.divide(BigDecimal.valueOf(DAY_SECONDS), 8, RoundingMode.HALF_UP)
        if (epoch == Epoch.EXCEL_1900 && days < BigDecimal(61)) days = days.subtract(BigDecimal.ONE)
        require(days.signum() >= 0)
        days.stripTrailingZeros().toPlainString()
    }
    else -> {
        val v = t.unixSeconds().subtract(BigDecimal.valueOf(epoch.origin)).divide(epoch.unit)
        (if (epoch.unit < BigDecimal.ONE) v.setScale(0, RoundingMode.FLOOR) else v.stripTrailingZeros()).toPlainString()
    }
}

/** Parses an ISO 8601 duration with calendar and clock parts, like P1Y2M3DT4H5M, PT90M, -P1D or P2W. */
internal fun parseIsoDuration(text: String): Pair<Period, Duration>? = runCatching {
    var s = text.trim().uppercase()
    val negative = s.startsWith("-")
    s = s.removePrefix("-").removePrefix("+")
    require(s.startsWith("P") && s.length > 1)
    val t = s.indexOf('T')
    val datePart = if (t < 0) s else s.substring(0, t)
    val period = if (datePart == "P") Period.ZERO else Period.parse(datePart)
    val duration = if (t < 0) Duration.ZERO else Duration.parse("P" + s.substring(t))
    if (negative) period.negated() to duration.negated() else period to duration
}.getOrNull()

internal fun addIsoDuration(t: Instant, zone: ZoneId, d: Pair<Period, Duration>): Instant =
    t.atZone(zone).plus(d.first).plus(d.second).toInstant()

/** "1 y 2 mo 3 d 4 h 5 min 6 s" from [start] to [end] (start <= end), calendar-aware in their zone. */
internal fun calendarDiff(start: ZonedDateTime, end: ZonedDateTime): String {
    var cursor = start
    val parts = listOf(ChronoUnit.YEARS to "y", ChronoUnit.MONTHS to "mo", ChronoUnit.DAYS to "d", ChronoUnit.HOURS to "h", ChronoUnit.MINUTES to "min", ChronoUnit.SECONDS to "s")
        .mapNotNull { (unit, name) ->
            val n = cursor.until(end, unit)
            cursor = cursor.plus(n, unit)
            if (n == 0L) null else "$n $name"
        }
    return parts.joinToString(" ").ifEmpty { "0 s" }
}

/** Monday-to-Friday days in [from, to). */
internal fun businessDays(from: LocalDate, to: LocalDate): Long {
    val days = ChronoUnit.DAYS.between(from, to)
    if (days <= 0) return 0
    var n = days / 7 * 5
    var d = from.plusDays(days / 7 * 7)
    while (d < to) {
        if (d.dayOfWeek.value <= 5) n++
        d = d.plusDays(1)
    }
    return n
}

private fun decimal(v: Double) = BigDecimal(v).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

/** Labeled rows for A - B. */
internal fun dateDiffRows(a: Instant, b: Instant, zone: ZoneId): List<Pair<String, String>> {
    val d = Duration.between(b, a)
    val sign = if (d.isNegative) "-" else ""
    val (start, end) = if (d.isNegative) a to b else b to a
    val secs = d.seconds + if (d.isNegative && d.nano > 0) 1 else 0 // truncate toward zero
    return listOf(
        "Duration (ISO 8601)" to d.toString(),
        "Calendar difference" to sign + calendarDiff(start.atZone(zone), end.atZone(zone)),
        "Total seconds" to secs.toString(),
        "Total hours" to decimal(d.toMillis() / 3_600_000.0),
        "Total days" to decimal(d.toMillis() / 86_400_000.0),
        "Business days (Mon-Fri)" to sign + businessDays(start.atZone(zone).toLocalDate(), end.atZone(zone).toLocalDate())
    )
}
