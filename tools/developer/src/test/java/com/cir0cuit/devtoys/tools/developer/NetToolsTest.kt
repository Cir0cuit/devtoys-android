package com.cir0cuit.devtoys.tools.developer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

class NetToolsTest {
    private fun ip(s: String) = parseIp(s)!!
    private fun cidrs(vararg s: String) = s.map { parseCidr(it) }

    // ---- CIDR ----

    @Test fun acceptsDottedMask() {
        assertEquals(parseCidr("192.168.1.0/24"), parseCidr("192.168.1.0 255.255.255.0"))
        assertEquals(24, parseCidr("192.168.1.0/255.255.255.0").prefix)
    }

    @Test fun rejectsNonContiguousMask() {
        assertNull(maskToPrefix("255.0.255.0"))
        assertTrue(runCatching { parseCidr("10.0.0.0/255.0.255.0") }.isFailure)
    }

    @Test fun ipv4Rows() {
        val rows = subnetRows(parseCidr("192.168.1.77/26")).toMap()
        assertEquals("192.168.1.64/26", rows["Network"])
        assertEquals("0.0.0.63", rows["Wildcard mask"])
        assertEquals("0xFFFFFFC0", rows["Netmask hex"])
        assertEquals("192.168.1.127", rows["Broadcast"])
        assertEquals("62", rows["Usable hosts"])
        assertEquals("C", rows["Class"])
        assertEquals("Private (RFC 1918)", rows["Address type"])
        assertEquals("1.168.192.in-addr.arpa", rows["Reverse DNS zone"])
    }

    @Test fun ipv6RowsHaveNoBroadcast() {
        val rows = subnetRows(parseCidr("2001:db8::/48")).toMap()
        assertNull(rows["Broadcast"])
        assertEquals("2001:db8:0:ffff:ffff:ffff:ffff:ffff", rows["Last address"])
        assertEquals("65536", rows["/64 subnets"])
        assertEquals("0.0.0.0.8.b.d.0.1.0.0.2.ip6.arpa", rows["Reverse DNS zone"])
        assertEquals("Documentation", rows["Address type"])
    }

    @Test fun classification() {
        assertEquals("Shared address space, CGNAT (RFC 6598)", classifyIp(ip("100.64.1.1")))
        assertEquals("Link-local", classifyIp(ip("fe80::1")))
        assertEquals("Unique local (ULA)", classifyIp(ip("fd12::1")))
        assertEquals("IPv4-mapped", classifyIp(ip("::ffff:1.2.3.4")))
        assertEquals("Public", classifyIp(ip("8.8.8.8")))
        assertEquals("Limited broadcast", classifyIp(ip("255.255.255.255")))
    }

    @Test fun rangeToCidr() {
        assertEquals(
            listOf("192.168.1.10/31", "192.168.1.12/30", "192.168.1.16/28", "192.168.1.32/27", "192.168.1.64/27", "192.168.1.96/30", "192.168.1.100/32"),
            rangeToCidrs(ip("192.168.1.10"), ip("192.168.1.100")).map { it.toString() }
        )
        assertEquals(listOf("0.0.0.0/0"), rangeToCidrs(ip("0.0.0.0"), ip("255.255.255.255")).map { it.toString() })
        assertEquals(listOf("::/0"), rangeToCidrs(ip("::"), ip("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff")).map { it.toString() })
    }

    @Test fun mergeCollapsesAdjacentAndOverlapping() {
        val merged = mergeCidrs(cidrs("10.0.1.0/24", "10.0.0.0/24", "10.0.2.0/23", "10.0.3.5/32", "2001:db8::/33", "2001:db8:8000::/33"))
        assertEquals(listOf("10.0.0.0/22", "2001:db8::/32"), merged.map { it.toString() })
    }

    @Test fun cidrListReportsLine() {
        val e = runCatching { parseCidrList("10.0.0.0/8\n# note\nnope") }.exceptionOrNull()
        assertTrue(e!!.message!!.startsWith("Line 3:"))
    }

    @Test fun membershipNamesTheMatch() {
        val out = membership("10.1.2.3\n11.0.0.1\nx", cidrs("192.168.0.0/16", "10.0.0.0/8"))
        assertEquals("10.1.2.3: in 10.0.0.0/8\n11.0.0.1: not in any range\nx: not an IP address", out)
    }

    @Test fun splitByPrefixCaps() {
        val (list, total) = splitByPrefix(parseCidr("192.168.1.0/24"), 26, 1024)
        assertEquals(listOf("192.168.1.0/26", "192.168.1.64/26", "192.168.1.128/26", "192.168.1.192/26"), list.map { it.cidr.toString() })
        assertEquals(BigInteger.valueOf(4), total)
        val (capped, big) = splitByPrefix(parseCidr("2001:db8::/48"), 64, 1024)
        assertEquals(1024, capped.size)
        assertEquals(BigInteger.valueOf(65536), big)
    }

    @Test fun vlsmLargestFirst() {
        val out = splitByHosts(parseCidr("192.168.1.0/24"), listOf(10, 50, 20))
        assertEquals(listOf("192.168.1.0/26", "192.168.1.64/27", "192.168.1.96/28"), out.map { it.cidr.toString() })
        assertEquals(listOf(50, 20, 10), out.map { it.needed })
        assertTrue(runCatching { splitByHosts(parseCidr("192.168.1.0/28"), listOf(10, 10)) }.isFailure)
    }

    // ---- IP converter ----

    @Test fun flexibleInput() {
        assertEquals(ip("192.168.1.1"), parseIpFlexible("3232235777"))
        assertEquals(ip("192.168.1.1"), parseIpFlexible("0xC0A80101"))
        assertEquals(ip("::1:0:0"), parseIpFlexible("4294967296"))
        assertNull(parseIpFlexible("0x"))
    }

    @Test fun ipv4ConverterRows() {
        val rows = ipRows(ip("192.168.1.1")).toMap()
        assertEquals("030052000401", rows["Octal"])
        assertEquals("2002:c0a8:101::/48", rows["6to4 prefix"])
        assertEquals("1.1.168.192.in-addr.arpa", rows["Reverse DNS (PTR)"])
    }

    @Test fun ipv6ConverterRows() {
        val rows = ipRows(ip("2002:c0a8:101::1")).toMap()
        assertEquals("192.168.1.1", rows["Embedded IPv4"])
        assertEquals("2002:c0a8:0101:0000:0000:0000:0000:0001", rows["Expanded"])
        assertEquals("1.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.1.0.1.0.8.a.0.c.2.0.0.2.ip6.arpa", rows["Reverse DNS (PTR)"])
    }

    @Test fun ula() {
        assertEquals("fd12:3456:789a::/48", ulaPrefix(0x123456789AL))
        assertEquals("fd00::/48", ulaPrefix(0))
    }

    // ---- MAC ----

    @Test fun ouiParsing() {
        val map = parseOui(sequenceOf("28-6F-B9   (hex)\t\tNokia", "286FB9     (base 16)\t\tNokia Shanghai Bell Co., Ltd.", "\t\t\tCN"))
        assertEquals(mapOf("286FB9" to "Nokia Shanghai Bell Co., Ltd."), map)
    }

    @Test fun extractsMacsFromArpOutput() {
        val text = """
            ? (192.168.1.1) at 00:1a:2b:3c:4d:5e [ether] on wlan0
            192.168.1.20 dev wlan0 lladdr da:a1:19:00:11:22 REACHABLE
              192.168.1.30          00-1A-2B-3C-4D-5F     dynamic
            Gi0/1  001a.2b3c.4d60
            fe80::aa:bb:cc:dd:ee:ff
        """.trimIndent()
        assertEquals(listOf("001A2B3C4D5E", "DAA119001122", "001A2B3C4D5F", "001A2B3C4D60"), macsFromInput(text).macs)
    }

    @Test fun singleMacFormats() {
        assertEquals(listOf("001A2B3C4D5E"), macsFromInput("00 1a 2b 3c 4d 5e").macs)
        assertEquals(listOf("001A2B3C4D5E"), macsFromInput("001A2B3C4D5E").macs)
    }

    @Test fun eui64BothWays() {
        val mac = "001A2B3C4D5E"
        val eui = macToEui64(mac)
        assertEquals("21a:2bff:fe3c:4d5e", eui.joinToString(":") { "%x".format(it) })
        val parsed = macsFromInput("fe80::21a:2bff:fe3c:4d5e")
        assertTrue(parsed.fromIpv6)
        assertEquals(listOf(mac), parsed.macs)
    }

    @Test fun macFlags() {
        assertTrue(isLocalMac("DAA119001122"))
        assertFalse(isLocalMac("001A2B3C4D5E"))
        assertEquals("multicast", macKind("01005E000001"))
        assertEquals("broadcast", macKind("FFFFFFFFFFFF"))
    }

    // ---- chmod ----

    @Test fun symbolicWithSpecialBits() {
        assertEquals("rwsr-xr-x", chmodSymbolic("4755".toInt(8)))
        assertEquals("rwxrwxrwt", chmodSymbolic("1777".toInt(8)))
        assertEquals("rw-r-Sr--", chmodSymbolic("2644".toInt(8)))
        assertEquals("4755", octalMode("4755".toInt(8)))
        assertEquals("644", octalMode("0644".toInt(8)))
    }

    @Test fun parsesLiterals() {
        assertEquals("4755".toInt(8) to null, parseChmodLiteral("rwsr-xr-x"))
        assertEquals("755".toInt(8) to 'd', parseChmodLiteral("drwxr-xr-x"))
        assertEquals("644".toInt(8) to '-', parseChmodLiteral("-rw-r--r--@"))
        assertEquals("1777".toInt(8) to null, parseChmodLiteral("1777"))
        assertNull(parseChmodLiteral("rwxr-xr-q"))
    }

    @Test fun appliesExpressions() {
        val m644 = "644".toInt(8)
        assertEquals("744".toInt(8), applyChmodExpression("u+x", m644))
        assertEquals("600".toInt(8), applyChmodExpression("go-rwx", m644))
        assertEquals("774".toInt(8), applyChmodExpression("u+x,g=u,o=r", m644))
        assertEquals("644".toInt(8), applyChmodExpression("a+X", m644))
        assertEquals("755".toInt(8), applyChmodExpression("a+rX", m644, isDirectory = true))
        assertEquals("4744".toInt(8), applyChmodExpression("u+xs", m644))
        assertEquals("1644".toInt(8), applyChmodExpression("+t", m644))
        assertNull(applyChmodExpression("u", m644))
        assertNull(applyChmodExpression("u+x,", m644))
        assertNull(applyChmodExpression("z+x", m644))
    }

    @Test fun bitGroupsBreakOnlyAfterSeparators() {
        assertEquals("11111111.\u200B11100000", wrapAtSeparators("11111111.11100000"))
        assertEquals("0101:\u200B1010", wrapAtSeparators("0101:1010"))
        assertEquals("192.168.1.1", wrapAtSeparators("192.168.1.1"))
        assertEquals("fe80:\u200B:\u200B1", wrapAtSeparators("fe80::1"))
        assertEquals("2001:\u200B0db8:\u200B0000:\u200B7334", wrapAtSeparators("2001:0db8:0000:7334"))
        assertEquals("1.2.3", wrapAtSeparators("1.2.3"))
    }
}
