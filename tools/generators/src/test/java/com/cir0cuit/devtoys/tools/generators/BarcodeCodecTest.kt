package com.cir0cuit.devtoys.tools.generators

import com.google.zxing.BarcodeFormat
import com.google.zxing.common.BitMatrix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BarcodeCodecTest {

    private val black = 0xFF000000.toInt()
    private val white = 0xFFFFFFFF.toInt()

    private class Image(val px: IntArray, val w: Int, val h: Int)

    private fun image(m: BitMatrix, kind: BarcodeKind, fg: Int = black, bg: Int = white): Image {
        val l = barcodeLayout(m, kind.quiet, withText = false)
        return Image(rasterize(m, l, 3, fg, bg), l.cols * 3, l.rows * 3)
    }

    private fun roundTrip(input: String, kind: BarcodeKind, opts: BarcodeOptions = BarcodeOptions()): List<ScanHit> {
        val img = image(encodeBarcode(input, kind, opts), kind)
        return decodeBarcodes(img.px, img.w, img.h)
    }

    @Test
    fun everyFormatRoundTripsItsSample() {
        val expected = mapOf(
            BarcodeKind.EAN_13 to "5901234123457", BarcodeKind.EAN_8 to "96385074", BarcodeKind.UPC_A to "036000291452",
            BarcodeKind.UPC_E to "01234565", BarcodeKind.CODABAR to "40156",
        )
        for (kind in BarcodeKind.entries) {
            val hits = roundTrip(kind.sample, kind)
            assertEquals(kind.name, listOf(ScanHit(kind.format, expected[kind] ?: kind.sample, hits.firstOrNull()?.ecLevel)), hits)
        }
    }

    @Test
    fun unicodeRoundTripsIn2dFormats() {
        for (kind in listOf(BarcodeKind.QR, BarcodeKind.DATA_MATRIX, BarcodeKind.AZTEC, BarcodeKind.PDF_417)) {
            for (s in listOf("Grüße aus Köln", "Zażółć gęślą jaźń", "日本語のテキスト")) {
                assertEquals("$kind $s", s, roundTrip(s, kind).single().text)
            }
        }
        for (kind in listOf(BarcodeKind.QR, BarcodeKind.AZTEC)) assertEquals("ok 😀👍", roundTrip("ok 😀👍", kind).single().text)
        assertTrue(validateBarcodeInput("ok 😀", BarcodeKind.PDF_417)!!.contains("cannot encode emoji"))
    }

    @Test
    fun needsUtf8OnlyBeyondLatin1() {
        assertFalse(needsUtf8("café über"))
        assertTrue(needsUtf8("Łódź"))
    }

    @Test
    fun qrErrorCorrectionIsReported() {
        assertEquals("H", roundTrip("hello", BarcodeKind.QR, BarcodeOptions(ec = EcLevel.H)).single().ecLevel)
    }

    @Test
    fun minimumSizeMatrixHasOneModulePerPixel() {
        val qr = encodeBarcode("hello", BarcodeKind.QR, BarcodeOptions(ec = EcLevel.L))
        assertEquals(21, qr.width) // version 1
        val ean = encodeBarcode("590123412345", BarcodeKind.EAN_13)
        assertEquals(95, ean.width)
        assertEquals(1, ean.height)
    }

    @Test
    fun tooLongForQrExplainsTheFix() {
        val e = runCatching { encodeBarcode("a".repeat(3000), BarcodeKind.QR) }.exceptionOrNull()
        assertTrue(e is IllegalArgumentException)
        assertTrue(e!!.message!!.startsWith("The content is too long for QR code."))
    }

    @Test
    fun dataMatrixRectangleAndGs1() {
        val rect = encodeBarcode("hello", BarcodeKind.DATA_MATRIX, BarcodeOptions(dmShape = DmShape.RECTANGLE))
        assertTrue(rect.width > rect.height)
        assertEquals("0109501101530003", roundTrip("0109501101530003", BarcodeKind.CODE_128, BarcodeOptions(gs1 = true)).single().text)
    }

    @Test
    fun checkDigits() {
        assertEquals('7', gtinCheckDigit("590123412345"))
        assertEquals("5901234123457", withCheckDigit("590123412345", BarcodeKind.EAN_13))
        assertEquals("5901234123457", withCheckDigit("5901234123457", BarcodeKind.EAN_13))
        assertEquals("01234565", withCheckDigit("0123456", BarcodeKind.UPC_E))
        assertEquals("ABC", withCheckDigit("ABC", BarcodeKind.CODE_128))
    }

    @Test
    fun validationExplainsTheProblem() {
        assertNull(validateBarcodeInput("5901234123457", BarcodeKind.EAN_13))
        assertEquals("The check digit should be 7, not 8. Fix it or remove it to have it added.", validateBarcodeInput("5901234123458", BarcodeKind.EAN_13))
        assertEquals("EAN-13 takes 12 digits, or 13 with the check digit. You entered 5.", validateBarcodeInput("12345", BarcodeKind.EAN_13))
        assertTrue(validateBarcodeInput("12a", BarcodeKind.EAN_8)!!.startsWith("EAN-8 takes digits only"))
        assertEquals("UPC-E numbers start with 0 or 1.", validateBarcodeInput("5123456", BarcodeKind.UPC_E))
        assertEquals("ITF needs an even number of digits. Add a leading 0.", validateBarcodeInput("123", BarcodeKind.ITF))
        assertTrue(validateBarcodeInput("lower", BarcodeKind.CODE_39)!!.contains("Remove 'l'"))
        assertTrue(validateBarcodeInput("Łódź", BarcodeKind.CODE_128)!!.contains("ASCII characters only"))
        assertNull(validateBarcodeInput("A40156B", BarcodeKind.CODABAR))
        assertTrue(validateBarcodeInput("40x", BarcodeKind.CODABAR)!!.contains("Remove 'x'"))
    }

    @Test
    fun scannerFindsRotatedInvertedAndMultipleCodes() {
        val ean = image(encodeBarcode("590123412345", BarcodeKind.EAN_13), BarcodeKind.EAN_13)
        val turned = decodeBarcodes(rotate90(ean.px, ean.w, ean.h), ean.h, ean.w)
        assertEquals("5901234123457", turned.single().text)

        val inverted = image(encodeBarcode("inverted", BarcodeKind.QR), BarcodeKind.QR, fg = white, bg = black)
        assertEquals("inverted", decodeBarcodes(inverted.px, inverted.w, inverted.h).single().text)

        val a = image(encodeBarcode("first", BarcodeKind.QR), BarcodeKind.QR)
        val b = image(encodeBarcode("second", BarcodeKind.QR), BarcodeKind.QR)
        val w = a.w + b.w
        val both = IntArray(w * a.h)
        for (y in 0 until a.h) {
            System.arraycopy(a.px, y * a.w, both, y * w, a.w)
            System.arraycopy(b.px, y * b.w, both, y * w + a.w, b.w)
        }
        assertEquals(setOf("first", "second"), decodeBarcodes(both, w, a.h).map { it.text }.toSet())
    }

    @Test
    fun blankImageHasNoCodes() {
        assertTrue(decodeBarcodes(IntArray(100 * 100) { white }, 100, 100).isEmpty())
    }

    @Test
    fun rotate90TurnsClockwise() {
        // 2x1 image [a b] becomes 1x2 [a / b].
        assertEquals(listOf(1, 2), rotate90(intArrayOf(1, 2), 2, 1).toList())
        // 1x2 image [a / b] becomes 2x1 [b a].
        assertEquals(listOf(2, 1), rotate90(intArrayOf(1, 2), 1, 2).toList())
    }

    @Test
    fun layoutAddsQuietZoneAndTextRow() {
        val ean = encodeBarcode("590123412345", BarcodeKind.EAN_13)
        val l = barcodeLayout(ean, 10, withText = true)
        assertEquals(115, l.cols)
        assertEquals(38, l.barRows)
        assertEquals(38 + 8 + 12, l.rows)
        val qr = barcodeLayout(encodeBarcode("hi", BarcodeKind.QR, BarcodeOptions(ec = EcLevel.L)), 4, withText = true)
        assertEquals(29, qr.cols)
        assertEquals(29, qr.rows)
    }

    @Test
    fun exportSizeHitsTheTargetExactly() {
        val qr = exportSize(BarcodeLayout(41, 41, 4, 4, 33, 0), 1024)
        assertEquals(ExportSize(24, 20, 20, 1024, 1024), qr)
        val ean = exportSize(BarcodeLayout(115, 58, 10, 4, 38, 12), 1024)
        assertEquals(ExportSize(8, 52, 52, 1024, 464 + 104), ean)
        val odd = exportSize(BarcodeLayout(29, 29, 4, 4, 21, 0), 1024)
        assertEquals(1024, odd.width)
        assertEquals(1024, odd.height)
        // Wider than the target at 1 px per module: natural size, no margin.
        assertEquals(ExportSize(1, 0, 0, 600, 100), exportSize(BarcodeLayout(600, 100, 2, 2, 96, 0), 512))
    }

    @Test
    fun rasterizeOffsetsByPadding() {
        val m = BitMatrix(2, 2).apply { set(0, 0) }
        val l = barcodeLayout(m, 0, withText = false)
        val px = rasterize(m, l, 2, black, white, padX = 1, padY = 1, w = 7, h = 7)
        assertEquals(49, px.size)
        val dark = px.indices.filter { px[it] == black }.map { it % 7 to it / 7 }
        assertEquals(listOf(1 to 1, 2 to 1, 1 to 2, 2 to 2), dark)
    }

    @Test
    fun svgDrawsRunsAndEscapesText() {
        val m = BitMatrix(4, 1).apply { set(0, 0); set(1, 0); set(3, 0) }
        val svg = barcodeSvg(m, barcodeLayout(m, 2, withText = true), "#000000", null, "a<b")
        assertTrue(svg.startsWith("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 8 36\""))
        assertTrue(svg.contains("d=\"M2,2h2v20h-2zM5,2h1v20h-1z\""))
        assertFalse(svg.contains("<rect"))
        assertTrue(svg.contains(">a&lt;b</text>"))
        assertTrue(barcodeSvg(m, barcodeLayout(m, 2, false), "#000000", "#FFFFFF", null).contains("<rect width=\"100%\" height=\"100%\" fill=\"#FFFFFF\"/>"))
    }

    @Test
    fun formatLabels() {
        assertEquals("QR code", formatLabel(BarcodeFormat.QR_CODE))
        assertEquals("GS1 DataBar", formatLabel(BarcodeFormat.RSS_14))
    }
}
