package com.cir0cuit.devtoys.tools.developer

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

// Pure color maths shared by the color tools. Colors are packed ARGB ints (as android.graphics.Color uses).

internal fun argb(a: Int, r: Int, g: Int, b: Int): Int = (a shl 24) or (r shl 16) or (g shl 8) or b
internal fun alphaOf(c: Int) = (c ushr 24) and 0xFF
internal fun redOf(c: Int) = (c shr 16) and 0xFF
internal fun greenOf(c: Int) = (c shr 8) and 0xFF
internal fun blueOf(c: Int) = c and 0xFF

/** #RRGGBB, or #RRGGBBAA when the color is not opaque. */
internal fun formatHex(c: Int): String =
    if (alphaOf(c) == 255) "#%06X".format(c and 0xFFFFFF) else "#%06X%02X".format(c and 0xFFFFFF, alphaOf(c))

/** 148 CSS named colors, name to RGB. */
internal val CSS_NAMED_COLORS: Map<String, Int> by lazy {
    ("aliceblue f0f8ff antiquewhite faebd7 aqua 00ffff aquamarine 7fffd4 azure f0ffff beige f5f5dc bisque ffe4c4 " +
        "black 000000 blanchedalmond ffebcd blue 0000ff blueviolet 8a2be2 brown a52a2a burlywood deb887 cadetblue 5f9ea0 " +
        "chartreuse 7fff00 chocolate d2691e coral ff7f50 cornflowerblue 6495ed cornsilk fff8dc crimson dc143c cyan 00ffff " +
        "darkblue 00008b darkcyan 008b8b darkgoldenrod b8860b darkgray a9a9a9 darkgreen 006400 darkgrey a9a9a9 " +
        "darkkhaki bdb76b darkmagenta 8b008b darkolivegreen 556b2f darkorange ff8c00 darkorchid 9932cc darkred 8b0000 " +
        "darksalmon e9967a darkseagreen 8fbc8f darkslateblue 483d8b darkslategray 2f4f4f darkslategrey 2f4f4f " +
        "darkturquoise 00ced1 darkviolet 9400d3 deeppink ff1493 deepskyblue 00bfff dimgray 696969 dimgrey 696969 " +
        "dodgerblue 1e90ff firebrick b22222 floralwhite fffaf0 forestgreen 228b22 fuchsia ff00ff gainsboro dcdcdc " +
        "ghostwhite f8f8ff gold ffd700 goldenrod daa520 gray 808080 green 008000 greenyellow adff2f grey 808080 " +
        "honeydew f0fff0 hotpink ff69b4 indianred cd5c5c indigo 4b0082 ivory fffff0 khaki f0e68c lavender e6e6fa " +
        "lavenderblush fff0f5 lawngreen 7cfc00 lemonchiffon fffacd lightblue add8e6 lightcoral f08080 lightcyan e0ffff " +
        "lightgoldenrodyellow fafad2 lightgray d3d3d3 lightgreen 90ee90 lightgrey d3d3d3 lightpink ffb6c1 " +
        "lightsalmon ffa07a lightseagreen 20b2aa lightskyblue 87cefa lightslategray 778899 lightslategrey 778899 " +
        "lightsteelblue b0c4de lightyellow ffffe0 lime 00ff00 limegreen 32cd32 linen faf0e6 magenta ff00ff maroon 800000 " +
        "mediumaquamarine 66cdaa mediumblue 0000cd mediumorchid ba55d3 mediumpurple 9370db mediumseagreen 3cb371 " +
        "mediumslateblue 7b68ee mediumspringgreen 00fa9a mediumturquoise 48d1cc mediumvioletred c71585 " +
        "midnightblue 191970 mintcream f5fffa mistyrose ffe4e1 moccasin ffe4b5 navajowhite ffdead navy 000080 " +
        "oldlace fdf5e6 olive 808000 olivedrab 6b8e23 orange ffa500 orangered ff4500 orchid da70d6 palegoldenrod eee8aa " +
        "palegreen 98fb98 paleturquoise afeeee palevioletred db7093 papayawhip ffefd5 peachpuff ffdab9 peru cd853f " +
        "pink ffc0cb plum dda0dd powderblue b0e0e6 purple 800080 rebeccapurple 663399 red ff0000 rosybrown bc8f8f " +
        "royalblue 4169e1 saddlebrown 8b4513 salmon fa8072 sandybrown f4a460 seagreen 2e8b57 seashell fff5ee " +
        "sienna a0522d silver c0c0c0 skyblue 87ceeb slateblue 6a5acd slategray 708090 slategrey 708090 snow fffafa " +
        "springgreen 00ff7f steelblue 4682b4 tan d2b48c teal 008080 thistle d8bfd8 tomato ff6347 turquoise 40e0d0 " +
        "violet ee82ee wheat f5deb3 white ffffff whitesmoke f5f5f5 yellow ffff00 yellowgreen 9acd32")
        .split(' ').chunked(2).associate { (name, hex) -> name to hex.toInt(16) }
}

// ---------- Parsing ----------

private val FUNCTION = Regex("""^(rgba?|hsla?|hwb|oklch|oklab)\((.*)\)$""")

/**
 * Parses #RGB, #RGBA, #RRGGBB, #RRGGBBAA (also without #), 0xRRGGBB, 0xAARRGGBB, rgb()/rgba(), hsl()/hsla(),
 * hwb(), oklab(), oklch() and CSS color names. Returns ARGB, or null when the text is not a color.
 */
internal fun parseColor(input: String): Int? {
    val s = input.trim().lowercase()
    if (s.isEmpty()) return null
    if (s == "transparent") return 0
    CSS_NAMED_COLORS[s]?.let { return it or (0xFF shl 24) }
    if (s.startsWith("0x")) {
        val hex = s.substring(2)
        if (!hex.all { it.isHexDigit() }) return null
        return when (hex.length) {
            6 -> hex.toInt(16) or (0xFF shl 24)
            8 -> hex.toLong(16).toInt() // Android order AARRGGBB
            else -> null
        }
    }
    val hex = s.removePrefix("#")
    if (hex.isNotEmpty() && hex.all { it.isHexDigit() }) {
        val full = when (hex.length) {
            3, 4 -> hex.map { "$it$it" }.joinToString("")
            6, 8 -> hex
            else -> return null
        }
        val rgb = full.substring(0, 6).toInt(16)
        val a = if (full.length == 8) full.substring(6).toInt(16) else 255
        return rgb or (a shl 24)
    }
    val m = FUNCTION.matchEntire(s) ?: return null
    val args = m.groupValues[2].split(Regex("""[\s,/]+""")).filter { it.isNotEmpty() }
    if (args.size !in 3..4) return null
    val alpha = if (args.size == 4) (number(args[3], 0.01) ?: return null).coerceIn(0.0, 1.0) else 1.0
    val a = (alpha * 255).roundToInt()
    return when (m.groupValues[1]) {
        "rgb", "rgba" -> {
            val c = args.take(3).map { (number(it, 2.55) ?: return null).roundToInt().coerceIn(0, 255) }
            argb(a, c[0], c[1], c[2])
        }
        "hsl", "hsla" -> {
            val h = hueOf(args[0]) ?: return null
            val sat = (number(args[1], 1.0) ?: return null) / 100
            val l = (number(args[2], 1.0) ?: return null) / 100
            fromUnitRgb(hslToRgb(h, sat.coerceIn(0.0, 1.0), l.coerceIn(0.0, 1.0)), a)
        }
        "hwb" -> {
            val h = hueOf(args[0]) ?: return null
            val w = ((number(args[1], 1.0) ?: return null) / 100).coerceIn(0.0, 1.0)
            val bl = ((number(args[2], 1.0) ?: return null) / 100).coerceIn(0.0, 1.0)
            val rgb = if (w + bl >= 1) DoubleArray(3) { w / (w + bl) }
            else hslToRgb(h, 1.0, 0.5).map { it * (1 - w - bl) + w }.toDoubleArray()
            fromUnitRgb(rgb, a)
        }
        "oklab" -> {
            val l = number(args[0], 0.01) ?: return null
            val oa = number(args[1], 0.004) ?: return null
            val ob = number(args[2], 0.004) ?: return null
            oklchToArgb(l, hypot(oa, ob), Math.toDegrees(atan2(ob, oa)), a)
        }
        else -> { // oklch
            val l = number(args[0], 0.01) ?: return null
            val c = number(args[1], 0.004) ?: return null
            val h = hueOf(args[2]) ?: return null
            oklchToArgb(l, c, h, a)
        }
    }
}

/** A CSS number; "50%" is multiplied by [percentScale]. "none" is 0. */
private fun number(token: String, percentScale: Double): Double? = when {
    token == "none" -> 0.0
    token.endsWith("%") -> token.dropLast(1).toDoubleOrNull()?.times(percentScale)
    else -> token.toDoubleOrNull()
}

private fun hueOf(token: String): Double? = when {
    token == "none" -> 0.0
    token.endsWith("deg") -> token.dropLast(3).toDoubleOrNull()
    token.endsWith("turn") -> token.dropLast(4).toDoubleOrNull()?.times(360)
    token.endsWith("rad") -> token.dropLast(3).toDoubleOrNull()?.let { Math.toDegrees(it) }
    else -> token.toDoubleOrNull()
}

private fun Char.isHexDigit() = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

// ---------- Conversions ----------

private fun unitRgb(c: Int) = doubleArrayOf(redOf(c) / 255.0, greenOf(c) / 255.0, blueOf(c) / 255.0)

private fun fromUnitRgb(rgb: DoubleArray, alpha: Int = 255): Int {
    fun ch(v: Double) = (v.coerceIn(0.0, 1.0) * 255).roundToInt()
    return argb(alpha, ch(rgb[0]), ch(rgb[1]), ch(rgb[2]))
}

private fun toLinear(v: Double) = if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
private fun toGamma(v: Double) = if (v <= 0.0031308) v * 12.92 else 1.055 * v.pow(1 / 2.4) - 0.055

private fun linearRgb(c: Int) = unitRgb(c).map(::toLinear).toDoubleArray()

/** Hue in degrees for RGB components in 0..1. */
private fun hue(rgb: DoubleArray): Double {
    val (r, g, b) = rgb.toList()
    val max = maxOf(r, g, b)
    val d = max - minOf(r, g, b)
    if (d == 0.0) return 0.0
    val h = when (max) {
        r -> 60 * (((g - b) / d) % 6)
        g -> 60 * ((b - r) / d + 2)
        else -> 60 * ((r - g) / d + 4)
    }
    return (h + 360) % 360
}

internal fun hslToRgb(h: Double, s: Double, l: Double): DoubleArray {
    val c = (1 - abs(2 * l - 1)) * s
    val hp = ((h % 360 + 360) % 360) / 60
    val x = c * (1 - abs(hp % 2 - 1))
    val (r, g, b) = when (hp.toInt()) {
        0 -> Triple(c, x, 0.0); 1 -> Triple(x, c, 0.0); 2 -> Triple(0.0, c, x)
        3 -> Triple(0.0, x, c); 4 -> Triple(x, 0.0, c); else -> Triple(c, 0.0, x)
    }
    val m = l - c / 2
    return doubleArrayOf(r + m, g + m, b + m)
}

/** OKLab (L, a, b) of the color, ignoring alpha. */
internal fun toOklab(c: Int): DoubleArray {
    val (r, g, b) = linearRgb(c).toList()
    val l = cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
    val m = cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
    val s = cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
    return doubleArrayOf(
        0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
        1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
        0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
    )
}

/** OKLCH (L, C, h in degrees). */
internal fun toOklch(c: Int): DoubleArray {
    val (l, a, b) = toOklab(c).toList()
    val c2 = hypot(a, b)
    return doubleArrayOf(l, c2, if (c2 < 1e-4) 0.0 else (Math.toDegrees(atan2(b, a)) + 360) % 360)
}

private fun oklchToLinear(l: Double, c: Double, h: Double): DoubleArray {
    val a = c * cos(Math.toRadians(h))
    val b = c * sin(Math.toRadians(h))
    val l1 = (l + 0.3963377774 * a + 0.2158037573 * b).pow(3)
    val m1 = (l - 0.1055613458 * a - 0.0638541728 * b).pow(3)
    val s1 = (l - 0.0894841775 * a - 1.2914855480 * b).pow(3)
    return doubleArrayOf(
        4.0767416621 * l1 - 3.3077115913 * m1 + 0.2309699292 * s1,
        -1.2684380046 * l1 + 2.6097574011 * m1 - 0.3413193965 * s1,
        -0.0041960863 * l1 - 0.7034186147 * m1 + 1.7076147010 * s1
    )
}

/** OKLCH to sRGB; out-of-gamut colors keep lightness and hue and lose chroma until they fit. */
internal fun oklchToArgb(l: Double, c: Double, h: Double, alpha: Int = 255): Int {
    val lc = l.coerceIn(0.0, 1.0)
    fun fits(rgb: DoubleArray) = rgb.all { it in -1e-4..1.0001 }
    var rgb = oklchToLinear(lc, c, h)
    if (!fits(rgb)) {
        var lo = 0.0
        var hi = c
        repeat(20) {
            val mid = (lo + hi) / 2
            if (fits(oklchToLinear(lc, mid, h))) lo = mid else hi = mid
        }
        rgb = oklchToLinear(lc, lo, h)
    }
    return fromUnitRgb(rgb.map { toGamma(it.coerceIn(0.0, 1.0)) }.toDoubleArray(), alpha)
}

/** CIE Lab with the D50 white point, as CSS lab() uses. */
internal fun toLab(c: Int): DoubleArray {
    val (r, g, b) = linearRgb(c).toList()
    // linear sRGB -> XYZ D65 -> Bradford -> XYZ D50 (CSS Color 4 matrices)
    val x65 = 0.41239079926595934 * r + 0.357584339383878 * g + 0.1804807884018343 * b
    val y65 = 0.21263900587151027 * r + 0.715168678767756 * g + 0.07219231536073371 * b
    val z65 = 0.01933081871559182 * r + 0.11919477979462598 * g + 0.9505321522496607 * b
    val x = 1.0479298208405488 * x65 + 0.022946793341019088 * y65 - 0.05019222954313557 * z65
    val y = 0.029627815688159344 * x65 + 0.990434484573249 * y65 - 0.01707382502938514 * z65
    val z = -0.009243058152591178 * x65 + 0.015055144896577895 * y65 + 0.7518742899580008 * z65
    fun f(t: Double) = if (t > 216.0 / 24389) cbrt(t) else (24389.0 / 27 * t + 16) / 116
    val fx = f(x / (0.3457 / 0.3585))
    val fy = f(y)
    val fz = f(z / ((1 - 0.3457 - 0.3585) / 0.3585))
    return doubleArrayOf(116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz))
}

// ---------- Formats ----------

/** Rounds to at most [decimals] places and drops trailing zeros. */
internal fun num(v: Double, decimals: Int): String {
    val s = BigDecimal(v).setScale(decimals, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
    return if (s == "-0") "0" else s
}

private fun pct(v: Double) = "${num(v * 100, 1)}%"

/** The nearest CSS color name by distance in OKLab. */
internal fun nearestNamedColor(c: Int): String {
    val lab = toOklab(c)
    return CSS_NAMED_COLORS.minBy { (_, rgb) ->
        val o = toOklab(rgb)
        (o[0] - lab[0]).pow(2) + (o[1] - lab[1]).pow(2) + (o[2] - lab[2]).pow(2)
    }.key
}

/** Every notation of [c] as (label, value) rows. */
internal fun colorFormats(c: Int): List<Pair<String, String>> {
    val (r, g, b) = Triple(redOf(c), greenOf(c), blueOf(c))
    val a = alphaOf(c)
    val opaque = a == 255
    val alpha = num(a / 255.0, 3)
    val slashAlpha = if (opaque) "" else " / $alpha"
    val unit = unitRgb(c)
    val max = unit.max()
    val min = unit.min()
    val h = num(hue(unit), 1)
    val l = (max + min) / 2
    val sHsl = if (max == min) 0.0 else (max - min) / (1 - abs(2 * l - 1))
    val sHsv = if (max == 0.0) 0.0 else (max - min) / max
    val k = 1 - max
    val cmy = if (k >= 1.0) DoubleArray(3) else DoubleArray(3) { (1 - unit[it] - k) / (1 - k) }
    val (labL, labA, labB) = toLab(c).toList()
    val (okL, okA, okB) = toOklab(c).toList()
    val (_, okC, okH) = toOklch(c).toList()
    val name = nearestNamedColor(c)
    val exactName = CSS_NAMED_COLORS[name] == (c and 0xFFFFFF) && opaque
    val argbHex = "%08X".format(c)
    return listOf(
        "Hex" to formatHex(c),
        "RGB" to if (opaque) "rgb($r, $g, $b)" else "rgba($r, $g, $b, $alpha)",
        "HSL" to if (opaque) "hsl($h, ${pct(sHsl)}, ${pct(l)})" else "hsla($h, ${pct(sHsl)}, ${pct(l)}, $alpha)",
        "HSV" to "hsv($h, ${pct(sHsv)}, ${pct(max)})",
        "HWB" to "hwb($h ${pct(min)} ${pct(1 - max)}$slashAlpha)",
        "CMYK" to "cmyk(${pct(cmy[0])}, ${pct(cmy[1])}, ${pct(cmy[2])}, ${pct(k)})",
        "Lab" to "lab(${num(labL, 2)} ${num(labA, 2)} ${num(labB, 2)}$slashAlpha)",
        "LCH" to "lch(${num(labL, 2)} ${num(hypot(labA, labB), 2)} ${num((Math.toDegrees(atan2(labB, labA)) + 360) % 360, 1)}$slashAlpha)",
        "OKLab" to "oklab(${num(okL, 4)} ${num(okA, 4)} ${num(okB, 4)}$slashAlpha)",
        "OKLCH" to "oklch(${num(okL, 4)} ${num(okC, 4)} ${num(okH, 1)}$slashAlpha)",
        "Android hex" to "#$argbHex",
        "Android int" to c.toString(),
        "Compose" to "Color(0x$argbHex)",
        "SwiftUI" to "Color(red: ${num(unit[0], 3)}, green: ${num(unit[1], 3)}, blue: ${num(unit[2], 3)}, opacity: $alpha)",
        "Name" to if (exactName) name else "≈ $name"
    )
}

// ---------- Monochromatic ----------

private const val MONO_MIN_V = 0.15f
private const val MONO_MAX_V = 0.95f

/**
 * [hsv] plus three colors of the same hue and saturation, lightest first. The new values are spread
 * between [MONO_MIN_V] and [MONO_MAX_V], lighter or darker depending on where the base sits, so none
 * collapses to black or white or repeats the base.
 */
internal fun monochromatic(hsv: FloatArray): List<FloatArray> {
    val v = hsv[2]
    val lighter = ((MONO_MAX_V - v) / (MONO_MAX_V - MONO_MIN_V) * 3).roundToInt().coerceIn(0, 3)
    val darker = 3 - lighter
    val values = List(lighter) { i -> v + (MONO_MAX_V - v) * (i + 1) / lighter } +
        List(darker) { i -> v - (v - MONO_MIN_V) * (i + 1) / darker }
    return (values.map { floatArrayOf(hsv[0], hsv[1], it) } + listOf(hsv)).sortedByDescending { it[2] }
}

// ---------- Tint and shade scale ----------

internal val SCALE_STEPS = listOf(50, 100, 200, 300, 400, 500, 600, 700, 800, 900, 950)

/** Tailwind-style 50..950 scale; the base stays at 500, the others move lightness in OKLCH. */
internal fun tintScale(base: Int): List<Pair<Int, Int>> {
    val (l, c, h) = toOklch(base).toList()
    // How far each step moves from the base toward white (positive) or toward a deep shade (negative).
    val moves = listOf(0.92, 0.82, 0.64, 0.44, 0.22, 0.0, -0.18, -0.38, -0.56, -0.74, -0.86)
    return SCALE_STEPS.zip(moves) { step, f ->
        step to when {
            f == 0.0 -> base or (0xFF shl 24)
            f > 0 -> oklchToArgb(l + (0.975 - l) * f, c * (1 - 0.7 * f), h)
            else -> oklchToArgb(l + (l - 0.2).coerceAtLeast(0.0) * f, c * (1 + 0.3 * f), h)
        }
    }
}

internal enum class ScaleFormat(val label: String) {
    CSS("CSS variables"), TAILWIND("Tailwind config"), ANDROID("Android colors.xml"), COMPOSE("Compose"), JSON("Design tokens (JSON)")
}

internal fun exportScale(scale: List<Pair<Int, Int>>, format: ScaleFormat, name: String = "primary"): String {
    fun hex(c: Int) = "#%06X".format(c and 0xFFFFFF)
    val cap = name.replaceFirstChar { it.uppercase() }
    return when (format) {
        ScaleFormat.CSS -> scale.joinToString("\n", ":root {\n", "\n}") { (s, c) -> "  --$name-$s: ${hex(c)};" }
        ScaleFormat.TAILWIND -> scale.joinToString(",\n", "colors: {\n  $name: {\n", "\n  }\n}") { (s, c) -> "    $s: '${hex(c)}'" }
        ScaleFormat.ANDROID -> scale.joinToString("\n", "<resources>\n", "\n</resources>") { (s, c) -> "    <color name=\"${name}_$s\">${hex(c)}</color>" }
        ScaleFormat.COMPOSE -> scale.joinToString("\n") { (s, c) -> "val $cap$s = Color(0x%08X)".format(c) }
        ScaleFormat.JSON -> scale.joinToString(",\n", "{\n  \"$name\": {\n", "\n  }\n}") { (s, c) ->
            "    \"$s\": { \"\$type\": \"color\", \"\$value\": \"${hex(c)}\" }"
        }
    }
}

// ---------- Contrast ----------

/** [fg] drawn over [bg] (both ARGB); the result is opaque. A translucent [bg] is first drawn over white. */
internal fun compositeOver(fg: Int, bg: Int): Int {
    val base = if (alphaOf(bg) == 255) bg else compositeOver(bg, 0xFFFFFFFF.toInt())
    val a = alphaOf(fg) / 255.0
    fun mix(f: Int, b: Int) = (f * a + b * (1 - a)).roundToInt()
    return argb(255, mix(redOf(fg), redOf(base)), mix(greenOf(fg), greenOf(base)), mix(blueOf(fg), blueOf(base)))
}

/** WCAG 2.x relative luminance of an opaque color. */
internal fun relativeLuminance(c: Int): Double {
    val (r, g, b) = linearRgb(c).toList()
    return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

/** WCAG 2.x contrast ratio (1..21) of text color [fg] on background [bg]. */
internal fun contrastRatio(fg: Int, bg: Int): Double {
    val back = compositeOver(bg, 0xFFFFFFFF.toInt())
    val l1 = relativeLuminance(compositeOver(fg, back))
    val l2 = relativeLuminance(back)
    return (maxOf(l1, l2) + 0.05) / (minOf(l1, l2) + 0.05)
}

/**
 * The text color closest to [fg] in OKLCH lightness (same hue and chroma where the gamut allows) that reaches
 * [target] on [bg], or null when neither black nor white side reaches it.
 */
internal fun suggestForeground(fg: Int, bg: Int, target: Double): Int? {
    if (contrastRatio(fg, bg) >= target) return fg
    val (l, c, h) = toOklch(fg).toList()
    val alpha = alphaOf(fg)
    val goal = target + 0.02 // margin so rounding to 8-bit channels does not drop below the target
    fun at(lightness: Double) = oklchToArgb(lightness, c, h, alpha)
    return listOf(0.0, 1.0).mapNotNull { end ->
        if (contrastRatio(at(end), bg) < goal) return@mapNotNull null
        var fail = l
        var pass = end
        repeat(30) {
            val mid = (fail + pass) / 2
            if (contrastRatio(at(mid), bg) >= goal) pass = mid else fail = mid
        }
        pass
    }.minByOrNull { abs(it - l) }?.let(::at)
}

/** Above this ratio text starts to glare (halation), which tires readers with astigmatism or dyslexia. */
internal const val COMFORT_MAX_CONTRAST = 15.0

/** The text color closest to [fg] in OKLCH lightness, moved toward [bg], whose contrast is at most [max]. */
internal fun softenForeground(fg: Int, bg: Int, max: Double): Int {
    if (contrastRatio(fg, bg) <= max) return fg
    val (l, c, h) = toOklch(fg).toList()
    val alpha = alphaOf(fg)
    val goal = max - 0.02 // margin for 8-bit rounding
    fun at(lightness: Double) = oklchToArgb(lightness, c, h, alpha)
    var tooHigh = l
    var ok = toOklch(compositeOver(bg, 0xFFFFFFFF.toInt()))[0]
    repeat(30) {
        val mid = (tooHigh + ok) / 2
        if (contrastRatio(at(mid), bg) <= goal) ok = mid else tooHigh = mid
    }
    return at(ok)
}

internal enum class ColorVision(val label: String, private val m: DoubleArray) {
    PROTANOPIA("Protanopia", doubleArrayOf(0.152286, 1.052583, -0.204868, 0.114503, 0.786281, 0.099216, -0.003882, -0.048116, 1.051998)),
    DEUTERANOPIA("Deuteranopia", doubleArrayOf(0.367322, 0.860646, -0.227968, 0.280085, 0.672501, 0.047413, -0.011820, 0.042940, 0.968881)),
    TRITANOPIA("Tritanopia", doubleArrayOf(1.255528, -0.076749, -0.178779, -0.078411, 0.930809, 0.147602, 0.004733, 0.691367, 0.303900));

    /** Machado et al. (2009) simulation at full severity, applied in linear sRGB. */
    fun simulate(c: Int): Int {
        val (r, g, b) = linearRgb(c).toList()
        val out = DoubleArray(3) { i -> toGamma((m[3 * i] * r + m[3 * i + 1] * g + m[3 * i + 2] * b).coerceIn(0.0, 1.0)) }
        return fromUnitRgb(out, alphaOf(c))
    }
}
