package com.cir0cuit.devtoys.tools.generators

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.MultiFormatWriter
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.Result
import com.google.zxing.ResultMetadataType
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.datamatrix.encoder.SymbolShapeHint
import com.google.zxing.multi.GenericMultipleBarcodeReader
import com.google.zxing.oned.UPCEReader
import java.nio.charset.StandardCharsets
import java.util.EnumMap

/** Barcode symbologies the generator can write; [quiet] is the recommended quiet zone in modules. */
internal enum class BarcodeKind(val format: BarcodeFormat, val label: String, val is2D: Boolean, val quiet: Int, val sample: String) {
    QR(BarcodeFormat.QR_CODE, "QR code", true, 4, "https://github.com/Cir0cuit/devtoys-android"),
    DATA_MATRIX(BarcodeFormat.DATA_MATRIX, "Data Matrix", true, 2, "DevToys"),
    AZTEC(BarcodeFormat.AZTEC, "Aztec", true, 2, "DevToys"),
    PDF_417(BarcodeFormat.PDF_417, "PDF417", true, 2, "DevToys"),
    CODE_128(BarcodeFormat.CODE_128, "Code 128", false, 10, "DEVTOYS-128"),
    CODE_39(BarcodeFormat.CODE_39, "Code 39", false, 10, "DEVTOYS 39"),
    CODE_93(BarcodeFormat.CODE_93, "Code 93", false, 10, "DEVTOYS93"),
    EAN_13(BarcodeFormat.EAN_13, "EAN-13", false, 10, "590123412345"),
    EAN_8(BarcodeFormat.EAN_8, "EAN-8", false, 10, "9638507"),
    UPC_A(BarcodeFormat.UPC_A, "UPC-A", false, 10, "03600029145"),
    UPC_E(BarcodeFormat.UPC_E, "UPC-E", false, 10, "0123456"),
    ITF(BarcodeFormat.ITF, "ITF", false, 10, "12345678"),
    CODABAR(BarcodeFormat.CODABAR, "Codabar", false, 10, "A40156B");

    val hasErrorCorrection get() = this == QR || this == AZTEC || this == PDF_417
    val supportsGs1 get() = this == CODE_128 || this == DATA_MATRIX
}

/** Error correction, mapped onto each symbology's own scale. */
internal enum class EcLevel(val qrPercent: Int, val aztecPercent: Int, val pdfLevel: Int) {
    L(7, 10, 1), M(15, 23, 2), Q(25, 36, 4), H(30, 50, 6);

    fun describe(kind: BarcodeKind): String = when (kind) {
        BarcodeKind.QR -> "Recovers about $qrPercent% of damaged modules."
        BarcodeKind.AZTEC -> "$aztecPercent% of the symbol is error correction."
        BarcodeKind.PDF_417 -> "Error correction level $pdfLevel of 8."
        else -> ""
    }
}

internal enum class DmShape(val label: String, val hint: SymbolShapeHint) {
    AUTO("Auto", SymbolShapeHint.FORCE_NONE), SQUARE("Square", SymbolShapeHint.FORCE_SQUARE), RECTANGLE("Rectangle", SymbolShapeHint.FORCE_RECTANGLE)
}

internal data class BarcodeOptions(
    val ec: EcLevel = EcLevel.M,
    val dmShape: DmShape = DmShape.AUTO,
    val pdfCompact: Boolean = false,
    val gs1: Boolean = false,
)

private const val CODE39_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ -.$/+%"
private const val CODABAR_BODY = "0123456789-$:/.+"
private const val CODABAR_GUARDS = "ABCDTN*E"

internal fun needsUtf8(input: String) = !StandardCharsets.ISO_8859_1.newEncoder().canEncode(input)

/** GS1 mod-10 check digit for the digits in [data] (EAN, UPC, ITF-14). */
internal fun gtinCheckDigit(data: String): Char {
    var sum = 0
    data.reversed().forEachIndexed { i, c -> sum += (c - '0') * if (i % 2 == 0) 3 else 1 }
    return '0' + (10 - sum % 10) % 10
}

/** Digits without the check digit for EAN/UPC kinds, null for the rest. */
private fun gtinDataLength(kind: BarcodeKind) = when (kind) {
    BarcodeKind.EAN_13 -> 12
    BarcodeKind.EAN_8, BarcodeKind.UPC_E -> 7
    BarcodeKind.UPC_A -> 11
    else -> null
}

private fun checkDigitFor(kind: BarcodeKind, data: String): Char =
    if (kind == BarcodeKind.UPC_E) gtinCheckDigit(UPCEReader.convertUPCEtoUPCA(data + "0").dropLast(1)) else gtinCheckDigit(data)

/** The number printed under an EAN/UPC code: [input] with its check digit appended when missing. */
internal fun withCheckDigit(input: String, kind: BarcodeKind): String {
    val len = gtinDataLength(kind) ?: return input
    return if (input.length == len && input.all { it in '0'..'9' }) input + checkDigitFor(kind, input) else input
}

/** Explains why [kind] cannot encode [input] and how to fix it, or null when it can. */
internal fun validateBarcodeInput(input: String, kind: BarcodeKind): String? {
    if (input.isEmpty()) return null
    val digitsOnly = input.all { it in '0'..'9' }
    gtinDataLength(kind)?.let { len ->
        if (!digitsOnly) return "${kind.label} takes digits only, for example ${kind.sample}."
        if (kind == BarcodeKind.UPC_E && input[0] != '0' && input[0] != '1') return "UPC-E numbers start with 0 or 1."
        if (input.length != len && input.length != len + 1) {
            return "${kind.label} takes $len digits, or ${len + 1} with the check digit. You entered ${input.length}."
        }
        if (input.length == len + 1) {
            val expected = checkDigitFor(kind, input.dropLast(1))
            if (input.last() != expected) return "The check digit should be $expected, not ${input.last()}. Fix it or remove it to have it added."
        }
        return null
    }
    val bad: (String) -> Char? = { allowed -> input.firstOrNull { it !in allowed } }
    return when (kind) {
        BarcodeKind.ITF -> when {
            !digitsOnly -> "ITF takes digits only."
            input.length % 2 != 0 -> "ITF needs an even number of digits. Add a leading 0."
            input.length > 80 -> "ITF holds at most 80 digits."
            else -> null
        }
        BarcodeKind.CODE_39 -> bad(CODE39_CHARS)?.let { "Code 39 supports 0-9, A-Z, space and - . $ / + %. Remove '$it' or use Code 128." }
        BarcodeKind.CODE_93, BarcodeKind.CODE_128 -> input.firstOrNull { it.code > 127 }
            ?.let { "${kind.label} supports ASCII characters only. Remove '$it' or use a QR code." }
        BarcodeKind.CODABAR -> {
            val body = if (input.length >= 2 && input.first().uppercaseChar() in CODABAR_GUARDS && input.last().uppercaseChar() in CODABAR_GUARDS) input.substring(1, input.length - 1) else input
            body.firstOrNull { it !in CODABAR_BODY }
                ?.let { "Codabar supports 0-9 and - $ : / . +, with optional A-D start and stop characters. Remove '$it'." }
        }
        BarcodeKind.DATA_MATRIX, BarcodeKind.PDF_417 ->
            if (input.any { it.isSurrogate() }) "${kind.label} cannot encode emoji. Remove them or use a QR or Aztec code." else null
        else -> null
    }
}

/**
 * Encodes [input] at its minimum size with no quiet zone: one pixel per module, and a single
 * row for 1D codes. Throws [IllegalArgumentException] with a readable message when it cannot.
 */
internal fun encodeBarcode(input: String, kind: BarcodeKind, opts: BarcodeOptions = BarcodeOptions()): BitMatrix {
    require(input.isNotEmpty()) { "Enter the content to encode." }
    validateBarcodeInput(input, kind)?.let { throw IllegalArgumentException(it) }
    val utf8 = needsUtf8(input)
    val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
    hints[EncodeHintType.MARGIN] = 0
    if (kind.is2D && utf8) hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
    when (kind) {
        BarcodeKind.QR -> hints[EncodeHintType.ERROR_CORRECTION] = opts.ec.name
        BarcodeKind.AZTEC -> hints[EncodeHintType.ERROR_CORRECTION] = opts.ec.aztecPercent
        BarcodeKind.PDF_417 -> {
            hints[EncodeHintType.ERROR_CORRECTION] = opts.ec.pdfLevel
            hints[EncodeHintType.PDF417_COMPACT] = opts.pdfCompact
        }
        BarcodeKind.DATA_MATRIX -> {
            hints[EncodeHintType.DATA_MATRIX_SHAPE] = opts.dmShape.hint
            // Only the minimal encoder writes ECI segments; the default one is ISO-8859-1 only.
            if (utf8) hints[EncodeHintType.DATA_MATRIX_COMPACT] = true
            if (opts.gs1) hints[EncodeHintType.GS1_FORMAT] = true
        }
        BarcodeKind.CODE_128 -> if (opts.gs1) hints[EncodeHintType.GS1_FORMAT] = true
        else -> Unit
    }
    try {
        return MultiFormatWriter().encode(input, kind.format, 0, 0, hints)
    } catch (e: Exception) {
        // WriterException for QR, Aztec and PDF417; IllegalArgument/IllegalState from the other writers.
        if (e !is WriterException && e !is IllegalArgumentException && e !is IllegalStateException) throw e
        val msg = e.message.orEmpty()
        if (SIZE_ERRORS.any { msg.contains(it, ignoreCase = true) }) {
            val fix = if (kind.hasErrorCorrection) " Shorten it or lower the error correction." else " Shorten it."
            throw IllegalArgumentException("The content is too long for ${kind.label}.$fix", e)
        }
        throw IllegalArgumentException("${kind.label} cannot encode this: $msg.", e)
    }
}

private val SIZE_ERRORS = listOf("too big", "too large", "too many", "fit", "symbol arrangement")

/** Geometry in modules: quiet zones, bar height for 1D codes and room for the text line. */
internal data class BarcodeLayout(val cols: Int, val rows: Int, val qx: Int, val qy: Int, val barRows: Int, val textRows: Int)

internal fun barcodeLayout(m: BitMatrix, quiet: Int, withText: Boolean): BarcodeLayout {
    val oneD = m.height == 1
    val bar = if (oneD) maxOf(m.width * 2 / 5, 20) else m.height
    val qy = if (oneD) minOf(quiet, 4) else quiet
    val text = if (oneD && withText) 12 else 0
    return BarcodeLayout(m.width + 2 * quiet, bar + 2 * qy + text, quiet, qy, bar, text)
}

/** ARGB pixels of [m] drawn with [scale] pixels per module. */
internal fun rasterize(m: BitMatrix, l: BarcodeLayout, scale: Int, fg: Int, bg: Int): IntArray {
    val w = l.cols * scale
    val px = IntArray(w * l.rows * scale) { bg }
    for (y in 0 until l.barRows) {
        val my = if (m.height == 1) 0 else y
        for (x in 0 until m.width) {
            if (!m.get(x, my)) continue
            val top = (y + l.qy) * scale
            val left = (x + l.qx) * scale
            for (dy in 0 until scale) px.fill(fg, (top + dy) * w + left, (top + dy) * w + left + scale)
        }
    }
    return px
}

/** Scalable SVG of [m]; dark modules are merged into one path of horizontal runs. */
internal fun barcodeSvg(m: BitMatrix, l: BarcodeLayout, fg: String, bg: String?, text: String?): String = buildString {
    append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 ${l.cols} ${l.rows}\" ")
    append("width=\"${l.cols * 8}\" height=\"${l.rows * 8}\" shape-rendering=\"crispEdges\">")
    if (bg != null) append("<rect width=\"100%\" height=\"100%\" fill=\"$bg\"/>")
    append("<path fill=\"$fg\" d=\"")
    val runHeight = if (m.height == 1) l.barRows else 1
    for (y in 0 until m.height) {
        var x = 0
        while (x < m.width) {
            if (!m.get(x, y)) { x++; continue }
            val start = x
            while (x < m.width && m.get(x, y)) x++
            append("M${start + l.qx},${y + l.qy}h${x - start}v${runHeight}h${start - x}z")
        }
    }
    append("\"/>")
    if (text != null && l.textRows > 0) {
        val escaped = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        val fit = if (text.length * 9 * 0.6 > m.width) " textLength=\"${m.width}\" lengthAdjust=\"spacingAndGlyphs\"" else ""
        append("<text x=\"${l.cols / 2.0}\" y=\"${l.qy + l.barRows + 10}\" font-family=\"monospace\" font-size=\"9\" text-anchor=\"middle\" fill=\"$fg\"$fit>$escaped</text>")
    }
    append("</svg>")
}

/** One decoded code; plain strings so it can be saved across configuration changes. */
internal data class ScanHit(val format: BarcodeFormat, val text: String, val ecLevel: String?)

internal fun rotate90(px: IntArray, w: Int, h: Int): IntArray {
    val out = IntArray(px.size)
    for (y in 0 until h) for (x in 0 until w) out[x * h + (h - 1 - y)] = px[y * w + x]
    return out
}

/**
 * Finds every barcode in an ARGB image. Tries both binarizers, then the image turned 90 degrees
 * so vertical 1D codes are found too. Inverted (light on dark) codes are included.
 */
internal fun decodeBarcodes(pixels: IntArray, width: Int, height: Int): List<ScanHit> {
    val hints = mapOf<DecodeHintType, Any>(DecodeHintType.TRY_HARDER to true, DecodeHintType.ALSO_INVERTED to true)
    var px = pixels
    var w = width
    var h = height
    repeat(2) {
        val source = RGBLuminanceSource(w, h, px)
        for (binarizer in listOf(HybridBinarizer(source), GlobalHistogramBinarizer(source))) {
            val found: Array<Result> = try {
                GenericMultipleBarcodeReader(MultiFormatReader()).decodeMultiple(BinaryBitmap(binarizer), hints)
            } catch (e: ReaderException) {
                emptyArray()
            }
            if (found.isNotEmpty()) {
                return found.distinctBy { it.barcodeFormat to it.text }.map {
                    ScanHit(it.barcodeFormat, it.text, it.resultMetadata?.get(ResultMetadataType.ERROR_CORRECTION_LEVEL)?.toString())
                }
            }
        }
        px = rotate90(px, w, h)
        w = h.also { h = w }
    }
    return emptyList()
}

internal fun formatLabel(format: BarcodeFormat): String =
    BarcodeKind.entries.firstOrNull { it.format == format }?.label ?: when (format) {
        BarcodeFormat.MAXICODE -> "MaxiCode"
        BarcodeFormat.RSS_14 -> "GS1 DataBar"
        BarcodeFormat.RSS_EXPANDED -> "GS1 DataBar Expanded"
        BarcodeFormat.UPC_EAN_EXTENSION -> "UPC/EAN extension"
        else -> format.name
    }
