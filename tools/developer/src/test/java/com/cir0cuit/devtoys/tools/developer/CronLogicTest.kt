package com.cir0cuit.devtoys.tools.developer

import com.cronutils.model.time.ExecutionTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class CronLogicTest {
    private val utc = ZoneId.of("UTC")
    private val noon = ZonedDateTime.of(2026, 10, 17, 12, 0, 0, 0, utc) // a Saturday

    private fun next(d: CronDialect, e: String) = ExecutionTime.forCron(parseCron(d, e).first).nextExecution(noon).get()
    private fun reason(block: () -> Any) = runCatching(block).exceptionOrNull()?.message.orEmpty()

    @Test fun dialectsParse() {
        assertEquals(ZonedDateTime.of(2026, 10, 19, 9, 0, 0, 0, utc), next(CronDialect.UNIX, "0 9 * * MON-FRI"))
        assertEquals(ZonedDateTime.of(2026, 10, 18, 0, 0, 0, 0, utc), next(CronDialect.UNIX, "@daily"))
        assertEquals(ZonedDateTime.of(2026, 10, 18, 0, 0, 0, 0, utc), next(CronDialect.SPRING, "@daily"))
        assertEquals(ZonedDateTime.of(2026, 10, 31, 9, 0, 0, 0, utc), next(CronDialect.QUARTZ, "0 0 9 L * ?"))
        assertEquals(ZonedDateTime.of(2026, 10, 17, 12, 5, 0, 0, utc), next(CronDialect.AWS, "cron(0/5 * ? * * *)"))
        assertEquals(ZonedDateTime.of(2026, 10, 19, 9, 0, 0, 0, utc), next(CronDialect.CRON4J, "0 9 * * MON-FRI"))
        assertTrue(runCatching { parseCron(CronDialect.UNIX, "0 0 9 * * *") }.isFailure)
    }

    @Test fun builderAddsQuestionMarkForQuartz() {
        assertEquals("0 0 9 * * ?", buildCron(CronDialect.QUARTZ, "0", "0", "9", "*", "*", "*"))
        assertEquals("0 0 9 ? * MON-FRI", buildCron(CronDialect.QUARTZ, "0", "0", "9", "*", "*", "MON-FRI"))
        assertEquals("cron(0 9 * * ? *)", buildCron(CronDialect.AWS, "0", "0", "9", "*", "*", "*"))
        assertEquals("0 9 * * MON-FRI", buildCron(CronDialect.UNIX, "0", "0", "9", "*", "*", "MON-FRI"))
        parseCron(CronDialect.QUARTZ, buildCron(CronDialect.QUARTZ, "0", "*/5", "*", "*", "*", "*"))
        parseCron(CronDialect.AWS, buildCron(CronDialect.AWS, "0", "*/5", "*", "*", "*", "SAT,SUN"))
    }

    @Test fun convertsToUnixFields() {
        assertEquals("0 9 * * 1-5", toUnixFields(CronDialect.QUARTZ, "0 0 9 ? * 2-6").joinToString(" "))
        assertEquals("*/5 * * * *", toUnixFields(CronDialect.AWS, "cron(0/5 * ? * * *)").joinToString(" "))
        assertEquals("10-59/15 * * * *", toUnixFields(CronDialect.QUARTZ, "0 10/15 * * * ?").joinToString(" "))
        assertEquals("0 9 * * MON-FRI", toUnixFields(CronDialect.SPRING, "0 0 9 * * MON-FRI").joinToString(" "))
        assertEquals("0 0 * * *", toUnixFields(CronDialect.UNIX, "@daily").joinToString(" "))
        assertTrue(reason { toUnixFields(CronDialect.SPRING, "*/5 * * * * *") }.contains("second"))
        assertTrue(reason { toUnixFields(CronDialect.QUARTZ, "0 0 9 L * ?") }.contains("L, W or #"))
        assertTrue(reason { toUnixFields(CronDialect.QUARTZ, "0 0 9 ? * 6#3") }.contains("L, W or #"))
        assertTrue(reason { toUnixFields(CronDialect.QUARTZ, "0 0 9 ? * * 2027") }.contains("year"))
    }

    @Test fun snippets() {
        val ams = ZoneId.of("Europe/Amsterdam")
        assertEquals("CRON_TZ=Europe/Amsterdam\n*/5 * * * * /path/to/command", cronSnippet(CronTarget.CRONTAB, CronDialect.UNIX, "*/5 * * * *", ams))
        assertTrue(cronSnippet(CronTarget.KUBERNETES, CronDialect.UNIX, "*/5 * * * *", ams).contains("  schedule: \"*/5 * * * *\"\n  timeZone: \"Europe/Amsterdam\""))
        assertTrue(cronSnippet(CronTarget.GITHUB, CronDialect.UNIX, "0 9 * * 1-5", ams).contains("- cron: \"0 9 * * 1-5\""))
        assertEquals("@Scheduled(cron = \"0 0 9 * * 1-5\", zone = \"Europe/Amsterdam\")", cronSnippet(CronTarget.SPRING, CronDialect.UNIX, "0 9 * * 1-5", ams))
        assertEquals("[Timer]\nOnCalendar=Mon..Fri *-*-* 9:0/15:00 Europe/Amsterdam\nPersistent=true", cronSnippet(CronTarget.SYSTEMD, CronDialect.UNIX, "*/15 9 * * 1-5", ams))
    }

    @Test fun onCalendarRefusesWhatItCannotExpress() {
        assertEquals("*-*-1,15 0:0:00", toOnCalendar("0 0 1,15 * *".split(" ")))
        assertEquals("*-1..6-* 3:30:00", toOnCalendar("30 3 * 1-6 *".split(" ")))
        assertTrue(reason { toOnCalendar("0 0 1 * 1".split(" ")) }.contains("both"))
        assertTrue(reason { toOnCalendar("0 0 * * MON".split(" ")) }.contains("weekday"))
        assertTrue(reason { toOnCalendar("1-30/5 0 * * *".split(" ")) }.contains("1-30/5"))
    }
}
