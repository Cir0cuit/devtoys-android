package com.cir0cuit.devtoys.tools.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

/** Vectors from RFC 2202 (HMAC-MD5/SHA-1) and RFC 4231 (HMAC-SHA-256/512). */
class HmacCalculatorTest {

    private val key = "Jefe".toByteArray()
    private val msg = "what do ya want for nothing?".toByteArray()

    private fun hmacHex(algo: String, key: ByteArray, data: ByteArray) = computeHmac(algo, key, data).toLowerHex()

    @Test
    fun rfc2202HmacMd5TestCase2() {
        assertEquals("750c783e6ab0b503eaa86e310a5db738", hmacHex("HmacMD5", key, msg))
    }

    @Test
    fun rfc2202HmacSha1TestCase2() {
        assertEquals("effcdf6ae5eb2fa2d27416d5f184df9c259a7c79", hmacHex("HmacSHA1", key, msg))
    }

    @Test
    fun rfc4231HmacSha256TestCase2() {
        assertEquals("5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843", hmacHex("HmacSHA256", key, msg))
    }

    @Test
    fun rfc4231HmacSha512TestCase2() {
        assertEquals(
            "164b7a7bfcf819e2e395fbe73b56e0a387bd64222e831fd610270cd7ea250554" +
                "9758bf75c05a994a6d034f65f8f0e6fdcaeab1a34d4a6b4b636e070a38bce737",
            hmacHex("HmacSHA512", key, msg),
        )
    }

    @Test
    fun rfc4231TestCase1WithHexKey() {
        // Key 0x0b * 20, data "Hi There".
        val hexKey = decodeInput("0b".repeat(20), 1)
        assertEquals(
            "b0344c61d8db38535ca8afceaf0bf12b881dc200c9833da726e9376c2e32cff7",
            hmacHex("HmacSHA256", hexKey, "Hi There".toByteArray()),
        )
        assertEquals(
            "87aa7cdea5ef619d4ff0b4241a1d6cb02379f4e2ce4ec2787ad0b30545e17cde" +
                "daa833b7d6b8a702038b274eaea3f4e4be9d914eeb61f1702e696c203a126854",
            hmacHex("HmacSHA512", hexKey, "Hi There".toByteArray()),
        )
    }

    @Test
    fun rfc4231TestCase3WithHexKeyAndHexData() {
        val mac = computeHmac("HmacSHA256", decodeInput("aa".repeat(20), 1), decodeInput("dd".repeat(50), 1))
        assertEquals("773ea91e36800e46854db8ebd09181a72959098b3ef8c122d9635514ced565fe", mac.toLowerHex())
    }

    @Test
    fun base64KeyMatchesRawBytes() {
        assertEquals(hmacHex("HmacSHA256", key, msg), hmacHex("HmacSHA256", decodeInput("SmVmZQ==", 2), msg))
    }

    @Test
    fun outputRows() {
        val mac = computeHmac("HmacSHA256", decodeInput("0b".repeat(20), 1), "Hi There".toByteArray())
        assertEquals(
            listOf(
                "Hex" to "b0344c61d8db38535ca8afceaf0bf12b881dc200c9833da726e9376c2e32cff7",
                "Base64" to "sDRMYdjbOFNcqK/OrwvxK4gdwgDJgz2nJuk3bC4yz/c=",
                "Base64url" to "sDRMYdjbOFNcqK_OrwvxK4gdwgDJgz2nJuk3bC4yz_c",
            ),
            hmacRows(mac),
        )
    }

    @Test
    fun signatureVerificationAcceptsWebhookFormats() {
        val mac = computeHmac("HmacSHA256", key, msg)
        val hex = mac.toLowerHex()
        assertTrue(signatureMatches(hex, mac))
        assertTrue(signatureMatches("sha256=$hex", mac))
        assertTrue(signatureMatches("v1=$hex", mac))
        assertTrue(signatureMatches(" v0=${hex.uppercase()} ", mac))
        assertTrue(signatureMatches(hmacRows(mac)[1].second, mac))
        assertTrue(signatureMatches(hmacRows(mac)[2].second, mac))
        assertFalse(signatureMatches(hex.dropLast(1) + "0", mac))
        assertFalse(signatureMatches("", mac))
    }

    @Test(expected = IllegalArgumentException::class)
    fun emptyKeyIsRejected() {
        computeHmac("HmacSHA256", ByteArray(0), msg)
    }

    @Test
    fun emptyMessageWithKeyIsStillAuthenticated() {
        // RFC 2104 defines HMAC for zero-length text; it must not be suppressed.
        assertEquals(
            referenceHmac("SHA-256", 64, "key".toByteArray(), ByteArray(0)),
            hmacHex("HmacSHA256", "key".toByteArray(), ByteArray(0)),
        )
    }

    @Test
    fun keyLongerThanBlockSizeIsHashedFirst() {
        val longKey = "k".repeat(131).toByteArray()
        assertEquals(
            referenceHmac("SHA-256", 64, longKey, "Test".toByteArray()),
            hmacHex("HmacSHA256", longKey, "Test".toByteArray()),
        )
    }

    @Test
    fun badKeyEncodingNamesTheField() {
        val e = runCatching { decodeInput("zz", 1, "Key") }.exceptionOrNull()
        assertEquals("Key is not valid hex. Use pairs of 0-9 and a-f.", e?.message)
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
