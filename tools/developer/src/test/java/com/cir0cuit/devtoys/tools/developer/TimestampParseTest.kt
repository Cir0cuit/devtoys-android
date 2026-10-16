package com.cir0cuit.devtoys.tools.developer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class TimestampParseTest {
    private val ams = ZoneId.of("Europe/Amsterdam")
    private fun parse(s: String) = parseTimestamp(s, ams)

    @Test fun detectsUnitsByMagnitude() {
        assertEquals("Unix seconds", parse("1792186953")?.kind)
        assertEquals(Instant.ofEpochSecond(1792186953), parse("1792186953")?.instant)
        assertEquals(Instant.ofEpochMilli(1792186953123), parse("1792186953123")?.instant)
        assertEquals(Instant.ofEpochSecond(1792186953, 123_456_000), parse("1792186953123456")?.instant)
        assertEquals(".NET ticks", parse("621355968000000000")?.kind)
        assertEquals(Instant.EPOCH, parse("621355968000000000")?.instant)
        assertEquals(Instant.ofEpochSecond(1792186953, 1), parse("1792186953000000001")?.instant)
        assertEquals(Instant.ofEpochSecond(-1, 500_000_000), parse("-0.5")?.instant)
    }

    @Test fun readsDateText() {
        assertEquals(Instant.parse("2026-10-16T21:42:33Z"), parse("2026-10-16T23:42:33+02:00")?.instant)
        assertEquals(Instant.parse("2026-10-16T21:42:33Z"), parse("2026-10-16 21:42:33Z")?.instant)
        // No offset: read in the chosen zone, DST from the zone rules (CEST in October, CET in January).
        assertEquals(Instant.parse("2026-10-16T21:42:33Z"), parse("2026-10-16T23:42:33")?.instant)
        assertEquals(Instant.parse("2026-01-16T22:42:33Z"), parse("2026-01-16T23:42:33")?.instant)
        assertEquals(Instant.parse("2026-10-15T22:00:00Z"), parse("2026-10-16")?.instant)
        assertEquals(Instant.parse("2026-10-16T21:42:33Z"), parse("Fri, 16 Oct 2026 21:42:33 GMT")?.instant)
        assertNull(parse("tomorrow"))
    }

    @Test fun relative() {
        val now = Instant.parse("2026-10-16T12:00:00Z")
        assertEquals("now", relativeTime(now, now))
        assertEquals("in 3 hours", relativeTime(now.plusSeconds(3 * 3600 + 5), now))
        assertEquals("2 days ago", relativeTime(now.minusSeconds(2 * 86400), now))
        assertEquals("1 minute ago", relativeTime(now.minusSeconds(61), now))
    }
}
