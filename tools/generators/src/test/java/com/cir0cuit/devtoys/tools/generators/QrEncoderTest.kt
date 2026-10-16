package com.cir0cuit.devtoys.tools.generators

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QrEncoderTest {

    private fun decode(matrix: BitMatrix): String {
        val w = matrix.width
        val h = matrix.height
        val black = 0xFF000000.toInt()
        val white = 0xFFFFFFFF.toInt()
        val pixels = IntArray(w * h) { i -> if (matrix.get(i % w, i / w)) black else white }
        val bitmap = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(w, h, pixels)))
        // ISO/IEC 18004: byte mode without an ECI header is ISO-8859-1; an ECI header overrides it.
        val hints = mapOf(DecodeHintType.PURE_BARCODE to true, DecodeHintType.CHARACTER_SET to "ISO-8859-1")
        return QRCodeReader().decode(bitmap, hints).text
    }

    @Test
    fun latin1InputUsesDefaultEncoding() {
        assertTrue(qrEncodeHints("https://devtoys.app").isEmpty())
        assertTrue(qrEncodeHints("café über").isEmpty())
    }

    @Test
    fun nonLatin1InputUsesUtf8() {
        assertEquals("UTF-8", qrEncodeHints("Łódź")[EncodeHintType.CHARACTER_SET])
        assertEquals("UTF-8", qrEncodeHints("日本語")[EncodeHintType.CHARACTER_SET])
        assertEquals("UTF-8", qrEncodeHints("emoji 😀")[EncodeHintType.CHARACTER_SET])
    }

    @Test
    fun matrixHasRequestedSize() {
        val m = encodeQrMatrix("hello", 512)
        assertEquals(512, m.width)
        assertEquals(512, m.height)
    }

    @Test
    fun asciiRoundTrip() {
        assertEquals("https://devtoys.app", decode(encodeQrMatrix("https://devtoys.app")))
    }

    @Test
    fun latin1RoundTrip() {
        val s = "Grüße aus Köln éè"
        assertEquals(s, decode(encodeQrMatrix(s)))
    }

    @Test
    fun polishRoundTrip() {
        val s = "Zażółć gęślą jaźń"
        assertEquals(s, decode(encodeQrMatrix(s)))
    }

    @Test
    fun cjkRoundTrip() {
        val s = "日本語のテキスト"
        assertEquals(s, decode(encodeQrMatrix(s)))
    }

    @Test
    fun emojiRoundTrip() {
        val s = "ok 😀👍"
        assertEquals(s, decode(encodeQrMatrix(s)))
    }

    @Test(expected = WriterException::class)
    fun inputBeyondVersion40CapacityThrowsWriterException() {
        // Version 40-L byte mode holds at most 2953 bytes (ISO/IEC 18004, Table 7).
        encodeQrMatrix("a".repeat(3000))
    }

    @Test
    fun inputAtVersion40ByteCapacityRoundTrips() {
        val s = "a".repeat(2953)
        assertEquals(s, decode(encodeQrMatrix(s, 1024)))
    }
}
