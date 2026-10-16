package com.cir0cuit.devtoys.tools.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

/** Vectors from RFC 2202 (HMAC-MD5/SHA-1) and RFC 4231 (HMAC-SHA-256/512). */
class HmacCalculatorTest {

    private val key = "Jefe"
    private val msg = "what do ya want for nothing?"

    @Test
    fun rfc2202HmacMd5TestCase2() {
        assertEquals("750c783e6ab0b503eaa86e310a5db738", computeHmac("HmacMD5", key, msg))
    }

    @Test
    fun rfc2202HmacSha1TestCase2() {
        assertEquals("effcdf6ae5eb2fa2d27416d5f184df9c259a7c79", computeHmac("HmacSHA1", key, msg))
    }

    @Test
    fun rfc4231HmacSha256TestCase2() {
        assertEquals(
            "5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843",
            computeHmac("HmacSHA256", key, msg),
        )
    }

    @Test
    fun rfc4231HmacSha512TestCase2() {
        assertEquals(
            "164b7a7bfcf819e2e395fbe73b56e0a387bd64222e831fd610270cd7ea250554" +
                "9758bf75c05a994a6d034f65f8f0e6fdcaeab1a34d4a6b4b636e070a38bce737",
            computeHmac("HmacSHA512", key, msg),
        )
    }

    @Test
    fun emptySecretYieldsEmptyOutput() {
        assertEquals("", computeHmac("HmacSHA256", "", "data"))
    }

    @Test
    fun emptyMessageWithKeyIsStillAuthenticated() {
        // RFC 2104 defines HMAC for zero-length text; it must not be suppressed.
        val out = computeHmac("HmacSHA256", "key", "")
        assertEquals(referenceHmac("SHA-256", 64, "key".toByteArray(), ByteArray(0)), out)
    }

    @Test
    fun keyLongerThanBlockSizeIsHashedFirst() {
        // RFC 4231 test case 6 structure: 131-byte key > 64-byte block.
        val longKey = "k".repeat(131)
        assertEquals(
            referenceHmac("SHA-256", 64, longKey.toByteArray(), "Test".toByteArray()),
            computeHmac("HmacSHA256", longKey, "Test"),
        )
    }

    @Test
    fun keyAndMessageEncodedAsUtf8() {
        val k = "klucz-ż"
        val m = "wiadomość ✓"
        assertEquals(
            referenceHmac("SHA-512", 128, k.toByteArray(Charsets.UTF_8), m.toByteArray(Charsets.UTF_8)),
            computeHmac("HmacSHA512", k, m),
        )
    }

    @Test
    fun unknownAlgorithmReturnsError() {
        assertTrue(computeHmac("HmacNOPE", "k", "m").startsWith("Error:"))
    }

    /** Straight RFC 2104: H((K' xor opad) || H((K' xor ipad) || text)). */
    private fun referenceHmac(alg: String, blockSize: Int, key: ByteArray, text: ByteArray): String {
        var k = key
        if (k.size > blockSize) k = MessageDigest.getInstance(alg).digest(k)
        val kp = k.copyOf(blockSize)
        val ipad = ByteArray(blockSize) { (kp[it].toInt() xor 0x36).toByte() }
        val opad = ByteArray(blockSize) { (kp[it].toInt() xor 0x5c).toByte() }
        val inner = MessageDigest.getInstance(alg).digest(ipad + text)
        return MessageDigest.getInstance(alg).digest(opad + inner).joinToString("") { "%02x".format(it) }
    }
}
