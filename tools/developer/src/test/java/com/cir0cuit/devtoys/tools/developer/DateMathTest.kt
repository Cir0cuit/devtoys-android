package com.cir0cuit.devtoys.tools.developer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.ZoneOffset

class DateMathTest {
    private val utc: ZoneId = ZoneOffset.UTC
    private val y2k = Instant.parse("2000-01-01T00:00:00Z")

    @Test fun linearEpochsRoundTrip() {
        assertEquals("116444736000000000", toEpoch(Epoch.FILETIME, Instant.EPOCH, utc))
        assertEquals("11644473600000000", toEpoch(Epoch.WEBKIT, Instant.EPOCH, utc))
        assertEquals("621355968000000000", toEpoch(Epoch.DOTNET, Instant.EPOCH, utc))
        assertEquals("0", toEpoch(Epoch.COCOA, Instant.parse("2001-01-01T00:00:00Z"), utc))
        assertEquals("2082844800", toEpoch(Epoch.HFS, Instant.EPOCH, utc))
        assertEquals("1792186953123", toEpoch(Epoch.UNIX_MS, Instant.ofEpochMilli(1792186953123), utc))
        assertEquals(Instant.EPOCH, fromEpoch(Epoch.FILETIME, "116444736000000000", utc))
        assertEquals(Instant.parse("2001-01-01T00:00:00.5Z"), fromEpoch(Epoch.COCOA, "0.5", utc))
        assertEquals(Instant.ofEpochSecond(1792186953, 123_000_000), fromEpoch(Epoch.UNIX_MS, "1792186953123", utc))
        // GPS is 18 s ahead of UTC.
        assertEquals(Instant.parse("2026-01-01T00:00:00Z"), fromEpoch(Epoch.GPS, toEpoch(Epoch.GPS, Instant.parse("2026-01-01T00:00:00Z"), utc), utc))
        assertEquals("18", toEpoch(Epoch.GPS, Instant.parse("1980-01-06T00:00:00Z"), utc))
    }

    @Test fun excelSerials() {
        assertEquals("36526", toEpoch(Epoch.EXCEL_1900, y2k, utc))
        assertEquals("35064", toEpoch(Epoch.EXCEL_1904, y2k, utc))
        assertEquals("36526.5", toEpoch(Epoch.EXCEL_1900, Instant.parse("2000-01-01T12:00:00Z"), utc))
        assertEquals(y2k, fromEpoch(Epoch.EXCEL_1900, "36526", utc))
        // The 1900 leap-year bug: serial 59 is 28 Feb 1900, 61 is 1 Mar 1900, 1 is 1 Jan 1900.
        assertEquals("59", toEpoch(Epoch.EXCEL_1900, Instant.parse("1900-02-28T00:00:00Z"), utc))
        assertEquals("61", toEpoch(Epoch.EXCEL_1900, Instant.parse("1900-03-01T00:00:00Z"), utc))
        assertEquals(Instant.parse("1900-01-01T00:00:00Z"), fromEpoch(Epoch.EXCEL_1900, "1", utc))
        // Wall-clock: read in the chosen zone.
        assertEquals(Instant.parse("1999-12-31T23:00:00Z"), fromEpoch(Epoch.EXCEL_1900, "36526", ZoneId.of("Europe/Amsterdam")))
    }

    @Test fun dosPacked() {
        assertEquals("0x00210000", toEpoch(Epoch.DOS, Instant.parse("1980-01-01T00:00:00Z"), utc))
        val t = Instant.parse("2026-10-17T13:45:58Z")
        assertEquals(t, fromEpoch(Epoch.DOS, toEpoch(Epoch.DOS, t, utc), utc))
        assertNull(fromEpoch(Epoch.DOS, "0x0", utc)) // month 0 is invalid
    }

    @Test fun unreadableValues() {
        assertNull(fromEpoch(Epoch.UNIX_S, "2026-10-17", utc))
        assertNull(fromEpoch(Epoch.FILETIME, "", utc))
    }

    @Test fun isoDurations() {
        assertEquals(Period.ZERO to Duration.ofMinutes(90), parseIsoDuration("PT90M"))
        assertEquals(Period.ofDays(1) to Duration.ofHours(2), parseIsoDuration("p1dt2h"))
        assertEquals(Period.ofDays(-14) to Duration.ZERO, parseIsoDuration("-P2W"))
        assertNull(parseIsoDuration("P"))
        assertNull(parseIsoDuration("PT"))
        assertNull(parseIsoDuration("90 minutes"))
    }

    @Test fun addingIsCalendarAware() {
        val ams = ZoneId.of("Europe/Amsterdam")
        // DST ends 2026-10-25: one calendar day is 25 hours, but PT24H is exactly 24.
        val before = Instant.parse("2026-10-24T10:00:00Z")
        assertEquals(Instant.parse("2026-10-25T11:00:00Z"), addIsoDuration(before, ams, parseIsoDuration("P1D")!!))
        assertEquals(Instant.parse("2026-10-25T10:00:00Z"), addIsoDuration(before, ams, parseIsoDuration("PT24H")!!))
        assertEquals(Instant.parse("2026-02-28T00:00:00Z"), addIsoDuration(Instant.parse("2026-01-31T00:00:00Z"), utc, parseIsoDuration("P1M")!!))
    }

    @Test fun differences() {
        val a = Instant.parse("2026-03-04T05:06:07Z")
        val b = Instant.parse("2025-01-01T00:00:00Z")
        val rows = dateDiffRows(a, b, utc).toMap()
        assertEquals("1 y 2 mo 3 d 5 h 6 min 7 s", rows["Calendar difference"])
        assertEquals("-1 y 2 mo 3 d 5 h 6 min 7 s", dateDiffRows(b, a, utc).toMap()["Calendar difference"])
        assertEquals("0 s", calendarDiff(a.atZone(utc), a.atZone(utc)))
        assertEquals("-90", dateDiffRows(Instant.EPOCH, Instant.ofEpochSecond(90), utc).toMap()["Total seconds"])
        assertEquals("1.5", dateDiffRows(Instant.ofEpochSecond(5400), Instant.EPOCH, utc).toMap()["Total hours"])
    }

    @Test fun businessDaysCountWeekdays() {
        val mon = LocalDate.of(2026, 10, 12)
        assertEquals(5L, businessDays(mon, mon.plusDays(7)))
        assertEquals(10L, businessDays(mon, mon.plusDays(14)))
        assertEquals(0L, businessDays(mon.plusDays(5), mon.plusDays(7))) // Sat, Sun
        assertEquals(1L, businessDays(mon.plusDays(4), mon.plusDays(6))) // Fri, Sat
        assertEquals(0L, businessDays(mon, mon))
    }
}
