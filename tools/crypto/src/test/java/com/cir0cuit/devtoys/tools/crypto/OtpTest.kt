package com.cir0cuit.devtoys.tools.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Vectors from RFC 4226 Appendix D, RFC 6238 Appendix B and RFC 4648 section 10. */
class OtpTest {

    private val sha1Seed = "12345678901234567890".toByteArray()
    private val sha256Seed = "12345678901234567890123456789012".toByteArray()
    private val sha512Seed = "1234567890123456789012345678901234567890123456789012345678901234".toByteArray()

    @Test
    fun rfc4226Hotp() {
        val expected = listOf("755224", "287082", "359152", "969429", "338314", "254676", "287922", "162583", "399871", "520489")
        expected.forEachIndexed { i, code -> assertEquals(code, hotp(sha1Seed, i.toLong())) }
    }

    @Test
    fun rfc6238Totp() {
        fun totp(seed: ByteArray, t: Long, algo: String) = hotp(seed, totpCounter(t, 30), algo, 8)
        assertEquals("94287082", totp(sha1Seed, 59, "SHA1"))
        assertEquals("46119246", totp(sha256Seed, 59, "SHA256"))
        assertEquals("90693936", totp(sha512Seed, 59, "SHA512"))
        assertEquals("07081804", totp(sha1Seed, 1111111109, "SHA1"))
        assertEquals("68084774", totp(sha256Seed, 1111111109, "SHA256"))
        assertEquals("25091201", totp(sha512Seed, 1111111109, "SHA512"))
        assertEquals("65353130", totp(sha1Seed, 20000000000, "SHA1"))
        assertEquals("77737706", totp(sha256Seed, 20000000000, "SHA256"))
        assertEquals("47863826", totp(sha512Seed, 20000000000, "SHA512"))
    }

    @Test
    fun base32Rfc4648() {
        assertEquals("MZXW6YTBOI", base32Encode("foobar".toByteArray()))
        assertEquals("MZXW6", base32Encode("foo".toByteArray()))
        assertArrayEquals("foobar".toByteArray(), base32Decode("MZXW6YTBOI======"))
        assertArrayEquals("foobar".toByteArray(), base32Decode("mzxw 6ytb oi"))
        assertArrayEquals(sha1Seed, base32Decode(base32Encode(sha1Seed)))
        assertEquals(32, randomBase32Secret().length)
    }

    @Test(expected = IllegalArgumentException::class)
    fun base32RejectsInvalidCharacters() {
        base32Decode("ABC1")
    }

    @Test
    fun matchOffsetChecksAdjacentSteps() {
        assertEquals(0, otpMatchOffset("287082", sha1Seed, 1, "SHA1", 6))
        assertEquals(-1, otpMatchOffset("755224", sha1Seed, 1, "SHA1", 6))
        assertEquals(1, otpMatchOffset("359152", sha1Seed, 1, "SHA1", 6))
        assertNull(otpMatchOffset("969429", sha1Seed, 1, "SHA1", 6))
        assertNull(otpMatchOffset("000000", sha1Seed, 0, "SHA1", 6))
    }

    @Test
    fun secretOfTypedUriDependsOnlyOnTheWholeField() {
        val uri = "otpauth://hotp/ACME:alice?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ&counter=0"
        // Every prefix a user passes through while typing must not throw.
        for (n in 1..uri.length) otpSecretOf(uri.take(n))
        assertEquals("", otpSecretOf("otpauth://hotp/ACME:alice?secret="))
        assertEquals("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ", otpSecretOf(uri))
        assertEquals("jbswy3dp", otpSecretOf("jbswy3dp"))
    }

    @Test
    fun parsesOtpAuthUri() {
        val p = parseOtpAuthUri("otpauth://totp/ACME%20Co:john.doe@email.com?secret=HXDMVJECJJWSRB3HWIZR4IFUGFTMXBOZ&issuer=ACME%20Co&algorithm=SHA256&digits=8&period=60")
        assertEquals(OtpParams("totp", "HXDMVJECJJWSRB3HWIZR4IFUGFTMXBOZ", "ACME Co", "john.doe@email.com", "SHA256", 8, 60, 0), p)
        val h = parseOtpAuthUri("otpauth://hotp/alice?secret=jbswy3dpehpk3pxp&counter=5")
        assertEquals(OtpParams("hotp", "JBSWY3DPEHPK3PXP", "", "alice", "SHA1", 6, 30, 5), h)
    }

    @Test
    fun buildRoundTripsThroughParse() {
        val p = OtpParams("totp", "JBSWY3DPEHPK3PXP", "My Co", "bob@example.com", "SHA512", 8, 30, 0)
        val uri = buildOtpAuthUri(p)
        assertEquals("otpauth://totp/My%20Co:bob%40example.com?secret=JBSWY3DPEHPK3PXP&issuer=My%20Co&algorithm=SHA512&digits=8&period=30", uri)
        assertEquals(p, parseOtpAuthUri(uri))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUriWithoutSecret() {
        parseOtpAuthUri("otpauth://totp/alice?issuer=x")
    }
}
