package com.cir0cuit.devtoys.tools.generators

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger
import java.util.Random
import java.util.UUID

class IdCodecsTest {
    private val now = 1_700_000_000_123L
    private val dns = UUID.fromString(UUID_NAMESPACES.getValue("DNS"))

    @Test
    fun nameBasedUuidsMatchKnownVectors() {
        // Python docs: uuid3/uuid5(NAMESPACE_DNS, 'python.org')
        assertEquals("6fa459ea-ee8a-3ca4-894e-db77e160355e", uuidNameBased(dns, "python.org", sha1 = false).toString())
        assertEquals("886313e1-3b8a-5372-9b90-0c9aee199e5d", uuidNameBased(dns, "python.org", sha1 = true).toString())
    }

    @Test
    fun v7CarriesTimestampVersionAndVariant() {
        val u = uuidV7(now, Random(1))
        assertEquals(7, u.version())
        assertEquals(2, u.variant())
        assertEquals(now, uuidTimestampMs(u))
    }

    @Test
    fun v1CarriesTimestampAndMulticastNode() {
        val u = uuidV1(now, Random(2))
        assertEquals(1, u.version())
        assertEquals(2, u.variant())
        assertEquals(now, uuidTimestampMs(u))
        assertTrue("multicast bit", u.node() and 0x010000000000L != 0L)
    }

    @Test
    fun v6TimestampIsDecoded() {
        // RFC 9562 appendix A.5 example: 1EC9414C-232A-6B00-B3C8-9F6BDECED846 = 2022-02-22T19:22:22Z
        val u = UUID.fromString("1EC9414C-232A-6B00-B3C8-9F6BDECED846")
        assertEquals(java.time.Instant.parse("2022-02-22T19:22:22Z").toEpochMilli(), uuidTimestampMs(u))
    }

    @Test
    fun v7BatchIsSorted() {
        val batch = generateUuids(UuidVersion.V7, 50, null, "", now, Random(3)).map { it.toString() }
        assertEquals(batch.sorted(), batch)
    }

    @Test
    fun uuidFormatsAndParsing() {
        val u = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
        assertEquals("{123E4567E89B12D3A456426614174000}", formatUuid(u, UuidFormat.BRACES, uppercase = true, hyphens = false))
        assertEquals("urn:uuid:123e4567-e89b-12d3-a456-426614174000", formatUuid(u, UuidFormat.URN, false, true))
        for (s in listOf("{123E4567-E89B-12D3-A456-426614174000}", "urn:uuid:123e4567-e89b-12d3-a456-426614174000", "123e4567e89b12d3a456426614174000")) {
            assertEquals(u, parseUuid(s))
        }
        assertNull(parseUuid("123e4567-e89b-12d3-a456-42661417400g"))
        assertEquals(UUID_MAX, parseUuid("ffffffff-ffff-ffff-ffff-ffffffffffff"))
    }

    @Test
    fun ulidKnownVectorAndRoundTrip() {
        assertEquals(1469918176385L, ulidTimestampMs("01ARYZ6S41TSV4RRFFQ69G5FAV"))
        val id = ulid(now, Random(4))
        assertEquals(26, id.length)
        assertTrue(id.all { it in CROCKFORD32 })
        assertEquals(now, ulidTimestampMs(id))
        assertEquals(now, ulidTimestampMs(id.lowercase()))
        assertNull(parseUlid("81ARYZ6S41TSV4RRFFQ69G5FAV")) // overflows 128 bits
    }

    @Test
    fun ksuidKnownVectorAndRoundTrip() {
        // segmentio/ksuid README example
        val b = parseKsuid("0ujtsYcgvSTl8PAuAdqWYSMnLOv")!!
        assertEquals(107608047, java.nio.ByteBuffer.wrap(b).int)
        assertEquals("b5a1cd34b5f99d1154fb6853345c9735", b.copyOfRange(4, 20).joinToString("") { "%02x".format(it) })
        val id = ksuid(now / 1000, Random(5))
        assertEquals(27, id.length)
        assertEquals(now / 1000 - KSUID_EPOCH, java.nio.ByteBuffer.wrap(parseKsuid(id)!!).int.toLong())
    }

    @Test
    fun snowflakeRoundTripAndDiscordExample() {
        val d = decodeSnowflake(175928847299117063L, DISCORD_EPOCH)
        assertEquals(1462015105796L, d.epochMs)
        assertEquals(7, d.sequence)
        val id = snowflake(now, TWITTER_EPOCH, 1023, 4095)
        assertEquals(Snowflake(now, 1023, 4095), decodeSnowflake(id, TWITTER_EPOCH))
    }

    @Test
    fun nanoIdUsesOnlyAlphabet() {
        val id = nanoId(64, "abc", Random(6))
        assertEquals(64, id.length)
        assertTrue(id.all { it in "abc" })
        assertEquals(126.0, idEntropyBits(21, 64), 1e-9)
    }

    @Test
    fun tokenEncodings() {
        val bytes = "foobar".toByteArray()
        assertEquals("666f6f626172", encodeToken(bytes, TokenEncoding.HEX))
        assertEquals("MZXW6YTBOI", encodeToken(bytes, TokenEncoding.BASE32)) // RFC 4648 vector, unpadded
        assertEquals("Zm9vYmFy", encodeToken(bytes, TokenEncoding.BASE64URL))
        assertEquals("1112", base58(byteArrayOf(0, 0, 0, 1))) // leading zeros become '1'
        assertEquals("StV1DL6CwTryKyV", base58("hello world".toByteArray()))
    }

    @Test
    fun baseCodecRoundTrip() {
        val v = BigInteger("123456789012345678901234567890")
        assertEquals(v, decodeBase(encodeBase(v, BASE62), BASE62))
        assertEquals("0000", encodeBase(BigInteger.ZERO, CROCKFORD32, 4))
    }

    @Test
    fun inspectorDetectsEachType() {
        fun type(s: String) = inspectId(s).toMap()["Type"]
        assertEquals("UUID", type("886313e1-3b8a-5372-9b90-0c9aee199e5d"))
        assertEquals("ULID", type("01ARYZ6S41TSV4RRFFQ69G5FAV"))
        assertEquals("KSUID", type("0ujtsYcgvSTl8PAuAdqWYSMnLOv"))
        assertEquals("MongoDB ObjectId", type("507f1f77bcf86cd799439011"))
        assertEquals("Snowflake", type("175928847299117063"))
        assertEquals("2012-10-17T21:13:27Z", inspectId("507f1f77bcf86cd799439011").toMap()["Time (UTC)"])
        assertEquals("v5", inspectId("886313e1-3b8a-5372-9b90-0c9aee199e5d").toMap()["Version"])
        assertNotNull(runCatching { inspectId("hello") }.exceptionOrNull())
    }

    @Test
    fun ulidAndUuidViewsAgree() {
        val rows = inspectId("01ARYZ6S41TSV4RRFFQ69G5FAV").toMap()
        assertEquals("01ARYZ6S41TSV4RRFFQ69G5FAV", inspectId(rows.getValue("As UUID")).toMap()["As ULID"])
    }
}
