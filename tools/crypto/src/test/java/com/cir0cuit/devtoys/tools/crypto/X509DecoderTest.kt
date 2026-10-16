package com.cir0cuit.devtoys.tools.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

/** Expected values from `openssl x509 -fingerprint` and `openssl pkey -pubin -outform der | sha256 | base64`. */
class X509DecoderTest {

    // openssl req -x509 -newkey ec (P-256), CN=test.example, SAN DNS/IP/email, KU, EKU, CA:TRUE pathlen:2, valid to 2126.
    private val pem = """
        -----BEGIN CERTIFICATE-----
        MIIB3zCCAYWgAwIBAgIUalaUKlAsJlMOzWGsKOdERVSVY60wCgYIKoZIzj0EAwIw
        FzEVMBMGA1UEAwwMdGVzdC5leGFtcGxlMCAXDTI2MTAxNjIyMjcyNloYDzIxMjYw
        OTIyMjIyNzI2WjAXMRUwEwYDVQQDDAx0ZXN0LmV4YW1wbGUwWTATBgcqhkjOPQIB
        BggqhkjOPQMBBwNCAAR95FksVWcyeLg1uEkD1oiRlr39u+xlhNEPZmm3UQY78XuR
        yk6cHzRTZc63zpGmzKwt6eTQU6Sfx9RndGSutlSzo4GsMIGpMB0GA1UdDgQWBBTd
        mJtbe8M0BxV6sgw7kMDVnCTqgzAfBgNVHSMEGDAWgBTdmJtbe8M0BxV6sgw7kMDV
        nCTqgzAkBgNVHREEHTAbggx0ZXN0LmV4YW1wbGWHBAoAAAGBBWFAYi5jMA4GA1Ud
        DwEB/wQEAwIChDAdBgNVHSUEFjAUBggrBgEFBQcDAQYIKwYBBQUHAwIwEgYDVR0T
        AQH/BAgwBgEB/wIBAjAKBggqhkjOPQQDAgNIADBFAiEAuDHdk0k3ZgGplAOJYPMu
        vBYesVlJMwRaQGV0BXUZrMMCIHDEZ192xaxsAcvLBNHCduYuT0WZmymC+BNrz1MX
        MPGf
        -----END CERTIFICATE-----
    """.trimIndent()

    private val keyId = "DD:98:9B:5B:7B:C3:34:07:15:7A:B2:0C:3B:90:C0:D5:9C:24:EA:83"

    @Test
    fun decodesExtensionsFingerprintsAndPin() {
        val fields = parseCertificates(pem).single().fields().toMap()
        assertEquals("DNS:test.example, IP:10.0.0.1, email:a@b.c", fields["Subject alternative names"])
        assertEquals("digitalSignature, keyCertSign", fields["Key usage"])
        assertEquals("serverAuth, clientAuth", fields["Extended key usage"])
        assertEquals("CA, path length 2", fields["Basic constraints"])
        assertEquals("EC 256 bits", fields["Public key"])
        assertEquals("6a56942a502c26530ecd61ac28e74445549563ad", fields["Serial number"])
        assertEquals(
            "22:66:A0:FA:61:55:76:9E:60:F8:06:35:A8:FF:10:79:11:1F:C2:7B:B9:61:10:88:38:FE:C9:35:E9:00:0D:12",
            fields["SHA-256 fingerprint"],
        )
        assertEquals("1E:31:C8:D7:87:7B:9B:D0:0B:DB:B2:61:92:96:2B:CE:91:D7:94:85", fields["SHA-1 fingerprint"])
        assertEquals("fyYJmZwvNBPafp1bV4ThmAXibgwQgQ+Z5MCG+MiN8x0=", fields["Public key pin (SHA-256)"])
        assertEquals(keyId, fields["Subject key identifier"])
        assertEquals(keyId, fields["Authority key identifier"])
        assertTrue(fields["Status"]!!.startsWith("Valid"))
    }

    @Test
    fun acceptsBareBase64BodyWithLineBreaks() {
        val body = pem.lines().filterNot { it.startsWith("-----") }.joinToString("\n")
        assertEquals(parseCertificates(pem).single(), parseCertificates(body).single())
    }

    @Test
    fun derFileBecomesPemAndRoundTrips() {
        val cert = parseCertificates(pem).single()
        val text = certificateFileToText(cert.encoded)
        assertTrue(text.startsWith("-----BEGIN CERTIFICATE-----\n"))
        assertEquals(cert, parseCertificates(text).single())
        assertEquals(cert, parseCertificates(cert.toPem()).single())
    }

    @Test
    fun pemFileIsKeptAsText() {
        assertEquals(pem, certificateFileToText(pem.toByteArray()))
    }

    @Test
    fun readsSeveralConcatenatedPemBlocks() {
        assertEquals(2, parseCertificates(pem + "\n" + pem).size)
    }

    @Test(expected = Exception::class)
    fun rejectsGarbage() {
        parseCertificates(Base64.getEncoder().encodeToString("not a certificate".toByteArray()))
    }
}
