package com.cir0cuit.devtoys.tools.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import javax.crypto.AEADBadTagException
import javax.crypto.BadPaddingException

class TextEncryptionTest {

    private val text = "Zażółć gęślą jaźń — hello"

    @Test
    fun decryptsOpenSslOutput() {
        // printf 'hello world' | openssl enc -aes-256-cbc -pbkdf2 -iter 1000 -salt -base64 -pass pass:secret
        assertEquals("hello world", decryptText("U2FsdGVkX18wktG4Ada7ztC7MUDbnWtApHUFHRJOAaQ=", gcm = false, usePassword = true, secret = "secret", iterations = 1000))
    }

    @Test
    fun passwordRoundTrips() {
        for (gcm in listOf(true, false)) {
            val blob = encryptText(text, gcm, true, "pw", 1000)
            assertEquals(text, decryptText(blob, gcm, true, "pw", 1000))
            assertNotEquals(blob, encryptText(text, gcm, true, "pw", 1000))
        }
        assertTrue(encryptText(text, false, true, "pw", 1000).startsWith("U2FsdGVkX1"))
    }

    @Test
    fun rawKeyRoundTrips() {
        val hexKey = "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f"
        for (gcm in listOf(true, false)) {
            assertEquals(text, decryptText(encryptText(text, gcm, false, hexKey, 0), gcm, false, hexKey, 0))
        }
        val b64Key = "AAECAwQFBgcICQoLDA0ODw==" // 16 bytes
        assertEquals(text, decryptText(encryptText(text, true, false, b64Key, 0), true, false, b64Key, 0))
    }

    @Test
    fun wrongPasswordFailsAuthentication() {
        val blob = encryptText(text, true, true, "right", 1000)
        try {
            decryptText(blob, true, true, "wrong", 1000)
            fail()
        } catch (e: AEADBadTagException) {
            assertTrue(cipherErrorMessage(e, true).startsWith("Authentication failed"))
        }
    }

    @Test(expected = BadPaddingException::class)
    fun tamperedCbcFails() {
        val key = "AAECAwQFBgcICQoLDA0ODw=="
        val blob = java.util.Base64.getDecoder().decode(encryptText("x", false, false, key, 0))
        // Flipping the last IV byte flips the last padding byte of the single block, so the padding check fails.
        blob[15] = (blob[15].toInt() xor 1).toByte()
        decryptText(java.util.Base64.getEncoder().encodeToString(blob), false, false, key, 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsShortKey() {
        parseRawAesKey("0011")
    }

    @Test
    fun nonBase64InputIsReportedAsSuch() {
        try {
            decryptText("meet at noon", true, true, "pw", 1000)
            fail()
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("not valid Base64"))
        }
    }
}
