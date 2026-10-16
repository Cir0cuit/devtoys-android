package com.cir0cuit.devtoys.tools.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

/** Vectors from RFC 1321 (MD5), FIPS 180-4 / NIST CSRC examples (SHA family) and the CRC catalogue check values. */
class HashCalculatorTest {

    private fun hex(text: String, algorithm: String) = hashBytes(text.toByteArray(), listOf(algorithm))[algorithm]!!.toLowerHex()

    @Test
    fun emptyStringDigests() {
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", hex("", "MD5"))
        assertEquals("da39a3ee5e6b4b0d3255bfef95601890afd80709", hex("", "SHA-1"))
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", hex("", "SHA-256"))
    }

    @Test
    fun abcDigests() {
        assertEquals("900150983cd24fb0d6963f7d28e17f72", hex("abc", "MD5"))
        assertEquals("a9993e364706816aba3e25717850c26c9cd0d89d", hex("abc", "SHA-1"))
        assertEquals("23097d223405d8228642a477bda255b32aadbce4bda0b3f7e36c9da7", hex("abc", "SHA-224"))
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", hex("abc", "SHA-256"))
        assertEquals(
            "cb00753f45a35e8bb5a03d699ac65007272c32ab0eded1631a8b605a43ff5bed8086072ba1e7cc2358baeca134c825a7",
            hex("abc", "SHA-384"),
        )
        assertEquals(
            "ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a" +
                "2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f",
            hex("abc", "SHA-512"),
        )
    }

    @Test
    fun checksumsAreFourBigEndianBytes() {
        assertEquals("cbf43926", hex("123456789", "CRC32"))
        assertEquals("024d0127", hex("abc", "Adler-32"))
    }

    @Test
    fun everyListedAlgorithmIsAvailable() {
        assertEquals(HASH_ALGORITHMS, hashBytes(ByteArray(1), HASH_ALGORITHMS).keys.toList())
    }

    @Test
    fun nonAsciiTextIsHashedAsUtf8() {
        val text = "zażółć ✓ 😀"
        val expected = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8)).toLowerHex()
        assertEquals(expected, hex(text, "SHA-256"))
    }

    @Test
    fun hexIsLowercaseTwoDigitsPerByteIncludingNegativeBytes() {
        assertEquals("00017f80ff", byteArrayOf(0, 1, 0x7f, 0x80.toByte(), 0xff.toByte()).toLowerHex())
        assertEquals("", ByteArray(0).toLowerHex())
    }

    @Test
    fun chunkedUpdatesMatchOneShot() {
        // File mode feeds chunks; chunk boundaries must not change the digest.
        val data = ByteArray(8192 * 3 + 17) { (it * 31).toByte() }
        val hashers = HASH_ALGORITHMS.map { Hasher(it) }
        var off = 0
        while (off < data.size) {
            val n = minOf(8192, data.size - off)
            hashers.forEach { it.update(data, off, n) }
            off += n
        }
        assertEquals(
            hashBytes(data, HASH_ALGORITHMS).values.map { it.toLowerHex() },
            hashers.map { it.result().toLowerHex() },
        )
    }

    @Test
    fun inputEncodings() {
        assertEquals("abc", decodeInput("61 62:63", 1).decodeToString())
        assertEquals("abc", decodeInput("YWJj", 2).decodeToString())
        assertEquals("abc", decodeInput("YW Jj\n", 2).decodeToString())
        assertEquals(listOf(0xfb.toByte(), 0xff.toByte()), decodeInput("-_8", 2).toList())
    }

    @Test(expected = IllegalArgumentException::class)
    fun oddHexIsRejected() {
        decodeInput("abc", 1)
    }

    @Test
    fun outputFormats() {
        val abc = hashBytes("abc".toByteArray(), listOf("SHA-256"))["SHA-256"]!!
        assertEquals("BA7816BF", formatBytes(abc, 1).take(8))
        assertEquals("ungWv48Bz+pBQUDeXa4iI7ADYaOWF3qctBD/YfIAFa0=", formatBytes(abc, 2))
    }

    @Test
    fun checksumMatchingAcceptsPrefixesColonsCaseAndBase64() {
        val digest = hashBytes("abc".toByteArray(), listOf("MD5"))["MD5"]!!
        assertTrue(checksumMatches("900150983cd24fb0d6963f7d28e17f72", digest))
        assertTrue(checksumMatches("  md5:900150983CD24FB0D6963F7D28E17F72 ", digest))
        assertTrue(checksumMatches("90:01:50:98:3C:D2:4F:B0:D6:96:3F:7D:28:E1:7F:72", digest))
        assertTrue(checksumMatches(formatBytes(digest, 2), digest))
        assertFalse(checksumMatches("900150983cd24fb0d6963f7d28e17f7", digest))
        assertFalse(checksumMatches("", digest))
    }

    @Test
    fun parsesGnuAndBsdChecksumFiles() {
        val sha = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
        val text = "$sha  a.txt\r\n${sha.uppercase()} *dir/b.bin\nSHA1 (c.iso) = a9993e364706816aba3e25717850c26c9cd0d89d\n# comment\n\n"
        assertEquals(
            listOf(
                SumEntry("SHA-256", "a.txt", sha),
                SumEntry("SHA-256", "dir/b.bin", sha),
                SumEntry("SHA-1", "c.iso", "a9993e364706816aba3e25717850c26c9cd0d89d"),
            ),
            parseChecksumFile(text),
        )
    }

    @Test
    fun verifiesByDisplayName() {
        val abc = FileHashes("a.txt", hashBytes("abc".toByteArray(), listOf("SHA-256")))
        val other = FileHashes("b.bin", hashBytes("x".toByteArray(), listOf("SHA-256")))
        val entries = parseChecksumFile(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad  a.txt\n" +
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad  ./b.bin\n" +
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad  c.iso\n"
        )
        assertEquals(
            listOf("a.txt" to VerifyStatus.OK, "./b.bin" to VerifyStatus.FAILED, "c.iso" to VerifyStatus.MISSING),
            verifyChecksums(entries, listOf(abc, other)),
        )
    }

    @Test
    fun checksumListRoundTrips() {
        val files = listOf(FileHashes("a.txt", hashBytes("abc".toByteArray(), listOf("SHA-256", "MD5"))))
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad  a.txt\n",
            checksumList(files, listOf("SHA-256")),
        )
        val tagged = checksumList(files, listOf("MD5", "SHA-256"))
        assertEquals("MD5 (a.txt) = 900150983cd24fb0d6963f7d28e17f72", tagged.lines()[0])
        assertEquals(
            listOf(VerifyStatus.OK, VerifyStatus.OK),
            verifyChecksums(parseChecksumFile(tagged), files).map { it.second },
        )
    }
}
