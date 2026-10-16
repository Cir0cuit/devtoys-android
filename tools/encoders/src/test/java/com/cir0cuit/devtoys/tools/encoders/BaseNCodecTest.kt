package com.cir0cuit.devtoys.tools.encoders

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class BaseNCodecTest {

    private fun enc(codec: BaseN, s: String) = codec.encode(s.toByteArray())
    private fun dec(codec: BaseN, s: String) = codec.decode(s).toString(Charsets.UTF_8)

    private fun assertRejected(codec: BaseN, s: String) {
        try {
            codec.decode(s)
        } catch (e: IllegalArgumentException) {
            return
        }
        throw AssertionError("expected $codec to reject '$s'")
    }

    @Test
    fun base32Rfc4648Vectors() {
        val vectors = listOf(
            "" to "", "f" to "MY======", "fo" to "MZXQ====", "foo" to "MZXW6===",
            "foob" to "MZXW6YQ=", "fooba" to "MZXW6YTB", "foobar" to "MZXW6YTBOI======"
        )
        for ((plain, encoded) in vectors) {
            assertEquals(encoded, enc(BaseN.BASE32, plain))
            assertEquals(plain, dec(BaseN.BASE32, encoded))
        }
    }

    @Test
    fun base32HexRfc4648Vectors() {
        assertEquals("CO======", enc(BaseN.BASE32_HEX, "f"))
        assertEquals("CPNMUOJ1E8======", enc(BaseN.BASE32_HEX, "foobar"))
        assertEquals("foobar", dec(BaseN.BASE32_HEX, "cpnmuoj1e8"))
    }

    @Test
    fun base32DecodeIsLenient() {
        assertEquals("foobar", dec(BaseN.BASE32, "mzxw 6ytb oi"))
        assertRejected(BaseN.BASE32, "MZXW1")
        assertRejected(BaseN.BASE32, "M")
    }

    @Test
    fun base58Vectors() {
        assertEquals("2NEpo7TZRRrLZSi2U", enc(BaseN.BASE58, "Hello World!"))
        assertEquals("Hello World!", dec(BaseN.BASE58, "2NEpo7TZRRrLZSi2U"))
        assertEquals("112", BaseN.BASE58.encode(byteArrayOf(0, 0, 1)))
        assertArrayEquals(byteArrayOf(0, 0, 1), BaseN.BASE58.decode("112"))
        assertArrayEquals(byteArrayOf(0), BaseN.BASE58.decode("1"))
        assertRejected(BaseN.BASE58, "0OIl")
    }

    @Test
    fun ascii85Vectors() {
        assertEquals("F*2M7/c", enc(BaseN.ASCII85, "sure."))
        assertEquals("9jqo^Bla", enc(BaseN.ASCII85, "Man is"))
        assertEquals("z@:B", BaseN.ASCII85.encode(byteArrayOf(0, 0, 0, 0, 'a'.code.toByte(), 'b'.code.toByte())))
        assertEquals("sure.", dec(BaseN.ASCII85, "F*2M7/c"))
        assertEquals("sure.", dec(BaseN.ASCII85, "<~F*2M\n7/c~>"))
        assertArrayEquals(byteArrayOf(0, 0, 0, 0, 'a'.code.toByte(), 'b'.code.toByte()), BaseN.ASCII85.decode("z@:B"))
        assertRejected(BaseN.ASCII85, "F*2M7/")
        assertRejected(BaseN.ASCII85, "vvvvv")
    }

    @Test
    fun z85SpecVector() {
        val bytes = byteArrayOf(0x86.toByte(), 0x4F, 0xD2.toByte(), 0x6F, 0xB5.toByte(), 0x59, 0xF7.toByte(), 0x5B)
        assertEquals("HelloWorld", BaseN.Z85.encode(bytes))
        assertArrayEquals(bytes, BaseN.Z85.decode("HelloWorld"))
        assertRejected(BaseN.Z85, "Hell")
        try {
            BaseN.Z85.encode(byteArrayOf(1, 2, 3))
            throw AssertionError("Z85 must reject 3 bytes")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun base36() {
        assertEquals("kmh", enc(BaseN.BASE36, "hi"))
        assertEquals("hi", dec(BaseN.BASE36, "KMH"))
    }

    @Test
    fun roundTripEveryCodec() {
        val bytes = byteArrayOf(0, 1, -1, 127, -128, 42, 0, 0)
        for (codec in BaseN.entries) {
            assertArrayEquals(codec.name, bytes, codec.decode(codec.encode(bytes)))
        }
    }
}
