package com.cir0cuit.devtoys.tools.crypto

import org.junit.Assert.assertEquals
import org.junit.Test
import java.security.MessageDigest

/** Vectors from RFC 1321 (MD5) and FIPS 180-4 / NIST CSRC examples (SHA family). */
class HashCalculatorTest {

    @Test
    fun algorithmOrderIsMd5Sha1Sha256Sha512() {
        assertEquals(listOf("MD5", "SHA-1", "SHA-256", "SHA-512"), HASH_ALGORITHMS)
    }

    @Test
    fun emptyStringDigests() {
        assertEquals(
            listOf(
                "d41d8cd98f00b204e9800998ecf8427e",
                "da39a3ee5e6b4b0d3255bfef95601890afd80709",
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce" +
                    "47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e",
            ),
            hashText(""),
        )
    }

    @Test
    fun abcDigests() {
        assertEquals(
            listOf(
                "900150983cd24fb0d6963f7d28e17f72",
                "a9993e364706816aba3e25717850c26c9cd0d89d",
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                "ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a" +
                    "2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f",
            ),
            hashText("abc"),
        )
    }

    @Test
    fun quickBrownFoxMd5() {
        assertEquals("9e107d9d372bb6826bd81d3542a419d6", hashText("The quick brown fox jumps over the lazy dog")[0])
    }

    @Test
    fun nonAsciiTextIsHashedAsUtf8() {
        val text = "zażółć ✓ \uD83D\uDE00"
        val utf8 = text.toByteArray(Charsets.UTF_8)
        val expected = HASH_ALGORITHMS.map { alg -> MessageDigest.getInstance(alg).digest(utf8).toLowerHex() }
        assertEquals(expected, hashText(text))
    }

    @Test
    fun hexIsLowercaseTwoDigitsPerByteIncludingNegativeBytes() {
        assertEquals("00017f80ff", byteArrayOf(0, 1, 0x7f, 0x80.toByte(), 0xff.toByte()).toLowerHex())
        assertEquals("", ByteArray(0).toLowerHex())
    }

    @Test
    fun digestLengths() {
        assertEquals(listOf(32, 40, 64, 128), hashText("x").map { it.length })
    }

    @Test
    fun chunkedUpdatesMatchOneShot() {
        // File mode feeds 8 KiB chunks; chunk boundaries must not change the digest.
        val data = ByteArray(8192 * 3 + 17) { (it * 31).toByte() }
        val streamed = newHashDigests()
        var off = 0
        while (off < data.size) {
            val n = minOf(8192, data.size - off)
            streamed.forEach { it.update(data, off, n) }
            off += n
        }
        val oneShot = newHashDigests().map { it.digest(data).toLowerHex() }
        assertEquals(oneShot, streamed.map { it.digest().toLowerHex() })
    }

    @Test
    fun newHashDigestsReturnsFreshInstances() {
        val a = newHashDigests()
        a.forEach { it.update(byteArrayOf(1, 2, 3)) }
        val b = newHashDigests()
        assertEquals(hashText(""), b.map { it.digest().toLowerHex() })
    }
}
