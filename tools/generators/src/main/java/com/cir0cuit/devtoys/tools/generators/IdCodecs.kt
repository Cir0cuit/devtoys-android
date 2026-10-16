package com.cir0cuit.devtoys.tools.generators

import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Base64
import java.util.Random
import java.util.UUID
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.sqrt

// ---- Big-number alphabets ----

internal const val CROCKFORD32 = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
internal const val BASE58 = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
internal const val BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
private const val RFC4648_32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

internal fun encodeBase(value: BigInteger, alphabet: String, width: Int = 0): String {
    val base = BigInteger.valueOf(alphabet.length.toLong())
    val sb = StringBuilder()
    var v = value
    while (v.signum() > 0) {
        val qr = v.divideAndRemainder(base)
        sb.append(alphabet[qr[1].toInt()])
        v = qr[0]
    }
    while (sb.length < width) sb.append(alphabet[0])
    return sb.reverse().toString()
}

internal fun decodeBase(text: String, alphabet: String): BigInteger? {
    val base = BigInteger.valueOf(alphabet.length.toLong())
    var v = BigInteger.ZERO
    for (c in text) {
        val d = alphabet.indexOf(c)
        if (d < 0) return null
        v = v.multiply(base).add(BigInteger.valueOf(d.toLong()))
    }
    return v
}

/** Unsigned big-endian bytes of [v], left-padded with zeros to [size]. */
private fun BigInteger.toFixedBytes(size: Int): ByteArray {
    val raw = toByteArray().let { if (it.size > size) it.copyOfRange(it.size - size, it.size) else it }
    return ByteArray(size - raw.size) + raw
}

private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }

// ---- Tokens ----

internal enum class TokenEncoding(val label: String) { HEX("Hex"), BASE64("Base64"), BASE64URL("Base64url"), BASE32("Base32"), BASE58("Base58") }

internal fun base32(bytes: ByteArray): String {
    val sb = StringBuilder()
    var buffer = 0
    var bits = 0
    for (b in bytes) {
        buffer = (buffer shl 8) or (b.toInt() and 0xFF)
        bits += 8
        while (bits >= 5) {
            sb.append(RFC4648_32[(buffer shr (bits - 5)) and 31])
            bits -= 5
        }
    }
    if (bits > 0) sb.append(RFC4648_32[(buffer shl (5 - bits)) and 31])
    return sb.toString()
}

internal fun base58(bytes: ByteArray): String {
    val zeros = bytes.takeWhile { it == 0.toByte() }.size
    val rest = encodeBase(BigInteger(1, bytes), BASE58)
    return "1".repeat(zeros) + rest
}

internal fun encodeToken(bytes: ByteArray, encoding: TokenEncoding): String = when (encoding) {
    TokenEncoding.HEX -> bytes.toHex()
    TokenEncoding.BASE64 -> Base64.getEncoder().encodeToString(bytes)
    TokenEncoding.BASE64URL -> Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    TokenEncoding.BASE32 -> base32(bytes)
    TokenEncoding.BASE58 -> base58(bytes)
}

// ---- UUID ----

internal enum class UuidVersion(val label: String) { V4("v4 random"), V7("v7 time-ordered"), V1("v1 time + node"), V5("v5 name, SHA-1"), V3("v3 name, MD5"), NIL("Nil"), MAX("Max") }

internal val UUID_NAMESPACES = linkedMapOf(
    "DNS" to "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
    "URL" to "6ba7b811-9dad-11d1-80b4-00c04fd430c8",
    "OID" to "6ba7b812-9dad-11d1-80b4-00c04fd430c8",
    "X.500" to "6ba7b814-9dad-11d1-80b4-00c04fd430c8",
)

/** 100-ns intervals between 1582-10-15 (UUID epoch) and 1970-01-01. */
private const val GREGORIAN_OFFSET = 0x01B21DD213814000L

private fun bytesToUuid(b: ByteArray): UUID {
    val bb = ByteBuffer.wrap(b)
    return UUID(bb.long, bb.long)
}

private fun uuidBytes(u: UUID): ByteArray =
    ByteBuffer.allocate(16).putLong(u.mostSignificantBits).putLong(u.leastSignificantBits).array()

private const val RFC_VARIANT_MASK = 0x3FFFFFFFFFFFFFFFL
private const val RFC_VARIANT = Long.MIN_VALUE // 0b10 in the top two bits

internal fun uuidV7(epochMs: Long, rng: Random): UUID {
    val msb = (epochMs shl 16) or 0x7000L or (rng.nextInt(0x1000).toLong())
    val lsb = (rng.nextLong() and RFC_VARIANT_MASK) or RFC_VARIANT
    return UUID(msb, lsb)
}

/** v1 with a random node that has the multicast bit set, so no MAC address leaks (RFC 9562 6.10). */
internal fun uuidV1(epochMs: Long, rng: Random): UUID {
    val ts = epochMs * 10_000 + GREGORIAN_OFFSET + rng.nextInt(10_000)
    val msb = ((ts and 0xFFFFFFFFL) shl 32) or (((ts ushr 32) and 0xFFFF) shl 16) or 0x1000L or ((ts ushr 48) and 0x0FFF)
    val clockSeq = rng.nextInt(0x4000).toLong()
    val node = (rng.nextLong() and 0xFFFFFFFFFFFFL) or 0x010000000000L
    val lsb = RFC_VARIANT or (clockSeq shl 48) or node
    return UUID(msb, lsb)
}

internal fun uuidNameBased(namespace: UUID, name: String, sha1: Boolean): UUID {
    val input = uuidBytes(namespace) + name.toByteArray(Charsets.UTF_8)
    if (!sha1) return UUID.nameUUIDFromBytes(input)
    val h = MessageDigest.getInstance("SHA-1").digest(input).copyOf(16)
    h[6] = ((h[6].toInt() and 0x0F) or 0x50).toByte()
    h[8] = ((h[8].toInt() and 0x3F) or 0x80).toByte()
    return bytesToUuid(h)
}

internal val UUID_NIL = UUID(0, 0)
internal val UUID_MAX = UUID(-1, -1)

internal enum class UuidFormat(val label: String) { STANDARD("Plain"), BRACES("Braces"), URN("URN"), BASE64("Base64") }

internal fun formatUuid(u: UUID, format: UuidFormat, uppercase: Boolean, hyphens: Boolean): String {
    if (format == UuidFormat.BASE64) return Base64.getEncoder().encodeToString(uuidBytes(u))
    var s = u.toString()
    if (!hyphens) s = s.replace("-", "")
    if (uppercase) s = s.uppercase()
    return when (format) {
        UuidFormat.BRACES -> "{$s}"
        UuidFormat.URN -> "urn:uuid:$s"
        else -> s
    }
}

/** Accepts canonical, braced, URN and 32-hex forms. */
internal fun parseUuid(text: String): UUID? {
    val s = text.trim().removePrefix("urn:uuid:").removePrefix("{").removeSuffix("}").replace("-", "")
    if (s.length != 32 || !s.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) return null
    return UUID(java.lang.Long.parseUnsignedLong(s.substring(0, 16), 16), java.lang.Long.parseUnsignedLong(s.substring(16), 16))
}

/** Unix milliseconds embedded in a v1, v6 or v7 UUID, else null. */
internal fun uuidTimestampMs(u: UUID): Long? {
    if (u.variant() != 2) return null
    val msb = u.mostSignificantBits
    val gregorian = when (u.version()) {
        1 -> u.timestamp()
        6 -> ((msb ushr 32) shl 28) or (((msb ushr 16) and 0xFFFF) shl 12) or (msb and 0xFFF)
        7 -> return msb ushr 16
        else -> return null
    }
    return Math.floorDiv(gregorian - GREGORIAN_OFFSET, 10_000L)
}

// ---- ULID ----

internal fun ulid(epochMs: Long, rng: Random): String {
    val rand = ByteArray(10).also(rng::nextBytes)
    val value = BigInteger.valueOf(epochMs).shiftLeft(80).or(BigInteger(1, rand))
    return encodeBase(value, CROCKFORD32, 26)
}

/** ULID as a 128-bit value; Crockford aliases (I, L -> 1, O -> 0) and lowercase are accepted. */
internal fun parseUlid(text: String): BigInteger? {
    val s = text.trim().uppercase().replace('I', '1').replace('L', '1').replace('O', '0')
    if (s.length != 26 || s[0] > '7') return null
    return decodeBase(s, CROCKFORD32)
}

internal fun ulidTimestampMs(text: String): Long? = parseUlid(text)?.shiftRight(80)?.toLong()

// ---- NanoID ----

internal const val NANOID_URL = "_-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
internal const val NANOID_NO_LOOKALIKES = "346789ABCDEFGHJKLMNPQRTUVWXYabcdefghijkmnpqrtwxyz"

/** [Random.nextInt] with a bound is already unbiased (rejection sampling), so no mask is needed. */
internal fun nanoId(size: Int, alphabet: String, rng: Random): String =
    String(CharArray(size) { alphabet[rng.nextInt(alphabet.length)] })

internal fun idEntropyBits(size: Int, alphabetSize: Int): Double = size * log2(alphabetSize.toDouble())

/** Birthday bound: IDs needed for a 1% collision chance. */
internal fun idsForOnePercentCollision(bits: Double): Double = sqrt(2 * ln(1 / 0.99)) * Math.pow(2.0, bits / 2)

// ---- Snowflake (41-bit ms, 10-bit machine, 12-bit sequence) ----

internal const val TWITTER_EPOCH = 1288834974657L
internal const val DISCORD_EPOCH = 1420070400000L

internal fun snowflake(epochMs: Long, customEpoch: Long, machine: Int, sequence: Int): Long =
    ((epochMs - customEpoch) shl 22) or ((machine.toLong() and 0x3FF) shl 12) or (sequence.toLong() and 0xFFF)

internal data class Snowflake(val epochMs: Long, val machine: Int, val sequence: Int)

internal fun decodeSnowflake(id: Long, customEpoch: Long) =
    Snowflake((id ushr 22) + customEpoch, ((id shr 12) and 0x3FF).toInt(), (id and 0xFFF).toInt())

// ---- KSUID (32-bit seconds since 1400000000 + 128 random bits, base62, 27 chars) ----

internal const val KSUID_EPOCH = 1_400_000_000L

internal fun ksuid(epochSeconds: Long, rng: Random): String {
    val bytes = ByteBuffer.allocate(20).putInt((epochSeconds - KSUID_EPOCH).toInt()).put(ByteArray(16).also(rng::nextBytes)).array()
    return encodeBase(BigInteger(1, bytes), BASE62, 27)
}

/** Raw 20 KSUID bytes, or null if [text] is not a KSUID. */
internal fun parseKsuid(text: String): ByteArray? {
    val s = text.trim()
    if (s.length != 27) return null
    val v = decodeBase(s, BASE62) ?: return null
    if (v.bitLength() > 160) return null
    return v.toFixedBytes(20)
}

// ---- Inspector ----

private fun timeRows(ms: Long): List<Pair<String, String>> {
    val instant = Instant.ofEpochMilli(ms)
    return listOf(
        "Time (UTC)" to instant.toString(),
        "Local time" to instant.atZone(ZoneId.systemDefault()).truncatedTo(ChronoUnit.MILLIS).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
        "Unix ms" to ms.toString(),
    )
}

private fun uuidRows(u: UUID, type: String): List<Pair<String, String>> {
    val variant = when (u.variant()) { 0 -> "NCS (legacy)"; 2 -> "RFC 9562"; 6 -> "Microsoft (legacy)"; else -> "Reserved" }
    val version = when {
        u == UUID_NIL -> "Nil"
        u == UUID_MAX -> "Max"
        u.variant() == 2 -> "v${u.version()}"
        else -> "n/a"
    }
    val rows = mutableListOf("Type" to type, "Version" to version, "Variant" to variant)
    uuidTimestampMs(u)?.let { rows += timeRows(it) }
    if (u.variant() == 2 && u.version() == 1) {
        rows += "Clock sequence" to u.clockSequence().toString()
        rows += "Node" to "%012x".format(u.node()).chunked(2).joinToString(":")
    }
    rows += "Canonical" to u.toString()
    rows += "Braced, uppercase" to "{${u.toString().uppercase()}}"
    rows += "Base64" to formatUuid(u, UuidFormat.BASE64, false, true)
    rows += "As ULID" to encodeBase(BigInteger(1, uuidBytes(u)), CROCKFORD32, 26)
    return rows
}

/**
 * Detects the ID type of [input] and returns label/value rows.
 * Throws [IllegalArgumentException] with a user-facing message when nothing matches.
 */
internal fun inspectId(input: String): List<Pair<String, String>> {
    val s = input.trim()
    require(s.isNotEmpty()) { "Paste an ID to inspect it." }
    val isHex = s.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }
    parseUuid(s)?.let { return uuidRows(it, "UUID") }
    if (s.length == 24 && isHex) {
        val seconds = s.substring(0, 8).toLong(16)
        return listOf("Type" to "MongoDB ObjectId") + timeRows(seconds * 1000) +
            listOf("Random / process" to s.substring(8, 18), "Counter" to s.substring(18).toLong(16).toString())
    }
    parseUlid(s)?.let { v ->
        val uuid = bytesToUuid(v.toFixedBytes(16))
        return listOf("Type" to "ULID") + timeRows(v.shiftRight(80).toLong()) +
            listOf("Randomness" to s.trim().uppercase().substring(10), "As UUID" to uuid.toString())
    }
    parseKsuid(s)?.let { b ->
        val seconds = ByteBuffer.wrap(b).int.toLong() and 0xFFFFFFFFL
        return listOf("Type" to "KSUID") + timeRows((seconds + KSUID_EPOCH) * 1000) + listOf("Payload" to b.copyOfRange(4, 20).toHex())
    }
    if (s.length in 15..19 && s.all { it.isDigit() }) {
        val id = s.toLongOrNull() ?: throw IllegalArgumentException("$s is too large for a 64-bit Snowflake ID.")
        val d = decodeSnowflake(id, DISCORD_EPOCH)
        val t = decodeSnowflake(id, TWITTER_EPOCH)
        return listOf(
            "Type" to "Snowflake",
            "Discord time (UTC)" to Instant.ofEpochMilli(d.epochMs).toString(),
            "Twitter / X time (UTC)" to Instant.ofEpochMilli(t.epochMs).toString(),
            "Machine ID" to "${d.machine} (high 5 bits ${d.machine shr 5}, low 5 bits ${d.machine and 31})",
            "Sequence" to d.sequence.toString(),
        )
    }
    throw IllegalArgumentException(
        "Not a recognized ID. Paste a UUID (32 hex digits), ULID (26 chars), KSUID (27 chars), ObjectId (24 hex digits) or Snowflake (15-19 digits)."
    )
}
