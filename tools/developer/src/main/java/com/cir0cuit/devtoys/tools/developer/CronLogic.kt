package com.cir0cuit.devtoys.tools.developer

import com.cronutils.descriptor.CronDescriptor
import com.cronutils.model.Cron
import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.model.field.CronFieldName
import com.cronutils.parser.CronParser
import java.time.ZoneId
import java.util.Locale

internal enum class CronDialect(val label: String, val type: CronType, val hasSeconds: Boolean, val hint: String) {
    UNIX("Unix", CronType.UNIX, false, "5 fields (minute hour day month weekday), like */5 * * * * or @daily"),
    SPRING("Spring", CronType.SPRING53, true, "6 fields (second minute hour day month weekday), like 0 */5 * * * *"),
    QUARTZ("Quartz", CronType.QUARTZ, true, "6 or 7 fields (second minute hour day month weekday [year]) with ? in day or weekday, like 0 0/5 * ? * *"),
    CRON4J("cron4j", CronType.CRON4J, false, "5 fields (minute hour day month weekday), like */5 * * * *"),
    AWS("AWS EventBridge", CronType.QUARTZ, false, "cron(minute hour day month weekday year) with ? in day or weekday, like cron(0/5 * ? * * *)")
}

private val UNIX_MACROS = mapOf(
    "@yearly" to "0 0 1 1 *", "@annually" to "0 0 1 1 *", "@monthly" to "0 0 1 * *", "@weekly" to "0 0 * * 0",
    "@daily" to "0 0 * * *", "@midnight" to "0 0 * * *", "@hourly" to "0 * * * *"
)

/** The expression cron-utils parses for [dialect]: Unix macros expanded, the AWS cron(...) wrapper stripped. */
internal fun normalizeCron(dialect: CronDialect, expr: String): String {
    val e = expr.trim()
    return when (dialect) {
        CronDialect.UNIX -> UNIX_MACROS[e.lowercase()] ?: e
        CronDialect.AWS -> "0 " + e.removePrefix("cron(").removeSuffix(")").trim()
        else -> e
    }
}

/** Parsed cron and its English description; throws when invalid. "@reboot" has no schedule and is rejected. */
internal fun parseCron(dialect: CronDialect, expr: String): Pair<Cron, String> {
    val cron = CronParser(CronDefinitionBuilder.instanceDefinitionFor(dialect.type)).parse(normalizeCron(dialect, expr))
    return cron to CronDescriptor.instance(Locale.ENGLISH).describe(cron)
}

/** A number outside its field's range; [field] matches the Builder field label. */
internal class CronFieldError(val field: String, val message: String)

private val FIELD_LABELS = mapOf(
    CronFieldName.SECOND to "Second", CronFieldName.MINUTE to "Minute", CronFieldName.HOUR to "Hour",
    CronFieldName.DAY_OF_MONTH to "Day of month", CronFieldName.MONTH to "Month", CronFieldName.DAY_OF_WEEK to "Day of week",
    CronFieldName.YEAR to "Year", CronFieldName.DAY_OF_YEAR to "Day of year"
)

/** The first number outside the range [dialect] allows for its field, or null when every number is in range. */
internal fun cronRangeError(dialect: CronDialect, expr: String): CronFieldError? {
    val defs = CronDefinitionBuilder.instanceDefinitionFor(dialect.type).fieldDefinitions.sortedBy { it.fieldName.order }
    val fields = normalizeCron(dialect, expr).split(Regex("\\s+"))
    fields.zip(defs).forEachIndexed { i, (value, def) ->
        // AWS has no seconds: the leading 0 comes from normalizeCron.
        if (dialect == CronDialect.AWS && i == 0) return@forEachIndexed
        val range = def.constraints.startRange..def.constraints.endRange
        val label = FIELD_LABELS.getValue(def.fieldName)
        // Values only: the step after / and the nth after # are not field values.
        val bad = value.split(",").flatMap { it.substringBefore('/').substringBefore('#').split('-') }
            .mapNotNull { it.toIntOrNull() }.firstOrNull { it !in range }
        if (bad != null) return CronFieldError(label, "$label $bad is out of range. ${dialect.label} allows ${range.first}-${range.last}.")
    }
    return null
}

/**
 * Builder fields joined for [dialect]. Quartz-style dialects need ? in day or weekday, so a * there
 * becomes ? on the side that is not set.
 */
internal fun buildCron(dialect: CronDialect, sec: String, min: String, hour: String, day: String, month: String, weekday: String): String {
    var d = day
    var w = weekday
    if (dialect == CronDialect.QUARTZ || dialect == CronDialect.AWS) {
        if (w == "*" || w == "?") {
            w = "?"
            if (d == "?") d = "*"
        } else if (d == "*") d = "?"
    }
    return when (dialect) {
        CronDialect.UNIX, CronDialect.CRON4J -> "$min $hour $d $month $w"
        CronDialect.AWS -> "cron($min $hour $d $month $w *)"
        else -> "$sec $min $hour $d $month $w"
    }
}

/**
 * The schedule as 5 Unix fields; throws with a reason when it can't be expressed
 * (seconds other than 0, a fixed year, or Quartz-only L, W and #).
 */
internal fun toUnixFields(dialect: CronDialect, expr: String): List<String> {
    var f = normalizeCron(dialect, expr).split(Regex("\\s+"))
    if (dialect == CronDialect.UNIX || dialect == CronDialect.CRON4J) {
        require(f.size == 5) { "Needs 5 fields." }
        return f
    }
    require(f.size in 6..7) { "Needs 6 or 7 fields." }
    require(f[0] == "0") { "Runs at second ${f[0]}; Unix cron has minute precision." }
    if (f.size == 7) {
        require(f[6] == "*" || f[6] == "?") { "Fixed to year ${f[6]}; Unix cron has no year field." }
        f = f.dropLast(1)
    }
    f = f.drop(1).map { if (it == "?") "*" else it }
    require(f.none { Regex("[LW#]").containsMatchIn(it.uppercase().replace(NAMES, "")) }) { "Uses L, W or #, which only Quartz supports." }
    // Quartz allows a/n steps; Unix needs */n or a-max/n.
    f = f.mapIndexed { i, v ->
        if (i == 4) v else v.split(",").joinToString(",") { p ->
            Regex("(\\d+)/(\\d+)").matchEntire(p)?.let { m ->
                val (start, step) = m.destructured
                if (start == FIELD_RANGES[i].first) "*/$step" else "$start-${FIELD_RANGES[i].second}/$step"
            } ?: p
        }
    }
    // Quartz numbers weekdays 1-7 from Sunday; Unix uses 0-6.
    if (dialect != CronDialect.SPRING) f = f.dropLast(1) + Regex("(?<!/)\\b\\d+").replace(f[4]) { (it.value.toInt() - 1).toString() }
    return f
}

private val NAMES = Regex("MON|TUE|WED|THU|FRI|SAT|SUN|JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC")

/** Lowest and highest value of minute, hour, day and month. */
private val FIELD_RANGES = listOf("0" to "59", "0" to "23", "1" to "31", "1" to "12")

internal enum class CronTarget(val label: String) {
    CRONTAB("crontab line"), KUBERNETES("Kubernetes CronJob"), GITHUB("GitHub Actions"), SPRING("Spring @Scheduled"), SYSTEMD("systemd OnCalendar")
}

/** Copy-ready snippet for [target]; throws with a reason when the schedule can't be converted. */
internal fun cronSnippet(target: CronTarget, dialect: CronDialect, expr: String, zone: ZoneId): String {
    if (target == CronTarget.SPRING) {
        val six = if (dialect == CronDialect.SPRING) expr.trim() else "0 " + toUnixFields(dialect, expr).joinToString(" ")
        return "@Scheduled(cron = \"$six\", zone = \"${zone.id}\")"
    }
    val five = toUnixFields(dialect, expr).joinToString(" ")
    return when (target) {
        CronTarget.CRONTAB -> "CRON_TZ=${zone.id}\n$five /path/to/command"
        CronTarget.KUBERNETES -> """
            |apiVersion: batch/v1
            |kind: CronJob
            |metadata:
            |  name: my-job
            |spec:
            |  schedule: "$five"
            |  timeZone: "${zone.id}"
            |  jobTemplate:
            |    spec:
            |      template:
            |        spec:
            |          containers:
            |            - name: my-job
            |              image: busybox
            |              command: ["/bin/sh", "-c", "echo hello"]
            |          restartPolicy: OnFailure
        """.trimMargin()
        CronTarget.GITHUB -> "on:\n  schedule:\n    # GitHub runs schedules in UTC.\n    - cron: \"$five\""
        CronTarget.SYSTEMD -> "[Timer]\nOnCalendar=${toOnCalendar(five.split(" "))} ${zone.id}\nPersistent=true"
        CronTarget.SPRING -> error("handled above")
    }
}

private val SYSTEMD_DAYS = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

/** Simple cases only: *, numbers, lists, ranges and * /n steps. */
internal fun toOnCalendar(f: List<String>): String {
    val (min, hour, day, month, weekday) = f
    require(day == "*" || weekday == "*") { "Sets both day and weekday; cron matches either, systemd needs both." }
    fun field(v: String, stepStart: Int): String = v.split(",").joinToString(",") { part ->
        when {
            part == "*" -> "*"
            Regex("\\*/\\d+").matches(part) -> "$stepStart/${part.substringAfter('/')}"
            Regex("\\d+(-\\d+)?").matches(part) -> part.replace("-", "..")
            else -> throw IllegalArgumentException("Uses $part, which this conversion does not handle.")
        }
    }
    val days = if (weekday == "*") "" else weekday.split(",").joinToString(",") { part ->
        val r = part.split("-")
        require(r.size <= 2 && r.all { Regex("[0-7]").matches(it) }) { "Uses weekday $part; use numbers 0-7." }
        r.joinToString("..") { SYSTEMD_DAYS[it.toInt()] }
    } + " "
    return "$days*-${field(month, 1)}-${field(day, 1)} ${field(hour, 0)}:${field(min, 0)}:00"
}
