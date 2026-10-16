package com.cir0cuit.devtoys.tools.developer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** IPv6 text parsing (RFC 4291 section 2.2, RFC 4007 zone ids) and formatting (RFC 5952). */
class CidrIpv6Test {
    private val tool = CidrCalculatorTool()

    private fun hextets(s: String): List<Int>? = tool.parseIpv6(s)?.let { b ->
        (0 until 8).map { i -> ((b[i * 2].toInt() and 0xFF) shl 8) or (b[i * 2 + 1].toInt() and 0xFF) }
    }

    // ---- parsing: valid forms ----

    @Test fun parsesFullForm() {
        assertEquals(listOf(0x2001, 0xdb8, 0, 0, 8, 0x800, 0x200c, 0x417a), hextets("2001:DB8:0:0:8:800:200C:417A"))
    }

    @Test fun parsesLeadingZeroGroups() {
        assertEquals(listOf(0x2001, 0xdb8, 0, 0, 0, 0, 0, 1), hextets("2001:0db8:0000:0000:0000:0000:0000:0001"))
    }

    @Test fun parsesCompressedForms() {
        assertEquals(listOf(0x2001, 0xdb8, 0, 0, 8, 0x800, 0x200c, 0x417a), hextets("2001:DB8::8:800:200C:417A"))
        assertEquals(listOf(0xff01, 0, 0, 0, 0, 0, 0, 0x101), hextets("FF01::101"))
        assertEquals(listOf(0, 0, 0, 0, 0, 0, 0, 1), hextets("::1"))
        assertEquals(List(8) { 0 }, hextets("::"))
        assertEquals(listOf(1, 0, 0, 0, 0, 0, 0, 0), hextets("1::"))
    }

    @Test fun doubleColonMayStandForASingleGroup() {
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7, 0), hextets("1:2:3:4:5:6:7::"))
        assertEquals(listOf(0, 2, 3, 4, 5, 6, 7, 8), hextets("::2:3:4:5:6:7:8"))
        assertEquals(listOf(1, 2, 3, 0, 5, 6, 7, 8), hextets("1:2:3::5:6:7:8"))
    }

    @Test fun parsesIpv4MappedAndCompatible() {
        assertEquals(listOf(0, 0, 0, 0, 0, 0xffff, 0x8190, 0x340d), hextets("::FFFF:129.144.52.13"))
        assertEquals(listOf(0, 0, 0, 0, 0, 0, 0x0d01, 0x4403), hextets("::13.1.68.3"))
        assertEquals(listOf(0, 0, 0, 0, 0, 0xffff, 0xc000, 0x0280), hextets("0:0:0:0:0:ffff:192.0.2.128"))
    }

    @Test fun stripsZoneId() {
        assertEquals(listOf(0xfe80, 0, 0, 0, 0, 0, 0, 1), hextets("fe80::1%eth0"))
        assertEquals(listOf(0xfe80, 0, 0, 0, 0, 0, 0, 1), hextets("fe80::1%3"))
    }

    @Test fun returnsSixteenBytes() {
        assertEquals(16, tool.parseIpv6("::1")!!.size)
    }

    // ---- parsing: invalid forms ----

    @Test fun rejectsEmptyZoneId() {
        assertNull(tool.parseIpv6("fe80::1%"))
    }

    @Test fun rejectsMultipleDoubleColons() {
        assertNull(tool.parseIpv6("1::2::3"))
        assertNull(tool.parseIpv6(":::"))
    }

    @Test fun rejectsWrongGroupCount() {
        assertNull(tool.parseIpv6("1:2:3:4:5:6:7"))
        assertNull(tool.parseIpv6("1:2:3:4:5:6:7:8:9"))
        assertNull(tool.parseIpv6("1:2:3:4:5:6:7:8::"))
        assertNull(tool.parseIpv6("::1:2:3:4:5:6:7:8"))
    }

    @Test fun rejectsSingleLeadingOrTrailingColon() {
        assertNull(tool.parseIpv6(":1:2:3:4:5:6:7"))
        assertNull(tool.parseIpv6("1:2:3:4:5:6:7:"))
    }

    @Test fun rejectsBadHexGroups() {
        assertNull(tool.parseIpv6("12345::"))
        assertNull(tool.parseIpv6("g::1"))
        assertNull(tool.parseIpv6("2001:db8::-1"))
    }

    @Test fun rejectsBadEmbeddedIpv4() {
        assertNull(tool.parseIpv6("::ffff:1.2.3"))
        assertNull(tool.parseIpv6("::ffff:1.2.3.256"))
        assertNull(tool.parseIpv6("::ffff:1.2.3.4.5"))
        assertNull(tool.parseIpv6("::ffff:1..3.4"))
        assertNull(tool.parseIpv6("::1.2.3.4:5"))
        assertNull(tool.parseIpv6("1:2:3:4:5:6:7:1.2.3.4"))
    }

    @Test fun rejectsNonIpv6() {
        assertNull(tool.parseIpv6("1.2.3.4"))
        assertNull(tool.parseIpv6(""))
        assertNull(tool.parseIpv6("localhost"))
    }

    // ---- formatting (RFC 5952 section 4) ----

    @Test fun formatSuppressesLeadingZerosAndLowercases() {
        assertEquals("2001:db8::abcd:1", tool.formatIpv6(listOf(0x2001, 0x0db8, 0, 0, 0, 0, 0xABCD, 1)))
    }

    @Test fun formatDoesNotCompressSingleZeroGroup() {
        assertEquals("2001:db8:0:1:1:1:1:1", tool.formatIpv6(listOf(0x2001, 0xdb8, 0, 1, 1, 1, 1, 1)))
    }

    @Test fun formatCompressesLongestRun() {
        assertEquals("2001:0:0:1::1", tool.formatIpv6(listOf(0x2001, 0, 0, 1, 0, 0, 0, 1)))
    }

    @Test fun formatCompressesFirstOfEqualRuns() {
        assertEquals("2001:db8::1:0:0:1", tool.formatIpv6(listOf(0x2001, 0xdb8, 0, 0, 1, 0, 0, 1)))
    }

    @Test fun formatEdges() {
        assertEquals("::", tool.formatIpv6(List(8) { 0 }))
        assertEquals("::1", tool.formatIpv6(listOf(0, 0, 0, 0, 0, 0, 0, 1)))
        assertEquals("2001:db8::", tool.formatIpv6(listOf(0x2001, 0xdb8, 0, 0, 0, 0, 0, 0)))
        assertEquals("1:2:3:4:5:6:7:8", tool.formatIpv6(listOf(1, 2, 3, 4, 5, 6, 7, 8)))
        assertEquals("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff", tool.formatIpv6(List(8) { 0xffff }))
    }

    @Test fun parseFormatRoundTrip() {
        for (s in listOf("2001:db8::1", "::", "::1", "fe80::", "2001:db8:0:1:1:1:1:1", "2001:0:0:1::1", "1:2:3:4:5:6:7:8")) {
            val h = hextets(s)
            assertNotNull(s, h)
            assertEquals(s, tool.formatIpv6(h!!))
        }
    }
}
