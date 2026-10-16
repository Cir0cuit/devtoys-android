package com.cir0cuit.devtoys.tools.developer

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.ECGenParameterSpec
import java.time.Instant
import java.time.ZoneOffset
import java.util.Base64

class JwtLogicTest {
    private val sample = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"

    private fun pem(type: String, der: ByteArray) =
        "-----BEGIN $type-----\n" + Base64.getMimeEncoder().encodeToString(der) + "\n-----END $type-----"

    private fun b64u(b: ByteArray) = Base64.getUrlEncoder().withoutPadding().encodeToString(b)

    private fun keys(alg: String, spec: Any): KeyPair = KeyPairGenerator.getInstance(alg).run {
        if (spec is Int) initialize(spec) else initialize(spec as ECGenParameterSpec)
        generateKeyPair()
    }

    @Test fun extractsTokensFromHeadersUrlsAndJson() {
        assertEquals(listOf(sample), extractTokens("Authorization: Bearer $sample\r\nHost: x"))
        assertEquals(listOf(sample), extractTokens("https://app/cb#id_token=$sample&state=1"))
        assertEquals(listOf(sample), extractTokens("Cookie: session=$sample; theme=dark"))
        assertEquals(listOf(sample), extractTokens("Token: $sample."))
        assertEquals(2, extractTokens("""{"id_token":"$sample","access_token":"eyJhbGciOiJub25lIn0.eyJhIjoxfQ."}""").size)
        assertEquals(listOf("abc"), extractTokens("bearer abc"))
        assertTrue(extractTokens("  ").isEmpty())
    }

    @Test fun decodesAndRejectsMalformed() {
        val jwt = decodeJwt(sample)
        assertEquals("John Doe", jwt.payload!!.getString("name"))
        assertEquals(32, jwt.signatureBytes)
        assertTrue(runCatching { decodeJwt("abc") }.exceptionOrNull()!!.message!!.startsWith("Not a JWT"))
    }

    @Test fun claimRowsAndWarnings() {
        val jwt = DecodedJwt(JSONObject("""{"alg":"none","kid":"k1"}"""), JSONObject("""{"exp":1000,"aud":["a","b"]}"""), 0)
        val rows = jwtClaimRows(jwt, Instant.ofEpochSecond(4600), ZoneOffset.UTC).toMap()
        assertEquals("1970-01-01 00:16:40 +00:00 (expired 1 hour ago)", rows["Expires (exp)"])
        assertEquals("a, b", rows["Audience (aud)"])
        assertEquals("k1", rows["Key ID (kid)"])
        assertEquals("none, signature empty", rows["Algorithm (alg)"])
        assertTrue(jwtWarnings(jwt).single().startsWith("alg is none"))
    }

    @Test fun verifiesHmacSample() {
        assertTrue(verifyJwt(sample, "your-256-bit-secret", false).valid)
        assertFalse(verifyJwt(sample, "wrong", false).valid)
        val b64 = Base64.getEncoder().encodeToString("your-256-bit-secret".toByteArray())
        assertTrue(verifyJwt(sample, b64, true).valid)
        assertTrue(verifyJwt(sample, """{"kty":"oct","k":"${b64u("your-256-bit-secret".toByteArray())}"}""", false).valid)
    }

    @Test fun signsAndVerifiesEveryAlgorithm() {
        val rsa = keys("RSA", 2048)
        val ec = mapOf("ES256" to "secp256r1", "ES384" to "secp384r1", "ES512" to "secp521r1").mapValues { keys("EC", ECGenParameterSpec(it.value)) }
        for (alg in JWT_SIGN_ALGS) {
            val pair = if (alg.startsWith("ES")) ec.getValue(alg) else rsa
            val (priv, pub) = if (alg.startsWith("HS")) "s3cret" to "s3cret"
            else pem("PRIVATE KEY", pair.private.encoded) to pem("PUBLIC KEY", pair.public.encoded)
            val token = signJwt("{}", """{"sub":"x","url":"https://a/b"}""", alg, priv, false)
            assertTrue(alg, verifyJwt(token, pub, false).valid)
            val parts = token.split(".")
            assertEquals("https://a/b", decodeJwt(token).payload!!.getString("url"))
            val tampered = parts[0] + "." + b64u("""{"sub":"y"}""".toByteArray()) + "." + parts[2]
            assertFalse(alg, verifyJwt(tampered, pub, false).valid)
        }
    }

    @Test fun verifiesWithJwkAndJwkSet() {
        val ec = keys("EC", ECGenParameterSpec("secp256r1"))
        val token = signJwt("""{"kid":"b"}""", "{}", "ES256", pem("PRIVATE KEY", ec.private.encoded), false)
        val w = (ec.public as ECPublicKey).w
        fun coord(v: BigInteger) = b64u(v.toByteArray().let { if (it.size > 32) it.copyOfRange(it.size - 32, it.size) else ByteArray(32 - it.size) + it })
        val jwk = """{"kty":"EC","crv":"P-256","kid":"b","x":"${coord(w.affineX)}","y":"${coord(w.affineY)}"}"""
        assertTrue(verifyJwt(token, jwk, false).valid)
        assertTrue(verifyJwt(token, """{"keys":[{"kty":"oct","kid":"a","k":"AA"},$jwk]}""", false).valid)

        val rsa = keys("RSA", 2048)
        val rsToken = signJwt("{}", "{}", "RS256", pem("PRIVATE KEY", rsa.private.encoded), false)
        val pub = rsa.public as RSAPublicKey
        fun unsigned(v: BigInteger) = b64u(v.toByteArray().let { if (it[0] == 0.toByte()) it.copyOfRange(1, it.size) else it })
        assertTrue(verifyJwt(rsToken, """{"kty":"RSA","n":"${unsigned(pub.modulus)}","e":"${unsigned(pub.publicExponent)}"}""", false).valid)
    }

    @Test fun keyMismatchAndNoneExplainTheProblem() {
        val rsa = keys("RSA", 2048)
        val pub = pem("PUBLIC KEY", rsa.public.encoded)
        assertTrue(verifyJwt(sample, pub, false).message.contains("shared secret"))
        val es = signJwt("{}", "{}", "ES256", pem("PRIVATE KEY", keys("EC", ECGenParameterSpec("secp256r1")).private.encoded), false)
        assertFalse(verifyJwt(es, pub, false).valid)
        assertFalse(verifyJwt("eyJhbGciOiJub25lIn0.eyJhIjoxfQ.", "x", false).valid)
    }

    @Test fun ecdsaSignatureFormatRoundTrips() {
        val raw = ByteArray(64) { (it + 1).toByte() }.also { it[0] = 0x80.toByte(); it[32] = 0 }
        assertTrue(raw.contentEquals(derToJose(joseToDer(raw), 32)))
        val big = ByteArray(132) { 0xff.toByte() }
        assertTrue(big.contentEquals(derToJose(joseToDer(big), 66)))
    }

    @Test fun setClaimAddsTimestamp() {
        assertEquals(1700000000L, JSONObject(setClaim("""{"sub":"x"}""", "exp", 1700000000L)).getLong("exp"))
    }
}
