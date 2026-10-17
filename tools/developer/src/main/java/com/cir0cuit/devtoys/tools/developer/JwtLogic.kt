package com.cir0cuit.devtoys.tools.developer

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.spec.MGF1ParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.PSSParameterSpec
import java.security.spec.RSAPublicKeySpec
import java.security.spec.X509EncodedKeySpec
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

internal val JWT_SIGN_ALGS = listOf("HS256", "HS384", "HS512", "RS256", "RS384", "RS512", "PS256", "PS384", "PS512", "ES256", "ES384", "ES512")

// JWE (5 parts) first, then JWS (3 parts). Ends on a part, so a trailing sentence dot is not taken.
private val TOKEN_RE = Regex("""eyJ[\w-]+\.[\w-]*\.[\w-]+\.[\w-]+\.[\w-]+|eyJ[\w-]+\.[\w-]+\.[\w-]*""")

/** Tokens found anywhere in [text]: an Authorization header, cookie, redirect URL or JSON response. */
internal fun extractTokens(text: String): List<String> {
    val found = TOKEN_RE.findAll(text).map { it.value }.distinct().toList()
    if (found.isNotEmpty()) return found
    val bare = text.trim().replace(Regex("^Bearer\\s+", RegexOption.IGNORE_CASE), "").trim()
    return if (bare.isEmpty()) emptyList() else listOf(bare)
}

internal class DecodedJwt(val header: JSONObject, val payload: JSONObject?, val signatureBytes: Int)

/** Decodes one compact token; throws IllegalArgumentException with a message for the user. */
internal fun decodeJwt(token: String): DecodedJwt {
    val parts = token.split(".")
    require(parts.size == 3 || parts.size == 5) { "Not a JWT. A token has dot-separated parts: header.payload.signature." }
    val header = runCatching { JSONObject(b64urlText(parts[0])) }.getOrNull()
        ?: throw IllegalArgumentException("Could not decode the header. Check that the token is complete.")
    if (parts.size == 5) return DecodedJwt(header, null, 0)
    val payload = runCatching { JSONObject(b64urlText(parts[1])) }.getOrNull()
        ?: throw IllegalArgumentException("Could not decode the payload. Check that the token is complete.")
    val sig = runCatching { b64url(parts[2]).size }.getOrNull()
        ?: throw IllegalArgumentException("Could not decode the signature. Check that the token is complete.")
    return DecodedJwt(header, payload, sig)
}

internal fun jwtPrettyJson(o: JSONObject): String = o.toString(2).replace("\\/", "/")

private val TIME_CLAIMS = listOf("exp" to "Expires", "nbf" to "Not before", "iat" to "Issued at", "auth_time" to "Signed in at", "updated_at" to "Profile updated at")
private val PAYLOAD_CLAIMS = listOf(
    "iss" to "Issuer", "sub" to "Subject", "aud" to "Audience", "azp" to "Authorized party",
    "jti" to "Token ID", "scope" to "Scopes", "scp" to "Scopes", "cnf" to "Confirmation key"
)
private val HEADER_CLAIMS = listOf("typ" to "Type", "kid" to "Key ID", "x5t" to "Certificate thumbprint")

/** Labeled rows: algorithm, known header and registered claims with meanings, and dates with a relative time. */
internal fun jwtClaimRows(jwt: DecodedJwt, now: Instant, zone: ZoneId): List<Pair<String, String>> {
    val rows = mutableListOf<Pair<String, String>>()
    val alg = jwt.header.optString("alg", "")
    rows += "Algorithm (alg)" to alg.ifEmpty { "Missing" } +
        if (jwt.payload != null) ", signature " + (if (jwt.signatureBytes == 0) "empty" else "${jwt.signatureBytes} bytes") else ", encrypted"
    for ((name, meaning) in HEADER_CLAIMS) jwt.header.opt(name)?.let { rows += "$meaning ($name)" to claimText(it) }
    val payload = jwt.payload ?: return rows
    val format = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss xxx").withZone(zone)
    for ((name, label) in TIME_CLAIMS) {
        val seconds = (payload.opt(name) as? Number)?.toLong() ?: continue
        val t = runCatching { Instant.ofEpochSecond(seconds) }.getOrNull() ?: continue
        val date = runCatching { format.format(t) }.getOrNull() ?: continue
        val relative = relativeTime(t, now)
        val note = when {
            name == "exp" && t.isBefore(now) -> "expired $relative"
            name == "nbf" && t.isAfter(now) -> "not valid yet, starts $relative"
            else -> relative
        }
        rows += "$label ($name)" to "$date ($note)"
    }
    for ((name, meaning) in PAYLOAD_CLAIMS) payload.opt(name)?.let { rows += "$meaning ($name)" to claimText(it) }
    return rows
}

private fun claimText(v: Any): String = when (v) {
    is JSONArray -> (0 until v.length()).joinToString(", ") { v.opt(it).toString() }
    is JSONObject -> v.toString().replace("\\/", "/")
    else -> v.toString()
}

internal fun jwtWarnings(jwt: DecodedJwt): List<String> = buildList {
    val alg = jwt.header.optString("alg", "")
    if (alg.equals("none", ignoreCase = true)) add("alg is none, so the token is unsigned. A server must reject it.")
    else if (alg.isEmpty()) add("The header has no alg, so the signature cannot be checked.")
    val payload = jwt.payload ?: return@buildList
    if (!payload.has("exp")) add("No exp claim, so the token never expires.")
}

internal data class JwtVerdict(val valid: Boolean, val message: String)

/** Checks the signature with an HMAC secret, a PEM public key or certificate, or a JWK / JWK set. */
internal fun verifyJwt(token: String, keyText: String, secretIsBase64: Boolean): JwtVerdict {
    val parts = token.split(".")
    if (parts.size != 3) return JwtVerdict(false, "Only signed tokens with 3 parts can be verified.")
    val header = runCatching { JSONObject(b64urlText(parts[0])) }.getOrElse { return JwtVerdict(false, "Could not decode the header.") }
    val alg = header.optString("alg", "")
    if (alg.equals("none", ignoreCase = true)) return JwtVerdict(false, "alg is none: there is no signature to check.")
    val data = "${parts[0]}.${parts[1]}".toByteArray(Charsets.US_ASCII)
    return try {
        val sig = b64url(parts[2])
        val ok = if (alg.startsWith("HS")) {
            MessageDigest.isEqual(hmac(alg, hmacSecret(keyText, secretIsBase64, alg), data), sig)
        } else {
            val signer = signer(alg)
            signer.initVerify(publicKey(keyText, alg, header.optString("kid", "")))
            signer.update(data)
            signer.verify(if (alg.startsWith("ES")) joseToDer(sig) else sig)
        }
        if (ok) JwtVerdict(true, "Signature valid ($alg)")
        else JwtVerdict(false, "Signature invalid. The key does not match, or the token was changed.")
    } catch (e: NoSuchAlgorithmException) {
        JwtVerdict(false, if (alg == "EdDSA") "EdDSA needs Android 13 or later." else "$alg is not supported on this device.")
    } catch (e: Exception) {
        JwtVerdict(false, e.message?.takeIf { e is IllegalArgumentException }
            ?: "This key cannot check $alg. RS and PS need an RSA key, ES an EC key, HS a shared secret.")
    }
}

/** Builds a signed compact token. [headerJson] gets alg set from [alg]. */
internal fun signJwt(headerJson: String, payloadJson: String, alg: String, keyText: String, secretIsBase64: Boolean): String {
    val header = jsonObjectOnly(headerJson.ifBlank { "{}" }, "header")
    val payload = jsonObjectOnly(payloadJson, "payload")
    header.put("alg", alg)
    if (!header.has("typ")) header.put("typ", "JWT")
    fun part(o: JSONObject) = b64urlEncode(o.toString().replace("\\/", "/").toByteArray(Charsets.UTF_8))
    val input = part(header) + "." + part(payload)
    val data = input.toByteArray(Charsets.US_ASCII)
    val sig = if (alg.startsWith("HS")) {
        hmac(alg, hmacSecret(keyText, secretIsBase64, alg), data)
    } else {
        val signer = signer(alg)
        signer.initSign(privateKey(keyText, alg))
        signer.update(data)
        signer.sign().let { if (alg.startsWith("ES")) derToJose(it, ecSize(alg)) else it }
    }
    return input + "." + b64urlEncode(sig)
}

/** Returns [payloadJson] with [claim] set, pretty printed. */
internal fun setClaim(payloadJson: String, claim: String, value: Long): String {
    return jwtPrettyJson(jsonObjectOnly(payloadJson.ifBlank { "{}" }, "payload").put(claim, value))
}

/** One JSON object and nothing after it; JSONObject(String) alone ignores trailing text like "{}xx". */
private fun jsonObjectOnly(text: String, what: String): JSONObject {
    val tokener = JSONTokener(text)
    val o = runCatching { JSONObject(tokener) }
        .getOrElse { throw IllegalArgumentException("The $what is not a JSON object. Check braces and quotes.") }
    require(tokener.nextClean() == 0.toChar()) { "The $what has text after the closing brace. Remove it." }
    return o
}

private fun b64url(s: String): ByteArray = Base64.getUrlDecoder().decode(s.trimEnd('='))
private fun b64urlText(s: String) = String(b64url(s), Charsets.UTF_8)
private fun b64urlEncode(b: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(b)

private fun hmac(alg: String, secret: ByteArray, data: ByteArray): ByteArray {
    val name = when (alg) {
        "HS256" -> "HmacSHA256"; "HS384" -> "HmacSHA384"; "HS512" -> "HmacSHA512"
        else -> throw IllegalArgumentException("$alg is not supported.")
    }
    require(secret.isNotEmpty()) { "Enter the shared secret." }
    return Mac.getInstance(name).run { init(SecretKeySpec(secret, name)); doFinal(data) }
}

private fun hmacSecret(keyText: String, base64: Boolean, alg: String): ByteArray {
    val t = keyText.trim()
    require(!t.startsWith("-----BEGIN")) { "$alg needs a shared secret, not a PEM key." }
    if (t.startsWith("{")) {
        val jwk = pickJwk(t, "")
        require(jwk.optString("kty") == "oct") { "$alg needs a shared secret. This JWK is not of type oct." }
        return b64url(jwk.getString("k"))
    }
    if (!base64) return keyText.toByteArray(Charsets.UTF_8)
    return runCatching { Base64.getDecoder().decode(t) }.recoverCatching { b64url(t) }
        .getOrElse { throw IllegalArgumentException("The secret is not valid Base64. Turn off Base64 secret or fix the text.") }
}

private fun signer(alg: String): Signature {
    val bits = alg.drop(2)
    return when (alg.take(2)) {
        "RS" -> Signature.getInstance("SHA${bits}withRSA")
        "ES" -> Signature.getInstance("SHA${bits}withECDSA")
        // Android names it SHA256withRSA/PSS; desktop Java only has RSASSA-PSS with parameters.
        "PS" -> runCatching { Signature.getInstance("SHA${bits}withRSA/PSS") }.getOrElse {
            Signature.getInstance("RSASSA-PSS").apply {
                setParameter(PSSParameterSpec("SHA-$bits", "MGF1", MGF1ParameterSpec("SHA-$bits"), bits.toInt() / 8, 1))
            }
        }
        "Ed" -> Signature.getInstance("Ed25519")
        else -> throw IllegalArgumentException("Unknown alg \"$alg\". Supported: HS, RS, PS, ES and EdDSA.")
    }
}

private fun keyFactory(alg: String): KeyFactory = KeyFactory.getInstance(
    when (alg.take(2)) { "ES" -> "EC"; "Ed" -> "Ed25519"; else -> "RSA" }
)

private fun ecSize(alg: String) = when (alg) { "ES256" -> 32; "ES384" -> 48; else -> 66 }

private fun pemBody(pem: String): ByteArray =
    Base64.getMimeDecoder().decode(pem.lines().filterNot { it.startsWith("-----") }.joinToString(""))

private fun publicKey(keyText: String, alg: String, kid: String): PublicKey {
    val t = keyText.trim()
    require(t.isNotEmpty()) { "Enter the public key." }
    return when {
        t.startsWith("-----BEGIN CERTIFICATE") ->
            CertificateFactory.getInstance("X.509").generateCertificate(ByteArrayInputStream(t.toByteArray())).publicKey
        t.startsWith("-----BEGIN PUBLIC KEY") -> keyFactory(alg).generatePublic(X509EncodedKeySpec(pemBody(t)))
        t.startsWith("-----BEGIN RSA PUBLIC KEY") ->
            throw IllegalArgumentException("PKCS#1 key. Convert it: openssl rsa -RSAPublicKey_in -in key.pem -pubout")
        t.contains("PRIVATE KEY") -> throw IllegalArgumentException("This is a private key. Paste the public key or certificate.")
        t.startsWith("{") -> jwkPublicKey(pickJwk(t, kid))
        else -> throw IllegalArgumentException("$alg needs a PEM public key, a certificate or a JWK.")
    }
}

private fun privateKey(keyText: String, alg: String): PrivateKey {
    val t = keyText.trim()
    require(t.isNotEmpty()) { "Enter the private key." }
    return when {
        t.startsWith("-----BEGIN PRIVATE KEY") -> keyFactory(alg).generatePrivate(PKCS8EncodedKeySpec(pemBody(t)))
        t.startsWith("-----BEGIN ENCRYPTED") -> throw IllegalArgumentException("Encrypted key. Decrypt it: openssl pkcs8 -topk8 -nocrypt -in key.pem")
        t.startsWith("-----BEGIN RSA PRIVATE") || t.startsWith("-----BEGIN EC PRIVATE") ->
            throw IllegalArgumentException("Key is not PKCS#8. Convert it: openssl pkcs8 -topk8 -nocrypt -in key.pem")
        else -> throw IllegalArgumentException("$alg needs a PEM private key (BEGIN PRIVATE KEY).")
    }
}

private fun pickJwk(text: String, kid: String): JSONObject {
    val o = runCatching { JSONObject(text) }.getOrElse { throw IllegalArgumentException("The JWK is not valid JSON.") }
    val keys = o.optJSONArray("keys") ?: return o
    val all = (0 until keys.length()).mapNotNull { keys.optJSONObject(it) }
    return all.firstOrNull { kid.isNotEmpty() && it.optString("kid") == kid } ?: all.firstOrNull()
        ?: throw IllegalArgumentException("The JWK set has no keys.")
}

// DER SubjectPublicKeyInfo prefixes; the raw key bytes follow.
private val SPKI_PREFIX = mapOf(
    "P-256" to "3059301306072a8648ce3d020106082a8648ce3d03010703420004",
    "P-384" to "3076301006072a8648ce3d020106052b8104002203620004",
    "P-521" to "30819b301006072a8648ce3d020106052b810400230381860004",
    "Ed25519" to "302a300506032b6570032100"
)
private val CURVE_SIZE = mapOf("P-256" to 32, "P-384" to 48, "P-521" to 66)

private fun jwkPublicKey(jwk: JSONObject): PublicKey = when (jwk.optString("kty")) {
    "RSA" -> KeyFactory.getInstance("RSA").generatePublic(
        RSAPublicKeySpec(BigInteger(1, b64url(jwk.getString("n"))), BigInteger(1, b64url(jwk.getString("e"))))
    )
    "EC" -> {
        val crv = jwk.optString("crv")
        val size = CURVE_SIZE[crv] ?: throw IllegalArgumentException("Curve \"$crv\" is not supported. Use P-256, P-384 or P-521.")
        fun coord(name: String) = unsigned(BigInteger(1, b64url(jwk.getString(name))), size)
        KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(hex(SPKI_PREFIX.getValue(crv)) + coord("x") + coord("y")))
    }
    "OKP" -> KeyFactory.getInstance("Ed25519").generatePublic(X509EncodedKeySpec(hex(SPKI_PREFIX.getValue("Ed25519")) + b64url(jwk.getString("x"))))
    "oct" -> throw IllegalArgumentException("This JWK is a shared secret (oct). It only checks HS tokens.")
    else -> throw IllegalArgumentException("JWK kty must be RSA, EC, OKP or oct.")
}

private fun hex(s: String) = ByteArray(s.length / 2) { s.substring(it * 2, it * 2 + 2).toInt(16).toByte() }

/** Big-endian unsigned bytes of [v], left-padded to [size]. */
private fun unsigned(v: BigInteger, size: Int): ByteArray {
    val b = v.toByteArray().let { if (it.size > size && it[0] == 0.toByte()) it.copyOfRange(1, it.size) else it }
    require(b.size <= size) { "Key or signature value is too long for the curve." }
    return ByteArray(size - b.size) + b
}

/** JWS ECDSA signature (R || S) to the ASN.1 DER form Java expects. */
internal fun joseToDer(raw: ByteArray): ByteArray {
    require(raw.isNotEmpty() && raw.size % 2 == 0) { "Signature length does not fit an ES algorithm." }
    val n = raw.size / 2
    val out = ByteArrayOutputStream()
    for (half in listOf(raw.copyOfRange(0, n), raw.copyOfRange(n, raw.size))) {
        val i = BigInteger(1, half).toByteArray()
        out.write(0x02); out.write(i.size); out.write(i)
    }
    val body = out.toByteArray()
    val len = if (body.size < 128) byteArrayOf(body.size.toByte()) else byteArrayOf(0x81.toByte(), body.size.toByte())
    return byteArrayOf(0x30) + len + body
}

/** ASN.1 DER ECDSA signature to the JWS form (R || S, each [size] bytes). */
internal fun derToJose(der: ByteArray, size: Int): ByteArray {
    var p = 1
    if (der[p].toInt() and 0x80 != 0) p += der[p].toInt() and 0x7f
    p++
    fun int(): ByteArray {
        require(der[p] == 0x02.toByte()) { "Unexpected ECDSA signature encoding." }
        val len = der[p + 1].toInt() and 0xff
        val v = BigInteger(1, der.copyOfRange(p + 2, p + 2 + len))
        p += 2 + len
        return unsigned(v, size)
    }
    return int() + int()
}
