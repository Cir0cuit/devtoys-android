package com.cir0cuit.devtoys.tools.developer

import java.math.BigInteger

/** An IPv4 (32-bit) or IPv6 (128-bit) address as an unsigned integer. */
internal data class IpAddr(val value: BigInteger, val v6: Boolean) {
    val bits: Int get() = if (v6) 128 else 32
    override fun toString() = formatIp(this)
}

/** An address with a prefix length. [ip] keeps the host bits as entered. */
internal data class Cidr(val ip: IpAddr, val prefix: Int) {
    val first: IpAddr get() = IpAddr(ip.value.and(prefixMask(ip.bits, prefix)), ip.v6)
    val last: IpAddr get() = IpAddr(first.value.or(hostMask(ip.bits, prefix)), ip.v6)
    val size: BigInteger get() = BigInteger.ONE.shiftLeft(ip.bits - prefix)
    operator fun contains(a: IpAddr) = a.v6 == ip.v6 && a.value >= first.value && a.value <= last.value
    override fun toString() = "${formatIp(first)}/$prefix"
}

internal fun hostMask(bits: Int, prefix: Int): BigInteger = BigInteger.ONE.shiftLeft(bits - prefix).subtract(BigInteger.ONE)

internal fun prefixMask(bits: Int, prefix: Int): BigInteger = hostMask(bits, 0).xor(hostMask(bits, prefix))

internal fun parseIpv4(s: String): Long? {
    val parts = s.split(".")
    if (parts.size != 4) return null
    var v = 0L
    for (p in parts) {
        if (p.length !in 1..3 || !p.all { it in '0'..'9' }) return null
        val n = p.toInt()
        if (n > 255) return null
        v = (v shl 8) or n.toLong()
    }
    return v
}

internal fun parseIp(s: String): IpAddr? {
    val t = s.trim()
    if (t.contains(':')) return parseIpv6Bytes(t)?.let { IpAddr(BigInteger(1, it), true) }
    return parseIpv4(t)?.let { IpAddr(BigInteger.valueOf(it), false) }
}

internal fun hextets(v: BigInteger): List<Int> = (7 downTo 0).map { v.shiftRight(it * 16).toInt() and 0xFFFF }

internal fun formatIp(a: IpAddr): String = if (a.v6) formatIpv6Hextets(hextets(a.value)) else formatIpv4(a.value.toLong())

internal fun formatIpv4(v: Long): String = "${v shr 24 and 0xFF}.${v shr 16 and 0xFF}.${v shr 8 and 0xFF}.${v and 0xFF}"

internal fun expandIpv6(v: BigInteger): String = hextets(v).joinToString(":") { "%04x".format(it) }

/** Parses "a/len", "a/255.255.255.0", "a 255.255.255.0" or a bare address (a host route). */
internal fun parseCidr(text: String): Cidr {
    val t = text.trim()
    require(t.isNotEmpty()) { "Enter an address and prefix, like 192.168.1.0/24." }
    val parts = t.split(Regex("\\s*/\\s*|\\s+"))
    val ip = parseIp(parts[0]) ?: throw IllegalArgumentException("\"${parts[0]}\" is not an IPv4 or IPv6 address. Use a form like 192.168.1.0/24 or 2001:db8::/32.")
    if (parts.size == 1) return Cidr(ip, ip.bits)
    require(parts.size == 2) { "Enter one address and one prefix, like 192.168.1.0/24." }
    val p = parts[1]
    val prefix = p.toIntOrNull() ?: (if (!ip.v6 && p.contains('.')) maskToPrefix(p) else null)
        ?: throw IllegalArgumentException("\"$p\" is not a prefix length or a contiguous netmask like 255.255.255.0.")
    require(prefix in 0..ip.bits) { "The prefix length must be from 0 to ${ip.bits}." }
    return Cidr(ip, prefix)
}

/** Prefix length of a dotted netmask, or null when the mask is not contiguous. */
internal fun maskToPrefix(s: String): Int? {
    val m = parseIpv4(s) ?: return null
    val prefix = java.lang.Long.bitCount(m)
    return prefix.takeIf { prefixMask(32, it).toLong() == m }
}

internal fun ipv4Class(v: Long): String = when ((v shr 24).toInt()) {
    in 0..127 -> "A"
    in 128..191 -> "B"
    in 192..223 -> "C"
    in 224..239 -> "D (multicast)"
    else -> "E (reserved)"
}

private val SPECIAL_RANGES: List<Pair<Cidr, String>> by lazy {
    listOf(
        "0.0.0.0/8" to "\"This\" network",
        "10.0.0.0/8" to "Private (RFC 1918)",
        "100.64.0.0/10" to "Shared address space, CGNAT (RFC 6598)",
        "127.0.0.0/8" to "Loopback",
        "169.254.0.0/16" to "Link-local",
        "172.16.0.0/12" to "Private (RFC 1918)",
        "192.0.0.0/24" to "IETF protocol assignments",
        "192.0.2.0/24" to "Documentation (TEST-NET-1)",
        "192.168.0.0/16" to "Private (RFC 1918)",
        "198.18.0.0/15" to "Benchmarking (RFC 2544)",
        "198.51.100.0/24" to "Documentation (TEST-NET-2)",
        "203.0.113.0/24" to "Documentation (TEST-NET-3)",
        "224.0.0.0/4" to "Multicast",
        "255.255.255.255/32" to "Limited broadcast",
        "240.0.0.0/4" to "Reserved",
        "::/128" to "Unspecified",
        "::1/128" to "Loopback",
        "::ffff:0:0/96" to "IPv4-mapped",
        "64:ff9b::/96" to "NAT64 (RFC 6052)",
        "2001::/32" to "Teredo",
        "2001:db8::/32" to "Documentation",
        "2002::/16" to "6to4",
        "fc00::/7" to "Unique local (ULA)",
        "fe80::/10" to "Link-local",
        "ff00::/8" to "Multicast",
        "2000::/3" to "Global unicast",
    ).map { (c, label) -> parseCidr(c) to label }
}

/** Special-purpose block an address belongs to (RFC 6890 and friends), or "Public" / "Reserved". */
internal fun classifyIp(a: IpAddr): String =
    SPECIAL_RANGES.firstOrNull { a in it.first }?.second ?: if (a.v6) "Reserved" else "Public"

/** PTR name of one address. */
internal fun ptrName(a: IpAddr): String =
    if (a.v6) a.value.toString(16).padStart(32, '0').reversed().toList().joinToString(".") + ".ip6.arpa"
    else formatIp(a).split('.').reversed().joinToString(".") + ".in-addr.arpa"

/** Reverse DNS zone covering a network: whole octets (IPv4) or nibbles (IPv6) of the prefix. */
internal fun reverseZone(c: Cidr): String {
    val labels = if (c.ip.v6) c.first.value.toString(16).padStart(32, '0').take(c.prefix / 4).map { it.toString() }
    else formatIp(c.first).split('.').take(c.prefix / 8)
    return (labels.reversed() + if (c.ip.v6) listOf("ip6", "arpa") else listOf("in-addr", "arpa")).joinToString(".")
}

/** Smallest list of CIDR blocks that exactly covers [start]..[end]. */
internal fun rangeToCidrs(start: IpAddr, end: IpAddr): List<Cidr> {
    require(start.v6 == end.v6) { "Start and end must both be IPv4 or both IPv6." }
    require(start.value <= end.value) { "The start address is after the end address. Swap them." }
    val out = mutableListOf<Cidr>()
    var cur = start.value
    while (cur <= end.value) {
        var size = if (cur.signum() == 0) start.bits else cur.lowestSetBit
        val remaining = end.value.subtract(cur).add(BigInteger.ONE)
        while (BigInteger.ONE.shiftLeft(size) > remaining) size--
        out += Cidr(IpAddr(cur, start.v6), start.bits - size)
        cur = cur.add(BigInteger.ONE.shiftLeft(size))
    }
    return out
}

/** Collapses overlapping and adjacent blocks; IPv4 first, then IPv6. */
internal fun mergeCidrs(list: List<Cidr>): List<Cidr> =
    list.groupBy { it.ip.v6 }.toSortedMap().values.flatMap { group ->
        val ranges = group.map { it.first.value to it.last.value }.sortedBy { it.first }
        val merged = mutableListOf<Pair<BigInteger, BigInteger>>()
        for (r in ranges) {
            val prev = merged.lastOrNull()
            if (prev != null && r.first <= prev.second.add(BigInteger.ONE)) {
                merged[merged.size - 1] = prev.first to prev.second.max(r.second)
            } else merged += r
        }
        val v6 = group[0].ip.v6
        merged.flatMap { (a, b) -> rangeToCidrs(IpAddr(a, v6), IpAddr(b, v6)) }
    }

/** One CIDR per line or comma; blank lines and # comments are skipped. */
internal fun parseCidrList(text: String): List<Cidr> =
    text.lines().flatMapIndexed { i, line ->
        line.substringBefore('#').split(',').map { it.trim() }.filter { it.isNotEmpty() }.map { entry ->
            try {
                parseCidr(entry)
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Line ${i + 1}: ${e.message}")
            }
        }
    }

/** One line per tested address: which block it falls in, if any. */
internal fun membership(addresses: String, blocks: List<Cidr>): String =
    addresses.lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n") { s ->
        val a = parseIp(s) ?: return@joinToString "$s: not an IP address"
        val hit = blocks.firstOrNull { a in it }
        if (hit != null) "$s: in ${hit.first}/${hit.prefix}" else "$s: not in any range"
    }

internal fun firstHost(c: Cidr): IpAddr = if (!c.ip.v6 && c.prefix < 31) IpAddr(c.first.value.add(BigInteger.ONE), false) else c.first

internal fun lastHost(c: Cidr): IpAddr = if (!c.ip.v6 && c.prefix < 31) IpAddr(c.last.value.subtract(BigInteger.ONE), false) else c.last

internal fun usableHosts(c: Cidr): BigInteger = if (!c.ip.v6 && c.prefix < 31) c.size.subtract(BigInteger.valueOf(2)) else c.size

internal data class Subnet(val cidr: Cidr, val needed: Int? = null)

/** Splits [c] into /[newPrefix] blocks. Returns at most [limit] of them and the total count. */
internal fun splitByPrefix(c: Cidr, newPrefix: Int, limit: Int): Pair<List<Subnet>, BigInteger> {
    require(newPrefix in c.prefix..c.ip.bits) { "The new prefix must be from ${c.prefix} to ${c.ip.bits}." }
    val total = BigInteger.ONE.shiftLeft(newPrefix - c.prefix)
    val step = BigInteger.ONE.shiftLeft(c.ip.bits - newPrefix)
    val n = total.min(BigInteger.valueOf(limit.toLong())).toInt()
    val list = (0 until n).map { i -> Subnet(Cidr(IpAddr(c.first.value.add(step.multiply(BigInteger.valueOf(i.toLong()))), c.ip.v6), newPrefix)) }
    return list to total
}

/** VLSM: allocates the largest host count first, each block as small as possible. */
internal fun splitByHosts(c: Cidr, needs: List<Int>): List<Subnet> {
    require(needs.isNotEmpty()) { "Enter host counts separated by commas, like 50, 20, 10." }
    var cursor = c.first.value
    return needs.sortedDescending().map { need ->
        val overhead = if (c.ip.v6) 0 else 2
        var hostBits = 0
        while (BigInteger.ONE.shiftLeft(hostBits) < BigInteger.valueOf(need.toLong() + overhead)) hostBits++
        require(hostBits <= c.ip.bits - c.prefix) { "$need hosts do not fit in a /${c.prefix}. Use a shorter prefix." }
        val block = Cidr(IpAddr(cursor, c.ip.v6), c.ip.bits - hostBits)
        require(block.last.value <= c.last.value) { "Not enough room: $need hosts do not fit in what is left of $c." }
        cursor = block.last.value.add(BigInteger.ONE)
        Subnet(block, need)
    }
}

/** Parses a decimal integer or 0x hex as an address: IPv4 when it fits 32 bits, else IPv6. */
internal fun parseIpFlexible(s: String): IpAddr? {
    val t = s.trim()
    parseIp(t)?.let { return it }
    val v = when {
        t.startsWith("0x", ignoreCase = true) -> t.drop(2).takeIf { it.isNotEmpty() && it.all { c -> c.isDigit() || c.lowercaseChar() in 'a'..'f' } }?.let { BigInteger(it, 16) }
        t.isNotEmpty() && t.all { it in '0'..'9' } -> BigInteger(t)
        else -> null
    } ?: return null
    return when {
        v.bitLength() <= 32 -> IpAddr(v, false)
        v.bitLength() <= 128 -> IpAddr(v, true)
        else -> null
    }
}

/** RFC 4193 ULA /48 prefix from a 40-bit global ID. */
internal fun ulaPrefix(globalId: Long): String = formatIpv6Hextets(
    listOf(0xFD00 or (globalId shr 32 and 0xFF).toInt(), (globalId shr 16 and 0xFFFF).toInt(), (globalId and 0xFFFF).toInt(), 0, 0, 0, 0, 0)
) + "/48"

internal fun parseIpv6Bytes(s: String): ByteArray? {
    if (s.endsWith('%')) return null
    var str = s.substringBefore('%')
    val lastColon = str.lastIndexOf(':')
    if (lastColon < 0) return null
    val tail = str.substring(lastColon + 1)
    if (tail.contains('.')) {
        val v = parseIpv4(tail) ?: return null
        str = str.substring(0, lastColon + 1) + java.lang.Long.toHexString(v shr 16) + ":" + java.lang.Long.toHexString(v and 0xFFFF)
    }

    fun groups(part: String): List<Int>? {
        if (part.isEmpty()) return emptyList()
        return part.split(":").map { g ->
            if (g.length !in 1..4 || !g.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) return null
            g.toInt(16)
        }
    }

    val dc = str.indexOf("::")
    val hextets: List<Int> = if (dc >= 0) {
        if (str.lastIndexOf("::") != dc) return null
        val head = groups(str.substring(0, dc)) ?: return null
        val rest = groups(str.substring(dc + 2)) ?: return null
        if (head.size + rest.size > 7) return null
        head + List(8 - head.size - rest.size) { 0 } + rest
    } else {
        groups(str)?.takeIf { it.size == 8 } ?: return null
    }

    val bytes = ByteArray(16)
    hextets.forEachIndexed { i, h ->
        bytes[i * 2] = (h shr 8).toByte()
        bytes[i * 2 + 1] = h.toByte()
    }
    return bytes
}

/** RFC 5952 text form: lowercase, longest run of two or more zero groups compressed. */
internal fun formatIpv6Hextets(hextets: List<Int>): String {
    var bestStart = -1
    var bestLen = 0
    var i = 0
    while (i < hextets.size) {
        if (hextets[i] == 0) {
            var j = i
            while (j < hextets.size && hextets[j] == 0) j++
            if (j - i > bestLen) { bestStart = i; bestLen = j - i }
            i = j
        } else {
            i++
        }
    }
    val hex = hextets.map { Integer.toHexString(it) }
    if (bestLen < 2) return hex.joinToString(":")
    val head = hex.subList(0, bestStart).joinToString(":")
    val tail = hex.subList(bestStart + bestLen, hex.size).joinToString(":")
    return "$head::$tail"
}
