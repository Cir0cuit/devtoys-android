package com.cir0cuit.devtoys.tools.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.math.BigInteger
import java.security.KeyFactory
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

class KeyPairGeneratorTest {

    private fun DataInputStream.sshString(): ByteArray = ByteArray(readInt()).also { readFully(it) }

    @Test
    fun rsaSshBlobAndJwk() {
        val pair = generateKeyPair(KeyType.RSA_2048)
        val pub = pair.public as RSAPublicKey
        val line = sshPublicKeyLine(pub, "me@host")
        val parts = line.split(" ")
        assertEquals("ssh-rsa", parts[0])
        assertEquals("me@host", parts[2])
        val ins = DataInputStream(ByteArrayInputStream(Base64.getDecoder().decode(parts[1])))
        assertEquals("ssh-rsa", String(ins.sshString()))
        assertEquals(pub.publicExponent, BigInteger(ins.sshString()))
        assertEquals(pub.modulus, BigInteger(ins.sshString()))
        assertTrue(sshFingerprint(pub).matches(Regex("SHA256:[A-Za-z0-9+/]{43}")))

        val jwk = toJwk(pair, includePrivate = true)
        val n = Regex("\"n\": \"([^\"]+)\"").find(jwk)!!.groupValues[1]
        assertEquals(pub.modulus, BigInteger(1, Base64.getUrlDecoder().decode(n)))
        listOf("kty", "e", "d", "p", "q", "dp", "dq", "qi").forEach { assertTrue(it, jwk.contains("\"$it\":")) }
        assertTrue(!toJwk(pair, includePrivate = false).contains("\"d\":"))
    }

    @Test
    fun ecSshBlobAndJwk() {
        for ((type, bits) in listOf(KeyType.EC_P256 to 256, KeyType.EC_P384 to 384, KeyType.EC_P521 to 521)) {
            val pair = generateKeyPair(type)
            val pub = pair.public as ECPublicKey
            val ins = DataInputStream(ByteArrayInputStream(sshPublicKeyBlob(pub)))
            assertEquals("ecdsa-sha2-nistp$bits", String(ins.sshString()))
            assertEquals("nistp$bits", String(ins.sshString()))
            val len = (bits + 7) / 8
            val point = ins.sshString()
            assertEquals(1 + 2 * len, point.size)
            assertEquals(4.toByte(), point[0])
            assertEquals(pub.w.affineX, BigInteger(1, point.copyOfRange(1, 1 + len)))
            assertTrue(sshPublicKeyLine(pub, "").startsWith("ecdsa-sha2-nistp$bits AAAA"))
            val jwk = toJwk(pair, includePrivate = true)
            assertTrue(jwk.contains("\"crv\": \"P-$bits\""))
            val d = Regex("\"d\": \"([^\"]+)\"").find(jwk)!!.groupValues[1]
            assertEquals(len, Base64.getUrlDecoder().decode(d).size)
        }
    }

    @Test
    fun pemWrapsSpki() {
        val pair = generateKeyPair(KeyType.EC_P256)
        val pem = toPem("PUBLIC KEY", pair.public.encoded)
        assertTrue(pem.startsWith("-----BEGIN PUBLIC KEY-----\n"))
        assertTrue(pem.endsWith("-----END PUBLIC KEY-----\n"))
        assertTrue(pem.lines().all { it.length <= 64 })
        val body = pem.lines().filter { !it.startsWith("-----") }.joinToString("")
        val decoded = Base64.getDecoder().decode(body)
        assertArrayEquals(pair.public.encoded, KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(decoded)).encoded)
    }
}
